/*
 * AnnotationSection.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast;

import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.patterns.BacktrackingInfo;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.Pattern;
import com.strobel.decompiler.patterns.Role;

public class AnnotationSection extends AstNode {
    @Override
    public NodeType getNodeType() {
        return NodeType.UNKNOWN;
    }

    public final JavaTokenNode getLeftParenthesisToken() {
        return getChildByRole(Roles.LEFT_PARENTHESIS);
    }

    public final JavaTokenNode getRightParenthesisToken() {
        return getChildByRole(Roles.RIGHT_PARENTHESIS);
    }

    public final String getAnnotationTarget() {
        return getChildByRole(Roles.IDENTIFIER).getName();
    }

    public final void setAnnotationTarget(final String value) {
        setChildByRole(Roles.IDENTIFIER, Identifier.create(value));
    }

    public final Identifier getAnnotationTargetToken() {
        return getChildByRole(Roles.IDENTIFIER);
    }

    public final void setAnnotationTargetToken(final Identifier value) {
        setChildByRole(Roles.IDENTIFIER, value);
    }

    public final AstNodeCollection<Annotation> getAnnotations() {
        return getChildrenByRole(Roles.ANNOTATION);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitAnnotationSection(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof AnnotationSection) {
            final AnnotationSection otherSection = (AnnotationSection) other;

            return !otherSection.isNull() &&
                   StringUtilities.equals(getAnnotationTarget(), otherSection.getAnnotationTarget()) &&
                   getAnnotations().matches(otherSection.getAnnotations(), match);
        }

        return false;
    }

    // <editor-fold defaultstate="collapsed" desc="Pattern Placeholder">

    public static AnnotationSection forPattern(final Pattern pattern) {
        return new PatternPlaceholder(VerifyArgument.notNull(pattern, "pattern"));
    }

    private final static class PatternPlaceholder extends AnnotationSection {
        final Pattern child;

        PatternPlaceholder(final Pattern child) {
            this.child = child;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.PATTERN;
        }

        @Override
        public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
            return visitor.visitPatternPlaceholder(this, child, data);
        }

        @Override
        public boolean matchesCollection(final Role role, final INode position, final Match match, final BacktrackingInfo backtrackingInfo) {
            return child.matchesCollection(role, position, match, backtrackingInfo);
        }

        @Override
        public boolean matches(final INode other, final Match match) {
            return child.matches(other, match);
        }
    }

    // </editor-fold>
}
