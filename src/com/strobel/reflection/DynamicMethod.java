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
        return new DynamicMethod(methodHandle, INVOKE);
    }

    public static DynamicMethod invokeExact(final MethodHandle methodHandle) {
        return new DynamicMethod(methodHandle, INVOKE_EXACT);
    }

    private final Type<?> _returnType;
    private final ParameterList _parameters;
    private final Method _invokeMethod;
    private final MethodHandle _methodHandle;

    private DynamicMethod(final MethodHandle methodHandle, final Method invokeMethod) {
        _methodHandle = VerifyArgument.notNull(methodHandle, "methodHandle");
        _invokeMethod = VerifyArgument.notNull(invokeMethod, "invokeMethod");
        
        final MethodType methodType = methodHandle.type();
        
        _returnType = Type.of(methodType.returnType());

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

    public MethodHandle getHandle() {
        return _methodHandle;
    }

    @Override
    public Type<?> getReturnType() {
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
