package com.strobel.expressions;

import com.strobel.core.VerifyArgument;
import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.TargetInvocationException;

/**
 * @author Mike Strobel
 */
public final class Delegate<T> {
    private final T _instance;
    private final MethodInfo _method;

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
        return _method.invoke(_instance, args);
    }
}
