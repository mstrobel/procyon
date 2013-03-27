/*
 * JavaOutputVisitor.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java;

import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.Pattern;

import static java.lang.String.format;

public final class JavaOutputVisitor implements IAstVisitor<JavaFormattingOptions, Void> {
    private final ITextOutput _output;

    public JavaOutputVisitor(final ITextOutput output) {
        _output = VerifyArgument.notNull(output, "output");
    }

    @Override
    public Void visitComment(final Comment node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitPatternPlaceholder(final AstNode node, final Pattern pattern, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitInvocationExpression(final InvocationExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitTypeReference(final TypeReferenceExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitJavaTokenNode(final JavaTokenNode node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitMemberReferenceExpression(final MemberReferenceExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitIdentifier(final Identifier node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitNullReferenceExpression(final NullReferenceExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitThisReferenceExpression(final ThisReferenceExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitSuperReferenceExpression(final SuperReferenceExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitClassOfExpression(final ClassOfExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitBlockStatement(final BlockStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitExpressionStatement(final ExpressionStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitBreakStatement(final BreakStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitContinueStatement(final ContinueStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitDoWhileStatement(final DoWhileStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitEmptyStatement(final EmptyStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitIfElseStatement(final IfElseStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitLabel(final LabelStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitReturnStatement(final ReturnStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitSwitchStatement(final SwitchStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitSwitchSection(final SwitchSection node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitCaseLabel(final CaseLabel node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitThrowStatement(final ThrowStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitCatchClause(final CatchClause node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitAnnotationSection(final AnnotationSection node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitAnnotation(final Annotation node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitNewLine(final NewLineNode node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitVariableDeclaration(final VariableDeclarationStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitVariableInitializer(final VariableInitializer node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitText(final TextNode node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitImportDeclaration(final ImportDeclaration node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitSimpleType(final SimpleType node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitMethodDeclaration(final MethodDeclaration node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitConstructorDeclaration(final ConstructorDeclaration node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitTypeParameterDeclaration(final TypeParameterDeclaration node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitParameterDeclaration(final ParameterDeclaration node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitFieldDeclaration(final FieldDeclaration node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitTypeDeclaration(final TypeDeclaration node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitConstructorInitializer(final ConstructorInitializer node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitCompilationUnit(final CompilationUnit node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitPackageDeclaration(final PackageDeclaration node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitArraySpecifier(final ArraySpecifier node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitComposedType(final ComposedType node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitWhileStatement(final WhileStatement node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitPrimitiveExpression(final PrimitiveExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitCastExpression(final CastExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitBinaryOperatorExpression(final BinaryOperatorExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitInstanceOfExpression(final InstanceOfExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitIndexerExpression(final IndexerExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitIdentifierExpression(final IdentifierExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitUnaryOperatorExpression(final UnaryOperatorExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitConditionalExpression(final ConditionalExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitArrayInitializerExpression(final ArrayInitializerExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitObjectCreationExpression(final ObjectCreationExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    @Override
    public Void visitArrayCreationExpression(final ArrayCreationExpression node, final JavaFormattingOptions data) {
        _output.writeLine(node.toString());
        return null;
    }

    // <editor-fold defaultstate="collapsed" desc="Utility Methods">

    public static String convertCharacter(final char ch) {
        switch (ch) {
            case '\\':
                return "\\\\";
            case '\0':
                return "\\0";
            case '\b':
                return "\\b";
            case '\f':
                return "\\f";
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\t':
                return "\\t";

            default:
                if (Character.isISOControl(ch) ||
                    Character.isSurrogate(ch) ||
                    Character.isWhitespace(ch) && ch != ' ') {

                    return format("\\u%1$04x", (int) ch);
                }
                else {
                    return String.valueOf(ch);
                }
        }
    }

    public static String convertString(final String s) {
        return convertString(s, false);
    }

    public static String convertString(final String s, final boolean quote) {
        final StringBuilder sb = new StringBuilder(Math.min(16, s.length()));

        if (quote) {
            sb.append('"');
        }

        for (int i = 0, n = s.length(); i < n; i++) {
            sb.append(convertCharacter(s.charAt(i)));
        }

        if (quote) {
            sb.append('"');
        }

        return sb.toString();
    }

    // </editor-fold>
}
