package com.strobel.assembler.metadata;

import com.strobel.core.VerifyArgument;

import java.util.List;

final class GenericMethodInstance extends MethodReference implements IGenericInstance {
    private final MethodReference _genericDefinition;
    private final TypeReference _returnType;
    private final ParameterDefinitionCollection _parameters;
    private final List<TypeReference> _typeArguments;

    private TypeReference _declaringType;

    GenericMethodInstance(
        final TypeReference declaringType,
        final MethodReference definition,
        final TypeReference returnType,
        final List<ParameterDefinition> parameters,
        final List<TypeReference> typeArguments) {

        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _genericDefinition = VerifyArgument.notNull(definition, "definition");
        _returnType = VerifyArgument.notNull(returnType, "returnType");
        _parameters = new ParameterDefinitionCollection(this);
        _typeArguments = VerifyArgument.notNull(typeArguments, "typeArguments");

        _parameters.addAll(VerifyArgument.notNull(parameters, "parameters"));
        _parameters.freeze();
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
    public boolean isGenericMethod() {
        return hasTypeArguments();
    }

    @Override
    public MethodDefinition resolve() {
        return _genericDefinition.resolve();
    }

    @Override
    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        return _genericDefinition.appendErasedSignature(sb);
    }

    @Override
    public final TypeReference getDeclaringType() {
        return _declaringType;
    }

    final void setDeclaringType(final TypeReference declaringType) {
        _declaringType = declaringType;
    }

    @Override
    public final String getName() {
        return _genericDefinition.getName();
    }
}
