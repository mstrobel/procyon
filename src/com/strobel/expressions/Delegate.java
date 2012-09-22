package com.strobel.expressions;

import com.strobel.core.VerifyArgument;
import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.TargetInvocationException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * @author Mike Strobel
 */
public final class Delegate<T> {
    private final T _instance;
    private final MethodInfo _method;
    private MethodHandle _methodHandle;
    private MethodHandle _spreadInvoker;

    Delegate(final T instance, final MethodInfo method) {
        _instance = VerifyArgument.notNull(instance, "instance");
        _method = VerifyArgument.notNull(method, "method");
    }

    public final T getInstance() {
        return _instance;
    }

    public final MethodInfo getMethod() {
        return _method;
    }
    
    public final MethodHandle getMethodHandle() {
        if (_methodHandle == null) {
            try {
                _methodHandle = MethodHandles
                    .lookup()
                    .unreflect(_method.getRawMethod())
                    .bindTo(_instance);
            }
            catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not resolve method handle.");
            }
        }
        return _methodHandle;
    }

    public final Object invokeDynamic(final Object... args) throws TargetInvocationException {
        try {
            if (_spreadInvoker == null) {
                final MethodHandle methodHandle = getMethodHandle();
                _spreadInvoker = methodHandle.asSpreader(Object[].class, _method.getParameters().size());
            }
            return _spreadInvoker.invoke(args);
        }
        catch (Throwable t) {
            throw new TargetInvocationException(t);
        }
    }
}
