package com.strobel.reflection;

import static com.strobel.reflection.Flags.all;

/**
 * @author Mike Strobel
 */
public enum CallingConvention {
    Standard(1),
    VarArgs(2),
    Any(VarArgs.mask | Standard.mask);

    private final int mask;

    CallingConvention(final int mask) {
        this.mask = mask;
    }

    public static CallingConvention fromMethodModifiers(final int modifiers) {
        if (all(modifiers, Type.VARARGS_MODIFIER)) {
            return VarArgs;
        }
        return Standard;
    }
}
