/*
 * TypeCache.java
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

import java.util.LinkedHashMap;

/**
 * @author strobelm
 */
@SuppressWarnings("unchecked")
final class TypeCache {

    private final LinkedHashMap<Key, Type> _map = new LinkedHashMap<>();
    private final LinkedHashMap<Class<?>, Type<?>> _erasedMap = new LinkedHashMap<>();
    private final LinkedHashMap<Type<?>, Type<?>> _arrayMap = new LinkedHashMap<>();

    public Key key(final Class<?> simpleType) {
        return new Key(simpleType);
    }

    public Key key(final Class<?> simpleType, final TypeList typeArguments) {
        return new Key(simpleType, typeArguments);
    }

    public Type find(final Key key) {
        return _map.get(key);
    }

    public <T> Type<T[]> getArrayType(final Type<T> elementType) {
        Type<T[]> arrayType = (Type<T[]>)_arrayMap.get(elementType);

        if (arrayType != null) {
            return arrayType;
        }

        arrayType = new ArrayType<>(elementType);
        add(arrayType);

        return arrayType;
    }

    public <T> Type<T> getGenericType(final Type<T> typeDefinition, final TypeList typeArguments) {
        final Key key = key(
            typeDefinition.getErasedClass(),
            typeArguments
        );

        Type genericType = _map.get(key);

        if (genericType == null) {
            genericType = new GenericType(
                typeDefinition.getGenericTypeDefinition(),
                typeArguments
            );

            final Type existing = _map.put(key, genericType);

            if (existing != null) {
                return existing;
            }
        }

        return genericType;
    }

    public <T> Type<T> find(final Class<T> clazz) {
        return (Type<T>)_erasedMap.get(clazz);
    }

    public int size() {
        return _map.size();
    }

    public void put(final Key key, final Type type) {
        final Class<?> erasedType = key._erasedType;

        if (!_erasedMap.containsKey(erasedType)) {
            if (type.isGenericType() && !type.isGenericTypeDefinition()) {
                _erasedMap.put(erasedType, type.getGenericTypeDefinition());
            }
            else {
                _erasedMap.put(erasedType, type);
            }
        }

        _map.put(key, type);

        if (type.isArray()) {
            final Type elementType = type.getElementType();
            if (!_arrayMap.containsKey(elementType)) {
                _arrayMap.put(elementType, type);
            }
        }
    }

    public void add(final Type type) {
        final TypeList typeArguments;

        if (type.isGenericType()) {
            typeArguments = type.getTypeBindings().getBoundTypes();
        }
        else {
            typeArguments = TypeList.empty();
        }

        put(key(type.getErasedClass(), typeArguments), type);
    }

    static class Key {
        private final Class<?> _erasedType;
        private final TypeList _typeParameters;
        private final int _hashCode;

        public Key(final Class<?> simpleType) {
            this(simpleType, null);
        }

        public Key(final Class<?> erasedType, final TypeList typeArguments) {
            _erasedType = erasedType;
            _typeParameters = typeArguments;

            int h = erasedType.getName().hashCode();

            if (typeArguments != null && !typeArguments.isEmpty()) {
                h = h * 31 + typeArguments.size();
            }

            _hashCode = h;
        }

        @Override
        public int hashCode() {
            return _hashCode;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this) {
                return true;
            }

            if (o == null || o.getClass() != getClass()) {
                return false;
            }

            final Key other = (Key)o;

            if (other._erasedType != _erasedType) {
                return false;
            }

            final TypeList otherArguments = other._typeParameters;

            if (_typeParameters == null || _typeParameters.isEmpty()) {
                return otherArguments == null || otherArguments.isEmpty();
            }

            if (otherArguments == null || otherArguments.size() != _typeParameters.size()) {
                return false;
            }

            for (int i = 0, n = _typeParameters.size(); i < n; ++i) {
                final Type parameter = _typeParameters.get(i);
                final Type otherParameter = otherArguments.get(i);
                if (parameter == null) {
                    if (otherParameter != null) {
                        return false;
                    }
                }
                else if (!parameter.equals(otherParameter)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public String toString() {
            return "Key{" +
                   "_erasedType=" + _erasedType +
                   ", _typeParameters=" + _typeParameters +
                   ", _hashCode=" + _hashCode +
                   '}';
        }
    }
}
