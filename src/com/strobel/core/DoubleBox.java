package com.strobel.core;

@SuppressWarnings("PublicField")
public final class DoubleBox implements IStrongBox {
    public double value;

    public DoubleBox() {}

    public DoubleBox(final double value) {
        this.value = value;
    }

    @Override
    public Double get() {
        return this.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(final Object value) {
        this.value = (Double) value;
    }
}
