package com.strobel.functions;

import com.strobel.util.ContractUtils;

@SuppressWarnings("unchecked")
public final class Functions {
    private Functions() {
        throw ContractUtils.unreachable();
    }

    private final static Function<?, String> OBJECT_TO_STRING = new Function<Object, String>() {
        @Override
        public String apply(final Object input) {
            return String.valueOf(input);
        }
    };

    public static <T> Function<T, String> objectToString() {
        return (Function<T, String>) OBJECT_TO_STRING;
    }
}
