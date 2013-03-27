/*
 * ConstructorInitializer.java
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

package com.strobel.decompiler.languages.java.ast;

import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;

public class ConstructorInitializer extends AstNode {
    public final static TokenRole SUPER_KEYWORD_ROLE = new TokenRole("super");
    public final static TokenRole THIS_KEYWORD_ROLE = new TokenRole("this");

    private ConstructorInitializerType _constructorInitializerType;

    public final ConstructorInitializerType getConstructorInitializerType() {
        return _constructorInitializerType;
    }

    public final void setConstructorInitializerType(final ConstructorInitializerType constructorInitializerType) {
        verifyNotFrozen();
        _constructorInitializerType = constructorInitializerType;
    }

    public final JavaTokenNode getLeftParenthesisToken() {
        return getChildByRole(Roles.LEFT_PARENTHESIS);
    }

    public final AstNodeCollection<Expression> getArguments() {
        return getChildrenByRole(Roles.ARGUMENT);
    }

    public final JavaTokenNode getRightParenthesisToken() {
        return getChildByRole(Roles.RIGHT_PARENTHESIS);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.UNKNOWN;
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitConstructorInitializer(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof ConstructorInitializer) {
            final ConstructorInitializer otherInitializer = (ConstructorInitializer) other;

            return !otherInitializer.isNull() &&
                   _constructorInitializerType == otherInitializer._constructorInitializerType &&
                   getArguments().matches(otherInitializer.getArguments(), match);
        }

        return false;
    }

// <editor-fold defaultstate="collapsed" desc="Null ConstructorInitializer">

    public final static ConstructorInitializer NULL = new NullConstructorInitializer();

    private static final class NullConstructorInitializer extends ConstructorInitializer {
        @Override
        public final boolean isNull() {
            return true;
        }

        @Override
        public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
            return null;
        }

        @Override
        public boolean matches(final INode other, final Match match) {
            return other == null || other.isNull();
        }
    }

    // </editor-fold>
}

