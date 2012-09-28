package com.strobel.compilerservices;

import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author strobelm
 */
public final class RuntimeHelpers {
    private RuntimeHelpers() {
        throw ContractUtils.unreachable();
    }

    public static void ensureClassInitialized(final Class<?> clazz) {
        getUnsafeInstance().ensureClassInitialized(VerifyArgument.notNull(clazz, "clazz"));
    }

    // <editor-fold defaultstate="collapsed" desc="Unsafe Access">

    private static Unsafe _unsafe;

    private static Unsafe getUnsafeInstance() {
        if (_unsafe != null) {
            return _unsafe;
        }

        try {
            _unsafe = Unsafe.getUnsafe();
        }
        catch (Throwable ignored) {
        }

        try {
            final Field instanceField = Unsafe.class.getDeclaredField("theUnsafe");
            instanceField.setAccessible(true);
            _unsafe = (Unsafe) instanceField.get(Unsafe.class);
        }
        catch (Throwable t) {
            throw new IllegalStateException(
                String.format(
                    "Could not load an instance of the %s class.",
                    Unsafe.class.getName()
                )
            );
        }

        return _unsafe;
    }

    // </editor-fold>
}
