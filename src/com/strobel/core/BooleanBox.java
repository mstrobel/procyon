package com.strobel.core;

@SuppressWarnings("PublicField")
public final class BooleanBox implements IStrongBox {
    public boolean value;

    public BooleanBox() {}

    public BooleanBox(final boolean value) {
        this.value = value;
    }

    @Override
    public Boolean get() {
        return this.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(final Object value) {
        this.value = (Boolean) value;
    }
}
