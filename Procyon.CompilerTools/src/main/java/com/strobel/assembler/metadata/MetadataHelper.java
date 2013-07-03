/*
 * MetadataHelper.java
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

import com.strobel.core.VerifyArgument;

public final class MetadataHelper {
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
            return type1;
        }

        if (isAssignableFrom(type2, type1)) {
            return type2;
        }

        final TypeDefinition c = type1.resolve();
        final TypeDefinition d = type2.resolve();

        if (c == null || d == null || c.isInterface() || d.isInterface()) {
            return BuiltinTypes.Object;
        }

        TypeDefinition current = c;

        while (current != null) {
            for (final TypeReference interfaceType : current.getExplicitInterfaces()) {
                if (isAssignableFrom(interfaceType, d)) {
                    return interfaceType;
                }
            }

            final TypeReference baseType = current.getBaseType();

            if (baseType != null) {
                current = baseType.resolve();
            }
            else {
                current = null;
            }

            if (current != null) {
                if (isAssignableFrom(current, d)) {
                    return current;
                }
            }
        }

        return BuiltinTypes.Object;
    }

    public static boolean isAssignableFrom(final TypeReference target, final TypeReference source) {
        VerifyArgument.notNull(source, "source");
        VerifyArgument.notNull(target, "target");

        if (target.isPrimitive()) {
            final JvmType targetJvmType = target.getSimpleType();
            final JvmType sourceJvmType = source.getSimpleType();

            if (targetJvmType == sourceJvmType) {
                return true;
            }

            if (sourceJvmType == JvmType.Boolean) {
                return false;
            }

            if (targetJvmType.isIntegral()) {
                return sourceJvmType.isIntegral() &&
                       sourceJvmType.bitWidth() <= targetJvmType.bitWidth();
            }

            if (targetJvmType.isFloating()) {
                return sourceJvmType.isFloating() &&
                       sourceJvmType.bitWidth() <= targetJvmType.bitWidth();
            }

            return false;
        }

        return BuiltinTypes.Object.equals(target) ||
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

            final TypeDefinition resolved = current.resolve();

            if (resolved == null) {
                return false;
            }

            if (resolvedBaseType != null && resolvedBaseType.isInterface()) {
                for (final TypeReference interfaceType : resolved.getExplicitInterfaces()) {
                    if (MetadataResolver.areEquivalent(interfaceType, resolvedBaseType)) {
                        return true;
                    }
                }
            }

            current = resolved.getBaseType();
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
}
