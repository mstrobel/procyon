package com.strobel.assembler.metadata;

import java.util.List;

/**
 * @author Mike Strobel
 */
final class ObjectType extends TypeDefinition {
    @Override
    public List<FieldDefinition> getDeclaredFields() {
        return null;
    }

    @Override
    public List<MethodDefinition> getDeclaredMethods() {
        return null;
    }

    @Override
    public List<TypeDefinition> getDeclaredTypes() {
        return null;
    }

    @Override
    public TypeReference getDeclaringType() {
        return null;
    }

    @Override
    public long getFlags() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }
}
