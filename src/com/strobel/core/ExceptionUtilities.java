/*
 * ExceptionUtilities.java
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

package com.strobel.core;

import com.strobel.reflection.TargetInvocationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

public final class ExceptionUtilities {
    public static RuntimeException asRuntimeException(final Throwable t) {
        VerifyArgument.notNull(t, "t");

        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }

        return new UndeclaredThrowableException(t, "An unhandled checked exception occurred.");
    }

    public static Throwable unwrap(final Throwable t) {
        final Throwable cause = t.getCause();

        if (cause == null || cause == t) {
            return t;
        }

        if (t instanceof InvocationTargetException ||
            t instanceof TargetInvocationException ||
            t instanceof UndeclaredThrowableException) {

            return unwrap(cause);
        }

        return t;
    }
}
