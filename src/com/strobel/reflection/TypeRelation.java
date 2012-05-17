package com.strobel.reflection;

public abstract class TypeRelation extends SimpleVisitor<Type<?>, Boolean> {
    public final Boolean visit(final TypeList types, final Type<?> p) {
        for (int i = 0, n = types.size(); i < n; i++) {
            if (visit(types.get(i), p)) {
                return true;
            }
        }
        return false;
    }
}
