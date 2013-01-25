package com.strobel.assembler.metadata;

import java.util.List;

/**
 * @author Mike Strobel
 */
final class ParameterizedType extends TypeReference implements IGenericInstance {
    private final TypeReference _genericDefinition;
    private final List<TypeReference> _typeParameters;

    ParameterizedType(final TypeReference genericDefinition, final List<TypeReference> typeParameters) {
        _genericDefinition = genericDefinition;
        _typeParameters = typeParameters;
    }

    @Override
    public String getName() {
        return _genericDefinition.getName();
    }

    @Override
    public String getPackageName() {
        return _genericDefinition.getPackageName();
    }

    @Override
    public TypeReference getDeclaringType() {
        return _genericDefinition.getDeclaringType();
    }

    @Override
    public long getFlags() {
        return _genericDefinition.getFlags();
    }

    @Override
    public boolean isGenericDefinition() {
        return false;
    }

    @Override
    public List<GenericParameter> getGenericParameters() {
        return _genericDefinition.getGenericParameters();
    }

    @Override
    public boolean hasTypeArguments() {
        return true;
    }

    @Override
    public List<TypeReference> getTypeArguments() {
        return _typeParameters;
    }

    @Override
    public IGenericParameterProvider getGenericDefinition() {
        return _genericDefinition;
    }

    @Override
    public TypeReference getUnderlyingType() {
        return _genericDefinition;
    }
}
