package com.strobel.core;

import com.strobel.util.EmptyArrayCache;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * @author Mike Strobel
 */
public final class ArrayUtilities {
    private ArrayUtilities() {}

    public static <T> boolean contains(final T[] array, final T item) {
        VerifyArgument.notNull(array, "array");
        return indexOf(array, item) != -1;
    }

    public static <T> int indexOf(final T[] array, final T item) {
        VerifyArgument.notNull(array, "array");
        if (item == null) {
            for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        }
        else {
            for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
                if (item.equals(array[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static <T> int lastIndexOf(final T[] array, final T item) {
        VerifyArgument.notNull(array, "array");
        if (item == null) {
            for (int i = array.length - 1; i >= 0; i--) {
                if (array[i] == null) {
                    return i;
                }
            }
        }
        else {
            for (int i = array.length - 1; i >= 0; i--) {
                if (item.equals(array[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] insert(final T[] array, final int index, final T value) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        final T[] newArray = (T[])Array.newInstance(
            array.getClass().getComponentType(),
            array.length + 1
        );

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + 1, remaining);
        }

        newArray[index] = value;

        return newArray;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> T[] insert(final T[] array, final int index, final T... values) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");
        VerifyArgument.elementsOfType(array.getClass().getComponentType(), values, "values");

        final int newItemCount = values.length;

        if (newItemCount == 0) {
            return array;
        }

        final T[] newArray = (T[])Array.newInstance(
            array.getClass().getComponentType(),
            array.length + newItemCount
        );

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + newItemCount, remaining);
        }

        System.arraycopy(values, 0, newArray, index, newItemCount);

        return newArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] append(final T[] array, final T value) {
        return insert(array, VerifyArgument.notNull(array, "array").length, value);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> T[] append(final T[] array, final T... values) {
        return insert(array, VerifyArgument.notNull(array, "array").length, values);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] prepend(final T[] array, final T value) {
        return insert(array, 0, value);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> T[] prepend(final T[] array, final T... values) {
        return insert(array, 0, values);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] remove(final T[] array, final int index) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length - 1, index, "index");

        if (array.length == 1) {
            return EmptyArrayCache.fromArrayType(array.getClass());
        }

        final T[] newArray = (T[])Array.newInstance(
            array.getClass().getComponentType(),
            array.length - 1
        );

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index - 1;

        if (remaining > 0) {
            System.arraycopy(array, index + 1, newArray, index, remaining);
        }

        return newArray;
    }

    public static <T> boolean isNullOrEmpty(final T[] array) {
        return array == null || array.length == 0;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> T[] removeAll(final T[] array, final T... values) {
        VerifyArgument.notNull(array, "array");

        if (isNullOrEmpty(array)) {
            return array;
        }

        final int count = values.length;

        int matchCount = 0;

        final int[] matchIndices = new int[count];

        for (int i = 0; i < count; i++) {
            final T value = values[i];
            final int index = indexOf(array, value);

            if (index == -1) {
                matchIndices[i] = Integer.MAX_VALUE;
                continue;
            }

            matchIndices[i] = index;
            ++matchCount;
        }

        if (matchCount == 0) {
            return array;
        }

        Arrays.sort(matchIndices);

        final T[] newArray = (T[])Array.newInstance(
            array.getClass().getComponentType(),
            array.length - matchCount
        );

        int sourcePosition = 0;

        for (int i = 0; i < matchCount; i++) {
            final int matchIndex = matchIndices[i];

            if (matchIndex == Integer.MAX_VALUE) {
                break;
            }

            System.arraycopy(array, sourcePosition, newArray, sourcePosition - i, matchIndex);

            sourcePosition = matchIndex + 1;
        }

        final int remaining = array.length - sourcePosition;

        if (remaining > 0) {
            System.arraycopy(array, sourcePosition, newArray, newArray.length - remaining, remaining);
        }

        return newArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] removeFirst(final T[] array, final T item) {
        final int index = indexOf(VerifyArgument.notNull(array, "array"), item);

        if (index == -1) {
            return array;
        }

        return remove(array, index);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] removeLast(final T[] array, final T item) {
        final int index = lastIndexOf(VerifyArgument.notNull(array, "array"), item);

        if (index == -1) {
            return array;
        }

        return remove(array, index);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> T[] retainAll(final T[] array, final T... values) {
        VerifyArgument.notNull(array, "array");

        if (isNullOrEmpty(array)) {
            return array;
        }

        final int count = values.length;

        int matchCount = 0;

        final int[] matchIndices = new int[count];

        for (int i = 0; i < count; i++) {
            final T value = values[i];
            final int index = indexOf(array, value);

            if (index == -1) {
                matchIndices[i] = Integer.MAX_VALUE;
                continue;
            }

            matchIndices[i] = index;
            ++matchCount;
        }

        if (matchCount == 0) {
            return EmptyArrayCache.fromArrayType(array.getClass());
        }

        Arrays.sort(matchIndices);

        final T[] newArray = (T[])Array.newInstance(
            array.getClass().getComponentType(),
            matchCount
        );

        for (int i = 0; i < matchCount; i++) {
            final int matchIndex = matchIndices[i];

            if (matchIndex == Integer.MAX_VALUE) {
                break;
            }

            newArray[i] = array[matchIndex];
        }

        return newArray;
    }
}
