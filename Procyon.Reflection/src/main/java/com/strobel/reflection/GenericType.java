/*
 * GenericType.java
 *
 * Copyright (c) 2012 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.reflection;

import com.strobel.annotations.NotNull;
import com.strobel.core.Fences;
import com.strobel.core.VerifyArgument;

import java.lang.annotation.Annotation;

/**
 * @author strobelm
 */
@SuppressWarnings({ "unchecked", "DoubleCheckedLocking" })
final class GenericType<T> extends Type<T> {
    final static TypeBinder GenericBinder = new TypeBinder();

    private final Type<?> _genericTypeDefinition;
    private final TypeBindings _typeBindings;

    private TypeList _interfaces;
    private Type<?> _baseType;

    private FieldList _fields;
    private ConstructorList _constructors;
    private MethodList _methods;
    private TypeList _nestedTypes;

    GenericType(final Type<?> genericTypeDefinition, final TypeBindings typeBindings) {
        _genericTypeDefinition = VerifyArgument.notNull(genericTypeDefinition, "genericTypeDefinition");
        _typeBindings = VerifyArgument.notNull(typeBindings, "typeBindings");
    }

    GenericType(final Type<?> genericTypeDefinition, final TypeList typeArguments) {
        _genericTypeDefinition = VerifyArgument.notNull(genericTypeDefinition, "genericTypeDefinition");

        _typeBindings = TypeBindings.create(
            genericTypeDefinition.getTypeBindings().getGenericParameters(),
            VerifyArgument.notNull(typeArguments, "typeArguments")
        );
    }

    GenericType(final Type<?> genericTypeDefinition, final Type<?>... typeArguments) {
        _genericTypeDefinition = VerifyArgument.notNull(genericTypeDefinition, "genericTypeDefinition");

        _typeBindings = TypeBindings.create(
            genericTypeDefinition.getTypeBindings().getGenericParameters(),
            VerifyArgument.notNull(typeArguments, "typeArguments")
        );
    }

    private void ensureBaseType() {
        if (_baseType == null) {
            synchronized (CACHE_LOCK) {
                if (_baseType == null) {
                    final Type<?> genericBaseType = _genericTypeDefinition.getBaseType();
                    if (genericBaseType == null || genericBaseType == nullType()) {
                        _baseType = Fences.orderWrites(nullType());
                    }
                    else {
                        _baseType = Fences.orderWrites(GenericBinder.visit(genericBaseType, _typeBindings));
                    }
                }
            }
        }
    }

    private void ensureInterfaces() {
        if (_interfaces == null) {
            synchronized (CACHE_LOCK) {
                if (_interfaces == null) {
                    final TypeList interfaces = GenericBinder.visit(_genericTypeDefinition.getExplicitInterfaces(), _typeBindings);
                    _interfaces = Fences.orderWrites(interfaces);
                }
            }
        }
    }

    private void ensureFields() {
        if (_fields == null) {
            synchronized (CACHE_LOCK) {
                if (_fields == null) {
                    final FieldList fields = GenericBinder.visit(this, _genericTypeDefinition.getDeclaredFields(), _typeBindings);
                    _fields = Fences.orderWrites(fields);
                }
            }
        }
    }

    private void ensureConstructors() {
        if (_constructors == null) {
            synchronized (CACHE_LOCK) {
                if (_constructors == null) {
                    final ConstructorList constructors = GenericBinder.visit(this, _genericTypeDefinition.getDeclaredConstructors(), _typeBindings);
                    _constructors = Fences.orderWrites(constructors);
                }
            }
        }
    }

    private void ensureMethods() {
        if (_methods == null) {
            synchronized (CACHE_LOCK) {
                if (_methods == null) {
                    final MethodList methods = GenericBinder.visit(this, _genericTypeDefinition.getDeclaredMethods(), _typeBindings);
                    _methods = Fences.orderWrites(methods);
                }
            }
        }
    }

    private void ensureNestedTypes() {
        if (_nestedTypes == null) {
            synchronized (CACHE_LOCK) {
                if (_nestedTypes == null) {
                    final TypeList nestedTypes = Helper.map(
                        _genericTypeDefinition.getDeclaredTypes(),
                        new TypeMapping() {
                            @Override
                            public Type<?> apply(final Type<?> type) {
                                return new RuntimeType<>(GenericType.this, type, _typeBindings);
                            }
                        }
                    );
                    _nestedTypes = Fences.orderWrites(nestedTypes);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getErasedClass() {
        return (Class<T>)_genericTypeDefinition.getErasedClass();
    }

    @Override
    public TypeList getExplicitInterfaces() {
        ensureInterfaces();
        return _interfaces;
    }

    @Override
    public Type<? super T> getBaseType() {
        ensureBaseType();
        final @SuppressWarnings("unchecked") Type<? super T> baseType = (Type<? super T>) _baseType;
        return baseType == nullType() ? null : baseType;
    }

    @Override
    public Type<?> getGenericTypeDefinition() {
        return _genericTypeDefinition;
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public Type<?> getDeclaringType() {
        return _genericTypeDefinition.getDeclaringType();
    }

    @Override
    public boolean isGenericType() {
        return true;
    }

    @Override
    public TypeBindings getTypeBindings() {
        return _typeBindings;
    }

    @Override
    public int getModifiers() {
        return _genericTypeDefinition.getModifiers();
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        return _genericTypeDefinition.isAnnotationPresent(annotationClass);
    }

    @Override
    public <A extends Annotation> A getAnnotation(final Class<A> annotationClass) {
        return _genericTypeDefinition.getAnnotation(annotationClass);
    }

    @NotNull
    @Override
    public Annotation[] getAnnotations() {
        return _genericTypeDefinition.getAnnotations();
    }

    @NotNull
    @Override
    public Annotation[] getDeclaredAnnotations() {
        return _genericTypeDefinition.getDeclaredAnnotations();
    }

    @Override
    public <P, R> R accept(final TypeVisitor<P, R> typeVisitor, final P parameter) {
        return typeVisitor.visitClassType(this, parameter);
    }

    @Override
    protected ConstructorList getDeclaredConstructors() {
        ensureConstructors();
        return _constructors;
    }

    @Override
    protected MethodList getDeclaredMethods() {
        ensureMethods();
        return _methods;
    }

    @Override
    protected FieldList getDeclaredFields() {
        ensureFields();
        return _fields;
    }

    @Override
    protected TypeList getDeclaredTypes() {
        ensureNestedTypes();
        return _nestedTypes;
    }
}

