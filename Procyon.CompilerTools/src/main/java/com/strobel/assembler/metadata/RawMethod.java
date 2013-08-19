package com.strobel.assembler.metadata;

import com.strobel.core.VerifyArgument;

import java.util.Collections;
import java.util.List;

public final class RawMethod extends MethodReference implements IGenericInstance {
    private final MethodReference _baseMethod;
    private final TypeReference _returnType;
    private final ParameterDefinitionCollection _parameters;

    private TypeReference _declaringType;

    public RawMethod(final MethodReference baseMethod) {
        VerifyArgument.notNull(baseMethod, "baseMethod");

        final TypeReference declaringType = baseMethod.getDeclaringType();

        _baseMethod = baseMethod;
        _declaringType = MetadataHelper.eraseRecursive(declaringType);
        _returnType = MetadataHelper.eraseRecursive(baseMethod.getReturnType());
        _parameters = new ParameterDefinitionCollection(this);

        for (final ParameterDefinition parameter : baseMethod.getParameters()) {
            if (parameter.hasName()) {
                _parameters.add(
                    new ParameterDefinition(
                        parameter.getSlot(),
                        parameter.getName(),
                        MetadataHelper.eraseRecursive(parameter.getParameterType())
                    )
                );
            }
            else {
                _parameters.add(
                    new ParameterDefinition(
                        parameter.getSlot(),
                        MetadataHelper.eraseRecursive(parameter.getParameterType())
                    )
                );
            }
        }

        _parameters.freeze();
    }

    public final MethodReference getBaseMethod() {
        return _baseMethod;
    }

    @Override
    public final boolean hasTypeArguments() {
        return false;
    }

    @Override
    public final List<TypeReference> getTypeArguments() {
        return Collections.emptyList();
    }

    @Override
    public final IGenericParameterProvider getGenericDefinition() {
        return (_baseMethod instanceof IGenericInstance) ? ((IGenericInstance) _baseMethod).getGenericDefinition()
                                                         : null;
    }

    @Override
    public final List<GenericParameter> getGenericParameters() {
        return Collections.emptyList();
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
        return _baseMethod.resolve();
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
        return _baseMethod.getName();
    }
}
