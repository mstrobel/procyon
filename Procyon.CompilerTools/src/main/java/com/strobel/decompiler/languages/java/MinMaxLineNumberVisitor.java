/*
 * MinMaxLineNumberVisitor.java
 *
 * Copyright (c) 2013-2022 Mike Strobel and other contributors
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

package com.strobel.decompiler.languages.java;

import com.strobel.decompiler.languages.java.ast.Annotation;
import com.strobel.decompiler.languages.java.ast.AnonymousObjectCreationExpression;
import com.strobel.decompiler.languages.java.ast.ArrayCreationExpression;
import com.strobel.decompiler.languages.java.ast.ArrayInitializerExpression;
import com.strobel.decompiler.languages.java.ast.AssertStatement;
import com.strobel.decompiler.languages.java.ast.AssignmentExpression;
import com.strobel.decompiler.languages.java.ast.BinaryOperatorExpression;
import com.strobel.decompiler.languages.java.ast.BlockStatement;
import com.strobel.decompiler.languages.java.ast.BreakStatement;
import com.strobel.decompiler.languages.java.ast.BytecodeConstant;
import com.strobel.decompiler.languages.java.ast.CastExpression;
import com.strobel.decompiler.languages.java.ast.ClassOfExpression;
import com.strobel.decompiler.languages.java.ast.ConditionalExpression;
import com.strobel.decompiler.languages.java.ast.ContinueStatement;
import com.strobel.decompiler.languages.java.ast.DepthFirstAstVisitor;
import com.strobel.decompiler.languages.java.ast.DoWhileStatement;
import com.strobel.decompiler.languages.java.ast.EmptyStatement;
import com.strobel.decompiler.languages.java.ast.Expression;
import com.strobel.decompiler.languages.java.ast.ExpressionStatement;
import com.strobel.decompiler.languages.java.ast.ForEachStatement;
import com.strobel.decompiler.languages.java.ast.ForStatement;
import com.strobel.decompiler.languages.java.ast.GotoStatement;
import com.strobel.decompiler.languages.java.ast.IdentifierExpression;
import com.strobel.decompiler.languages.java.ast.IfElseStatement;
import com.strobel.decompiler.languages.java.ast.IndexerExpression;
import com.strobel.decompiler.languages.java.ast.InlinedBytecodeExpression;
import com.strobel.decompiler.languages.java.ast.InstanceOfExpression;
import com.strobel.decompiler.languages.java.ast.InvocationExpression;
import com.strobel.decompiler.languages.java.ast.LabelStatement;
import com.strobel.decompiler.languages.java.ast.LabeledStatement;
import com.strobel.decompiler.languages.java.ast.LambdaExpression;
import com.strobel.decompiler.languages.java.ast.LocalTypeDeclarationStatement;
import com.strobel.decompiler.languages.java.ast.MemberReferenceExpression;
import com.strobel.decompiler.languages.java.ast.MethodGroupExpression;
import com.strobel.decompiler.languages.java.ast.NullReferenceExpression;
import com.strobel.decompiler.languages.java.ast.ObjectCreationExpression;
import com.strobel.decompiler.languages.java.ast.ParenthesizedExpression;
import com.strobel.decompiler.languages.java.ast.PrimitiveExpression;
import com.strobel.decompiler.languages.java.ast.ReturnStatement;
import com.strobel.decompiler.languages.java.ast.SuperReferenceExpression;
import com.strobel.decompiler.languages.java.ast.SwitchExpression;
import com.strobel.decompiler.languages.java.ast.SwitchStatement;
import com.strobel.decompiler.languages.java.ast.SynchronizedStatement;
import com.strobel.decompiler.languages.java.ast.ThisReferenceExpression;
import com.strobel.decompiler.languages.java.ast.ThrowStatement;
import com.strobel.decompiler.languages.java.ast.TryCatchStatement;
import com.strobel.decompiler.languages.java.ast.TypeReferenceExpression;
import com.strobel.decompiler.languages.java.ast.UnaryOperatorExpression;
import com.strobel.decompiler.languages.java.ast.VariableDeclarationStatement;
import com.strobel.decompiler.languages.java.ast.WhileStatement;

public class MinMaxLineNumberVisitor extends DepthFirstAstVisitor<Void, Void> {

    private final LineNumberTableConverter lineNumberTableConverter;
    private int minLineNumber = Integer.MAX_VALUE;
    private int maxLineNumber = Integer.MIN_VALUE;

    public MinMaxLineNumberVisitor(final LineNumberTableConverter lineNumberTableConverter) {
        this.lineNumberTableConverter = lineNumberTableConverter;
    }

    private void updateMinMaxLineNumbers(int offset) {
        if (offset != Expression.MYSTERY_OFFSET) {
            int lineNumber = lineNumberTableConverter.getLineForOffset(offset);
            minLineNumber = Math.min(minLineNumber, lineNumber);
            maxLineNumber = Math.max(maxLineNumber, lineNumber);
        }
    }

    @Override
    public Void visitInvocationExpression(InvocationExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitInvocationExpression(node, data);
    }

    @Override
    public Void visitTypeReference(TypeReferenceExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitTypeReference(node, data);
    }

    @Override
    public Void visitMemberReferenceExpression(MemberReferenceExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitMemberReferenceExpression(node, data);
    }

    @Override
    public Void visitNullReferenceExpression(NullReferenceExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitNullReferenceExpression(node, data);
    }

    @Override
    public Void visitThisReferenceExpression(ThisReferenceExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitThisReferenceExpression(node, data);
    }

    @Override
    public Void visitSuperReferenceExpression(SuperReferenceExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitSuperReferenceExpression(node, data);
    }

    @Override
    public Void visitClassOfExpression(ClassOfExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitClassOfExpression(node, data);
    }

    @Override
    public Void visitBlockStatement(BlockStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitBlockStatement(node, data);
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitExpressionStatement(node, data);
    }

    @Override
    public Void visitBreakStatement(BreakStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitBreakStatement(node, data);
    }

    @Override
    public Void visitContinueStatement(ContinueStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitContinueStatement(node, data);
    }

    @Override
    public Void visitDoWhileStatement(DoWhileStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitDoWhileStatement(node, data);
    }

    @Override
    public Void visitEmptyStatement(EmptyStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitEmptyStatement(node, data);
    }

    @Override
    public Void visitIfElseStatement(IfElseStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitIfElseStatement(node, data);
    }

    @Override
    public Void visitLabelStatement(LabelStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitLabelStatement(node, data);
    }

    @Override
    public Void visitLabeledStatement(LabeledStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitLabeledStatement(node, data);
    }

    @Override
    public Void visitReturnStatement(ReturnStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitReturnStatement(node, data);
    }

    @Override
    public Void visitSwitchStatement(SwitchStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitSwitchStatement(node, data);
    }

    @Override
    public Void visitSwitchExpression(SwitchExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitSwitchExpression(node, data);
    }

    @Override
    public Void visitThrowStatement(ThrowStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitThrowStatement(node, data);
    }

    @Override
    public Void visitAnnotation(Annotation node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitAnnotation(node, data);
   }

    @Override
    public Void visitVariableDeclaration(VariableDeclarationStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitVariableDeclaration(node, data);
   }

    @Override
    public Void visitWhileStatement(WhileStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitWhileStatement(node, data);
    }

    @Override
    public Void visitPrimitiveExpression(PrimitiveExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitPrimitiveExpression(node, data);
  }

    @Override
    public Void visitCastExpression(CastExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitCastExpression(node, data);
   }

    @Override
    public Void visitBinaryOperatorExpression(BinaryOperatorExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitBinaryOperatorExpression(node, data);
    }

    @Override
    public Void visitInstanceOfExpression(InstanceOfExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitInstanceOfExpression(node, data);
   }

    @Override
    public Void visitIndexerExpression(IndexerExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitIndexerExpression(node, data);
    }

    @Override
    public Void visitIdentifierExpression(IdentifierExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitIdentifierExpression(node, data);
    }

    @Override
    public Void visitUnaryOperatorExpression(UnaryOperatorExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitUnaryOperatorExpression(node, data);
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitConditionalExpression(node, data);
   }

    @Override
    public Void visitArrayInitializerExpression(ArrayInitializerExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitArrayInitializerExpression(node, data);
   }

    @Override
    public Void visitObjectCreationExpression(ObjectCreationExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitObjectCreationExpression(node, data);
  }

    @Override
    public Void visitArrayCreationExpression(ArrayCreationExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitArrayCreationExpression(node, data);
    }

    @Override
    public Void visitAssignmentExpression(AssignmentExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitAssignmentExpression(node, data);
    }

    @Override
    public Void visitForStatement(ForStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitForStatement(node, data);
    }

    @Override
    public Void visitForEachStatement(ForEachStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitForEachStatement(node, data);
    }

    @Override
    public Void visitTryCatchStatement(TryCatchStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitTryCatchStatement(node, data);
    }

    @Override
    public Void visitGotoStatement(GotoStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitGotoStatement(node, data);
    }

    @Override
    public Void visitParenthesizedExpression(ParenthesizedExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitParenthesizedExpression(node, data);
    }

    @Override
    public Void visitSynchronizedStatement(SynchronizedStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitSynchronizedStatement(node, data);
    }

    @Override
    public Void visitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitAnonymousObjectCreationExpression(node, data);
    }

    @Override
    public Void visitMethodGroupExpression(MethodGroupExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitMethodGroupExpression(node, data);
    }

    @Override
    public Void visitAssertStatement(AssertStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitAssertStatement(node, data);
    }

    @Override
    public Void visitLambdaExpression(LambdaExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitLambdaExpression(node, data);
    }

    @Override
    public Void visitLocalTypeDeclarationStatement(LocalTypeDeclarationStatement node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitLocalTypeDeclarationStatement(node, data);
    }

    @Override
    public Void visitInlinedBytecode(InlinedBytecodeExpression node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitInlinedBytecode(node, data);
    }

    @Override
    public Void visitBytecodeConstant(BytecodeConstant node, Void data) {
        updateMinMaxLineNumbers(node.getOffset());
        return super.visitBytecodeConstant(node, data);
    }

    public int getMinLineNumber() {
        return minLineNumber;
    }

    public int getMaxLineNumber() {
        return maxLineNumber;
    }
}