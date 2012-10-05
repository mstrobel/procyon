package com.strobel.core;

@SuppressWarnings("PublicField")
public final class ShortBox implements IStrongBox {
    public short value;

    public ShortBox() {}

    public ShortBox(final short value) {
        this.value = value;
    }

    @Override
    public Short get() {
        return this.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(final Object value) {
        this.value = (Short) value;
    }
}
