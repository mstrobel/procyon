package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.lang.annotation.Annotation;
import java.lang.reflect.TypeVariable;

/**
 * @author Mike Strobel
 */
final class GenericParameterType extends Type {
    private final int _position;
    private final TypeVariable<?> _typeVariable;
    private final Type _declaringType;
    private final MethodInfo _declaringMethod;
    private final Class<?> _erasedClass;

    GenericParameterType(final TypeVariable<?> typeVariable, final Type declaringType, final int position) {
        _typeVariable = VerifyArgument.notNull(typeVariable, "typeVariable");
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _declaringMethod = null;
        _position = position;
        _erasedClass = resolveErasedClass();
    }

    GenericParameterType(final TypeVariable<?> typeVariable, final MethodInfo declaringMethod, final int position ) {
        _typeVariable = VerifyArgument.notNull(typeVariable, "typeVariable");
        _declaringType = null;
        _declaringMethod = VerifyArgument.notNull(declaringMethod, "declaringMethod");
        _position = position;
        _erasedClass = resolveErasedClass();
    }

    private Class<?> resolveErasedClass() {
        return java.lang.Object.class;
    }

    @Override
    public MemberCollection<? extends MemberInfo> getMember(final String name, final int bindingFlags, final MemberType... memberTypes) {
        return MemberCollection.empty();
    }

    @Override
    public FieldInfo getField(final String name, final int bindingFlags) {
        return null;
    }

    @Override
    public MethodInfo getMethod(final String name, final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes) {
        return null;
    }

    @Override
    public ConstructorInfo getConstructor(final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes) {
        return null;
    }

    @Override
    public MemberCollection<? extends MemberInfo> getMembers(final int bindingFlags) {
        return MemberCollection.empty();
    }

    @Override
    public FieldCollection getFields(final int bindingFlags) {
        return FieldCollection.empty();
    }

    @Override
    public MethodCollection getMethods(final int bindingFlags, final CallingConvention callingConvention) {
        return MethodCollection.empty();
    }

    @Override
    public ConstructorCollection getConstructors(final int bindingFlags) {
        return ConstructorCollection.empty();
    }

    @Override
    public TypeCollection getNestedTypes(final int bindingFlags) {
        return TypeCollection.empty();
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public String getName() {
        return _typeVariable.getName();
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    public MethodInfo getDeclaringMethod() {
        return _declaringMethod;
    }

    @Override
    public boolean isGenericParameter() {
        return true;
    }

    @Override
    public Class<?> getErasedClass() {
        return _erasedClass;
    }

    @Override
    public int getGenericParameterPosition() {
        return _position;
    }

    @Override
    int getModifiers() {
        return 0;
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        return false;
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return new Annotation[0];
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return new Annotation[0];
    }
}
