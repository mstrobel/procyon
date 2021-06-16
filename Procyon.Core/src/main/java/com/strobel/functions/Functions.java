package com.strobel.functions;

import com.strobel.core.VerifyArgument;

public final class Functions {
    public static <T, R> Function<T, R> ofSupplier(final Supplier<R> supplier) {
        VerifyArgument.notNull(supplier, "supplier");
        return new Function<T, R>() {
            @Override
            public R apply(final T input) {
                return supplier.get();
            }
        };
    }
}
