package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * @author Mike Strobel
 */
public final class DynamicMethod extends MethodInfo {
    private static final Method INVOKE_EXACT;
    private static final Method INVOKE;

    static {
        try {
            INVOKE = MethodHandle.class.getMethod("invoke", Object[].class);
            INVOKE_EXACT = MethodHandle.class.getMethod("invokeExact", Object[].class);
        }
        catch (NoSuchMethodException e) {
            throw Error.targetInvocationException(e);
        }
    }

    public static DynamicMethod invoke(final MethodHandle methodHandle) {
        return new DynamicMethod(methodHandle.type(), INVOKE);
    }

    public static DynamicMethod invokeExact(final MethodHandle methodHandle) {
        return new DynamicMethod(methodHandle.type(), INVOKE_EXACT);
    }

    private final Type<?> _returnType;
    private final ParameterList _parameters;
    private final Method _invokeMethod;

    private DynamicMethod(final MethodType methodType, final Method invokeMethod) {
        _returnType = Type.of(VerifyArgument.notNull(methodType, "methodType").returnType());
        _invokeMethod = VerifyArgument.notNull(invokeMethod, "invokeMethod");

        final ParameterInfo[] parameters = new ParameterInfo[methodType.parameterCount()];

        for (int i = 0, n = parameters.length; i < n; i++) {
            parameters[i] = new ParameterInfo(
                "p" + i,
                i,
                Type.of(methodType.parameterType(i))
            );
        }

        _parameters = new ParameterList(parameters);
    }

    @Override
    public Type getReturnType() {
        return _returnType;
    }

    @Override
    public Method getRawMethod() {
        return _invokeMethod;
    }

    @Override
    public Type getDeclaringType() {
        return Types.MethodHandle;
    }

    @Override
    public int getModifiers() {
        return _invokeMethod.getModifiers();
    }

    @Override
    public ParameterList getParameters() {
        return _parameters;
    }
}
