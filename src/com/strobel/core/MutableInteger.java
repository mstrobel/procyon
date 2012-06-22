package com.strobel.core;

/**
 * @author strobelm
 */
public final class MutableInteger {
    private int _value;

    public MutableInteger() {}

    public MutableInteger(final int value) {
        _value = value;
    }

    public int getValue() {
        return _value;
    }

    public void setValue(final int value) {
        _value = value;
    }
    
    public MutableInteger increment() {
        ++_value;
        return this;
    }

    public MutableInteger decrement() {
        --_value;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MutableInteger that = (MutableInteger)o;

        return _value == that._value;
    }

    @Override
    public int hashCode() {
        return _value;
    }
}
