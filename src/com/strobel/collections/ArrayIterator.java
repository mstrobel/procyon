package com.strobel.collections;

import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;

import java.util.Iterator;

/**
 * @author strobelm
 */
public final class ArrayIterator<E> implements Iterator<E> {
    private final E[] _elements;
    private int _index;

    public ArrayIterator(final E[] elements) {
        _elements = VerifyArgument.notNull(elements, "elements");
    }

    @Override
    public boolean hasNext() {
        return _index < _elements.length;
    }

    @Override
    public E next() {
        return _elements[_index++];
    }

    @Override
    public void remove() {
        throw ContractUtils.unsupported();
    }
}
