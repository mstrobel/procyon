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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

public final class ExceptionUtilities {
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
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

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public static String getMessage(final Throwable t) {
        final String message = VerifyArgument.notNull(t, "t").getMessage();

        if (StringUtilities.isNullOrWhitespace(message)) {
            return t.getClass().getSimpleName() + " was thrown.";
        }

        return message;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public static String getStackTraceString(final Throwable t) {
        VerifyArgument.notNull(t, "t");

        try (final ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
             final PrintWriter writer = new PrintWriter(stream)) {

            t.printStackTrace(writer);

            writer.flush();
            stream.flush();

            return StringUtilities.trimRight(stream.toString());
        }
        catch (Throwable ignored) {
            return t.toString();
        }
    }
}
