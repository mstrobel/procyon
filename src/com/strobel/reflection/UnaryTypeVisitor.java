package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public abstract class UnaryTypeVisitor<R> extends SimpleVisitor<Void, R> {
    public final R visit(final Type<?> t) {
        return t.accept(this, null);
    }
}
