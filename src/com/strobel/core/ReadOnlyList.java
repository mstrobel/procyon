package com.strobel.core;

import java.util.*;

/**
 * @author Mike Strobel
 */
public class ReadOnlyList<T> implements IReadOnlyList<T>, List<T>, RandomAccess {
    private final int _offset;
    private final int _length;
    private final T[] _elements;

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public ReadOnlyList(final T... elements) {
        VerifyArgument.notNull(elements, "elements");

        _offset = 0;
        _length = elements.length;
        _elements = (T[])Arrays.copyOf(elements, elements.length, elements.getClass());
    }

    @SuppressWarnings("unchecked")
    public ReadOnlyList(final T[] elements, final int offset, final int length) {
        VerifyArgument.notNull(elements, "elements");

        _elements = (T[])Arrays.copyOf(elements, elements.length, elements.getClass());

        subListRangeCheck(offset, offset + length, _elements.length);

        _offset = offset;
        _length = length;
    }

    private ReadOnlyList(final ReadOnlyList<T> baseList, final int offset, final int length) {
        VerifyArgument.notNull(baseList, "baseList");

        final T[] elements = baseList._elements;

        subListRangeCheck(offset, offset + length, elements.length);

        _elements = elements;
        _offset = offset;
        _length = length;
    }

    @Override
    public final int size() {
        return _length - _offset;
    }

    @Override
    public final boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsAll(final Iterable<? extends T> c) {
        VerifyArgument.notNull(c, "c");
        for (final T element : c) {
            if (!ArrayUtilities.contains(_elements, element)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final boolean contains(final Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public final Iterator<T> iterator() {
        return new ReadOnlyCollectionIterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final T[] toArray() {
        return (T[])Arrays.copyOfRange(_elements, _offset, _length, _elements.getClass());
    }

    @Override
    @SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
    public final <T> T[] toArray(final T[] a) {
        final int length = _length;

        if (a.length < length) {
            return (T[])Arrays.copyOfRange(_elements, _offset, _length, _elements.getClass());
        }

        System.arraycopy(_elements, _offset, a, 0, length);

        if (a.length > length) {
            a[length] = null;
        }

        return a;
    }

    @Override
    public final boolean add(final T T) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public final boolean remove(final Object o) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public final boolean containsAll(final Collection<?> c) {
        for (final Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final boolean addAll(final Collection<? extends T> c) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public final boolean addAll(final int index, final Collection<? extends T> c) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public final boolean removeAll(final Collection<?> c) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public final boolean retainAll(final Collection<?> c) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public final void clear() {
        throw Error.unmodifiableCollection();
    }

    @Override
    public final T get(final int index) {
        return _elements[_offset + index];
    }

    @Override
    public final T set(final int index, final T element) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public final void add(final int index, final T element) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public final T remove(final int index) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public final int indexOf(final Object o) {
        final T[] elements = _elements;
        final int start = _offset;
        final int end = start + _length;

        if (o == null) {
            for (int i = start; i < end; i++) {
                if (elements[i] == null) {
                    return i;
                }
            }
        }
        else {
            for (int i = start; i < end; i++) {
                if (o.equals(elements[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public final int lastIndexOf(final Object o) {
        final T[] elements = _elements;
        final int start = _offset;
        final int end = start + _length;

        if (o == null) {
            for (int i = end - 1; i >= start; i--) {
                if (elements[i] == null) {
                    return i;
                }
            }
        }
        else {
            for (int i = end - 1; i >= start; i--) {
                if (o.equals(elements[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public final ListIterator<T> listIterator() {
        return new ReadOnlyCollectionIterator();
    }

    @Override
    public final ListIterator<T> listIterator(final int index) {
        return new ReadOnlyCollectionIterator(index);
    }

    protected static void subListRangeCheck(final int fromIndex, final int toIndex, final int size) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        }
        if (toIndex > size) {
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
    }

    @Override
    public final List<T> subList(final int fromIndex, final int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size());
        return new ReadOnlyList<>(this, _offset + fromIndex, _offset + toIndex);
    }

    private final class ReadOnlyCollectionIterator implements ListIterator<T> {
        private int _position = -1;

        ReadOnlyCollectionIterator() {}

        ReadOnlyCollectionIterator(final int startPosition) {
            if (startPosition < -1 || startPosition >= size()) {
                throw new IllegalArgumentException();
            }
            _position = startPosition;
        }

        @Override
        public final boolean hasNext() {
            return _position + 1 < size();
        }

        @Override
        public final T next() {
            if (!hasNext()) {
                throw new IllegalStateException();
            }
            return get(++_position);
        }

        @Override
        public final boolean hasPrevious() {
            return _position > 0;
        }

        @Override
        public final T previous() {
            if (!hasPrevious()) {
                throw new IllegalStateException();
            }
            return get(--_position);
        }

        @Override
        public final int nextIndex() {
            return _position + 1;
        }

        @Override
        public final int previousIndex() {
            return _position + 1;
        }

        @Override
        public final void remove() {
            throw Error.unmodifiableCollection();
        }

        @Override
        public final void set(final T T) {
            throw Error.unmodifiableCollection();
        }

        @Override
        public final void add(final T T) {
            throw Error.unmodifiableCollection();
        }
    }
}
