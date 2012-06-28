package com.strobel.reflection;

import com.strobel.core.ArrayUtilities;
import com.strobel.reflection.emit.BytecodeGenerator;
import com.strobel.reflection.emit.TypeBuilder;
import com.strobel.util.ContractUtils;

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
    
    public void setReturnType(final Type<?> type) {
        throw ContractUtils.unreachable();
    }
    
    public void setParameterTypes(final Type<?>... types) {
        if (ArrayUtilities.isNullOrEmpty(types)) {
            setParameters(TypeList.empty());
        }
        else {
            setParameters(Type.list(types));
        }
    }

    public void setParameters(final TypeList types) {
        throw ContractUtils.unreachable();
    }
    
    public void defineParameter(final int position, final String name) {
        throw ContractUtils.unreachable();
    }
}
