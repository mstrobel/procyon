/*
 * PushNegationTransform.java
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

import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.BinaryOperatorExpression;
import com.strobel.decompiler.languages.java.ast.BinaryOperatorType;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.Expression;
import com.strobel.decompiler.languages.java.ast.UnaryOperatorExpression;
import com.strobel.decompiler.languages.java.ast.UnaryOperatorType;
import com.strobel.functions.Function;

public class PushNegationTransform extends ContextTrackingVisitor<AstNode> implements IAstTransform {
    private final static Function<AstNode, AstNode> NEGATE_FUNCTION = new Function<AstNode, AstNode>() {
        @Override
        public AstNode apply(final AstNode n) {
            if (n instanceof UnaryOperatorExpression) {
                final UnaryOperatorExpression unary = (UnaryOperatorExpression) n;

                if (unary.getOperator() == UnaryOperatorType.NOT) {
                    final Expression operand = unary.getExpression();
                    operand.remove();
                    return operand;
                }
            }
            return new UnaryOperatorExpression(UnaryOperatorType.NOT, (Expression) n);
        }
    };

    public PushNegationTransform(final DecompilerContext context) {
        super(context);
    }

    @Override
    public AstNode visitUnaryOperatorExpression(final UnaryOperatorExpression node, final Void _) {
        if (node.getOperator() == UnaryOperatorType.NOT &&
            node.getExpression() instanceof BinaryOperatorExpression) {

            final BinaryOperatorExpression binary = (BinaryOperatorExpression) node.getExpression();

            boolean successful = true;

            switch (binary.getOperator()) {
                case EQUALITY:
                    binary.setOperator(BinaryOperatorType.INEQUALITY);
                    break;

                case INEQUALITY:
                    binary.setOperator(BinaryOperatorType.EQUALITY);
                    break;

                case GREATER_THAN:
                    binary.setOperator(BinaryOperatorType.LESS_THAN_OR_EQUAL);
                    break;

                case GREATER_THAN_OR_EQUAL:
                    binary.setOperator(BinaryOperatorType.LESS_THAN);
                    break;

                case LESS_THAN:
                    binary.setOperator(BinaryOperatorType.GREATER_THAN_OR_EQUAL);
                    break;

                case LESS_THAN_OR_EQUAL:
                    binary.setOperator(BinaryOperatorType.GREATER_THAN);
                    break;

                default:
                    successful = false;
                    break;
            }

            if (successful) {
                node.replaceWith(binary);
                return binary.acceptVisitor(this, _);
            }

            successful = true;

            switch (binary.getOperator()) {
                case LOGICAL_AND:
                    binary.setOperator(BinaryOperatorType.LOGICAL_OR);
                    break;

                case LOGICAL_OR:
                    binary.setOperator(BinaryOperatorType.LOGICAL_AND);
                    break;

                default:
                    successful = false;
                    break;
            }

            if (successful) {
                binary.getLeft().replaceWith(NEGATE_FUNCTION);
                binary.getRight().replaceWith(NEGATE_FUNCTION);
                node.replaceWith(binary);
                return binary.acceptVisitor(this, _);
            }
        }

        return super.visitUnaryOperatorExpression(node, _);
    }
}
