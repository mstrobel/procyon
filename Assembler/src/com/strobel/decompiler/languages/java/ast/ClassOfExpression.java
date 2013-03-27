/*
 * ClassOfExpression.java
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

public final class ClassOfExpression extends Expression {
    public final static TokenRole ClassKeywordRole = new TokenRole("class");

    public ClassOfExpression() {
    }

    public ClassOfExpression(final AstType type) {
        addChild(type, Roles.Type);
    }

    public final AstType getType() {
        return getChildByRole(Roles.Type);
    }

    public final void setType(final AstType type) {
        setChildByRole(Roles.Type, type);
    }

    public final JavaTokenNode getDotToken() {
        return getChildByRole(Roles.Dot);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.acceptClassOfExpression(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        return other instanceof ClassOfExpression &&
               getType().matches(((ClassOfExpression) other).getType(), match);
    }
}
