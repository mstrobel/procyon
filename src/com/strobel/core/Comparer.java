package com.strobel.core;

/**
 * @author Mike Strobel
 */
public final class Comparer {
    private Comparer() {}

    public static <T> boolean notEqual(final T o1, final T o2) {
        return o1 == null ? o2 != null
                          : !o1.equals(o2);
    }

    public static <T> boolean equals(final T o1, final T o2) {
        return o1 == null ? o2 == null
                          : o1.equals(o2);
    }

    public static <T extends Comparable<? super T>> int compare(final T o1, final T o2) {
        if (o1 == null) {
            return o2 == null ? 0 : -1;
        }
        return o1.compareTo(o2);
    }
}
