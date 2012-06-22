package com.strobel.reflection.emit;

import com.strobel.reflection.LocalVariableInfo;
import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.Type;

/**
 * @author strobelm
 */
public final class LocalBuilder extends LocalVariableInfo {
    private final int _localIndex;
    private final Type _localType;
    private final MethodInfo _methodBuilder;

    public LocalBuilder(final int localIndex, final Type localType, final MethodInfo methodBuilder) {
        _localIndex = localIndex;
        _localType = localType;
        _methodBuilder = methodBuilder;
    }

    @Override
    public int getLocalIndex() {
        return _localIndex;
    }

    @Override
    public Type<?> getLocalType() {
        return _localType;
    }

    MethodInfo getMethodBuilder() {
        return _methodBuilder;
    }
}
