package com.strobel.assembler.metadata;

import com.strobel.core.VerifyArgument;

import java.util.List;

final class GenericMethodInstance extends MethodReference implements IGenericInstance {
    private final MethodReference _genericDefinition;
    private final TypeReference _returnType;
    private final List<ParameterDefinition> _parameters;
    private final List<TypeReference> _typeArguments;

    GenericMethodInstance(
        final MethodReference definition,
        final TypeReference returnType,
        final List<ParameterDefinition> parameters,
        final List<TypeReference> typeArguments) {

        _genericDefinition = VerifyArgument.notNull(definition, "definition");
        _returnType = VerifyArgument.notNull(returnType, "returnType");
        _parameters = VerifyArgument.notNull(parameters, "parameters");
        _typeArguments = VerifyArgument.notNull(typeArguments, "typeArguments");
    }

    @Override
    public final boolean hasTypeArguments() {
        return !_typeArguments.isEmpty();
    }

    @Override
    public final List<TypeReference> getTypeArguments() {
        return _typeArguments;
    }

    @Override
    public final IGenericParameterProvider getGenericDefinition() {
        return _genericDefinition;
    }

    @Override
    public final TypeReference getReturnType() {
        return _returnType;
    }

    @Override
    public final List<ParameterDefinition> getParameters() {
        return _parameters;
    }

    @Override
    public final TypeReference getDeclaringType() {
        return _genericDefinition.getDeclaringType();
    }

    @Override
    public final String getName() {
        return _genericDefinition.getName();
    }
}
