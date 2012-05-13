package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;

/**
 * @author strobelm
 */
class ArrayType<T> extends Type<T> {
    private final Type<?> _elementType;
    private final Class<T> _erasedClass;

    @SuppressWarnings("unchecked")
    ArrayType(final Type<?> elementType) {
        _elementType = VerifyArgument.notNull(elementType, "elementType");
        _erasedClass = (Class<T>)Array.newInstance(elementType.getErasedClass(), 0).getClass();
    }

    @Override
    public Class<T> getErasedClass() {
        return _erasedClass;
    }

    @Override
    public Type getElementType() {
        return _elementType;
    }

    @Override
    public boolean isArray() {
        return true;
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
    public MemberList<? extends MemberInfo> getMember(final String name, final int bindingFlags, final MemberType[] memberTypes) {
        return MemberList.empty();
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
    public MemberList<? extends MemberInfo> getMembers(final int bindingFlags) {
        return MemberList.empty();
    }

    @Override
    public FieldList getFields(final int bindingFlags) {
        return FieldList.empty();
    }

    @Override
    public MethodList getMethods(final int bindingFlags, final CallingConvention callingConvention) {
        return MethodList.empty();
    }

    @Override
    public ConstructorList getConstructors(final int bindingFlags) {
        return ConstructorList.empty();
    }

    @Override
    public TypeList getNestedTypes(final int bindingFlags) {
        return TypeList.empty();
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public TypeBindings getTypeBindings() {
        return _elementType.getTypeBindings();
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

    @Override
    public <P, R> R accept(final TypeVisitor<P, R> visitor, final P parameter) {
        return visitor.visitArrayType(this, parameter);
    }
}

