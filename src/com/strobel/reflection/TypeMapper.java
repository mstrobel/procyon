package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public abstract class TypeMapper<T> extends DefaultTypeVisitor<T, Type<?>> {
    @Override
    public Type<?> visitType(final Type<?> type, final T parameter) {
        return type;
    }

    public TypeList visit(final TypeList types, final T parameter) {
        Type<?>[] newTypes = null;

        for (int i = 0, n = types.size(); i < n; i++) {
            final Type oldType = types.get(i);
            final Type newType = visit(oldType, parameter);
            if (newType != oldType) {
                if (newTypes == null) {
                    newTypes = types.toArray();
                }
                newTypes[i] = newType;
            }
        }

        if (newTypes != null) {
            return Type.list(newTypes);
        }

        return types;
    }

    public TypeList visit(final TypeList types) {
        return visit(types, null);
    }
}
