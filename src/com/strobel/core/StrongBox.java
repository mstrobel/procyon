package com.strobel.core;

/**
 * @author Mike Strobel
 */
public final class StrongBox<T> {
    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(final T value) {
        this.value = value;
    }
}
