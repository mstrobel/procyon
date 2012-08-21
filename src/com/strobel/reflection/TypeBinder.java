package com.strobel.reflection;

import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;
import com.strobel.util.TypeUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * @author Mike Strobel
 */
class TypeBinder extends TypeMapper<TypeBindings> {
    final static Method GET_CLASS_METHOD;

    static {
        Method getClassMethod;

        try {
            getClassMethod = Object.class.getMethod("getClass");
        }
        catch (NoSuchMethodException ignored) {
            getClassMethod = null;
        }

        GET_CLASS_METHOD = getClassMethod;
    }

    public ConstructorList visit(final Type<?> declaringType, final ConstructorList constructors, final TypeBindings bindings) {
        VerifyArgument.notNull(constructors, "constructors");

        ConstructorInfo[] newConstructors = null;

        for (int i = 0, n = constructors.size(); i < n; i++) {
            final ConstructorInfo oldConstructor = constructors.get(i);
            final ConstructorInfo newConstructor = visitConstructor(declaringType, oldConstructor, bindings);

            if (newConstructor != oldConstructor) {
                if (newConstructors == null) {
                    newConstructors = constructors.toArray();
                }
                newConstructors[i] = newConstructor;
            }
        }

        if (newConstructors != null) {
            return new ConstructorList(newConstructors);
        }

        return constructors;
    }

    public FieldList visit(final Type<?> declaringType, final FieldList fields, final TypeBindings bindings) {
        VerifyArgument.notNull(fields, "fields");

        FieldInfo[] newFields = null;

        for (int i = 0, n = fields.size(); i < n; i++) {
            final FieldInfo oldField = fields.get(i);
            final FieldInfo newField = visitField(declaringType, oldField, bindings);

            if (newField != oldField) {
                if (newFields == null) {
                    newFields = fields.toArray();
                }
                newFields[i] = newField;
            }
        }

        if (newFields != null) {
            return new FieldList(newFields);
        }

        return fields;
    }

    public MethodList visit(final Type<?> declaringType, final MethodList methods, final TypeBindings bindings) {
        VerifyArgument.notNull(methods, "methods");

        MethodInfo[] newMethods = null;

        for (int i = 0, n = methods.size(); i < n; i++) {
            final MethodInfo oldMethod = methods.get(i);
            final MethodInfo newMethod = visitMethod(declaringType, oldMethod, bindings);

            if (newMethod != oldMethod) {
                if (newMethods == null) {
                    newMethods = methods.toArray();
                }
                newMethods[i] = newMethod;
            }
        }

        if (newMethods != null) {
            return new MethodList(newMethods);
        }

        return methods;
    }

    public TypeBindings visitTypeBindings(final TypeBindings typeBindings, final TypeBindings bindings) {
        TypeBindings newTypeBindings = typeBindings;

        for (final Type<?> genericParameter : typeBindings.getGenericParameters()) {
            final Type<?> oldBoundType = typeBindings.getBoundType(genericParameter);
            final Type<?> newBoundType = visit(oldBoundType, bindings);

            if (oldBoundType != newBoundType) {
                newTypeBindings = newTypeBindings.withAdditionalBinding(
                    genericParameter,
                    newBoundType
                );
            }
        }

        return newTypeBindings;
    }

    public FieldInfo visitField(final Type<?> declaringType, final FieldInfo field, final TypeBindings bindings) {
        final Type<?> oldFieldType = field.getFieldType();
        final Type<?> newFieldType = visit(field.getFieldType(), bindings);

        final Type<?> actualDeclaringType;
        final Type oldDeclaringType = field.getDeclaringType();

        if (!TypeUtils.areEquivalent(oldDeclaringType, declaringType) &&
            oldDeclaringType.isGenericTypeDefinition() &&
            TypeUtils.areEquivalent(oldDeclaringType, declaringType.getGenericTypeDefinition())) {

            actualDeclaringType = declaringType;
        }
        else {
            actualDeclaringType = oldDeclaringType;
        }

        if (TypeUtils.areEquivalent(oldFieldType, newFieldType) &&
            TypeUtils.areEquivalent(actualDeclaringType, declaringType)) {

            return field;
        }

        return new ReflectedField(actualDeclaringType, field.getRawField(), newFieldType);
    }

