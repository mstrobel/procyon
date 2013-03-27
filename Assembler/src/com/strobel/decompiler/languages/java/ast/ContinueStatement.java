/*
 * ContinueStatement.java
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

public class ContinueStatement extends Statement {
    public final static TokenRole CONTINUE_KEYWORD_ROLE = new TokenRole("continue");

    public final JavaTokenNode getContinueToken() {
        return getChildByRole(CONTINUE_KEYWORD_ROLE);
    }

    public final JavaTokenNode getSemicolonToken() {
        return getChildByRole(Roles.SEMICOLON);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitContinueStatement(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        return other instanceof ContinueStatement;
    }
}
