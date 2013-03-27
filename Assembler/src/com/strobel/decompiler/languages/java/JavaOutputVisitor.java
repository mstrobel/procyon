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
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.Comment;
import com.strobel.decompiler.languages.java.ast.IAstVisitor;
import com.strobel.decompiler.languages.java.ast.Identifier;
import com.strobel.decompiler.languages.java.ast.InvocationExpression;
import com.strobel.decompiler.languages.java.ast.JavaTokenNode;
import com.strobel.decompiler.languages.java.ast.MemberReferenceExpression;
import com.strobel.decompiler.languages.java.ast.NullReferenceExpression;
import com.strobel.decompiler.languages.java.ast.TypeReferenceExpression;
import com.strobel.decompiler.patterns.Pattern;

import static java.lang.String.format;

public final class JavaOutputVisitor implements IAstVisitor<JavaFormattingOptions, Void> {
    private final ITextOutput _output;

    public JavaOutputVisitor(final ITextOutput output) {
        _output = VerifyArgument.notNull(output, "output");
    }

    @Override
    public Void visitComment(final Comment comment, final JavaFormattingOptions data) {
        return null;
    }

    @Override
    public Void visitPatternPlaceholder(
        final AstNode patternPlaceholder, final Pattern pattern, final JavaFormattingOptions data) {
        return null;
    }

    @Override
    public Void visitInvocationExpression(final InvocationExpression invocationExpression, final JavaFormattingOptions data) {
        return null;
    }

    @Override
    public Void visitTypeReference(final TypeReferenceExpression typeReferenceExpression, final JavaFormattingOptions data) {
        return null;
    }

    @Override
    public Void visitJavaTokenNode(final JavaTokenNode javaTokenNode, final JavaFormattingOptions data) {
        return null;
    }

    @Override
    public Void visitMemberReferenceExpression(final MemberReferenceExpression memberReferenceExpression, final JavaFormattingOptions data) {
        return null;
    }

    @Override
    public Void visitIdentifier(final Identifier identifier, final JavaFormattingOptions data) {
        return null;
    }

    @Override
    public Void acceptNullReferenceExpression(final NullReferenceExpression nullReferenceExpression, final JavaFormattingOptions data) {
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
