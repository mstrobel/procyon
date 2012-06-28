package com.strobel.core;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("PublicField")
public final class StrongBox<T> {
    public T value;

    public StrongBox() {}

    public StrongBox(final T value) {
        this.value = value;
    }
}
