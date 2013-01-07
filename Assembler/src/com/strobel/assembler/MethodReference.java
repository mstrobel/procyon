package com.strobel.assembler;

import java.util.Collections;
import java.util.List;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:29 PM
 */
public abstract class MethodReference extends MemberReference implements IMethodSignature,
                                                                         IGenericParameterProvider {

    public abstract TypeReference getReturnType();

    public boolean hasParameters() {
        return !getParameters().isEmpty();
    }

    public abstract List<ParameterDefinition> getParameters();

    public List<GenericParameter> getGenericParameters() {
        return Collections.emptyList();
    }
}

