package com.strobel.reflection;

public abstract class SimpleVisitor<P, R> extends DefaultTypeVisitor<P, R> {
    @Override
    public R visitCapturedType(final Type<?> t, final P s) {
        return visitTypeParameter(t, s);
    }
}
