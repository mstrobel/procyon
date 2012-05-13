package com.strobel.reflection;

import com.strobel.util.TypeUtils;
import com.sun.tools.javac.util.List;

import java.util.ArrayList;

/**
 * @author Mike Strobel
 */
final class TypeBinder extends TypeVisitor<TypeBindings, Type<?>> {
    public TypeBindings visitTypeBindings(final TypeBindings typeBindings, final TypeBindings bindings) {
        TypeBindings newTypeBindings = typeBindings;

        for (final Type genericParameter : typeBindings.getGenericParameters()) {
            final Type oldBoundType = typeBindings.getBoundType(genericParameter);
            final Type newBoundType = visit(oldBoundType, bindings);

            if (oldBoundType != newBoundType) {
                newTypeBindings = newTypeBindings.withAdditionalBinding(
                    genericParameter,
                    newBoundType
                );
            }
        }

        return newTypeBindings;
    }

    public FieldInfo visitField(final Type declaringType, final FieldInfo field, final TypeBindings bindings) {
        final Type<?> oldFieldType = field.getFieldType();
        final Type<?> newFieldType = visit(field.getFieldType(), bindings);

        if (TypeUtils.areEquivalent(oldFieldType, newFieldType) &&
            TypeUtils.areEquivalent(field.getDeclaringType(), declaringType)) {

            return field;
        }

        return new ReflectedField(
            declaringType,
            field.getRawField(),
            newFieldType
        );
    }

    public MethodInfo visitMethod(final Type declaringType, final MethodInfo method, final TypeBindings bindings) {
        final Type oldReturnType = method.getReturnType();
        final Type<?> returnType = visit(oldReturnType, bindings);
        final ParameterList parameters = method.getParameters();
        final TypeList thrown = method.getThrownTypes();
        final Type<?>[] parameterTypes = new Type<?>[parameters.size()];
        final Type<?>[] thrownTypes = new Type<?>[thrown.size()];

        boolean hasChanged = !oldReturnType.isEquivalentTo(returnType);
        boolean thrownTypesChanged = false;

        for (int i = 0, n = parameterTypes.length; i < n; i++) {
            final Type oldParameterType = parameters.get(i).getParameterType();

            parameterTypes[i] = visit(oldParameterType, bindings);

            if (!oldParameterType.isEquivalentTo(parameterTypes[i])) {
                hasChanged = true;
            }
        }

        for (int i = 0, n = thrownTypes.length; i < n; i++) {
            final Type<?> oldThrownType = thrown.get(i);
            final Type<?> newThrownType = visit(oldThrownType, bindings);

            thrownTypes[i] = newThrownType;

            if (!oldThrownType.isEquivalentTo(newThrownType)) {
                thrownTypesChanged = true;
            }
        }

        hasChanged |= thrownTypesChanged;

        if (!hasChanged) {
            if (!TypeUtils.areEquivalent(method.getDeclaringType(), declaringType)) {
                return new ReflectedMethod(
                    declaringType,
                    method.getRawMethod(),
                    method.getParameters(),
                    method.getReturnType(),
                    method.getThrownTypes(),
                    method.getTypeBindings()
                );
            }
            return method;
        }

        final ArrayList<ParameterInfo> newParameters = new ArrayList<>();

        for (int i = 0, n = parameterTypes.length; i < n; i++) {
            newParameters.add(
                new ParameterInfo(
                    parameters.get(i).getName(),
                    parameterTypes[i]
                )
            );
        }

        return new ReflectedMethod(
            declaringType,
            method.getRawMethod(),
            new ParameterList(newParameters),
            returnType,
            thrownTypesChanged ? new TypeList(thrownTypes) : thrown,
            visitTypeBindings(method.getTypeBindings(), bindings)
        );
    }

