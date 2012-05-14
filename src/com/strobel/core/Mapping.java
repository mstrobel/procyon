package com.strobel.core;

/**
 * @author Mike Strobel
 */
public abstract class Mapping<T> {
    private final String _name;

    protected Mapping() {
        this(null);
    }

    protected Mapping(final String name) {
        _name = name;
    }

    public abstract T apply(final T t);

    @Override
    public String toString() {
        if (_name != null) {
            return _name;
        }
        return super.toString();
    }
}