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

    public static <T> boolean referenceEquals(final T o1, final T o2) {
        return o1 == o2;
    }

    public static <T extends Comparable<? super T>> int compare(final T o1, final T o2) {
        if (o1 == null) {
            return o2 == null ? 0 : -1;
        }
        return o1.compareTo(o2);
    }

    @SuppressWarnings({ "unchecked" })
    public static int compare(final Object a, final Object b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        final Class<?> aClass = a.getClass();
        final Class<?> bClass = b.getClass();

        if (Comparable.class.isInstance(a) && aClass.isAssignableFrom(bClass)) {
            return ((Comparable<Object>)a).compareTo(b);
        }

        if (Comparable.class.isInstance(b) && bClass.isAssignableFrom(aClass)) {
            return ((Comparable<Object>)b).compareTo(a);
        }

        throw new IllegalArgumentException("Values must be comparable.");
    }
}
