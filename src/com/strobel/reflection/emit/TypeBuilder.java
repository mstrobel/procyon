package com.strobel.reflection.emit;

import com.strobel.core.VerifyArgument;
import com.strobel.reflection.FieldInfo;
import com.strobel.reflection.MethodBase;
import com.strobel.reflection.MethodBuilder;
import com.strobel.reflection.Type;
import com.strobel.reflection.TypeList;
import com.strobel.util.ContractUtils;

import javax.lang.model.element.Modifier;
import java.util.Set;

/**
 * @author strobelm
 */
public final class TypeBuilder extends Type {
    private final ConstantPool _constantPool;

    public TypeBuilder() {
        _constantPool = new ConstantPool();
    }

    @Override
    public Type getDeclaringType() {
        return null;
    }

    public MethodBuilder defineMethod(
        final String name,
        final Set<Modifier> modifiers,
        final Type<?> returnType,
        final TypeList parameterTypes) {

        throw ContractUtils.unreachable();
    }

    @Override
    public int getModifiers() {
        return 0;
    }

    int getTypeToken(final Type<?> type) {
        VerifyArgument.notNull(type, "type");
        return _constantPool.getTypeInfo(type).index;
    }

    int getMethodToken(final MethodBase method) {
        VerifyArgument.notNull(method, "method");
        return _constantPool.getMethodReference(method).index;
    }

    int getFieldToken(final FieldInfo field) {
        VerifyArgument.notNull(field, "field");
        return _constantPool.getFieldReference(field).index;
    }

    int getConstantToken(final int value) {
        return _constantPool.getIntegerConstant(value).index;
    }

    int getConstantToken(final long value) {
        return _constantPool.getLongConstant(value).index;
    }

    int getConstantToken(final float value) {
        return _constantPool.getFloatConstant(value).index;
    }

    int getConstantToken(final double value) {
        return _constantPool.getDoubleConstant(value).index;
    }

    int getStringToken(final String value) {
        return _constantPool.getStringConstant(value).index;
    }
}
