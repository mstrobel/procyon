/*
 * MethodHandle.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler.metadata;

import com.strobel.core.HashUtilities;
import com.strobel.core.VerifyArgument;

public final class MethodHandle {
    private final MethodReference _method;
    private final MethodHandleType _handleType;

    public MethodHandle(final MethodReference method, final MethodHandleType handleType) {
        _method = VerifyArgument.notNull(method, "method");
        _handleType = VerifyArgument.notNull(handleType, "handleType");
    }

    @Override
    public int hashCode() {
        return HashUtilities.combineHashCodes(_method, _handleType);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MethodHandle) {
            final MethodHandle other = (MethodHandle) obj;

            return other._handleType == _handleType &&
                   other._method.isEquivalentTo(_method);
        }
        return false;
    }

    public final MethodHandleType getHandleType() {
        return _handleType;
    }

    public final MethodReference getMethod() {
        return _method;
    }

    @Override
    public final String toString() {
        return _handleType + " " + _method.getFullName() + ":" + _method.getSignature();
    }
}