    public ParameterList visitParameters(final ParameterList parameters, final TypeBindings bindings) {
        VerifyArgument.notNull(parameters, "parameters");

        ParameterInfo[] newParameters = null;

        for (int i = 0, n = parameters.size(); i < n; i++) {
            final ParameterInfo oldParameter = parameters.get(i);
            final Type<?> oldParameterType = oldParameter.getParameterType();
            final Type<?> newParameterType = visit(oldParameterType, bindings);

            if (newParameterType != oldParameterType) {
                if (newParameters == null) {
                    newParameters = parameters.toArray();
                }
                newParameters[i] = new ParameterInfo(oldParameter.getName(), i, newParameterType);
            }
        }

        if (newParameters != null) {
            return new ParameterList(newParameters);
        }

        return parameters;
    }

    public MemberInfo visitMember(final Type<?> declaringType, final MemberInfo member, final TypeBindings bindings) {
        switch (member.getMemberType()) {
            case Constructor:
                return visitConstructor(declaringType, (ConstructorInfo) member, bindings);

            case Field:
                return visitField(declaringType, (FieldInfo)member, bindings);

            case Method:
                return visitMethod(declaringType, (MethodInfo)member, bindings);

            case TypeInfo:
            case NestedType:
                return visitType((Type<?>)member, bindings);

            default:
                throw ContractUtils.unreachable();
        }
    }

    public MethodInfo visitMethod(final Type<?> declaringType, final MethodInfo method, final TypeBindings bindings) {
        if (method.isGenericMethod()) {
            boolean hasChanged = false;

            final MethodInfo newDefinition;

            if (!method.isGenericMethodDefinition()) {
                final MethodInfo oldDefinition = method.getGenericMethodDefinition();

                newDefinition = visitMethod(declaringType, oldDefinition, bindings);
                hasChanged = newDefinition != oldDefinition;
            }
            else {
                newDefinition = method.getGenericMethodDefinition();
            }

            final TypeBindings oldBindings = method.getTypeBindings();
            final TypeBindings newBindings;

            if (oldBindings.hasUnboundParameters()) {
                newBindings = visitTypeBindings(
                    oldBindings,
                    bindings.withAdditionalBindings(oldBindings)
                );

                hasChanged |= newBindings != oldBindings;
            }
            else {
                newBindings = oldBindings;
            }

            if (hasChanged) {
                return newDefinition.makeGenericMethod(newBindings.getBoundTypes());
            }

            return method;
        }

        final Type<?> oldReturnType = method.getReturnType();
        final Type<?> returnType = visit(oldReturnType, bindings);
        final ParameterList oldParameters = method.getParameters();
        final ParameterList newParameters = visitParameters(oldParameters, bindings);
        final TypeList oldThrownTypes = method.getThrownTypes();
        final Type<?>[] newThrownTypes = new Type<?>[oldThrownTypes.size()];

        boolean hasChanged = !oldReturnType.equals(returnType) || oldParameters != newParameters;
        boolean thrownTypesChanged = false;

        for (int i = 0, n = newThrownTypes.length; i < n; i++) {
            final Type<?> oldThrownType = oldThrownTypes.get(i);
            final Type<?> newThrownType = visit(oldThrownType, bindings);

            newThrownTypes[i] = newThrownType;

            if (!oldThrownType.equals(newThrownType)) {
                thrownTypesChanged = true;
            }
        }

        hasChanged |= thrownTypesChanged;

        final Type<?> actualDeclaringType;
        final Type oldDeclaringType = method.getDeclaringType();

        if (!TypeUtils.areEquivalent(oldDeclaringType, declaringType) &&
            oldDeclaringType.isGenericTypeDefinition() &&
            declaringType.isGenericType() &&
            TypeUtils.areEquivalent(oldDeclaringType, declaringType.getGenericTypeDefinition())) {

            actualDeclaringType = declaringType;
        }
        else {
            actualDeclaringType = oldDeclaringType;
        }

        if (!hasChanged) {
            if (!TypeUtils.areEquivalent(oldDeclaringType, declaringType)) {
                return new ReflectedMethod(
                    actualDeclaringType,
                    declaringType,
                    method.getRawMethod(),
                    oldParameters,
                    method.getReturnType(),
                    method.getThrownTypes(),
                    method.getTypeBindings()
                );
            }
            return method;
        }

        return new ReflectedMethod(
            actualDeclaringType,
            declaringType,
            method.getRawMethod(),
            newParameters,
            returnType,
            thrownTypesChanged ? new TypeList(newThrownTypes) : oldThrownTypes,
            method.getTypeBindings()
        );
    }

