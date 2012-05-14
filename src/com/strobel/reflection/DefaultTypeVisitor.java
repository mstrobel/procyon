package com.strobel.reflection;

public abstract class DefaultTypeVisitor<P, R> extends TypeVisitor<P, R> {
    @Override
    public R visitClassType(final Type<?> type, final P parameter) {
        return visitType(type, parameter);
    }

    @Override
    public R visitPrimitiveType(final Type<?> type, final P parameter) {
        return visitType(type, parameter);
    }

    @Override
    public R visitTypeParameter(final Type<?> type, final P parameter) {
        return visitType(type, parameter);
    }

    @Override
    public R visitWildcardType(final Type<?> type, final P parameter) {
        return visitType(type, parameter);
    }

    @Override
    public R visitCapturedType(final Type<?> type, final P parameter) {
        return visitType(type, parameter);
    }

    @Override
    public R visitArrayType(final Type<?> type, final P parameter) {
        return visitType(type, parameter);
    }
}
