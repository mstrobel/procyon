package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.lang.annotation.Annotation;

/**
 * @author strobelm
 */
class GenericType extends Type {

    private final Type _genericTypeDefinition;
    private final TypeBindings _typeArguments;
    private final TypeList _interfaces;
    private final Type _baseType;

    GenericType(final Type genericTypeDefinition, final TypeBindings typeArguments) {
        _genericTypeDefinition = VerifyArgument.notNull(genericTypeDefinition, "genericTypeDefinition");
        _typeArguments = VerifyArgument.notNull(typeArguments, "typeArguments");
        _interfaces = resolveInterfaces();
        _baseType = resolveBaseType();
    }

    GenericType(final Type genericTypeDefinition, final TypeList typeArguments) {
        _genericTypeDefinition = VerifyArgument.notNull(genericTypeDefinition, "genericTypeDefinition");

        _typeArguments = TypeBindings.create(
            genericTypeDefinition.getTypeBindings().getGenericParameters(),
            VerifyArgument.notNull(typeArguments, "typeArguments")
        );

        _interfaces = resolveInterfaces();
        _baseType = resolveBaseType();
    }

    GenericType(final Type genericTypeDefinition, final Type... typeArguments) {
        _genericTypeDefinition = VerifyArgument.notNull(genericTypeDefinition, "genericTypeDefinition");

        _typeArguments = TypeBindings.create(
            genericTypeDefinition.getTypeBindings().getGenericParameters(),
            VerifyArgument.notNull(typeArguments, "typeArguments")
        );

        _interfaces = resolveInterfaces();
        _baseType = resolveBaseType();
    }

    private Type resolveBaseType() {
        return TYPE_RESOLVER._resolveSuperClass(
            new TypeResolver.ClassStack(_genericTypeDefinition.getErasedClass()),
            _genericTypeDefinition.getErasedClass(),
            _typeArguments
        );
    }

    private TypeList resolveInterfaces() {
        return TYPE_RESOLVER._resolveSuperInterfaces(
            new TypeResolver.ClassStack(_genericTypeDefinition.getErasedClass()),
            _genericTypeDefinition.getErasedClass(),
            _typeArguments
        );
    }

    @Override
    public Class<?> getErasedClass() {
        return _genericTypeDefinition.getErasedClass();
    }

    @Override
    public TypeList getInterfaces() {
        return _interfaces;
    }

    @Override
    public Type getBaseType() {
        return _baseType;
    }

    @Override
    public Type getGenericTypeDefinition() {
        return _genericTypeDefinition;
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public Type getDeclaringType() {
        return _genericTypeDefinition.getDeclaringType();
    }

    @Override
    public final boolean isGenericType() {
        return true;
    }

    @Override
    public TypeBindings getTypeBindings() {
        return _typeArguments;
    }

    @Override
    int getModifiers() {
        return _genericTypeDefinition.getModifiers();
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        return _genericTypeDefinition.isAnnotationPresent(annotationClass);
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
        return _genericTypeDefinition.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return _genericTypeDefinition.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return _genericTypeDefinition.getDeclaredAnnotations();
    }
}
