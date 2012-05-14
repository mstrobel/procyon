package com.strobel.core;

/**
 * @author Mike Strobel
 */
public interface IEqualityComparator<T> {
    boolean equals(final T o1, final T o2);
    int hash(final T o);
}
