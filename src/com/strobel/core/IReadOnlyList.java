package com.strobel.core;

import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * @author Mike Strobel
 */
public interface IReadOnlyList<T> extends Iterable<T>, RandomAccess {
    int size();

    <U extends T> int indexOf(U o);
    <U extends T> int lastIndexOf(U o);

    boolean isEmpty();
    <U extends T> boolean contains(U o);
    boolean containsAll(Iterable<? extends T> c);

    T get(int index);

    T[] toArray();
    <T> T[] toArray(T[] a);

    ListIterator<T> listIterator();
    ListIterator<T> listIterator(int index);
}
