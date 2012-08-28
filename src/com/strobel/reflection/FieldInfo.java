package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import javax.lang.model.element.Modifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * @author Mike Strobel
 */
public abstract class FieldInfo extends MemberInfo {
    public abstract Type getFieldType();
    public abstract Field getRawField();

    public boolean isEnumConstant() {
        return (getModifiers() & Type.ENUM_MODIFIER) == Type.ENUM_MODIFIER;
    }

    @Override
    public final MemberType getMemberType() {
        return MemberType.Field;
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
        return getRawField().getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return getRawField().getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getRawField().getDeclaredAnnotations();
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        return getRawField().isAnnotationPresent(annotationClass);
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public StringBuilder appendDescription(final StringBuilder sb) {
        StringBuilder s = sb;

        for (final javax.lang.model.element.Modifier modifier : Flags.asModifierSet(getModifiers())) {
            s.append(modifier.toString());
            s.append(' ');
        }

        final Type fieldType = getFieldType();

        if (fieldType.isGenericParameter()) {
            s.append(fieldType.getName());
        }
        else {
            s = fieldType.appendBriefDescription(s);
        }

        s.append(' ');
        s.append(getName());

        return s;
    }

    @Override
    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        StringBuilder s = sb;

        if (isStatic()) {
            s.append(Modifier.STATIC.toString());
            s.append(' ');
        }
        
        final Type fieldType = getFieldType();

        if (fieldType.isGenericParameter()) {
            s.append(fieldType.getName());
        }
        else {
            s = fieldType.appendBriefDescription(s);
        }

        s.append(' ');
        s.append(getName());

        return s;
    }

    @Override
    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        StringBuilder s = sb;
        
        for (final javax.lang.model.element.Modifier modifier : Flags.asModifierSet(getModifiers())) {
            s.append(modifier.toString());
            s.append(' ');
        }

        s = getFieldType().getErasedType().appendErasedDescription(s);
        s.append(' ');
        s.append(getName());

        return s;
    }

    @Override
    public StringBuilder appendSimpleDescription(final StringBuilder sb) {
        StringBuilder s = sb;

        if (isStatic()) {
            s.append(Modifier.STATIC.toString());
            s.append(' ');
        }

        final Type fieldType = getFieldType();

        if (fieldType.isGenericParameter()) {
            s.append(fieldType.getName());
        }
        else {
            s = fieldType.appendSimpleDescription(s);
        }

        s.append(' ');
        s.append(getName());

        return s;
    }

    @Override
    public StringBuilder appendSignature(final StringBuilder sb) {
        return getFieldType().appendSignature(sb);
    }

    @Override
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
