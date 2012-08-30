package com.strobel.util;

import com.strobel.core.VerifyArgument;

import java.lang.reflect.Array;
import java.util.HashMap;

/**
 * @author Mike Strobel
 */
public final class EmptyArrayCache {
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    public static final char[] EMPTY_CHAR_ARRAY = new char[0];
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final short[] EMPTY_SHORT_ARRAY = new short[0];
    public static final int[] EMPTY_INT_ARRAY = new int[0];
    public static final long[] EMPTY_LONG_ARRAY = new long[0];
    public static final float[] EMPTY_FLOAT_ARRAY = new float[0];
    public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

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
