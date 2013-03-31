/*
 * LabelStatement.java
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

public class LabelStatement extends Statement {
    public LabelStatement() {
    }

    public LabelStatement(final String name) {
        setLabel(name);
    }

    public final String getLabel() {
        return getChildByRole(Roles.IDENTIFIER).getName();
    }

    public final void setLabel(final String value) {
        setChildByRole(Roles.IDENTIFIER, Identifier.create(value));
    }

    public final Identifier getLabelToken() {
        return getChildByRole(Roles.IDENTIFIER);
    }

    public final void setLabelToken(final Identifier value) {
        setChildByRole(Roles.IDENTIFIER, value);
    }

    public final JavaTokenNode getColonToken() {
        return getChildByRole(Roles.COLON);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitLabelStatement(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        return other instanceof LabelStatement &&
               matchString(getLabel(), ((LabelStatement) other).getLabel());
    }
}
