package com.strobel.assembler.metadata;

import com.strobel.core.ArrayUtilities;

import java.util.List;

public abstract class TypeMapper<T> extends DefaultTypeVisitor<T, TypeReference> {
    @Override
    public TypeReference visitType(final TypeReference type, final T parameter) {
        return type;
    }

    public List<? extends TypeReference> visit(final List<? extends TypeReference> types, final T parameter) {
        TypeReference[] newTypes = null;

        for (int i = 0, n = types.size(); i < n; i++) {
            final TypeReference oldType = types.get(i);
            final TypeReference newType = visit(oldType, parameter);

            if (newType != oldType) {
                if (newTypes == null) {
                    newTypes = types.toArray(new TypeReference[types.size()]);
                }
                newTypes[i] = newType;
            }
        }

        if (newTypes != null) {
            return ArrayUtilities.asUnmodifiableList(newTypes);
        }

        return types;
    }

    public List<? extends TypeReference> visit(final List<? extends TypeReference> types) {
        return visit(types, null);
    }
}
