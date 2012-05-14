package com.strobel.reflection;

import com.strobel.core.VerifyArgument;
import com.sun.tools.javac.util.List;

import java.lang.annotation.Annotation;

/**
 * @author strobelm
 */
class GenericType<T> extends Type<T> {

    private final Type _genericTypeDefinition;
    private final TypeBindings _typeArguments;
    private final TypeList _interfaces;
    private final Type _baseType;

    private List<FieldInfo> _fields = List.nil();
    private List<ConstructorInfo> _constructors = List.nil();
    private List<MethodInfo> _staticMethods = List.nil();
    private List<MethodInfo> _instanceMethods = List.nil();
    private List<Type<?>> _nestedTypes = List.nil();

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

    void addField(final FieldInfo field) {
        _fields = _fields.append(VerifyArgument.notNull(field, "field"));
    }

    void addConstructor(final ConstructorInfo constructor) {
        _constructors = _constructors.append(VerifyArgument.notNull(constructor, "constructor"));
    }

    void addMethod(final MethodInfo method) {
        if (method.isStatic()) {
            _staticMethods = _staticMethods.append(VerifyArgument.notNull(method, "method"));
        }
        else {
            _instanceMethods = _instanceMethods.append(VerifyArgument.notNull(method, "method"));
        }
    }

    void addNestedType(final Type<?> nestedType) {
        _nestedTypes = _nestedTypes.append(VerifyArgument.notNull(nestedType, "nestedType"));
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
    @SuppressWarnings("unchecked")
    public Class<T> getErasedClass() {
        return (Class<T>) _genericTypeDefinition.getErasedClass();
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

    @Override
    public <P, R> R accept(final TypeVisitor<P, R> typeVisitor, final P parameter) {
        return typeVisitor.visitClassType(this, parameter);
    }

    @Override
    ConstructorList getResolvedConstructors() {
        if (_constructors.isEmpty()) {
            return ConstructorList.empty();
        }
        return new ConstructorList(_constructors);
    }

    @Override
    MethodList getResolvedInstanceMethods() {
        if (_instanceMethods.isEmpty()) {
            return MethodList.empty();
        }
        return new MethodList(_instanceMethods);
    }

    @Override
    MethodList getResolvedStaticMethods() {
        if (_staticMethods.isEmpty()) {
            return MethodList.empty();
        }
        return new MethodList(_staticMethods);
    }

    @Override
    FieldList getResolvedFields() {
        if (_fields.isEmpty()) {
            return FieldList.empty();
        }
        return new FieldList(_fields);
    }

    @Override
    TypeList getResolvedNestedTypes() {
        if (_nestedTypes.isEmpty()) {
            return TypeList.empty();
        }
        return new TypeList(_nestedTypes);
    }

    void complete() {
        getResolvedFields();
        getResolvedConstructors();
        getResolvedStaticMethods();
        getResolvedInstanceMethods();
        getResolvedNestedTypes();
    }
}
