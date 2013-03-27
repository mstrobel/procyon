/*
 * ArrayInitializerExpression.java
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

import java.util.Collections;

/**
 * @author strobelm
 */
public class ArrayInitializerExpression extends Expression {
    public ArrayInitializerExpression() {
    }

    public ArrayInitializerExpression(final Iterable<Expression> elements) {
        if (elements != null) {
            final AstNodeCollection<Expression> elementsCollection = getElements();

            for (final Expression element : elements) {
                elementsCollection.add(element);
            }
        }
    }

    public ArrayInitializerExpression(final Expression... elements) {
        if (elements != null) {
            Collections.addAll(getElements(), elements);
        }
    }

    public final JavaTokenNode getLeftBraceToken() {
        return getChildByRole(Roles.LEFT_BRACE);
    }

    public final AstNodeCollection<Expression> getElements() {
        return getChildrenByRole(Roles.EXPRESSION);
    }

    public final JavaTokenNode getRightBraceToken() {
        return getChildByRole(Roles.RIGHT_BRACE);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitArrayInitializerExpression(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof ArrayInitializerExpression) {
            final ArrayInitializerExpression otherInitializer = (ArrayInitializerExpression) other;

            return !otherInitializer.isNull() &&
                   getElements().matches(otherInitializer.getElements(), match);
        }

        return false;
    }

    // <editor-fold defaultstate="collapsed" desc="Null ArrayInitializerExpression">

    public final static ArrayInitializerExpression NULL = new NullArrayInitializerExpression();

    private static final class NullArrayInitializerExpression extends ArrayInitializerExpression {
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
