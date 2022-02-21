package com.strobel.compilerservices;

import com.strobel.util.ContractUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class UnsafeAccess {
    private UnsafeAccess() {
        throw ContractUtils.unreachable();
    }

    private static Object _unsafe;

    public static Object unsafe() {
        if (_unsafe != null) {
            return _unsafe;
        }

        try {
            final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");

            try {
                final Method getUnsafe = unsafeClass.getDeclaredMethod("getUnsafe");
                _unsafe = getUnsafe.invoke(null);
                return _unsafe;
            }
            catch (final Throwable ignored) {
            }

            final Field instanceField = unsafeClass.getDeclaredField("theUnsafe");
            instanceField.setAccessible(true);
            _unsafe = instanceField.get(null);
        }
        catch (final Throwable t) {
            return new IllegalStateException("Could not load an instance of the sun.misc.Unsafe class.");
        }

        return _unsafe;
    }
}
