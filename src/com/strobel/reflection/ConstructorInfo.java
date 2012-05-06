package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.lang.reflect.Constructor;

/**
 * @author Mike Strobel
 */
public abstract class ConstructorInfo extends MethodBase {
    @Override
    public final MemberType getMemberType() {
        return MemberType.Constructor;
    }
}

class ReflectedConstructor extends ConstructorInfo {
    private final Type _declaringType;
    private final ParameterList _parameters;
    private final Constructor _rawConstructor;

    ReflectedConstructor(final Type declaringType, final Constructor rawConstructor, final ParameterList parameters) {
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _rawConstructor = VerifyArgument.notNull(rawConstructor, "rawConstructor");
        _parameters = VerifyArgument.notNull(parameters, "parameters");
    }

    @Override
    public ParameterList getParameters() {
        return _parameters;
    }

    @Override
    public String getName() {
        return _rawConstructor.getName();
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    int getModifiers() {
        return _rawConstructor.getModifiers();
    }
}
