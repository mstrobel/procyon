/*
 * RuntimeHelpers.java
 *
 * Copyright (c) 2012 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.compilerservices;

import com.strobel.core.ExceptionUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * @author strobelm
 */
public final class RuntimeHelpers {
    @SuppressWarnings("ALL")
    protected static void ensureInitializedSafely(final Class<?> clazz) {
        try {
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
        }
        catch (final Throwable t) {
            ExceptionUtilities.wrapOrThrow(t);
        }
    }

    private RuntimeHelpers() {
        throw ContractUtils.unreachable();
    }

    public static void ensureClassInitialized(final Class<?> clazz) {
        try {
            LazyInit.ENSURE_INITIALIZED.invokeExact((Class<?>) VerifyArgument.notNull(clazz, "clazz"));
        }
        catch (final Throwable t) {
            throw ExceptionUtilities.wrapOrThrow(t);
        }
    }

    private final static class LazyInit {
        final static MethodHandle ENSURE_INITIALIZED = makeEnsureInitialized();

        @SuppressWarnings("UnnecessaryLocalVariable")
        private static MethodHandle makeEnsureInitialized() {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();

            try {
                final Object unsafe = UnsafeAccess.unsafe();
                final Method ensureClassInitialized = unsafe.getClass().getDeclaredMethod("ensureClassInitialized", Class.class);

                if (!ensureClassInitialized.isAnnotationPresent(Deprecated.class)) {
                    final MethodHandle handle = lookup.unreflect(ensureClassInitialized);
                    final MethodHandle boundHandle = MethodHandles.insertArguments(handle, 0, unsafe);

                    return boundHandle;
                }
            }
            catch (final Throwable ignored) {
                ExceptionUtilities.rethrowCritical(ignored);
            }

            try {
                return lookup.findStatic(
                    RuntimeHelpers.class,
                    "ensureInitializedSafely",
                    MethodType.methodType(void.class, Class.class)
                );
            }
            catch (final ReflectiveOperationException e) {
                throw ExceptionUtilities.wrapOrThrow(e);
            }
        }
    }
}
