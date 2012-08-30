package com.strobel.core;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("PublicField")
public final class StrongBox<T> implements IStrongBox {
    public T value;

    public StrongBox() {}

    public StrongBox(final T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return this.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(final Object value) {
        this.value = (T) value;
    }
}
