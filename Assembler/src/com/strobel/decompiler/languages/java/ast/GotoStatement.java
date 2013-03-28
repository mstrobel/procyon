/*
 * GotoStatement.java
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

import com.strobel.core.StringUtilities;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;

public class GotoStatement extends Statement {
    public final static TokenRole GOTO_KEYWORD_ROLE = new TokenRole("goto");

    public GotoStatement() {
    }

    public GotoStatement(final String label) {
        setLabel(label);
    }

    public final JavaTokenNode getGotoToken() {
        return getChildByRole(GOTO_KEYWORD_ROLE);
    }

    public final JavaTokenNode getSemicolonToken() {
        return getChildByRole(Roles.SEMICOLON);
    }

    public final String getLabel() {
        return getChildByRole(Roles.IDENTIFIER).getName();
    }

    public final void setLabel(final String value) {
        if (StringUtilities.isNullOrEmpty(value)) {
            setChildByRole(Roles.IDENTIFIER, Identifier.create(null));
        }
        else {
            setChildByRole(Roles.IDENTIFIER, Identifier.create(value));
        }
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitGotoStatement(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        return other instanceof GotoStatement &&
               matchString(getLabel(), ((GotoStatement) other).getLabel());
    }
}
