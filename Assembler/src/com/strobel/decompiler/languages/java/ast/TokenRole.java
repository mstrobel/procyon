/*
 * TokenRole.java
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

import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.patterns.Role;

public final class TokenRole extends Role<JavaTokenNode> {
    private final String _token;
    private final int _length;

    public final String getToken() {
        return _token;
    }

    public final int getLength() {
        return _length;
    }

    public TokenRole(final String token) {
        super(token, JavaTokenNode.class, JavaTokenNode.NULL);

        _token = VerifyArgument.notNull(token, "token");
        _length = token.length();
    }
}
