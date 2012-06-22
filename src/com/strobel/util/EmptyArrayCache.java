package com.strobel.util;

import com.strobel.core.VerifyArgument;

import java.lang.reflect.Array;
import java.util.HashMap;

/**
 * @author Mike Strobel
 */
public final class EmptyArrayCache {
    public static final Object[] EmptyObjectArray = new Object[0];
    public static final boolean[] EmptyBooleanArray = new boolean[0];
    public static final char[] EmptyCharArray = new char[0];
    public static final byte[] EmptyByteArray = new byte[0];
    public static final short[] EmptyShortArray = new short[0];
    public static final int[] EmptyIntArray = new int[0];
    public static final long[] EmptyLongArray = new long[0];
    public static final float[] EmptyFloatArray = new float[0];
    public static final double[] EmptyDoubleArray = new double[0];

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
