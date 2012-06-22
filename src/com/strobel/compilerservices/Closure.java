package com.strobel.compilerservices;

/**
 * Represents the runtime state of a dynamically generated method.
 *
 * @author strobelm
 */
@SuppressWarnings("PublicField")
public final class Closure {

    /**
     * Represents the non-trivial constants and locally executable expressions that are
     * referenced by a dynamically generated method.
     */
    public final Object[] constants;

    /**
     * Represents the hoisted local variables from the parent context.
     */
    public final Object[] locals;

    /**
     * Creates an object to hold state of a dynamically generated method.
     *
     * @param constants The constant values used by the method.
     * @param locals    The hoisted local variables from the parent context.
     */
    public Closure(final Object[] constants, final Object[] locals) {
        this.constants = constants;
        this.locals = locals;
    }
} 
