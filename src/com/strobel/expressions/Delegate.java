package com.strobel.expressions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Mike Strobel
 */
public abstract class Delegate<T> {
    private final T _target;
    private final Method _method;

    Delegate(final T target, final Method method) {
        _target = target;
        _method = method;
    }

    public T getTarget() {
        return _target;
    }

    public final Object invokeDynamic(final Object... args)
        throws InvocationTargetException {
        try {
            return _method.invoke(_target, args);
        }
        catch (IllegalAccessException ignored) {
            throw new InvocationTargetException(ignored);
        }
    }
}
