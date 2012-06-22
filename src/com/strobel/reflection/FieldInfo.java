package com.strobel.reflection;

import com.strobel.core.VerifyArgument;
import com.sun.tools.javac.code.Flags;

import java.lang.reflect.Field;

/**
 * @author Mike Strobel
 */
public abstract class FieldInfo extends MemberInfo {
    public abstract Type getFieldType();
    public abstract boolean isEnumConstant();

    public abstract Field getRawField();

    @Override
    public final MemberType getMemberType() {
        return MemberType.Field;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    public String getSignature() {
        return appendSignature(new StringBuilder()).toString();
    }

    public String getErasedSignature() {
        return appendErasedSignature(new StringBuilder()).toString();
    }

    public String getDescription() {
        return appendDescription(new StringBuilder()).toString();
    }

    public String getErasedDescription() {
        return appendErasedDescription(new StringBuilder()).toString();
    }

    public StringBuilder appendDescription(final StringBuilder sb) {
        StringBuilder s = sb;

        for (final javax.lang.model.element.Modifier modifier : Flags.asModifierSet(getModifiers())) {
            s.append(modifier.toString());
            s.append(' ');
        }

        s = getFieldType().appendBriefDescription(s);

        s.append(' ');
        s.append(getName());

        return s;
    }

    public StringBuilder appendErasedDescription(final StringBuilder sb) {

        for (final javax.lang.model.element.Modifier modifier : Flags.asModifierSet(getModifiers())) {
            sb.append(modifier.toString());
            sb.append(' ');
        }

        sb.append(getFieldType().getErasedClass().getName());
        sb.append(' ');
        sb.append(getName());

        return sb;
    }

    public StringBuilder appendSignature(final StringBuilder sb) {
        return getFieldType().appendSignature(sb);
    }

    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        return getFieldType().appendErasedSignature(sb);
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
    public Field getRawField() {
        return _rawField;
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
    public int getModifiers() {
        return _rawField.getModifiers();
    }
}
