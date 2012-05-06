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
    public MemberList<? extends MemberInfo> getMember(final String name, final int bindingFlags, final MemberType[] memberTypes) {
        return _genericTypeDefinition.getMember(name, bindingFlags, memberTypes);
    }

    @Override
    public FieldInfo getField(final String name, final int bindingFlags) {
        return _genericTypeDefinition.getField(name, bindingFlags);
    }

    @Override
    public MethodInfo getMethod(final String name, final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes) {
        return _genericTypeDefinition.getMethod(name, bindingFlags, callingConvention, parameterTypes);
    }

    @Override
    public ConstructorInfo getConstructor(final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes) {
        return _genericTypeDefinition.getConstructor(bindingFlags, callingConvention, parameterTypes);
    }

    @Override
    public MemberList<? extends MemberInfo> getMembers(final int bindingFlags) {
        return _genericTypeDefinition.getMembers(bindingFlags);
    }

    @Override
    public FieldList getFields(final int bindingFlags) {
        return _genericTypeDefinition.getFields(bindingFlags);
    }

    @Override
    public MethodList getMethods(final int bindingFlags, final CallingConvention callingConvention) {
        return _genericTypeDefinition.getMethods(bindingFlags, callingConvention);
    }

    @Override
    public ConstructorList getConstructors(final int bindingFlags) {
        return _genericTypeDefinition.getConstructors(bindingFlags);
    }

    @Override
    public TypeList getNestedTypes(final int bindingFlags) {
        return _genericTypeDefinition.getNestedTypes(bindingFlags);
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
