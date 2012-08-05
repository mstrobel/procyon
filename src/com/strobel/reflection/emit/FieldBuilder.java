package com.strobel.reflection.emit;

import com.strobel.core.ReadOnlyList;
import com.strobel.core.VerifyArgument;
import com.strobel.reflection.FieldInfo;
import com.strobel.reflection.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * @author strobelm
 */
@SuppressWarnings("PackageVisibleField")
public final class FieldBuilder extends FieldInfo {
    private final TypeBuilder _typeBuilder;
    private final String _name;
    private final Type<?> _type;
    private final int _modifiers;
    private final Object _constantValue;

    private ReadOnlyList<AnnotationBuilder> _annotations;

    FieldInfo generatedField;

    FieldBuilder(final TypeBuilder typeBuilder, final String name, final Type<?> type, final int modifiers, final Object constantValue) {
        _constantValue = constantValue;
        _typeBuilder = VerifyArgument.notNull(typeBuilder, "typeBuilder");
        _name = VerifyArgument.notNull(name, "name");
        _type = VerifyArgument.notNull(type, "type");
        _modifiers = modifiers;
        _annotations = ReadOnlyList.emptyList();
    }

    FieldInfo getCreatedField() {
        _typeBuilder.verifyCreated();
        return generatedField;
    }

    public void addCustomAnnotation(final AnnotationBuilder annotation) {
        VerifyArgument.notNull(annotation, "annotation");
        final AnnotationBuilder[] newAnnotations = new AnnotationBuilder[this._annotations.size() + 1];
        _annotations.toArray(newAnnotations);
        newAnnotations[this._annotations.size()] = annotation;
        _annotations = new ReadOnlyList<>(newAnnotations);
    }

    public ReadOnlyList<AnnotationBuilder> getCustomAnnotations() {
        return _annotations;
    }

    @Override
    public Type getFieldType() {
        if (generatedField != null) {
            return generatedField.getFieldType();
        }
        return _type;
    }

    @Override
    public Field getRawField() {
        final FieldInfo createdField = getCreatedField();
        return createdField.getRawField();
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public TypeBuilder getDeclaringType() {
        return _typeBuilder;
    }

    @Override
    public int getModifiers() {
        return _modifiers;
    }

    public Object getConstantValue() {
        return _constantValue;
    }

    @Override
    public Type getReflectedType() {
        return _typeBuilder;
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
        _typeBuilder.verifyCreated();
        return generatedField.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        _typeBuilder.verifyCreated();
        return generatedField.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        _typeBuilder.verifyCreated();
        return _typeBuilder.getDeclaredAnnotations();
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        _typeBuilder.verifyCreated();
        return generatedField.isAnnotationPresent(annotationClass);
    }
}
