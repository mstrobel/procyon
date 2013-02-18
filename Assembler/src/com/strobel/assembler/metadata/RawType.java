package com.strobel.assembler.metadata;

import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class RawType extends TypeReference {
    private final TypeReference _genericTypeDefinition;

    public RawType(final TypeReference genericTypeDefinition) {
        _genericTypeDefinition = VerifyArgument.notNull(genericTypeDefinition, "genericTypeDefinition");
    }

    @Override
    public String getFullName() {
        return _genericTypeDefinition.getFullName();
    }

    @Override
    public String getInternalName() {
        return _genericTypeDefinition.getInternalName();
    }

    @Override
    public TypeReference getDeclaringType() {
        return _genericTypeDefinition.getDeclaringType();
    }

    @Override
    public String getSimpleName() {
        return _genericTypeDefinition.getSimpleName();
    }

    @Override
    public String getName() {
        return _genericTypeDefinition.getName();
    }

    @Override
    public TypeReference getUnderlyingType() {
        return _genericTypeDefinition;
    }
}
