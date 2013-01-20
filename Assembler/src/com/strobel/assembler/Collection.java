package com.strobel.assembler;

import com.strobel.core.VerifyArgument;

import java.util.AbstractList;
import java.util.ArrayList;

/**
 * @author Mike Strobel
 */
public class Collection<E> extends AbstractList<E> {
    private final ArrayList<E> _items;

    public Collection() {
        _items = new ArrayList<>();
    }

    @Override
    public final int size() {
        return _items.size();
    }

    @Override
    public final E get(final int index) {
        return _items.get(index);
    }

    @Override
    public final boolean add(final E e) {
        add(size(), e);
        return true;
    }

    @Override
    public final E set(final int index, final E element) {
        VerifyArgument.notNull(element, "element");
        beforeSet(index, element);
        _items.set(index, element);
        return super.set(index, element);
    }

    @Override
    public final void add(final int index, final E element) {
        VerifyArgument.notNull(element, "element");
        final boolean append = index == size();
        _items.add(index, element);
        afterAdd(index, element, append);
    }

    @Override
    public final E remove(final int index) {
        final E e = _items.remove(index);
        if (e != null) {
            afterRemove(index, e);
        }
        return e;
    }

    @Override
    public final void clear() {
        beforeClear();
        super.clear();
    }

    @Override
    public final boolean remove(final Object o) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final int index = _items.indexOf(o);

        if (index < 0) {
            return false;
        }

        return remove(index) != null;
    }

    protected void afterAdd(final int index, final E e, final boolean appended) {}
    protected void beforeSet(final int index, final E e) {}
    protected void afterRemove(final int index, final E e) {}
    protected void beforeClear() {}
}
