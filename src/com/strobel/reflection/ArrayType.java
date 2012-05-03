package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;

/**
 * @author strobelm
 */
class ArrayType extends Type {
    private final Type _elementType;
    private final Class<?> _erasedClass;

    ArrayType(final Type elementType) {
        _elementType = VerifyArgument.notNull(elementType, "elementType");
        _erasedClass = Array.newInstance(elementType.getErasedClass(), 0).getClass();
    }

    @Override
    public Class<?> getErasedClass() {
        return _erasedClass;
    }

    @Override
    public boolean isGenericType() {
        return _elementType.isGenericType();
    }
    
    @Override
    public Type getGenericTypeDefinition() {
        if (_elementType.isGenericTypeDefinition()) {
            return this;
        }
        return _elementType.getGenericTypeDefinition().makeArrayType();
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
    public TypeCollection getGenericParameters() {
        return _elementType.getGenericParameters();
    }

    @Override
    public Type getDeclaringType() {
        return null;
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

    @Override
    public StringBuilder appendSignature(final StringBuilder sb) {
        sb.append('[');
        return _elementType.appendSignature(sb);
    }

    @Override
    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        sb.append('[');
        return _elementType.appendErasedSignature(sb.append('['));
    }

    public StringBuilder appendBriefDescription(final StringBuilder sb)
    {
        return _elementType.appendBriefDescription(sb).append("[]");
    }

    public StringBuilder appendFullDescription(final StringBuilder sb) {
        return appendBriefDescription(sb);
    }
}

