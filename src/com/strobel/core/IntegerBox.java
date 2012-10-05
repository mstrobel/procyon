package com.strobel.core;

@SuppressWarnings("PublicField")
public final class IntegerBox implements IStrongBox {
    public int value;

    public IntegerBox() {}

    public IntegerBox(final int value) {
        this.value = value;
    }

    @Override
    public Integer get() {
        return this.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(final Object value) {
        this.value = (Integer) value;
    }
}
