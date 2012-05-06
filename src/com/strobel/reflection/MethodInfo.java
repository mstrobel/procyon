package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public abstract class MethodInfo extends MethodBase {
    public abstract boolean isStatic();
    public abstract Type getReturnType();
}
