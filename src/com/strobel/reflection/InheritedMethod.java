package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author Mike Strobel
 */
final class InheritedMethod extends MethodInfo {
    private final static Method GET_CLASS_METHOD;

    static {
        Method getClassMethod;

        try { getClassMethod = Object.class.getMethod("getClass"); }
        catch (NoSuchMethodException ignored) { getClassMethod = null; }

        GET_CLASS_METHOD = getClassMethod;
    }

    private final MethodInfo _method;
    private final Type<?> _inheritingType;
    private final Type<?> _returnTypeOverride;

    InheritedMethod(final MethodInfo method, final Type<?> inheritingType) {
        VerifyArgument.notNull(method, "method");

        _method = method instanceof InheritedMethod
                  ? ((InheritedMethod)method)._method
                  : method;

        _inheritingType = VerifyArgument.notNull(inheritingType, "inheritingType");

        if (GET_CLASS_METHOD.equals(_method.getRawMethod())) {
            _returnTypeOverride = Type.of(Class.class)
                                      .makeGenericType(
                                          Type.makeExtendsWildcard(_inheritingType)
                                      );
        }
        else {
            _returnTypeOverride = null;
        }
    }

    @Override
    public boolean isStatic() {
        return _method.isStatic();
    }

    @Override
    public boolean isAbstract() {
        return _method.isAbstract();
    }

    @Override
    public Type getReturnType() {
        if (_returnTypeOverride != null) {
            return _returnTypeOverride;
        }
        return _method.getReturnType();
    }

    @Override
    public Method getRawMethod() {
        return _method.getRawMethod();
    }

    @Override
    public String getName() {
        return _method.getName();
    }

    @Override
    public StringBuilder appendDescription(final StringBuilder sb) {
        return _method.appendDescription(sb);
    }

    @Override
    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        return _method.appendErasedDescription(sb);
    }

    @Override
    public boolean isGenericMethod() {
        return _method.isGenericMethod();
    }

    @Override
    public boolean isGenericMethodDefinition() {
        return _method.isGenericMethodDefinition();
    }

    @Override
    public TypeBindings getTypeBindings() {
        return _method.getTypeBindings();
    }

    @Override
    public TypeList getTypeArguments() {
        return _method.getTypeArguments();
    }

    @Override
    public TypeList getGenericMethodParameters() {
        return _method.getGenericMethodParameters();
    }

    @Override
    public MethodInfo getGenericMethodDefinition() {
        return _method.getGenericMethodDefinition();
    }

    @Override
    public boolean containsGenericParameters() {
        return _method.containsGenericParameters();
    }

    @Override
    public MethodInfo makeGenericMethod(final TypeList typeArguments) {
        return _method.makeGenericMethod(typeArguments);
    }

    @Override
    public ParameterList getParameters() {
        return _method.getParameters();
    }

    @Override
    public TypeList getThrownTypes() {
        return _method.getThrownTypes();
    }

    @Override
    public CallingConvention getCallingConvention() {
        return _method.getCallingConvention();
    }

    @Override
    public String toString() {
        return _method.toString();
    }

    @Override
    public String getSignature() {
        return _method.getSignature();
    }

    @Override
    public String getErasedSignature() {
        return _method.getErasedSignature();
    }

    @Override
    public String getDescription() {
        return _method.getDescription();
    }

    @Override
    public String getErasedDescription() {
        return _method.getErasedDescription();
    }

    @Override
    public StringBuilder appendSignature(final StringBuilder sb) {
        return _method.appendSignature(sb);
    }

    @Override
    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        return _method.appendErasedSignature(sb);
    }

    @Override
    public Type getDeclaringType() {
        return _method.getDeclaringType();
    }

    @Override
    public Type getReflectedType() {
        return _inheritingType;
    }

    @Override
    public boolean isFinal() {
        return _method.isFinal();
    }

    @Override
    public int getModifiers() {
        return _method.getModifiers();
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        return _method.isAnnotationPresent(annotationClass);
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
        return _method.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return _method.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return _method.getDeclaredAnnotations();
    }
}