    public ConstructorInfo visitConstructor(final Type<?> declaringType, final ConstructorInfo constructor, final TypeBindings bindings) {
        final ParameterList parameters = constructor.getParameters();
        final TypeList thrown = constructor.getThrownTypes();
        final Type<?>[] parameterTypes = new Type<?>[parameters.size()];
        final Type<?>[] thrownTypes = new Type<?>[thrown.size()];

        boolean hasChanged = false;
        boolean thrownTypesChanged = false;

        for (int i = 0, n = parameterTypes.length; i < n; i++) {
            final Type<?> oldParameterType = parameters.get(i).getParameterType();
            parameterTypes[i] = visit(oldParameterType, bindings);
            if (!oldParameterType.equals(parameterTypes[i])) {
                hasChanged = true;
            }
        }

        for (int i = 0, n = thrownTypes.length; i < n; i++) {
            final Type<?> oldThrownType = thrown.get(i);
            final Type<?> newThrownType = visit(oldThrownType, bindings);

            thrownTypes[i] = newThrownType;

            if (!oldThrownType.equals(newThrownType)) {
                thrownTypesChanged = true;
            }
        }

        hasChanged |= thrownTypesChanged;

        final Type<?> actualDeclaringType;
        final Type oldDeclaringType = constructor.getDeclaringType();

        if (!TypeUtils.areEquivalent(oldDeclaringType, declaringType) &&
            oldDeclaringType.isGenericTypeDefinition() &&
            TypeUtils.areEquivalent(oldDeclaringType, declaringType.getGenericTypeDefinition())) {

            actualDeclaringType = declaringType;
        }
        else {
            actualDeclaringType = oldDeclaringType;
        }

        if (!hasChanged) {
            if (!TypeUtils.areEquivalent(constructor.getDeclaringType(), declaringType)) {
                return new ReflectedConstructor(
                    actualDeclaringType,
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
                    i,
                    parameterTypes[i]
                )
            );
        }

        return new ReflectedConstructor(
            actualDeclaringType,
            constructor.getRawConstructor(),
            new ParameterList(newParameters),
            new TypeList(thrownTypes)
        );
    }

    @Override
    public Type<?> visitClassType(final Type<?> type, final TypeBindings bindings) {
        if (bindings.containsGenericParameter(type)) {
            return bindings.getBoundType(type);
        }

        final TypeBindings oldTypeBindings = type.getTypeBindings();
        final TypeBindings newTypeBindings = visitTypeBindings(oldTypeBindings, bindings);

        if (oldTypeBindings != newTypeBindings) {
            final Type<?> cachedType = Type.CACHE.find(
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

            return genericType;
        }

        return type;
    }

    @Override
    public Type<?> visitTypeParameter(final Type<?> type, final TypeBindings bindings) {
        if (bindings.containsGenericParameter(type)) {
            return bindings.getBoundType(type);
        }

        final Type<?> upperBound = type.getExtendsBound();
        final Type<?> newUpperBound = visit(upperBound, bindings);

        if (newUpperBound != upperBound) {
            if (type.getDeclaringMethod() != null) {
                return new GenericParameter(
                    type.getFullName(),
                    type.getDeclaringMethod(),
                    newUpperBound,
                    type.getGenericParameterPosition()
                );
            }
            return new GenericParameter(
                type.getFullName(),
                type.getDeclaringType(),
                newUpperBound,
                type.getGenericParameterPosition()
            );
        }

        return type;
    }

    @Override
    public Type<?> visitWildcardType(final Type<?> type, final TypeBindings bindings) {
        final Type<?> oldLower = type.getSuperBound();
        final Type<?> oldUpper = type.getExtendsBound();
        final Type<?> newLower = visit(oldLower, bindings);
        final Type<?> newUpper = visit(oldUpper, bindings);

        if (newLower != oldLower || newUpper != oldUpper) {
            return new WildcardType<>(newUpper, newLower);
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
}

class TypeEraser extends TypeBinder {
    @Override
    public Type<?> visit(final Type<?> type) {
        return visit(type, TypeBindings.empty());
    }

    @Override
    public Type<?> visitClassType(final Type<?> type, final TypeBindings bindings) {
        if (type instanceof ErasedType<?>) {
            return type;
        }
        return type.getErasedType();
    }

    @Override
    public Type<?> visitTypeParameter(final Type<?> type, final TypeBindings bindings) {
        return visit(type.getExtendsBound());
    }

    @Override
    public Type<?> visitWildcardType(final Type<?> type, final TypeBindings bindings) {
        return visit(type.getExtendsBound());
    }

    @Override
    public Type<?> visitArrayType(final Type<?> type, final TypeBindings bindings) {
        final Type<?> oldElementType = type.getElementType();
        final Type<?> newElementType = visit(oldElementType);

        if (newElementType != oldElementType) {
            return newElementType.makeArrayType();
        }

        return type;
    }
}