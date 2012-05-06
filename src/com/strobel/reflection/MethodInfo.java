package com.strobel.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author Mike Strobel
 */
public abstract class MethodInfo extends MethodBase {
    public abstract boolean isStatic();
    public abstract Type getReturnType();

    @Override
    public final MemberType getMemberType() {
        return MemberType.Method;
    }

    @Override
    public StringBuilder appendDescription(final StringBuilder sb) {
        return getReturnType().appendSignature(super.appendDescription(sb));
    }

    @Override
    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        return getReturnType().appendErasedSignature(super.appendDescription(sb));
    }
}

class ReflectedMethod extends MethodInfo {
    private final Type _declaringType;
    private final Method _rawMethod;
    private final ParameterList _parameters;
    private final Type _returnType;

    ReflectedMethod(final Type declaringType, final Method rawMethod, final ParameterList parameters, final Type returnType) {
        _declaringType = declaringType;
        _rawMethod = rawMethod;
        _parameters = parameters;
        _returnType = returnType;
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    @Override
    public Type getReturnType() {
        return _returnType;
    }

    @Override
    public String getName() {
        return _rawMethod.getName();
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    int getModifiers() {
        return _rawMethod.getModifiers();
    }

    @Override
    public ParameterList getParameters() {
        return _parameters;
    }

    @Override
    public CallingConvention getCallingConvention() {
        return _rawMethod.isVarArgs() ? CallingConvention.VarArgs : CallingConvention.Standard;
    }
}