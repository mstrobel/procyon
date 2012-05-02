package com.strobel.expressions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Mike Strobel
 */
public abstract class Delegate {
    private final Object _target;
    private final Method _method;

    Delegate(final Object target, final Method method) {
        _target = target;
        _method = method;
    }

    public final Object invoke(final Object... args)
        throws InvocationTargetException {
        try {
            return _method.invoke(_target, args);
        }
        catch (IllegalAccessException ignored) {
            throw new InvocationTargetException(ignored);
        }
    }
}
