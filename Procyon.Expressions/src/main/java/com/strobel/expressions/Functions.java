/*
 * Functions.java
 *
 * Copyright (c) 2021 Mike Strobel
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

import com.strobel.core.Selector;
import com.strobel.reflection.Type;

final class Functions {
    private final static Selector<? super Expression, Type<?>> EXPRESSION_TYPE = new Selector<Expression, Type<?>>() {
        @Override
        public Type<?> select(final Expression expression) {
            return expression.getType();
        }
    };

    private final static Selector<Type<?>, String> TYPE_NAME = new Selector<Type<?>, String>() {
        @Override
        public String select(final Type<?> type) {
            return type.getName();
        }
    };

    static <E extends Expression> Selector<? super E, Type<?>> expressionType() {
        return EXPRESSION_TYPE;
    }

    static Selector<Type<?>, String> typeName() {
        return TYPE_NAME;
    }
}
