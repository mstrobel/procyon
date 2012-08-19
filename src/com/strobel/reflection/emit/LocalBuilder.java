package com.strobel.reflection.emit;

import com.strobel.reflection.LocalVariableInfo;
import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.Type;

/**
 * @author strobelm
 */
@SuppressWarnings("PackageVisibleField")
public final class LocalBuilder extends LocalVariableInfo {
    private final int _localIndex;
    private final String _name;
    private final Type _localType;
    private final MethodInfo _methodBuilder;

    int startOffset = -1;
    int endOffset = -1;

    public LocalBuilder(final int localIndex, final String name, final Type localType, final MethodInfo methodBuilder) {
        _localIndex = localIndex;
        _name = name != null ? name : "$" + localIndex;
        _localType = localType;
        _methodBuilder = methodBuilder;
    }

    @Override
    public int getLocalIndex() {
        return _localIndex;
    }

    public String getName() {
        return _name;
    }

    @Override
    public Type<?> getLocalType() {
        return _localType;
    }

    MethodInfo getMethodBuilder() {
        return _methodBuilder;
    }

    @Override
    public String toString() {
        return _localType.getBriefDescription() + " " + _name;
    }
}
