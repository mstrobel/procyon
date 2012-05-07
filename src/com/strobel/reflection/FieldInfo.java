package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.lang.reflect.Field;

/**
 * @author Mike Strobel
 */
public abstract class FieldInfo extends MemberInfo {
    public abstract Type getFieldType();
    public abstract boolean isEnumConstant();

    @Override
    public final MemberType getMemberType() {
        return MemberType.Field;
    }
}

class ReflectedField extends FieldInfo {
    private final Type _declaringType;
    private final Field _rawField;
    private final Type _fieldType;

    ReflectedField(final Type declaringType, final Field rawField, final Type fieldType) {
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _rawField = VerifyArgument.notNull(rawField, "rawField");
        _fieldType = VerifyArgument.notNull(fieldType, "fieldType");
    }

    @Override
    public Type getFieldType() {
        return _fieldType;
    }

    @Override
    public boolean isEnumConstant() {
        return _rawField.isEnumConstant();
    }

    @Override
    public String getName() {
        return _rawField.getName();
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    int getModifiers() {
        return _rawField.getModifiers();
    }
}