    public ConstructorInfo visitConstructor(final Type declaringType, final ConstructorInfo constructor, final TypeBindings bindings) {
        final ParameterList parameters = constructor.getParameters();
        final TypeList thrown = constructor.getThrownTypes();
        final Type<?>[] parameterTypes = new Type<?>[parameters.size()];
        final Type<?>[] thrownTypes = new Type<?>[thrown.size()];

        boolean hasChanged = false;
        boolean thrownTypesChanged = false;

        for (int i = 0, n = parameterTypes.length; i < n; i++) {
            final Type oldParameterType = parameters.get(i).getParameterType();
            parameterTypes[i] = visit(oldParameterType, bindings);
            if (!oldParameterType.isEquivalentTo(parameterTypes[i])) {
                hasChanged = true;
            }
        }

        for (int i = 0, n = thrownTypes.length; i < n; i++) {
            final Type<?> oldThrownType = thrown.get(i);
            final Type<?> newThrownType = visit(oldThrownType, bindings);

            thrownTypes[i] = newThrownType;

            if (!oldThrownType.isEquivalentTo(newThrownType)) {
                thrownTypesChanged = true;
            }
        }

        hasChanged |= thrownTypesChanged;

        if (!hasChanged) {
            if (!TypeUtils.areEquivalent(constructor.getDeclaringType(), declaringType)) {
                return new ReflectedConstructor(
                    declaringType,
                    constructor.getRawConstructor(),
                    constructor.getParameters(),
                    thrown
                );
            }

            return constructor;
        }

        final ArrayList<ParameterInfo> newParameters = new ArrayList<>();

        for (int i = 0, n = parameterTypes.length; i < n; i++) {
            newParameters.add(
                new ParameterInfo(
                    parameters.get(i).getName(),
                    parameterTypes[i]
                )
            );
        }

        return new ReflectedConstructor(
            declaringType,
            constructor.getRawConstructor(),
            new ParameterList(newParameters),
            new TypeList(thrownTypes)
        );
    }

    @Override
    public Type<?> visitType(final Type<?> type, final TypeBindings bindings) {
        if (bindings.containsGenericParameter(type)) {
            return bindings.getBoundType(type);
        }

        final TypeBindings oldTypeBindings = type.getTypeBindings();
        final TypeBindings newTypeBindings = visitTypeBindings(oldTypeBindings, bindings);

        if (oldTypeBindings != newTypeBindings) {
            final Type cachedType = Type.CACHE.find(
                Type.CACHE.key(
                    type.getErasedClass(),
                    newTypeBindings.getBoundTypes()
                )
            );

            if (cachedType != null) {
                return cachedType;
            }

            final GenericType genericType = new GenericType(
                type.getGenericTypeDefinition(),
                newTypeBindings
            );

            Type.CACHE.add(genericType);

            for (final FieldInfo field : type.getResolvedFields()) {
                genericType.addField(visitField(genericType, field, bindings));
            }

            for (final ConstructorInfo constructor : type.getResolvedConstructors()) {
                genericType.addConstructor(visitConstructor(genericType, constructor, bindings));
            }

            for (final MethodInfo method : type.getResolvedStaticMethods()) {
                genericType.addMethod(visitMethod(genericType, method, bindings));
            }

            for (final MethodInfo method : type.getResolvedInstanceMethods()) {
                genericType.addMethod(visitMethod(genericType, method, bindings));
            }

            return genericType;
        }

        return type;
    }

    @Override
    public Type<?> visitTypeParameter(final Type<?> type, final TypeBindings bindings) {
        if (bindings.containsGenericParameter(type)) {
            return bindings.getBoundType(type);
        }

        final TypeList bounds = type.getGenericParameterConstraints();

        boolean changed = false;
        List<Type> newBounds = List.nil();

        for (int i = 0, n = bounds.size(); i < n; i++) {
            final Type constraint = bounds.get(i);
            final Type newConstraint = visit(constraint, bindings);

            newBounds = newBounds.append(newConstraint);

            if (newConstraint != constraint) {
                changed = true;
            }
        }

        if (changed) {
            return new GenericParameter(
                type.getName(),
                type.getDeclaringType(),
                Type.list(newBounds),
                type.getGenericParameterPosition()
            );
        }

        return type;
    }

    @Override
    public Type<?> visitWildcard(final Type<?> type, final TypeBindings bindings) {
        final Type<?> oldLower = type.getLowerBound();
        final Type<?> oldUpper = type.getGenericParameterConstraints().get(0);
        final Type<?> newLower = visit(oldLower, bindings);
        final Type<?> newUpper = visit(oldUpper, bindings);

        if (newLower != oldLower || newUpper != oldUpper) {
            return new WildcardType(newUpper, newLower);
        }

        return type;
    }

    @Override
    public Type<?> visitArrayType(final Type<?> type, final TypeBindings bindings) {
        final Type<?> oldElementType = type.getElementType();
        final Type<?> newElementType = visit(oldElementType, bindings);

        if (TypeUtils.areEquivalent(oldElementType, newElementType)) {
            return type;
        }

        return newElementType.makeArrayType();
    }

    @Override
    public Type<?> visitPrimitiveType(final Type<?> type, final TypeBindings parameter) {
        return type;
    }

    @Override
    public Type<?> visitUnknown(final Type<?> type, final TypeBindings parameter) {
        return type;
    }
}
