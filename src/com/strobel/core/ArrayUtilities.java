package com.strobel.core;

/**
 * @author Mike Strobel
 */
public final class ArrayUtilities {
    private ArrayUtilities() {}

    public static <T> boolean contains(final T[] array, final T item) {
        return indexOf(array, item) != -1;
    }

    public static <T> int indexOf(final T[] array, final T item) {
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
}
