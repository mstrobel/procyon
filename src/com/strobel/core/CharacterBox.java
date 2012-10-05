package com.strobel.core;

@SuppressWarnings("PublicField")
public final class CharacterBox implements IStrongBox {
    public char value;

    public CharacterBox() {}

    public CharacterBox(final char value) {
        this.value = value;
    }

    @Override
    public Character get() {
        return this.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(final Object value) {
        this.value = (Character) value;
    }
}
