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
        throw ContractUtils.unreachable();
    }

    int getMethodToken(final MethodBase method) {
        VerifyArgument.notNull(method, "method");
        throw ContractUtils.unreachable();
    }

    int getFieldToken(final FieldInfo field) {
        VerifyArgument.notNull(field, "field");
        throw ContractUtils.unreachable();
    }

    int getConstantToken(final int value) {
        throw ContractUtils.unreachable();
    }

    int getConstantToken(final long value) {
        throw ContractUtils.unreachable();
    }

    int getConstantToken(final float value) {
        throw ContractUtils.unreachable();
    }

    int getConstantToken(final double value) {
        throw ContractUtils.unreachable();
    }

    int getStringToken(final String value) {
        throw ContractUtils.unreachable();
    }
}
