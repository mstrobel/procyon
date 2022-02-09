/*
 * ExpressionContext.java
 *
 * Copyright (c) 2022 Mike Strobel
 *
 * This source code is based on the Dynamic Language Runtime from Microsoft,
 *   Copyright (c) Microsoft Corporation.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.expressions;

import com.strobel.annotations.NotNull;
import com.strobel.core.VerifyArgument;

import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;

public final class ExpressionContext implements AutoCloseable {
    private final static ExpressionContext DEFAULT = new ExpressionContext(generated.PackageAccess.defaultPackage(), true);

    private final static ThreadLocal<ArrayDeque<ExpressionContext>> THREAD_CONTEXT = new ThreadLocal<ArrayDeque<ExpressionContext>>() {
        @Override
        protected ArrayDeque<ExpressionContext> initialValue() {
            final ArrayDeque<ExpressionContext> stack = new ArrayDeque<>();
            stack.push(DEFAULT);
            return stack;
        }
    };

    private final MethodHandles.Lookup packageAccess;
    private final boolean isDefault;

    private ExpressionContext(final @NotNull MethodHandles.Lookup packageAccess, final boolean isDefault) {
        this.packageAccess = VerifyArgument.notNull(packageAccess, "packageAccess");
        this.isDefault = isDefault;
    }

    public static ExpressionContext defaultContext() {
        return DEFAULT;
    }

    public static ExpressionContext create(final @NotNull MethodHandles.Lookup packageAccess) {
        return new ExpressionContext(packageAccess, false);
    }

    public static ExpressionContext push(final @NotNull MethodHandles.Lookup packageAccess) {
        return new ExpressionContext(packageAccess, false).push();
    }

    public boolean isDefaultContext() {
        return isDefault;
    }

    public MethodHandles.Lookup packageAccess() {
        return packageAccess;
    }

    public ExpressionContext push() {
        final ArrayDeque<ExpressionContext> stack = THREAD_CONTEXT.get();
        stack.addFirst(this);
        return this;
    }

    public static ExpressionContext current() {
        final ArrayDeque<ExpressionContext> stack = THREAD_CONTEXT.get();
        final ExpressionContext top = stack.peekFirst();
        return top != null ? top : DEFAULT;
    }

    @Override
    public void close() {
        pop();
    }

    public void pop() {
        final ArrayDeque<ExpressionContext> stack = THREAD_CONTEXT.get();
        stack.removeFirstOccurrence(this);
    }
}
