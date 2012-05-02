package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public abstract class TypeContext {
    public final static TypeContext SYSTEM = new SystemTypeContext();

    public abstract ClassLoader getClassLoader();
}

class SystemTypeContext extends TypeContext {

    private final ClassLoader _systemClassLoader;

    public SystemTypeContext() {
        _systemClassLoader = ClassLoader.getSystemClassLoader();
    }

    @Override
    public ClassLoader getClassLoader() {
        return _systemClassLoader;
    }
}