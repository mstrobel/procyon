package com.strobel.core;

/**
 * @author Mike Strobel
 */
public final class StrongBox<T> {
    private T _value;

    public T getValue() {
        return _value;
    }

    public void setValue(final T value) {
        this._value = value;
    }
}
