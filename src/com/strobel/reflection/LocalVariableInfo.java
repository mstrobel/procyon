package com.strobel.reflection;

/**
 * @author strobelm
 */
public abstract class LocalVariableInfo {
    protected LocalVariableInfo() {}

    public abstract Type<?> getLocalType();
    public abstract int getLocalIndex();

    @Override
    public String toString() {
        return getLocalType().getBriefDescription();
    }
}
