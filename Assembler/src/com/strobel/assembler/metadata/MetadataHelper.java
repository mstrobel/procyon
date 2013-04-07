/*
 * MetadataHelper.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
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
import com.strobel.reflection.SimpleType;

public final class MetadataHelper {
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
            return BuiltinTypes.Object;
        }

        TypeReference result = findCommonSuperTypeCore(elementType1, elementType2);

        while (rank1-- > 0) {
            result = result.makeArrayType();
        }

        return result;
    }

    private static TypeReference findCommonSuperTypeCore(final TypeReference type1, final TypeReference type2) {
        final TypeDefinition c = type1.resolve();
        final TypeDefinition d = type2.resolve();

        if (isAssignableFrom(c, d)) {
            return type1;
        }

        if (isAssignableFrom(d, c)) {
            return type2;
        }

        if (c.isInterface() || d.isInterface()) {
            return BuiltinTypes.Object;
        }

        TypeDefinition current = c;

        do {
            final TypeReference baseType = current.getBaseType();

            if (baseType == null || (current = baseType.resolve()) == null) {
                return BuiltinTypes.Object;
            }
        }
        while (!isAssignableFrom(current, d));

        return current;
    }

    public static boolean isAssignableFrom(final TypeReference target, final TypeReference source) {
        VerifyArgument.notNull(source, "source");
        VerifyArgument.notNull(target, "target");

        if (target.isPrimitive()) {
            final SimpleType targetSimpleType = target.getSimpleType();
            final SimpleType sourceSimpleType = source.getSimpleType();

            if (targetSimpleType == sourceSimpleType) {
                return true;
            }

            if (sourceSimpleType == SimpleType.Boolean) {
                return false;
            }

            if (targetSimpleType.isIntegral()) {
                return sourceSimpleType.isIntegral() &&
                       sourceSimpleType.bitWidth() <= targetSimpleType.bitWidth();
            }

            if (targetSimpleType.isFloating()) {
                return sourceSimpleType.isFloating() &&
                       sourceSimpleType.bitWidth() <= targetSimpleType.bitWidth();
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

        while (current != null) {
            if (MetadataResolver.areEquivalent(current, baseType)) {
                return true;
            }

            final TypeDefinition resolved = current.resolve();

            if (resolved == null) {
                return false;
            }

            current = resolved.getBaseType();
        }

        return false;
    }
}
