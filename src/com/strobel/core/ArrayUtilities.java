package com.strobel.core;

import com.strobel.util.EmptyArrayCache;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mike Strobel
 */
public final class ArrayUtilities {
    private ArrayUtilities() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] makeArray(final Class<T> elementClass, final List<T> list) {
        VerifyArgument.notNull(elementClass, "elementClass");
        VerifyArgument.notNull(list, "list");

        final T[] array = (T[])Array.newInstance(elementClass, list.size());

        return list.toArray(array);
    }

    public static <T> boolean contains(final T[] array, final T value) {
        VerifyArgument.notNull(array, "array");
        return indexOf(array, value) != -1;
    }

    public static <T> int indexOf(final T[] array, final T value) {
        VerifyArgument.notNull(array, "array");
        if (value == null) {
            for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        }
        else {
            for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
                if (value.equals(array[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static <T> int lastIndexOf(final T[] array, final T value) {
        VerifyArgument.notNull(array, "array");
        if (value == null) {
            for (int i = array.length - 1; i >= 0; i--) {
                if (array[i] == null) {
                    return i;
                }
            }
        }
        else {
            for (int i = array.length - 1; i >= 0; i--) {
                if (value.equals(array[i])) {
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

        if (values == null || values.length == 0) {
            return array;
        }

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
        if (array == null) {
            if (value == null) {
                throw new IllegalArgumentException("At least one value must be specified if 'array' is null.");
            }
            final T[] newArray = (T[])Array.newInstance(value.getClass(), 1);
            newArray[0] = value;
            return newArray;
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, value);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> T[] append(final T[] array, final T... values) {
        if (array == null) {
            if (values == null || values.length == 0) {
                throw new IllegalArgumentException("At least one value must be specified if 'array' is null.");
            }
            final T[] newArray = (T[])Array.newInstance(values.getClass().getComponentType(), values.length);
            System.arraycopy(values, 0, newArray, 0, values.length);
            return newArray;
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, values);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] prepend(final T[] array, final T value) {
        if (array == null) {
            if (value == null) {
                throw new IllegalArgumentException("At least one value must be specified if 'array' is null.");
            }
            final T[] newArray = (T[])Array.newInstance(value.getClass(), 1);
            newArray[0] = value;
            return newArray;
        }
        return insert(array, 0, value);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> T[] prepend(final T[] array, final T... values) {
        if (array == null) {
            if (values == null || values.length == 0) {
                throw new IllegalArgumentException("At least one value must be specified if 'array' is null.");
            }
            final T[] newArray = (T[])Array.newInstance(values.getClass().getComponentType(), values.length);
            System.arraycopy(values, 0, newArray, 0, values.length);
            return newArray;
        }
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
    public static <T> T[] removeFirst(final T[] array, final T value) {
        final int index = indexOf(VerifyArgument.notNull(array, "array"), value);

        if (index == -1) {
            return array;
        }

        return remove(array, index);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] removeLast(final T[] array, final T value) {
        final int index = lastIndexOf(VerifyArgument.notNull(array, "array"), value);

        if (index == -1) {
            return array;
        }

        return remove(array, index);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> T[] retainAll(final T[] array, final T... values) {
        VerifyArgument.notNull(array, "array");

        if (isNullOrEmpty(values)) {
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

    ////////////////////////////////////////////////////////////////////////////////
    // PRIMITIVE ARRAY SPECIALIZATIONS OF SEARCH FUNCTIONS                        //
    ////////////////////////////////////////////////////////////////////////////////

    public static boolean isNullOrEmpty(final boolean[] array) {
        return array == null || array.length == 0;
    }

    public static boolean contains(final boolean[] array, final boolean value) {
        VerifyArgument.notNull(array, "array");
        return indexOf(array, value) != -1;
    }

    public static int indexOf(final boolean[] array, final boolean value) {
        VerifyArgument.notNull(array, "array");
        for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(final boolean[] array, final boolean value) {
        VerifyArgument.notNull(array, "array");
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isNullOrEmpty(final char[] array) {
        return array == null || array.length == 0;
    }

    public static boolean contains(final char[] array, final char value) {
        VerifyArgument.notNull(array, "array");
        return indexOf(array, value) != -1;
    }

    public static int indexOf(final char[] array, final char value) {
        VerifyArgument.notNull(array, "array");
        for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(final char[] array, final char value) {
        VerifyArgument.notNull(array, "array");
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isNullOrEmpty(final byte[] array) {
        return array == null || array.length == 0;
    }

    public static boolean contains(final byte[] array, final byte value) {
        VerifyArgument.notNull(array, "array");
        return indexOf(array, value) != -1;
    }

    public static int indexOf(final byte[] array, final byte value) {
        VerifyArgument.notNull(array, "array");
        for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(final byte[] array, final byte value) {
        VerifyArgument.notNull(array, "array");
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isNullOrEmpty(final short[] array) {
        return array == null || array.length == 0;
    }

    public static boolean contains(final short[] array, final short value) {
        VerifyArgument.notNull(array, "array");
        return indexOf(array, value) != -1;
    }

    public static int indexOf(final short[] array, final short value) {
        VerifyArgument.notNull(array, "array");
        for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(final short[] array, final short value) {
        VerifyArgument.notNull(array, "array");
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isNullOrEmpty(final int[] array) {
        return array == null || array.length == 0;
    }

    public static boolean contains(final int[] array, final int value) {
        VerifyArgument.notNull(array, "array");
        return indexOf(array, value) != -1;
    }

    public static int indexOf(final int[] array, final int value) {
        VerifyArgument.notNull(array, "array");
        for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(final int[] array, final int value) {
        VerifyArgument.notNull(array, "array");
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isNullOrEmpty(final long[] array) {
        return array == null || array.length == 0;
    }

    public static boolean contains(final long[] array, final long value) {
        VerifyArgument.notNull(array, "array");
        return indexOf(array, value) != -1;
    }

    public static int indexOf(final long[] array, final long value) {
        VerifyArgument.notNull(array, "array");
        for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(final long[] array, final long value) {
        VerifyArgument.notNull(array, "array");
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isNullOrEmpty(final float[] array) {
        return array == null || array.length == 0;
    }

    public static boolean contains(final float[] array, final float value) {
        VerifyArgument.notNull(array, "array");
        return indexOf(array, value) != -1;
    }

    public static int indexOf(final float[] array, final float value) {
        VerifyArgument.notNull(array, "array");
        for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(final float[] array, final float value) {
        VerifyArgument.notNull(array, "array");
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isNullOrEmpty(final double[] array) {
        return array == null || array.length == 0;
    }

    public static boolean contains(final double[] array, final double value) {
        VerifyArgument.notNull(array, "array");
        return indexOf(array, value) != -1;
    }

    public static int indexOf(final double[] array, final double value) {
        VerifyArgument.notNull(array, "array");
        for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(final double[] array, final double value) {
        VerifyArgument.notNull(array, "array");
        for (int i = array.length - 1; i >= 0; i--) {
            if (value == array[i]) {
                return i;
            }
        }
        return -1;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // PRIMITIVE ARRAY SPECIALIZATIONS OF MANIPULATION FUNCTIONS                  //
    ////////////////////////////////////////////////////////////////////////////////

    public static boolean[] append(final boolean[] array, final boolean value) {
        if (isNullOrEmpty(array)) {
            return new boolean[] { value };
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, value);
    }

    public static boolean[] append(final boolean[] array, final boolean... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, values);
    }

    public static boolean[] prepend(final boolean[] array, final boolean value) {
        if (isNullOrEmpty(array)) {
            return new boolean[] { value };
        }
        return insert(array, 0, value);
    }

    public static boolean[] prepend(final boolean[] array, final boolean... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, 0, values);
    }

    public static boolean[] remove(final boolean[] array, final int index) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length - 1, index, "index");

        if (array.length == 1) {
            return EmptyArrayCache.EmptyBooleanArray;
        }

        final boolean[] newArray = new boolean[array.length - 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index - 1;

        if (remaining > 0) {
            System.arraycopy(array, index + 1, newArray, index, remaining);
        }

        return newArray;
    }

    public static boolean[] insert(final boolean[] array, final int index, final boolean value) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        final boolean[] newArray = new boolean[array.length + 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + 1, remaining);
        }

        newArray[index] = value;

        return newArray;
    }

    public static boolean[] insert(final boolean[] array, final int index, final boolean... values) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        if (values == null || values.length == 0) {
            return array;
        }

        final int newItemCount = values.length;

        if (newItemCount == 0) {
            return array;
        }

        final boolean[] newArray = new boolean[array.length + newItemCount];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + newItemCount, remaining);
        }

        System.arraycopy(values, 0, newArray, index, newItemCount);

        return newArray;
    }

    public static char[] append(final char[] array, final char value) {
        if (isNullOrEmpty(array)) {
            return new char[] { value };
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, value);
    }

    public static char[] append(final char[] array, final char... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, values);
    }

    public static char[] prepend(final char[] array, final char value) {
        if (isNullOrEmpty(array)) {
            return new char[] { value };
        }
        return insert(array, 0, value);
    }

    public static char[] prepend(final char[] array, final char... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, 0, values);
    }

    public static char[] remove(final char[] array, final int index) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length - 1, index, "index");

        if (array.length == 1) {
            return EmptyArrayCache.EmptyCharArray;
        }

        final char[] newArray = new char[array.length - 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index - 1;

        if (remaining > 0) {
            System.arraycopy(array, index + 1, newArray, index, remaining);
        }

        return newArray;
    }

    public static char[] insert(final char[] array, final int index, final char value) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        final char[] newArray = new char[array.length + 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + 1, remaining);
        }

        newArray[index] = value;

        return newArray;
    }

    public static char[] insert(final char[] array, final int index, final char... values) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        if (values == null || values.length == 0) {
            return array;
        }

        final int newItemCount = values.length;

        if (newItemCount == 0) {
            return array;
        }

        final char[] newArray = new char[array.length + newItemCount];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + newItemCount, remaining);
        }

        System.arraycopy(values, 0, newArray, index, newItemCount);

        return newArray;
    }

    public static byte[] append(final byte[] array, final byte value) {
        if (isNullOrEmpty(array)) {
            return new byte[] { value };
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, value);
    }

    public static byte[] append(final byte[] array, final byte... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, values);
    }

    public static byte[] prepend(final byte[] array, final byte value) {
        if (isNullOrEmpty(array)) {
            return new byte[] { value };
        }
        return insert(array, 0, value);
    }

    public static byte[] prepend(final byte[] array, final byte... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, 0, values);
    }

    public static byte[] remove(final byte[] array, final int index) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length - 1, index, "index");

        if (array.length == 1) {
            return EmptyArrayCache.EmptyByteArray;
        }

        final byte[] newArray = new byte[array.length - 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index - 1;

        if (remaining > 0) {
            System.arraycopy(array, index + 1, newArray, index, remaining);
        }

        return newArray;
    }

    public static byte[] insert(final byte[] array, final int index, final byte value) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        final byte[] newArray = new byte[array.length + 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + 1, remaining);
        }

        newArray[index] = value;

        return newArray;
    }

    public static byte[] insert(final byte[] array, final int index, final byte... values) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        if (values == null || values.length == 0) {
            return array;
        }

        final int newItemCount = values.length;

        if (newItemCount == 0) {
            return array;
        }

        final byte[] newArray = new byte[array.length + newItemCount];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + newItemCount, remaining);
        }

        System.arraycopy(values, 0, newArray, index, newItemCount);

        return newArray;
    }

    public static short[] append(final short[] array, final short value) {
        if (isNullOrEmpty(array)) {
            return new short[] { value };
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, value);
    }

    public static short[] append(final short[] array, final short... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, values);
    }

    public static short[] prepend(final short[] array, final short value) {
        if (isNullOrEmpty(array)) {
            return new short[] { value };
        }
        return insert(array, 0, value);
    }

    public static short[] prepend(final short[] array, final short... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, 0, values);
    }

    public static short[] remove(final short[] array, final int index) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length - 1, index, "index");

        if (array.length == 1) {
            return EmptyArrayCache.EmptyShortArray;
        }

        final short[] newArray = new short[array.length - 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index - 1;

        if (remaining > 0) {
            System.arraycopy(array, index + 1, newArray, index, remaining);
        }

        return newArray;
    }

    public static short[] insert(final short[] array, final int index, final short value) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        final short[] newArray = new short[array.length + 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + 1, remaining);
        }

        newArray[index] = value;

        return newArray;
    }

    public static short[] insert(final short[] array, final int index, final short... values) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        if (values == null || values.length == 0) {
            return array;
        }

        final int newItemCount = values.length;

        if (newItemCount == 0) {
            return array;
        }

        final short[] newArray = new short[array.length + newItemCount];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + newItemCount, remaining);
        }

        System.arraycopy(values, 0, newArray, index, newItemCount);

        return newArray;
    }

    public static int[] append(final int[] array, final int value) {
        if (isNullOrEmpty(array)) {
            return new int[] { value };
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, value);
    }

    public static int[] append(final int[] array, final int... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, values);
    }

    public static int[] prepend(final int[] array, final int value) {
        if (isNullOrEmpty(array)) {
            return new int[] { value };
        }
        return insert(array, 0, value);
    }

    public static int[] prepend(final int[] array, final int... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, 0, values);
    }

    public static int[] remove(final int[] array, final int index) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length - 1, index, "index");

        if (array.length == 1) {
            return EmptyArrayCache.EmptyIntArray;
        }

        final int[] newArray = new int[array.length - 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index - 1;

        if (remaining > 0) {
            System.arraycopy(array, index + 1, newArray, index, remaining);
        }

        return newArray;
    }

    public static int[] insert(final int[] array, final int index, final int value) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        final int[] newArray = new int[array.length + 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + 1, remaining);
        }

        newArray[index] = value;

        return newArray;
    }

    public static int[] insert(final int[] array, final int index, final int... values) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        if (values == null || values.length == 0) {
            return array;
        }

        final int newItemCount = values.length;

        if (newItemCount == 0) {
            return array;
        }

        final int[] newArray = new int[array.length + newItemCount];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + newItemCount, remaining);
        }

        System.arraycopy(values, 0, newArray, index, newItemCount);

        return newArray;
    }

    public static long[] append(final long[] array, final long value) {
        if (isNullOrEmpty(array)) {
            return new long[] { value };
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, value);
    }

    public static long[] append(final long[] array, final long... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, values);
    }

    public static long[] prepend(final long[] array, final long value) {
        if (isNullOrEmpty(array)) {
            return new long[] { value };
        }
        return insert(array, 0, value);
    }

    public static long[] prepend(final long[] array, final long... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, 0, values);
    }

    public static long[] remove(final long[] array, final int index) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length - 1, index, "index");

        if (array.length == 1) {
            return EmptyArrayCache.EmptyLongArray;
        }

        final long[] newArray = new long[array.length - 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index - 1;

        if (remaining > 0) {
            System.arraycopy(array, index + 1, newArray, index, remaining);
        }

        return newArray;
    }

    public static long[] insert(final long[] array, final int index, final long value) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        final long[] newArray = new long[array.length + 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + 1, remaining);
        }

        newArray[index] = value;

        return newArray;
    }

    public static long[] insert(final long[] array, final int index, final long... values) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        if (values == null || values.length == 0) {
            return array;
        }

        final int newItemCount = values.length;

        if (newItemCount == 0) {
            return array;
        }

        final long[] newArray = new long[array.length + newItemCount];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + newItemCount, remaining);
        }

        System.arraycopy(values, 0, newArray, index, newItemCount);

        return newArray;
    }

    public static float[] append(final float[] array, final float value) {
        if (isNullOrEmpty(array)) {
            return new float[] { value };
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, value);
    }

    public static float[] append(final float[] array, final float... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, values);
    }

    public static float[] prepend(final float[] array, final float value) {
        if (isNullOrEmpty(array)) {
            return new float[] { value };
        }
        return insert(array, 0, value);
    }

    public static float[] prepend(final float[] array, final float... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, 0, values);
    }

    public static float[] remove(final float[] array, final int index) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length - 1, index, "index");

        if (array.length == 1) {
            return EmptyArrayCache.EmptyFloatArray;
        }

        final float[] newArray = new float[array.length - 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index - 1;

        if (remaining > 0) {
            System.arraycopy(array, index + 1, newArray, index, remaining);
        }

        return newArray;
    }

    public static float[] insert(final float[] array, final int index, final float value) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        final float[] newArray = new float[array.length + 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + 1, remaining);
        }

        newArray[index] = value;

        return newArray;
    }

    public static float[] insert(final float[] array, final int index, final float... values) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        if (values == null || values.length == 0) {
            return array;
        }

        final int newItemCount = values.length;

        if (newItemCount == 0) {
            return array;
        }

        final float[] newArray = new float[array.length + newItemCount];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + newItemCount, remaining);
        }

        System.arraycopy(values, 0, newArray, index, newItemCount);

        return newArray;
    }

    public static double[] append(final double[] array, final double value) {
        if (isNullOrEmpty(array)) {
            return new double[] { value };
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, value);
    }

    public static double[] append(final double[] array, final double... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, VerifyArgument.notNull(array, "array").length, values);
    }

    public static double[] prepend(final double[] array, final double value) {
        if (isNullOrEmpty(array)) {
            return new double[] { value };
        }
        return insert(array, 0, value);
    }

    public static double[] prepend(final double[] array, final double... values) {
        if (isNullOrEmpty(array)) {
            if (isNullOrEmpty(values)) {
                return values;
            }
            return Arrays.copyOf(values, values.length);
        }
        return insert(array, 0, values);
    }

    public static double[] remove(final double[] array, final int index) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length - 1, index, "index");

        if (array.length == 1) {
            return EmptyArrayCache.EmptyDoubleArray;
        }

        final double[] newArray = new double[array.length - 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index - 1;

        if (remaining > 0) {
            System.arraycopy(array, index + 1, newArray, index, remaining);
        }

        return newArray;
    }

    public static double[] insert(final double[] array, final int index, final double value) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        final double[] newArray = new double[array.length + 1];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + 1, remaining);
        }

        newArray[index] = value;

        return newArray;
    }

    public static double[] insert(final double[] array, final int index, final double... values) {
        VerifyArgument.notNull(array, "array");
        VerifyArgument.inRange(0, array.length, index, "index");

        if (values == null || values.length == 0) {
            return array;
        }

        final int newItemCount = values.length;

        if (newItemCount == 0) {
            return array;
        }

        final double[] newArray = new double[array.length + newItemCount];

        System.arraycopy(array, 0, newArray, 0, index);

        final int remaining = array.length - index;

        if (remaining > 0) {
            System.arraycopy(array, index, newArray, index + newItemCount, remaining);
        }

        System.arraycopy(values, 0, newArray, index, newItemCount);

        return newArray;
    }
}