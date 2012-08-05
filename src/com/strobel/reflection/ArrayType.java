package com.strobel.reflection;

import com.strobel.core.VerifyArgument;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;

import javax.lang.model.type.TypeKind;
import java.lang.annotation.Annotation;

/**
 * @author strobelm
 */
public class ArrayType<T> extends Type<T> {
    private final static GenericsFactory REFLECTION_FACTORY = CoreReflectionFactory.make(null, null);

    private final Type<?> _elementType;
    private final Class<T> _erasedClass;
    private final FieldList _fields = FieldList.empty();
    private final MethodList _methods = MethodList.empty();

    @SuppressWarnings("unchecked")
    ArrayType(final Type<?> elementType) {
        _elementType = VerifyArgument.notNull(elementType, "elementType");
        _erasedClass = (Class<T>)REFLECTION_FACTORY.makeArrayType(elementType.getErasedClass());
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.ARRAY;
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
    public TypeBindings getTypeBindings() {
        return _elementType.getTypeBindings();
    }

    @Override
    public Type getDeclaringType() {
        return null;
    }

    @Override
    public int getModifiers() {
        return 0;
    }

    @Override
    protected MethodList getDeclaredMethods() {
        return _methods;
    }

    @Override
    public FieldList getDeclaredFields() {
        return _fields;
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

    public StringBuilder appendSimpleDescription(final StringBuilder sb)
    {
        return _elementType.appendSimpleDescription(sb).append("[]");
    }

    public StringBuilder appendFullDescription(final StringBuilder sb) {
        return appendBriefDescription(sb);
    }

    @Override
    public <P, R> R accept(final TypeVisitor<P, R> visitor, final P parameter) {
        return visitor.visitArrayType(this, parameter);
    }
}

