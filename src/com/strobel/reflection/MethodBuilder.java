package com.strobel.reflection;

import com.strobel.reflection.emit.BytecodeGenerator;
import com.strobel.reflection.emit.TypeBuilder;

import java.lang.reflect.Method;

/**
 * @author Mike Strobel
 */
public final class MethodBuilder extends MethodInfo {
    private BytecodeGenerator _generator;
    private boolean _isFinished;

    public BytecodeGenerator getBytecodeGenerator() {
        return _generator;
    }

    @Override
    public Type getReturnType() {
        return null;
    }

    @Override
    public Method getRawMethod() {
        return null;
    }

    @Override
    public TypeBuilder getDeclaringType() {
        return null;
    }

    @Override
    public int getModifiers() {
        return 0;
    }

    public boolean isTypeCreated() {
        return false;
    }

    public boolean isFinished() {
        return _isFinished;
    }
}
