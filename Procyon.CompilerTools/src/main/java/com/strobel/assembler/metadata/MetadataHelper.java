/*
 * java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler.metadata;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MetadataHelper {
    private final static Set<String> BOXED_TYPES;

    static {
        final HashSet<String> boxedNumericTypes = new HashSet<>();

        boxedNumericTypes.add("java/lang/Byte");
        boxedNumericTypes.add("java/lang/Character");
        boxedNumericTypes.add("java/lang/Short");
        boxedNumericTypes.add("java/lang/Integer");
        boxedNumericTypes.add("java/lang/Long");
        boxedNumericTypes.add("java/lang/Float");
        boxedNumericTypes.add("java/lang/Double");

        BOXED_TYPES = boxedNumericTypes;
    }

    public static boolean isEnclosedBy(final TypeReference innerType, final TypeReference outerType) {
        if (innerType == null) {
            return false;
        }

        for (TypeReference current = innerType.getDeclaringType();
             current != null;
             current = current.getDeclaringType()) {

            if (MetadataResolver.areEquivalent(current, outerType)) {
                return true;
            }
        }

        final TypeDefinition resolvedInnerType = innerType.resolve();

        return resolvedInnerType != null &&
               isEnclosedBy(resolvedInnerType.getBaseType(), outerType);
    }

    public static TypeReference findCommonSuperType(final TypeReference type1, final TypeReference type2) {
        VerifyArgument.notNull(type1, "type1");
        VerifyArgument.notNull(type2, "type2");

        int rank1 = 0;
        int rank2 = 0;

        TypeReference elementType1 = type1;
        TypeReference elementType2 = type2;

        while (elementType1.isArray()) {
            elementType1 = elementType1.getElementType();
            ++rank1;
        }

        while (elementType2.isArray()) {
            elementType2 = elementType2.getElementType();
            ++rank2;
        }

        if (rank1 != rank2) {
            return BuiltinTypes.Object;
        }

        if (rank1 != 0 && (elementType1.isPrimitive() || elementType2.isPrimitive())) {
            if (elementType1.isPrimitive() && elementType2.isPrimitive()) {
                return doNumericPromotion(elementType1, elementType2);
            }
            return BuiltinTypes.Object;
        }

        while (elementType1.isBoundedType()) {
            elementType1 = elementType1.hasSuperBound() ? elementType1.getSuperBound()
                                                        : elementType1.getExtendsBound();
        }

        while (elementType2.isBoundedType()) {
            elementType2 = elementType2.hasSuperBound() ? elementType2.getSuperBound()
                                                        : elementType2.getExtendsBound();
        }

        TypeReference result = findCommonSuperTypeCore(elementType1, elementType2);

        while (rank1-- > 0) {
            result = result.makeArrayType();
        }

        return result;
    }

    private static TypeReference doNumericPromotion(final TypeReference leftType, final TypeReference rightType) {
        final JvmType left = leftType.getSimpleType();
        final JvmType right = rightType.getSimpleType();

        if (left == right) {
            return leftType;
        }

        if (left == JvmType.Double || right == JvmType.Double) {
            return BuiltinTypes.Double;
        }

        if (left == JvmType.Float || right == JvmType.Float) {
            return BuiltinTypes.Float;
        }

        if (left == JvmType.Long || right == JvmType.Long) {
            return BuiltinTypes.Long;
        }

        if (left.isNumeric() && left != JvmType.Boolean || right.isNumeric() && right != JvmType.Boolean) {
            return BuiltinTypes.Integer;
        }

        return leftType;
    }

    private static TypeReference findCommonSuperTypeCore(final TypeReference type1, final TypeReference type2) {
        if (isAssignableFrom(type1, type2)) {
            return substituteGenericArguments(type1, type2);
        }

        if (isAssignableFrom(type2, type1)) {
            return substituteGenericArguments(type2, type1);
        }

        final TypeDefinition c = type1.resolve();
        final TypeDefinition d = type2.resolve();

        if (c == null || d == null || c.isInterface() || d.isInterface()) {
            return BuiltinTypes.Object;
        }

        TypeReference current = c;

        while (current != null) {
            for (final TypeReference interfaceType : getInterfaces(current)) {
                if (isAssignableFrom(interfaceType, d)) {
                    return interfaceType;
                }
            }

            current = getBaseType(current);

            if (current != null) {
                if (isAssignableFrom(current, d)) {
                    return current;
                }
            }
        }

        return BuiltinTypes.Object;

//        do {
//            final TypeReference baseType = current.getBaseType();
//
//            if (baseType == null || (current = baseType.resolve()) == null) {
//                return BuiltinTypes.Object;
//            }
//        }
//        while (!isAssignableFrom(current, d));
//
//        return current;
    }

    public static NumericConversionType getNumericConversionType(final TypeReference target, final TypeReference source) {
        VerifyArgument.notNull(source, "source");
        VerifyArgument.notNull(target, "target");

        if (target == source && BOXED_TYPES.contains(target.getInternalName())) {
            return NumericConversionType.IDENTITY;
        }

        if (!source.isPrimitive()) {
            final TypeReference unboxedSourceType;

            switch (source.getInternalName()) {
                case "java/lang/Byte":
                    unboxedSourceType = BuiltinTypes.Byte;
                    break;
                case "java/lang/Character":
                    unboxedSourceType = BuiltinTypes.Character;
                    break;
                case "java/lang/Short":
                    unboxedSourceType = BuiltinTypes.Short;
                    break;
                case "java/lang/Integer":
                    unboxedSourceType = BuiltinTypes.Integer;
                    break;
                case "java/lang/Long":
                    unboxedSourceType = BuiltinTypes.Long;
                    break;
                case "java/lang/Float":
                    unboxedSourceType = BuiltinTypes.Float;
                    break;
                case "java/lang/Double":
                    unboxedSourceType = BuiltinTypes.Double;
                    break;
                default:
                    return NumericConversionType.NONE;
            }

            final NumericConversionType unboxedConversion = getNumericConversionType(target, unboxedSourceType);

            switch (unboxedConversion) {
                case IDENTITY:
                case IMPLICIT:
                    return NumericConversionType.IMPLICIT;
                case EXPLICIT:
                    return NumericConversionType.NONE;
                default:
                    return unboxedConversion;
            }
        }

        if (!target.isPrimitive()) {
            final TypeReference unboxedTargetType;

            switch (target.getInternalName()) {
                case "java/lang/Byte":
                    unboxedTargetType = BuiltinTypes.Byte;
                    break;
                case "java/lang/Character":
                    unboxedTargetType = BuiltinTypes.Character;
                    break;
                case "java/lang/Short":
                    unboxedTargetType = BuiltinTypes.Short;
                    break;
                case "java/lang/Integer":
                    unboxedTargetType = BuiltinTypes.Integer;
                    break;
                case "java/lang/Long":
                    unboxedTargetType = BuiltinTypes.Long;
                    break;
                case "java/lang/Float":
                    unboxedTargetType = BuiltinTypes.Float;
                    break;
                case "java/lang/Double":
                    unboxedTargetType = BuiltinTypes.Double;
                    break;
                default:
                    return NumericConversionType.NONE;
            }

            switch (getNumericConversionType(unboxedTargetType, source)) {
                case IDENTITY:
                    return NumericConversionType.IMPLICIT;
                case IMPLICIT:
                    return NumericConversionType.EXPLICIT_TO_UNBOXED;
                case EXPLICIT:
                    return NumericConversionType.EXPLICIT;
                default:
                    return NumericConversionType.NONE;
            }
        }

        final JvmType targetJvmType = target.getSimpleType();
        final JvmType sourceJvmType = source.getSimpleType();

        if (targetJvmType == sourceJvmType) {
            return NumericConversionType.IDENTITY;
        }

        if (sourceJvmType == JvmType.Boolean) {
            return NumericConversionType.NONE;
        }

        switch (targetJvmType) {
            case Float:
            case Double:
                if (sourceJvmType.bitWidth() <= targetJvmType.bitWidth()) {
                    return NumericConversionType.IMPLICIT;
                }
                return NumericConversionType.EXPLICIT;

            case Byte:
            case Short:
            case Integer:
            case Long:
                if (sourceJvmType.isIntegral() &&
                    sourceJvmType.bitWidth() <= targetJvmType.bitWidth()) {

                    return NumericConversionType.IMPLICIT;
                }

                return NumericConversionType.EXPLICIT;
        }

        return NumericConversionType.NONE;
    }

    public static boolean hasImplicitNumericConversion(final TypeReference target, final TypeReference source) {
        VerifyArgument.notNull(source, "source");
        VerifyArgument.notNull(target, "target");

        if (target == source) {
            return true;
        }

        if (!target.isPrimitive() || !source.isPrimitive()) {
            return false;
        }

        final JvmType targetJvmType = target.getSimpleType();
        final JvmType sourceJvmType = source.getSimpleType();

        if (targetJvmType == sourceJvmType) {
            return true;
        }

        if (sourceJvmType == JvmType.Boolean) {
            return false;
        }

        switch (targetJvmType) {
            case Float:
            case Double:
                return sourceJvmType.bitWidth() <= targetJvmType.bitWidth();

            case Byte:
            case Short:
            case Integer:
            case Long:
                return sourceJvmType.isIntegral() &&
                       sourceJvmType.bitWidth() <= targetJvmType.bitWidth();
        }

        return false;
    }

    public static boolean isAssignableFrom(final TypeReference target, final TypeReference source) {
        VerifyArgument.notNull(source, "source");
        VerifyArgument.notNull(target, "target");

        if (target == source) {
            return true;
        }

        if (target.isArray()) {
            return source.isArray() &&
                   isAssignableFrom(target.getElementType(), source.getElementType());
        }

        if (target.isPrimitive()) {
            if (source == BuiltinTypes.Null) {
                return false;
            }

            final JvmType targetJvmType = target.getSimpleType();
            final JvmType sourceJvmType = source.getSimpleType();

            if (sourceJvmType == JvmType.Boolean) {
                return false;
            }

            switch (targetJvmType) {
                case Boolean:
                case Character:
                    return targetJvmType == sourceJvmType;

                case Float:
                case Double:
                    return sourceJvmType.isFloating() &&
                           sourceJvmType.bitWidth() <= targetJvmType.bitWidth();

                case Byte:
                case Short:
                case Integer:
                case Long:
                    return sourceJvmType.isIntegral() &&
                           sourceJvmType.bitWidth() <= targetJvmType.bitWidth();
            }
        }

        return source == BuiltinTypes.Null ||
               BuiltinTypes.Object.equals(target) ||
               isSubType(source, target);
    }

    public static boolean isSubType(final TypeReference type, final TypeReference baseType) {
        VerifyArgument.notNull(type, "type");
        VerifyArgument.notNull(baseType, "baseType");

        TypeReference current = type;

        final TypeDefinition resolvedBaseType = baseType.resolve();

        while (current != null) {
            if (MetadataResolver.areEquivalent(current, baseType)) {
                return true;
            }

            if (resolvedBaseType != null && resolvedBaseType.isInterface()) {
                for (final TypeReference interfaceType : getInterfaces(current)) {
                    if (MetadataResolver.areEquivalent(interfaceType, resolvedBaseType)) {
                        return true;
                    }
                }
            }

            current = getBaseType(current);
        }

        return false;
    }

    public static boolean isPrimitiveBoxType(final TypeReference type) {
        VerifyArgument.notNull(type, "type");

        switch (type.getInternalName()) {
            case "java/lang/Void":
            case "java/lang/Boolean":
            case "java/lang/Byte":
            case "java/lang/Character":
            case "java/lang/Short":
            case "java/lang/Integer":
            case "java/lang/Long":
            case "java/lang/Float":
            case "java/lang/Double":
                return true;

            default:
                return false;
        }
    }

    public static TypeReference getUnderlyingPrimitiveTypeOrSelf(final TypeReference type) {
        VerifyArgument.notNull(type, "type");

        switch (type.getInternalName()) {
            case "java/lang/Void":
                return BuiltinTypes.Void;
            case "java/lang/Boolean":
                return BuiltinTypes.Boolean;
            case "java/lang/Byte":
                return BuiltinTypes.Byte;
            case "java/lang/Character":
                return BuiltinTypes.Character;
            case "java/lang/Short":
                return BuiltinTypes.Short;
            case "java/lang/Integer":
                return BuiltinTypes.Integer;
            case "java/lang/Long":
                return BuiltinTypes.Long;
            case "java/lang/Float":
                return BuiltinTypes.Float;
            case "java/lang/Double":
                return BuiltinTypes.Double;
            default:
                return type;
        }
    }

    public static TypeReference getBaseType(final TypeReference type) {
        if (type == null) {
            return null;
        }

        final TypeDefinition resolvedType = type.resolve();

        if (resolvedType == null) {
            return null;
        }

        return substituteGenericArguments(resolvedType.getBaseType(), type);
    }

    public static List<TypeReference> getInterfaces(final TypeReference type) {
        if (type == null) {
            return Collections.emptyList();
        }

        final TypeDefinition resolvedType = type.resolve();

        if (resolvedType == null) {
            return Collections.emptyList();
        }

        final List<TypeReference> interfaces = resolvedType.getExplicitInterfaces();
        final TypeReference[] boundInterfaces = new TypeReference[interfaces.size()];

        for (int i = 0; i < boundInterfaces.length; i++) {
            boundInterfaces[i] = substituteGenericArguments(interfaces.get(i), type);
        }

        return ArrayUtilities.asUnmodifiableList(boundInterfaces);
    }

    public static TypeReference asSubType(final TypeReference type, final TypeReference baseType) {
        VerifyArgument.notNull(type, "type");
        VerifyArgument.notNull(baseType, "baseType");

        return substituteGenericArguments(type, getSubTypeMappings(type, baseType));
    }

    @SuppressWarnings("ConstantConditions")
    private static Map<TypeReference, TypeReference> getSubTypeMappings(final TypeReference type, final TypeReference baseType) {
        VerifyArgument.notNull(type, "type");
        VerifyArgument.notNull(baseType, "baseType");

        TypeReference current = type;

        final List<? extends TypeReference> baseArguments;

        if (baseType.isGenericDefinition()) {
            baseArguments = baseType.getGenericParameters();
        }
        else if (baseType.isGenericType()) {
            baseArguments = ((IGenericInstance) baseType).getTypeArguments();
        }
        else {
            baseArguments = Collections.emptyList();
        }

        final TypeDefinition resolvedBaseType = baseType.resolve();

        while (current != null) {
            final TypeDefinition resolved = current.resolve();
            
            if (resolvedBaseType != null &&
                resolvedBaseType.isGenericDefinition() &&
                MetadataResolver.areEquivalent(resolved, resolvedBaseType)) {

                if (current instanceof IGenericInstance) {
                    final List<? extends TypeReference> typeArguments = ((IGenericInstance) current).getTypeArguments();

                    if (baseArguments.size() == typeArguments.size()) {
                        final Map<TypeReference, TypeReference> map = new HashMap<>();

                        for (int i = 0; i < typeArguments.size(); i++) {
                            map.put(typeArguments.get(i), baseArguments.get(i));
                        }

                        return map;
                    }
                }
                else if (baseType instanceof IGenericInstance &&
                         resolved.isGenericDefinition()) {

                    final List<GenericParameter> genericParameters = resolved.getGenericParameters();
                    final List<? extends TypeReference> typeArguments = ((IGenericInstance) baseType).getTypeArguments();

                    if (genericParameters.size() == typeArguments.size()) {
                        final Map<TypeReference, TypeReference> map = new HashMap<>();

                        for (int i = 0; i < typeArguments.size(); i++) {
                            map.put(genericParameters.get(i), typeArguments.get(i));
                        }

                        return map;
                    }
                }
            }

            if (resolvedBaseType != null && resolvedBaseType.isInterface()) {
                for (final TypeReference interfaceType : getInterfaces(current)) {
                    final Map<TypeReference, TypeReference> interfaceMap = getSubTypeMappings(interfaceType, baseType);

                    if (!interfaceMap.isEmpty()) {
                        return interfaceMap;
                    }
                }
            }

            current = getBaseType(current);
        }

        return Collections.emptyMap();
    }

    public static MethodReference asMemberOf(final MethodReference method, final TypeReference baseType) {
        VerifyArgument.notNull(method, "method");
        VerifyArgument.notNull(baseType, "baseType");

        final Map<TypeReference, TypeReference> map = getSubTypeMappings(method.getDeclaringType(), baseType);

        return TypeSubstitutionVisitor.instance().visitMethod(method, map);
    }

    public static FieldReference asMemberOf(final FieldReference field, final TypeReference baseType) {
        VerifyArgument.notNull(field, "field");
        VerifyArgument.notNull(baseType, "baseType");

        final Map<TypeReference, TypeReference> map = getSubTypeMappings(field.getDeclaringType(), baseType);

        return TypeSubstitutionVisitor.instance().visitField(field, map);
    }

    public static TypeReference substituteGenericArguments(
        final TypeReference inputType,
        final TypeReference substitutionsProvider) {

        if (inputType == null) {
            return null;
        }

        if (substitutionsProvider == null || !substitutionsProvider.isGenericType()) {
            return inputType;
        }

        final List<? extends TypeReference> genericParameters = substitutionsProvider.getGenericParameters();
        final List<? extends TypeReference> typeArguments;

        if (substitutionsProvider.isGenericDefinition()) {
            typeArguments = genericParameters;
        }
        else {
            typeArguments = ((IGenericInstance) substitutionsProvider).getTypeArguments();
        }

        if (!isGenericSubstitutionNeeded(inputType) ||
            typeArguments.isEmpty() ||
            typeArguments.size() != genericParameters.size()) {

            return inputType;
        }

        final Map<TypeReference, TypeReference> map = new HashMap<>();

        for (int i = 0; i < typeArguments.size(); i++) {
            map.put(genericParameters.get(i), typeArguments.get(i));
        }

        return substituteGenericArguments(inputType, map);
    }

    public static TypeReference substituteGenericArguments(
        final TypeReference inputType,
        final MethodReference substitutionsProvider) {

        if (inputType == null) {
            return null;
        }

        if (substitutionsProvider == null || !isGenericSubstitutionNeeded(inputType)) {
            return inputType;
        }

        final TypeReference declaringType = substitutionsProvider.getDeclaringType();

        assert declaringType != null;

        if (!substitutionsProvider.isGenericMethod() && !declaringType.isGenericType()) {
            return null;
        }

        final List<? extends TypeReference> methodGenericParameters;
        final List<? extends TypeReference> genericParameters;
        final List<? extends TypeReference> methodTypeArguments;
        final List<? extends TypeReference> typeArguments;

        if (substitutionsProvider.isGenericMethod()) {
            methodGenericParameters = substitutionsProvider.getGenericParameters();
        }
        else {
            methodGenericParameters = Collections.emptyList();
        }

        if (substitutionsProvider.isGenericDefinition()) {
            methodTypeArguments = methodGenericParameters;
        }
        else {
            methodTypeArguments = ((IGenericInstance) substitutionsProvider).getTypeArguments();
        }

        if (declaringType.isGenericType()) {
            genericParameters = declaringType.getGenericParameters();

            if (declaringType.isGenericDefinition()) {
                typeArguments = genericParameters;
            }
            else {
                typeArguments = ((IGenericInstance) declaringType).getTypeArguments();
            }
        }
        else {
            genericParameters = Collections.emptyList();
            typeArguments = Collections.emptyList();
        }

        if (methodTypeArguments.isEmpty() && typeArguments.isEmpty()) {
            return inputType;
        }

        final Map<TypeReference, TypeReference> map = new HashMap<>();

        if (methodTypeArguments.size() == methodGenericParameters.size()) {
            for (int i = 0; i < methodTypeArguments.size(); i++) {
                map.put(methodGenericParameters.get(i), methodTypeArguments.get(i));
            }
        }

        if (typeArguments.size() == genericParameters.size()) {
            for (int i = 0; i < typeArguments.size(); i++) {
                map.put(genericParameters.get(i), typeArguments.get(i));
            }
        }

        return substituteGenericArguments(inputType, map);
    }

    public static TypeReference substituteGenericArguments(
        final TypeReference inputType,
        final Map<TypeReference, TypeReference> substitutionsProvider) {

        if (inputType == null) {
            return null;
        }

        if (substitutionsProvider == null || substitutionsProvider.isEmpty()) {
            return inputType;
        }

        return TypeSubstitutionVisitor.instance().visit(inputType, substitutionsProvider);
    }

    private static boolean isGenericSubstitutionNeeded(final TypeReference type) {
        if (type == null) {
            return false;
        }

        final TypeDefinition resolvedType = type.resolve();

        return resolvedType != null &&
               resolvedType.containsGenericParameters();
    }
}
