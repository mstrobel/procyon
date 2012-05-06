package com.strobel.util;

import com.strobel.core.VerifyArgument;

import java.lang.reflect.Array;
import java.util.HashMap;

/**
 * @author Mike Strobel
 */
public final class EmptyArrayCache {
    private EmptyArrayCache() {}

    private final static HashMap<Class<?>, Object[]> _cache = new HashMap<>();

    @SuppressWarnings("unchecked")
    public synchronized static <T> T[] fromElementType(final Class<T> elementType) {
        VerifyArgument.notNull(elementType, "elementType");

        final T[] cachedArray = (T[]) _cache.get(elementType);

        if (cachedArray != null) {
            return cachedArray;
        }

        final T[] newArray = (T[])Array.newInstance(elementType, 0);

        _cache.put(elementType, newArray);

        return newArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromArrayType(final Class<? extends Object[]> arrayType) {
        VerifyArgument.notNull(arrayType, "arrayType");

        return (T)fromElementType(arrayType.getComponentType());
    }
}
