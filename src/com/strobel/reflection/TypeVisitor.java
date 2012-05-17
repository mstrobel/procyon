package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("ALL")
public class TypeVisitor<P, R> {
    public R visit(final Type<?> type) {
        return visit(type, null);
    }

    public final R visit(final Type<?> type, final P parameter) {
        return type.accept(this, parameter);
    }

    public R visitClassType(final Type<?> type, final P parameter) {
       return null;
    }

    public R visitPrimitiveType(final Type<?> type, final P parameter) {
        return null;
    }

    public R visitTypeParameter(final Type<?> type, final P parameter) {
        return null;
    }

    public R visitWildcardType(final Type<?> type, final P parameter) {
        return null;
    }

    public R visitArrayType(final Type<?> type, final P parameter) {
        return null;
    }

    public R visitType(final Type<?> type, final P parameter) {
        return null;
    }

    public R visitCapturedType(final Type<?> type, final P parameter) {
        return null;
    }
}

