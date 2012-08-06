package com.strobel.reflection.emit;

import com.strobel.core.ReadOnlyList;
import com.strobel.reflection.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("PackageVisibleField")
public final class ConstructorBuilder extends ConstructorInfo {
    private final MethodBuilder _methodBuilder;

    ConstructorInfo generatedConstructor;

    public ConstructorBuilder(final int modifiers, final TypeList parameterTypes, final TypeBuilder declaringType) {
        _methodBuilder = new MethodBuilder(
            "<init>",
            modifiers,
            null,
            parameterTypes,
            declaringType
        );

        declaringType.addMethodToList(_methodBuilder);
    }

    private void verifyTypeCreated() {
        _methodBuilder.getDeclaringType().verifyCreated();
    }

    public boolean isFinished() {
        return _methodBuilder.isFinished();
    }

    public boolean isTypeCreated() {
        return _methodBuilder.isTypeCreated();
    }

    public BytecodeGenerator getCodeGenerator() {
        return _methodBuilder.getCodeGenerator();
    }

    @Override
    public Constructor<?> getRawConstructor() {
        verifyTypeCreated();
        return null;
    }

    @Override
    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        final TypeList parameterTypes = getParameterTypes();

        StringBuilder s = sb;
        s.append('(');

        for (int i = 0, n = parameterTypes.size(); i < n; ++i) {
            s = parameterTypes.get(i).getErasedType().appendErasedSignature(s);
        }

        s.append(')');
        s = PrimitiveTypes.Void.appendErasedSignature(s);

        return s;
    }

    @Override
    public StringBuilder appendSimpleDescription(final StringBuilder sb) {
        StringBuilder s = PrimitiveTypes.Void.appendBriefDescription(sb);

        s.append(' ');
        s.append(getName());
        s.append('(');

        final ParameterBuilder[] parameters = _methodBuilder.parameterBuilders;

        for (int i = 0, n = parameters.length; i < n; ++i) {
            final ParameterBuilder p = parameters[i];
            if (i != 0) {
                s.append(", ");
            }
            s = p.getParameterType().appendSimpleDescription(s);
        }

        s.append(')');

        final TypeList thrownTypes = getThrownTypes();

        if (!thrownTypes.isEmpty()) {
            s.append(" throws ");

            for (int i = 0, n = thrownTypes.size(); i < n; ++i) {
                final Type t = thrownTypes.get(i);
                if (i != 0) {
                    s.append(", ");
                }
                s = t.appendSimpleDescription(s);
            }
        }

        return s;
    }

    @Override
    public Type getDeclaringType() {
        return _methodBuilder.getDeclaringType();
    }

    @Override
    public int getModifiers() {
        return _methodBuilder.getModifiers();
    }

    @Override
    public ParameterList getParameters() {
        verifyTypeCreated();
        return generatedConstructor.getParameters();
    }

    TypeList getParameterTypes() {
        return _methodBuilder.getParameterTypes();
    }

    public void defineParameter(final int position, final String name) {
        _methodBuilder.defineParameter(position, name);
    }

    @Override
    public Type getReflectedType() {
        return _methodBuilder.getReflectedType();
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
        return _methodBuilder.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return _methodBuilder.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return _methodBuilder.getDeclaredAnnotations();
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        return _methodBuilder.isAnnotationPresent(annotationClass);
    }

    public void addCustomAnnotation(final AnnotationBuilder annotation) {
        _methodBuilder.addCustomAnnotation(annotation);
    }

    public ReadOnlyList<AnnotationBuilder> getCustomAnnotations() {
        return _methodBuilder.getCustomAnnotations();
    }
}
