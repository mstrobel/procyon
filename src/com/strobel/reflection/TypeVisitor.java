package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("ALL")
public class TypeVisitor<P, R> {
    public final R visit(final Type<?> type, final P parameter) {
        return type.accept(this, parameter);
    }

    public R visitType(final Type<?> type, final P parameter) {
        return null;
    }

    public R visitPrimitiveType(final Type<?> type, final P parameter) {
        return null;
    }

    public R visitTypeParameter(final Type<?> type, final P parameter) {
        return null;
    }

    public R visitWildcard(final Type<?> type, final P parameter) {
        return null;
    }

    public R visitArrayType(final Type<?> type, final P parameter) {
        return null;
    }

    public R visitUnknown(final Type<?> type, final P parameter) {
        return null;
    }
}
