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
    public TypeReference getDeclaringType() {
        return _genericTypeDefinition.getDeclaringType();
    }

    @Override
    public long getFlags() {
        return _genericTypeDefinition.getFlags();
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
