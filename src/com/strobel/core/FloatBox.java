package com.strobel.core;

@SuppressWarnings("PublicField")
public final class FloatBox implements IStrongBox {
    public float value;

    public FloatBox() {}

    public FloatBox(final float value) {
        this.value = value;
    }

    @Override
    public Float get() {
        return this.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(final Object value) {
        this.value = (Float) value;
    }
}
