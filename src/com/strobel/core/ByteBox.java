package com.strobel.core;

@SuppressWarnings("PublicField")
public final class ByteBox implements IStrongBox {
    public byte value;

    public ByteBox() {}

    public ByteBox(final byte value) {
        this.value = value;
    }

    @Override
    public Byte get() {
        return this.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(final Object value) {
        this.value = (Byte) value;
    }
}
