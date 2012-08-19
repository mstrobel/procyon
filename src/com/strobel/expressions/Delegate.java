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

    public final Object invokeDynamic(final Object... args) throws TargetInvocationException {
        try {
            if (_methodHandle == null) {
                _methodHandle = MethodHandles
                    .lookup()
                    .unreflect(_method.getRawMethod())
                    .bindTo(_instance)
                    .asSpreader(Object[].class, _method.getParameters().size());
            }
            return _methodHandle.invoke(args);
        }
        catch (Throwable t) {
            throw new TargetInvocationException(t);
        }
    }
}
