package com.strobel.core;

@SuppressWarnings("PublicField")
public final class LongBox implements IStrongBox {
    public long value;

    public LongBox() {}

    public LongBox(final long value) {
        this.value = value;
    }

    @Override
    public Long get() {
        return this.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(final Object value) {
        this.value = (Long) value;
    }
}
