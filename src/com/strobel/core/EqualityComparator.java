package com.strobel.core;

/**
 * @author Mike Strobel
 */
public interface EqualityComparator<T> {
    boolean equals(final T o1, final T o2);
    int hash(final T o);
}
