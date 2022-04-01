/*
 * InvertIfStatementsTransform.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.LineNumberTableAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.LineNumberTableConverter;
import com.strobel.decompiler.languages.java.LineNumberVisitor;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.BlockStatement;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.Expression;
import com.strobel.decompiler.languages.java.ast.IfElseStatement;
import com.strobel.decompiler.languages.java.ast.Statement;
import com.strobel.decompiler.languages.java.ast.UnaryOperatorExpression;
import com.strobel.decompiler.languages.java.ast.UnaryOperatorType;

public class InvertIfStatementsTransform extends ContextTrackingVisitor<Void> {
    public InvertIfStatementsTransform(final DecompilerContext context) {
        super(context);
    }

    @Override
    public Void visitIfElseStatement(final IfElseStatement node, final Void data) {
        super.visitIfElseStatement(node, data);

        final Statement trueStatement = node.getTrueStatement();
        final Statement falseStatement = node.getFalseStatement();

        final LineNumberTableAttribute lineNumberTableAttribute = SourceAttribute.find(AttributeNames.LineNumberTable, context.getCurrentMethod().getSourceAttributes());
        if (lineNumberTableAttribute == null) {
            return null;
        }
        final LineNumberTableConverter lineNumberTableConverter = new LineNumberTableConverter(lineNumberTableAttribute);
        final LineNumberVisitor trueLineNumberVisitor = new LineNumberVisitor(lineNumberTableConverter);
        final LineNumberVisitor nextLineNumbervisitor = new LineNumberVisitor(lineNumberTableConverter);
        final AstNode nextSibling = node.getNextSibling();
        if ((falseStatement == null || falseStatement.isNull()) && nextSibling != null) {
            trueStatement.acceptVisitor(trueLineNumberVisitor, null);
            nextSibling.acceptVisitor(nextLineNumbervisitor, null);
            if (nextLineNumbervisitor.getMaxLineNumber() != Integer.MIN_VALUE && trueLineNumberVisitor.getMinLineNumber() != Integer.MAX_VALUE
                    && nextLineNumbervisitor.getMaxLineNumber() < trueLineNumberVisitor.getMinLineNumber()) {
                final Expression condition = node.getCondition();
                condition.remove();
                if (condition instanceof UnaryOperatorExpression && ((UnaryOperatorExpression) condition).getOperator() == UnaryOperatorType.NOT) {
                    Expression expression = ((UnaryOperatorExpression) condition).getExpression();
                    expression.remove();
                    node.setCondition(expression);
                } else {
                    node.setCondition(new UnaryOperatorExpression(UnaryOperatorType.NOT, condition));
                }
                trueStatement.remove();
                node.setFalseStatement(trueStatement);
                BlockStatement blockStatement = new BlockStatement();
                for (Statement next = node.getNextStatement(); next != null; next = node.getNextStatement()) {
                    next.remove();
                    blockStatement.add(next);
                }
                node.setTrueStatement(blockStatement);
            }
        }

        return null;
    }
}
