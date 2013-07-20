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

import com.strobel.collections.ListBuffer;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.Pair;
import com.strobel.core.Predicate;
import com.strobel.core.Predicates;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;

import java.util.*;

import static com.strobel.core.CollectionUtilities.firstOrDefault;

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

            if (isSameType(current, outerType)) {
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
                TypeReference promotedType = doNumericPromotion(elementType1, elementType2);

                while (rank1-- > 0) {
                    promotedType = promotedType.makeArrayType();
                }

                return promotedType;
            }
            return BuiltinTypes.Object;
        }

        while (!elementType1.isUnbounded()) {
            elementType1 = elementType1.hasSuperBound() ? elementType1.getSuperBound()
                                                        : elementType1.getExtendsBound();
        }

        while (!elementType2.isUnbounded()) {
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

    public static ConversionType getConversionType(final TypeReference target, final TypeReference source) {
        VerifyArgument.notNull(source, "source");
        VerifyArgument.notNull(target, "target");

        final TypeReference underlyingTarget = getUnderlyingPrimitiveTypeOrSelf(target);
        final TypeReference underlyingSource = getUnderlyingPrimitiveTypeOrSelf(source);

        if (underlyingTarget.getSimpleType().isNumeric() && underlyingSource.getSimpleType().isNumeric()) {
            return getNumericConversionType(target, source);
        }

        if (isSameType(target, source, true)) {
            return ConversionType.IDENTITY;
        }

        if (isAssignableFrom(target, source)) {
            return ConversionType.IMPLICIT;
        }

        int targetRank = 0;
        int sourceRank = 0;

        TypeReference targetElementType = target;
        TypeReference sourceElementType = source;

        while (targetElementType.isArray()) {
            ++targetRank;
            targetElementType = targetElementType.getElementType();
        }

        while (sourceElementType.isArray()) {
            ++sourceRank;
            sourceElementType = sourceElementType.getElementType();
        }

        if (sourceRank != targetRank) {
            return ConversionType.NONE;
        }

        return ConversionType.EXPLICIT;
    }

    public static ConversionType getNumericConversionType(final TypeReference target, final TypeReference source) {
        VerifyArgument.notNull(source, "source");
        VerifyArgument.notNull(target, "target");

        if (target == source && BOXED_TYPES.contains(target.getInternalName())) {
            return ConversionType.IDENTITY;
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
                    return ConversionType.NONE;
            }

            final ConversionType unboxedConversion = getNumericConversionType(target, unboxedSourceType);

            switch (unboxedConversion) {
                case IDENTITY:
                case IMPLICIT:
                    return ConversionType.IMPLICIT;
                case EXPLICIT:
                    return ConversionType.NONE;
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
                    return ConversionType.NONE;
            }

            switch (getNumericConversionType(unboxedTargetType, source)) {
                case IDENTITY:
                    return ConversionType.IMPLICIT;
                case IMPLICIT:
                    return ConversionType.EXPLICIT_TO_UNBOXED;
                case EXPLICIT:
                    return ConversionType.EXPLICIT;
                default:
                    return ConversionType.NONE;
            }
        }

        final JvmType targetJvmType = target.getSimpleType();
        final JvmType sourceJvmType = source.getSimpleType();

        if (targetJvmType == sourceJvmType) {
            return ConversionType.IDENTITY;
        }

        if (sourceJvmType == JvmType.Boolean) {
            return ConversionType.NONE;
        }

        switch (targetJvmType) {
            case Float:
            case Double:
                if (sourceJvmType.bitWidth() <= targetJvmType.bitWidth()) {
                    return ConversionType.IMPLICIT;
                }
                return ConversionType.EXPLICIT;

            case Byte:
            case Short:
            case Integer:
            case Long:
                if (sourceJvmType.isIntegral() &&
                    sourceJvmType.bitWidth() <= targetJvmType.bitWidth()) {

                    return ConversionType.IMPLICIT;
                }

                return ConversionType.EXPLICIT;
        }

        return ConversionType.NONE;
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

    public static boolean isConvertible(final TypeReference source, final TypeReference target) {
        VerifyArgument.notNull(source, "source");
        VerifyArgument.notNull(target, "target");

        final boolean tPrimitive = source.isPrimitive();
        final boolean sPrimitive = target.isPrimitive();

        if (source == BuiltinTypes.Null) {
            return !tPrimitive;
        }

        if (tPrimitive == sPrimitive) {
            return isSubTypeUnchecked(source, target);
        }

        if (tPrimitive) {
            switch (getNumericConversionType(target, source)) {
                case IDENTITY:
                case IMPLICIT:
                    return true;
                default:
                    return false;
            }
        }

        return isSubType(getUnderlyingPrimitiveTypeOrSelf(source), target);
    }

    private static boolean isSubTypeUnchecked(final TypeReference t, final TypeReference s) {
        return isSubtypeUncheckedInternal(t, s);
    }

    private static boolean isSubtypeUncheckedInternal(final TypeReference t, final TypeReference s) {
        if (t == s) {
            return true;
        }

        if (t == null || s == null) {
            return false;
        }

        if (t.isArray() && s.isArray()) {
            if (t.getElementType().isPrimitive()) {
                return isSameType(getElementType(t), getElementType(s));
            }
            else {
                return isSubTypeUnchecked(getElementType(t), getElementType(s));
            }
        }
        else if (isSubType(t, s)) {
            return true;
        }
        else if (t.isGenericParameter() && t.hasExtendsBound()) {
            return isSubTypeUnchecked(getUpperBound(t), s);
        }
        else if (!isRawType(s)) {
            final TypeReference t2 = asSuper(t, s);
            if (t2 != null && isRawType(t2)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isAssignableFrom(final TypeReference target, final TypeReference source) {
        return isConvertible(source, target);
    }

    public static boolean isSubType(final TypeReference type, final TypeReference baseType) {
        VerifyArgument.notNull(type, "type");
        VerifyArgument.notNull(baseType, "baseType");

        return IS_SUBTYPE_VISITOR.visit(type, baseType);
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
        final List<TypeReference> result = INTERFACES_VISITOR.visit(type);
        return result != null ? result : Collections.<TypeReference>emptyList();
    }

    public static TypeReference asSubType(final TypeReference type, final TypeReference baseType) {
        VerifyArgument.notNull(type, "type");
        VerifyArgument.notNull(baseType, "baseType");

        return substituteGenericArguments(type, getSubTypeMappings(type, baseType));
    }

    public static TypeReference asSuper(final TypeReference t, final TypeReference s) {
        VerifyArgument.notNull(t, "t");
        VerifyArgument.notNull(s, "s");

        return AS_SUPER_VISITOR.visit(t, s);
    }

    @SuppressWarnings("ConstantConditions")
    public static Map<TypeReference, TypeReference> getSubTypeMappings(final TypeReference type, final TypeReference baseType) {
        VerifyArgument.notNull(type, "type");
        VerifyArgument.notNull(baseType, "baseType");

        if (type.isArray() && baseType.isArray()) {
            TypeReference elementType = type.getElementType();
            TypeReference baseElementType = baseType.getElementType();

            while (elementType.isArray() && baseElementType.isArray()) {
                elementType = elementType.getElementType();
                baseElementType = baseElementType.getElementType();
            }

            return getSubTypeMappings(elementType, baseElementType);
        }

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
                isSameType(resolved, resolvedBaseType)) {

                if (current instanceof IGenericInstance &&
                    baseType instanceof IGenericInstance) {

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

        final MethodReference asMember = TypeSubstitutionVisitor.instance().visitMethod(method, map);

        if (asMember != method && asMember instanceof GenericMethodInstance) {
            ((GenericMethodInstance) asMember).setDeclaringType(baseType);
        }

        return asMember;
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

    public static List<MethodReference> findMethods(final TypeReference type) {
        return findMethods(type, Predicates.alwaysTrue());
    }

    public static List<MethodReference> findMethods(
        final TypeReference type,
        final Predicate<? super MethodReference> filter) {

        VerifyArgument.notNull(type, "type");
        VerifyArgument.notNull(filter, "filter");

        final Set<String> descriptors = new HashSet<>();
        final ArrayDeque<TypeReference> agenda = new ArrayDeque<>();

        List<MethodReference> results = null;

        agenda.addLast(type);
        descriptors.add(type.getInternalName());

        while (!agenda.isEmpty()) {
            final TypeDefinition resolvedType = agenda.removeFirst().resolve();

            if (resolvedType == null) {
                break;
            }

            final TypeReference baseType = resolvedType.getBaseType();

            if (baseType != null && descriptors.add(baseType.getInternalName())) {
                agenda.addLast(baseType);
            }

            for (final TypeReference interfaceType : resolvedType.getExplicitInterfaces()) {
                if (interfaceType != null && descriptors.add(interfaceType.getInternalName())) {
                    agenda.addLast(interfaceType);
                }
            }

            for (final MethodDefinition method : resolvedType.getDeclaredMethods()) {
                if (filter.test(method)) {
                    final String key = method.getName() + ":" + method.getErasedSignature();

                    if (descriptors.add(key)) {
                        if (results == null) {
                            results = new ArrayList<>();
                        }

                        final MethodReference asMember = asMemberOf(method, type);

                        results.add(asMember != null ? asMember : method);
                    }
                }
            }
        }

        return results != null ? results
                               : Collections.<MethodReference>emptyList();
    }

    public static boolean isOverloadCheckingRequired(final MethodReference method) {
        final MethodDefinition resolved = method.resolve();
        final boolean isVarArgs = resolved != null && resolved.isVarArgs();
        final TypeReference declaringType = (resolved != null ? resolved : method).getDeclaringType();
        final int parameterCount = (resolved != null ? resolved.getParameters() : method.getParameters()).size();

        final List<MethodReference> methods = findMethods(
            declaringType,
            Predicates.and(
                MetadataFilters.<MethodReference>matchName(method.getName()),
                new Predicate<MethodReference>() {
                    @Override
                    public boolean test(final MethodReference m) {
                        final List<ParameterDefinition> p = m.getParameters();

                        final MethodDefinition r = m instanceof MethodDefinition ? (MethodDefinition) m
                                                                                 : m.resolve();

                        if (r != null && r.isBridgeMethod()) {
                            return false;
                        }

                        if (isVarArgs) {
                            if (r != null && r.isVarArgs()) {
                                return true;
                            }
                            return p.size() >= parameterCount;
                        }

                        if (p.size() < parameterCount) {
                            return r != null && r.isVarArgs();
                        }

                        return p.size() == parameterCount;
                    }
                }
            )
        );

        return methods.size() > 1;
    }

    public static TypeReference getLowerBound(final TypeReference t) {
        return LOWER_BOUND_VISITOR.visit(t);
    }

    public static TypeReference getUpperBound(final TypeReference t) {
        return UPPER_BOUND_VISITOR.visit(t);
    }

    public static TypeReference getElementType(final TypeReference t) {
        if (t.isArray()) {
            return t.getElementType();
        }

        if (t.isWildcardType()) {
            return getElementType(getUpperBound(t));
        }

        return null;
    }

    public static TypeReference getSuperType(final TypeReference t) {
        if (t == null) {
            return null;
        }

        return SUPER_VISITOR.visit(t);
    }

    public static boolean isSubTypeNoCapture(final TypeReference type, final TypeReference baseType) {
        return isSubType(type, baseType, false);
    }

    public static boolean isSubType(final TypeReference type, final TypeReference baseType, final boolean capture) {
        if (type == baseType) {
            return true;
        }

        if (type == null || baseType == null) {
            return false;
        }

        if (baseType instanceof CompoundTypeReference) {
            final CompoundTypeReference c = (CompoundTypeReference) type;

            if (!isSubType(type, getSuperType(c), capture)) {
                return false;
            }

            for (final TypeReference interfaceType : c.getInterfaces()) {
                if (!isSubType(type, interfaceType, capture)) {
                    return false;
                }
            }

            return true;
        }

        final TypeReference lower = getLowerBound(baseType);

        if (lower != baseType) {
            return isSubType(capture ? capture(type) : type, lower, false);
        }

        return IS_SUBTYPE_VISITOR.visit(capture ? capture(type) : type, baseType);
    }

    private static TypeReference capture(final TypeReference type) {
        // TODO: Implement wildcard capture.
        return type;
    }

    public static Map<TypeReference, TypeReference> adapt(final TypeReference source, final TypeReference target) {
        final Adapter adapter = new Adapter();
        adapter.visit(source, target);
        return adapter.mapping;
    }

    private static Map<TypeReference, TypeReference> adaptSelf(final TypeReference t) {
        final TypeDefinition r = t.resolve();

        return r != null ? adapt(r, t)
                         : Collections.<TypeReference, TypeReference>emptyMap();
    }

    private static TypeReference rewriteSupers(final TypeReference t) {
        if (!(t instanceof IGenericInstance)) {
            return t;
        }

        final Map<TypeReference, TypeReference> map = adaptSelf(t);

        if (map.isEmpty()) {
            return t;
        }

        Map<TypeReference, TypeReference> rewrite = null;

        for (final TypeReference k : map.keySet()) {
            final TypeReference original = map.get(k);

            TypeReference s = rewriteSupers(original);

            if (s.hasSuperBound() && !s.hasExtendsBound()) {
                s = WildcardType.unbounded();

                if (rewrite == null) {
                    rewrite = new HashMap<>(map);
                }
            }
            else if (s != original) {
                s = WildcardType.makeExtends(getUpperBound(s));

                if (rewrite == null) {
                    rewrite = new HashMap<>(map);
                }
            }

            if (rewrite != null) {
                map.put(k, s);
            }
        }

        if (rewrite != null) {
            return substituteGenericArguments(t, rewrite);
        }

        else {
            return t;
        }
    }

    /**
     * Check if {@code t} contains {@code s}.
     *
     * <p>{@code T} contains {@code S} if:
     *
     * <p>{@code L(T) <: L(S) && U(S) <: U(T)}
     *
     * <p>This relation is only used by isSubType(), that is:
     *
     * <p>{@code C<S> <: C<T> if T contains S.}
     *
     * <p>Because of F-bounds, this relation can lead to infinite recursion.  Thus, we must
     * somehow break that recursion.  Notice that containsType() is only called from isSubType().
     * Since the arguments have already been checked against their bounds, we know:
     *
     * <p>{@code U(S) <: U(T) if T is "super" bound (U(T) *is* the bound)}
     *
     * <p>{@code L(T) <: L(S) if T is "extends" bound (L(T) is bottom)}
     *
     * @param t
     *     a type
     * @param s
     *     a type
     */
    public static boolean containsType(final TypeReference t, final TypeReference s) {
        return CONTAINS_TYPE_VISITOR.visit(t, s);
    }

    public static boolean isSameType(final TypeReference t, final TypeReference s) {
        return isSameType(t, s, false);
    }

    public static boolean isSameType(final TypeReference t, final TypeReference s, final boolean strict) {
        return strict ? SAME_TYPE_VISITOR_STRICT.visit(t, s)
                      : SAME_TYPE_VISITOR_LOOSE.visit(t, s);
    }

    public static boolean areSameTypes(final List<? extends TypeReference> t, final List<? extends TypeReference> s) {
        return areSameTypes(t, s, false);
    }

    public static boolean areSameTypes(
        final List<? extends TypeReference> t,
        final List<? extends TypeReference> s,
        final boolean strict) {

        if (t.size() != s.size()) {
            return false;
        }

        for (int i = 0, n = t.size(); i < n; i++) {
            if (!isSameType(t.get(i), s.get(i), strict)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isCaptureOf(final TypeReference t, final TypeReference s) {
        return isSameWildcard(t, s);
    }

    private static boolean isSameWildcard(final TypeReference t, final TypeReference s) {
        VerifyArgument.notNull(t, "t");
        VerifyArgument.notNull(s, "s");

        if (!t.isWildcardType() || !s.isWildcardType()) {
            return false;
        }

        if (t.isUnbounded()) {
            return s.isUnbounded();
        }

        if (t.hasSuperBound()) {
            return s.hasSuperBound() && isSameType(t.getSuperBound(), s.getSuperBound());
        }

        return s.hasExtendsBound() && isSameType(t.getExtendsBound(), s.getExtendsBound());
    }

    private static List<? extends TypeReference> getTypeArguments(final TypeReference t) {
        if (t instanceof IGenericInstance) {
            return ((IGenericInstance) t).getTypeArguments();
        }

        if (t.isGenericType()) {
            return t.getGenericParameters();
        }

        return Collections.emptyList();
    }

    private static boolean containsType(final List<? extends TypeReference> t, final List<? extends TypeReference> s) {
        if (t.size() != s.size()) {
            return false;
        }

        if (t.isEmpty()) {
            return true;
        }

        for (int i = 0, n = t.size(); i < n; i++) {
            if (!containsType(t.get(i), s.get(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean containsTypeEquivalent(final TypeReference t, final TypeReference s) {
        return isSameType(t, s) ||
               containsType(t, s) && containsType(s, t);
    }

    private static boolean containsTypeEquivalent(final List<? extends TypeReference> t, final List<? extends TypeReference> s) {
        if (t.size() != s.size()) {
            return false;
        }

        for (int i = 0, n = t.size(); i < n; i++) {
            if (!containsTypeEquivalent(t.get(i), s.get(i))) {
                return false;
            }
        }

        return true;
    }

    private final static ThreadLocal<HashSet<Pair<TypeReference, TypeReference>>> CONTAINS_TYPE_CACHE =
        new ThreadLocal<HashSet<Pair<TypeReference, TypeReference>>>() {
            @Override
            protected final HashSet<Pair<TypeReference, TypeReference>> initialValue() {
                return new HashSet<>();
            }
        };

    private final static ThreadLocal<HashSet<Pair<TypeReference, TypeReference>>> ADAPT_CACHE =
        new ThreadLocal<HashSet<Pair<TypeReference, TypeReference>>>() {
            @Override
            protected final HashSet<Pair<TypeReference, TypeReference>> initialValue() {
                return new HashSet<>();
            }
        };

    private static boolean containsTypeRecursive(final TypeReference t, final TypeReference s) {
        final HashSet<Pair<TypeReference, TypeReference>> cache = CONTAINS_TYPE_CACHE.get();
        final Pair<TypeReference, TypeReference> pair = new Pair<>(t, s);

        if (cache.add(pair)) {
            try {
                return containsType(getTypeArguments(t), getTypeArguments(s));
            }
            finally {
                cache.remove(pair);
            }
        }
        else {
            return containsType(getTypeArguments(t), getTypeArguments(rewriteSupers(s)));
        }
    }

    private static TypeReference arraySuperType(final TypeReference t) {
        final TypeDefinition resolved = t.resolve();

        if (resolved != null) {
            final IMetadataResolver resolver = resolved.getResolver();
            final TypeReference cloneable = resolver.lookupType("java/lang/Cloneable");
            final TypeReference serializable = resolver.lookupType("java/io/Serializable");

            if (cloneable != null) {
                if (serializable != null) {
                    return new CompoundTypeReference(
                        null,
                        ArrayUtilities.asUnmodifiableList(cloneable, serializable)
                    );
                }
                return cloneable;
            }

            if (serializable != null) {
                return serializable;
            }
        }

        return BuiltinTypes.Object;
    }

    public static boolean isRawType(final TypeReference t) {
        if (t == null) {
            return false;
        }

        if (t instanceof RawType) {
            return true;
        }

        if (t.isGenericType()) {
            return false;
        }

        final TypeReference r = t.resolve();

        if (r != null && r.isGenericType()) {
            return true;
        }

        return false;
    }

    public static List<TypeReference> eraseRecursive(final List<TypeReference> types) {
        ArrayList<TypeReference> result = null;

        for (int i = 0, n = types.size(); i < n; i++) {
            final TypeReference type = types.get(i);
            final TypeReference erased = eraseRecursive(type);

            if (result != null) {
                result.set(i, erased);
            }
            else if (type != erased) {
                result = new ArrayList<>(types);
                result.set(i, erased);
            }
        }

        return result != null ? result : types;
    }

    public static TypeReference eraseRecursive(final TypeReference type) {
        //
        // TODO: Implement recursive type erasure.
        //
        final TypeReference resolved = VerifyArgument.notNull(type, "type").resolve();

        return resolved.isGenericDefinition() ? new RawType(resolved)
                                              : type;
    }

    private static TypeReference classBound(final TypeReference t) {
        //
        // TODO: Implement class bound computation.
        //
        return t;
    }

    // <editor-fold defaultstate="collapsed" desc="Visitors">

    private final static TypeMapper<Void> UPPER_BOUND_VISITOR = new TypeMapper<Void>() {
        @Override
        public TypeReference visitType(final TypeReference t, final Void ignored) {
            return t.isUnbounded() || t.hasSuperBound() ? BuiltinTypes.Object
                                                        : visit(t.getExtendsBound());
        }
    };

    private final static TypeMapper<Void> LOWER_BOUND_VISITOR = new TypeMapper<Void>() {
        @Override
        public TypeReference visitWildcard(final WildcardType t, final Void ignored) {
            return t.hasSuperBound() ? visit(t.getSuperBound())
                                     : BuiltinTypes.Bottom;
        }
    };

    private final static TypeRelation IS_SUBTYPE_VISITOR = new TypeRelation() {
        @Override
        public Boolean visitArrayType(final ArrayType t, final TypeReference s) {
            if (s.isArray()) {
                final TypeReference et = getElementType(t);
                final TypeReference es = getElementType(s);

                if (et.isPrimitive()) {
                    return isSameType(et, es);
                }

                return isSubTypeNoCapture(et, es);
            }

            final String sName = s.getInternalName();

            return StringUtilities.equals(sName, "java/lang/Object") ||
                   StringUtilities.equals(sName, "java/lang/Cloneable") ||
                   StringUtilities.equals(sName, "java/io/Serializable");
        }

        @Override
        public Boolean visitBottomType(final TypeReference t, final TypeReference s) {
            switch (t.getSimpleType()) {
                case Object:
                case Array:
                case TypeVariable:
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public Boolean visitClassType(final TypeReference t, final TypeReference s) {
            final TypeReference superType = asSuper(t, s);

            return superType != null &&
                   StringUtilities.equals(superType.getInternalName(), s.getInternalName()) &&
                   // You're not allowed to write
                   //     Vector<Object> vec = new Vector<String>();
                   // But with wildcards you can write
                   //     Vector<? extends Object> vec = new Vector<String>();
                   // which means that subtype checking must be done
                   // here instead of same-type checking (via containsType).
                   (!(s instanceof IGenericInstance) || containsTypeRecursive(s, superType)) &&
                   isSubTypeNoCapture(superType.getDeclaringType(), s.getDeclaringType());
        }

        @Override
        public Boolean visitCompoundType(final CompoundTypeReference t, final TypeReference s) {
            return super.visitCompoundType(t, s);
        }

        @Override
        public Boolean visitGenericParameter(final GenericParameter t, final TypeReference s) {
            return isSubTypeNoCapture(
                t.hasExtendsBound() ? t.getExtendsBound() : BuiltinTypes.Object,
                s
            );
        }

        @Override
        public Boolean visitParameterizedType(final TypeReference t, final TypeReference s) {
            return visitClassType(t, s);
        }

        @Override
        public Boolean visitPrimitiveType(final PrimitiveType t, final TypeReference s) {
            final JvmType jt = t.getSimpleType();
            final JvmType js = s.getSimpleType();

            switch (jt) {
                case Boolean:
                    return js == JvmType.Boolean;

                case Byte:
                    return js != JvmType.Character && jt.bitWidth() < js.bitWidth();

                case Character:
                    return js != JvmType.Short && jt.bitWidth() < js.bitWidth();

                case Short:
                case Integer:
                case Long:
                case Float:
                case Double:
                    return jt.bitWidth() < js.bitWidth();

                case Void:
                    return s.getSimpleType() == JvmType.Void;

                default:
                    return Boolean.FALSE;
            }
        }

        @Override
        public Boolean visitRawType(final TypeReference t, final TypeReference s) {
            return visitClassType(t, s);
        }

        @Override
        public Boolean visitWildcard(final WildcardType t, final TypeReference s) {
            //
            // We shouldn't be here.  Return FALSE to avoid crash.
            //
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitType(final TypeReference t, final TypeReference s) {
            return Boolean.FALSE;
        }
    };

    private final static TypeRelation CONTAINS_TYPE_VISITOR = new TypeRelation() {
        private TypeReference U(final TypeReference t) {
            TypeReference current = t;

            while (current.isWildcardType()) {
                if (current.isUnbounded()) {
                    return BuiltinTypes.Object;
                }

                if (current.hasSuperBound()) {
                    return current.getSuperBound();
                }

                current = current.getExtendsBound();
            }

            return current;
        }

        private TypeReference L(final TypeReference t) {
            TypeReference current = t;

            while (current.isWildcardType()) {
                if (current.isUnbounded() || current.hasExtendsBound()) {
                    return BuiltinTypes.Bottom;
                }

                current = current.getSuperBound();
            }

            return current;
        }

        @Override
        public Boolean visitType(final TypeReference t, final TypeReference s) {
            return isSameType(t, s);
        }

        @Override
        public Boolean visitWildcard(final WildcardType t, final TypeReference s) {
            return isSameWildcard(t, s) ||
                   isCaptureOf(s, t) ||
                   (t.hasExtendsBound() || isSubTypeNoCapture(L(t), getLowerBound(s))) &&
                   (t.hasSuperBound() || isSubTypeNoCapture(getUpperBound(s), U(t)));
        }
    };

    private final static TypeMapper<TypeReference> AS_SUPER_VISITOR = new TypeMapper<TypeReference>() {
        @Override
        public TypeReference visitType(final TypeReference t, final TypeReference s) {
            return null;
        }

        @Override
        public TypeReference visitArrayType(final ArrayType t, final TypeReference s) {
            return isSubType(t, s) ? s : null;
        }

        @Override
        public TypeReference visitClassType(final TypeReference t, final TypeReference s) {
            if (StringUtilities.equals(t.getInternalName(), s.getInternalName())) {
                return t;
            }

            final TypeReference st = getSuperType(t);

            if (st != null &&
                (st.getSimpleType() == JvmType.Object ||
                 st.getSimpleType() == JvmType.TypeVariable)) {

                final TypeReference x = asSuper(st, s);

                if (x != null) {
                    return x;
                }
            }

            final TypeDefinition ds = s.resolve();

            if (ds != null && ds.isInterface()) {
                for (final TypeReference i : getInterfaces(t)) {
                    final TypeReference x = asSuper(i, s);

                    if (x != null) {
                        return x;
                    }
                }
            }

            return null;
        }

        @Override
        public TypeReference visitGenericParameter(final GenericParameter t, final TypeReference s) {
            if (isSameType(t, s)) {
                return t;
            }
            return asSuper(t.hasExtendsBound() ? t.getExtendsBound() : BuiltinTypes.Object, s);
        }

        @Override
        public TypeReference visitNullType(final TypeReference t, final TypeReference s) {
            return super.visitNullType(t, s);
        }

        @Override
        public TypeReference visitParameterizedType(final TypeReference t, final TypeReference s) {
//            final TypeReference r = this.visitClassType(t, s);
//            return substituteGenericArguments(r, adapt(t, s));
            return this.visitClassType(t, s);
        }

        @Override
        public TypeReference visitPrimitiveType(final PrimitiveType t, final TypeReference s) {
            return super.visitPrimitiveType(t, s);
        }

        @Override
        public TypeReference visitRawType(final TypeReference t, final TypeReference s) {
            return this.visitClassType(t, s);
        }

        @Override
        public TypeReference visitWildcard(final WildcardType t, final TypeReference s) {
            return super.visitWildcard(t, s);
        }
    };

    private final static TypeMapper<Void> SUPER_VISITOR = new TypeMapper<Void>() {
        @Override
        public TypeReference visitType(final TypeReference t, final Void ignored) {
            return null;
        }

        @Override
        public TypeReference visitArrayType(final ArrayType t, final Void ignored) {
            final TypeReference et = getElementType(t);

            if (et.isPrimitive() || isSameType(et, BuiltinTypes.Object)) {
                return arraySuperType(et);
            }

            final TypeReference superType = getSuperType(et);

            return superType != null ? superType.makeArrayType()
                                     : null;
        }

        @Override
        public TypeReference visitCompoundType(final CompoundTypeReference t, final Void aVoid) {
            //
            // TODO: Is this correct?
            //

            final TypeReference bt = t.getBaseType();

            if (bt != null) {
                return getSuperType(bt);
            }

            return t;
        }

        @Override
        public TypeReference visitClassType(final TypeReference t, final Void ignored) {
            final TypeDefinition resolved = t.resolve();

            if (resolved == null) {
                return BuiltinTypes.Object;
            }

            TypeReference superType;

            if (resolved.isInterface()) {
                superType = resolved.getBaseType();

                if (superType == null) {
                    superType = firstOrDefault(resolved.getExplicitInterfaces());
                }
            }
            else {
                superType = resolved.getBaseType();
            }

            if (superType == null) {
                return null;
            }

            if (resolved.isGenericDefinition()) {
                if (!t.isGenericType()) {
                    return eraseRecursive(superType);
                }

                if (t.isGenericDefinition()) {
                    return superType;
                }

                return substituteGenericArguments(superType, classBound(t));
            }

            return superType;
        }

        @Override
        public TypeReference visitGenericParameter(final GenericParameter t, final Void ignored) {
            return t.hasExtendsBound() ? t.getExtendsBound()
                                       : BuiltinTypes.Object;
        }

        @Override
        public TypeReference visitNullType(final TypeReference t, final Void ignored) {
            return BuiltinTypes.Object;
        }

        @Override
        public TypeReference visitParameterizedType(final TypeReference t, final Void ignored) {
            return visitClassType(t, ignored);
        }

        @Override
        public TypeReference visitRawType(final TypeReference t, final Void ignored) {
            final TypeReference genericDefinition = t.getUnderlyingType();

            if (!(genericDefinition instanceof TypeDefinition)) {
                return BuiltinTypes.Object;
            }

            final TypeReference baseType = ((TypeDefinition) genericDefinition).getBaseType();

            return baseType.isGenericType() ? eraseRecursive(baseType)
                                            : baseType;
        }

        @Override
        public TypeReference visitWildcard(final WildcardType t, final Void ignored) {
            if (t.isUnbounded()) {
                return BuiltinTypes.Object;
            }

            if (t.hasExtendsBound()) {
                return t.getExtendsBound();
            }

            //
            // A note on wildcards: there is no good way to determine a supertype for a
            // super bounded wildcard.
            //
            return null;
        }
    };

    private final static class Adapter extends DefaultTypeVisitor<TypeReference, Void> {
        final ListBuffer<TypeReference> from = ListBuffer.lb();
        final ListBuffer<TypeReference> to = ListBuffer.lb();
        final Map<TypeReference, TypeReference> mapping = new HashMap<>();

        private void adaptRecursive(
            final List<? extends TypeReference> source,
            final List<? extends TypeReference> target) {

            if (source.size() == target.size()) {
                for (int i = 0, n = source.size(); i < n; i++) {
                    adaptRecursive(source.get(i), target.get(i));
                }
            }
        }

        @Override
        public Void visitClassType(final TypeReference source, final TypeReference target) {
            adaptRecursive(getTypeArguments(source), getTypeArguments(target));
            return null;
        }

        @Override
        public Void visitParameterizedType(final TypeReference source, final TypeReference target) {
            adaptRecursive(getTypeArguments(source), getTypeArguments(target));
            return null;
        }

        private void adaptRecursive(final TypeReference source, final TypeReference target) {
            final HashSet<Pair<TypeReference, TypeReference>> cache = ADAPT_CACHE.get();
            final Pair<TypeReference, TypeReference> pair = Pair.create(source, target);

            if (cache.add(pair)) {
                try {
                    visit(source, target);
                }
                finally {
                    cache.remove(pair);
                }
            }
        }

        @Override
        public Void visitArrayType(final ArrayType source, final TypeReference target) {
            if (target.isArray()) {
                adaptRecursive(getElementType(source), getElementType(target));
            }
            return null;
        }

        @Override
        public Void visitWildcard(final WildcardType source, final TypeReference target) {
            if (source.hasExtendsBound()) {
                adaptRecursive(getUpperBound(source), getUpperBound(target));
            }
            else if (source.hasSuperBound()) {
                adaptRecursive(getLowerBound(source), getLowerBound(target));
            }
            return null;
        }

        @Override
        public Void visitGenericParameter(final GenericParameter source, final TypeReference target) {
            TypeReference value = mapping.get(source);

            if (value != null) {
                if (value.hasSuperBound() && target.hasSuperBound()) {
                    value = isSubType(getLowerBound(value), getLowerBound(target)) ? target
                                                                                   : value;
                }
                else if (value.hasExtendsBound() && target.hasExtendsBound()) {
                    value = isSubType(getUpperBound(value), getUpperBound(target)) ? value
                                                                                   : target;
                }
                else if (!isSameType(value, target)) {
                    throw new AdaptFailure();
                }
            }
            else {
                value = target;
                from.append(source);
                to.append(target);
            }

            mapping.put(source, value);

            return null;
        }
    }

    public static class AdaptFailure extends RuntimeException {
        static final long serialVersionUID = -7490231548272701566L;
    }

    private final static SameTypeVisitor SAME_TYPE_VISITOR_LOOSE = new LooseSameTypeVisitor();
    private final static SameTypeVisitor SAME_TYPE_VISITOR_STRICT = new StrictSameTypeVisitor();

    abstract static class SameTypeVisitor extends TypeRelation {
        abstract boolean areSameGenericParameters(final GenericParameter gp1, final GenericParameter gp2);
        abstract protected boolean containsTypes(final List<? extends TypeReference> t1, final List<? extends TypeReference> t2);

        @Override
        public Boolean visit(final TypeReference t, final TypeReference s) {
            if (t == null) {
                return s == null;
            }

            if (s == null) {
                return false;
            }

            return t.accept(this, s);
        }

        @Override
        public Boolean visitType(final TypeReference t, final TypeReference s) {
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitArrayType(final ArrayType t, final TypeReference s) {
            return s.isArray() &&
                   containsTypeEquivalent(getElementType(t), getElementType(s));
        }

        @Override
        public Boolean visitBottomType(final TypeReference t, final TypeReference s) {
            return t == s;
        }

        @Override
        public Boolean visitClassType(final TypeReference t, final TypeReference s) {
            if (t.isGenericDefinition()) {
                if (s.isGenericDefinition()) {
                    return StringUtilities.equals(t.getInternalName(), s.getInternalName()) &&
                           visit(t.getDeclaringType(), s.getDeclaringType());
                }
                return false;
            }

            if (s.getSimpleType() == JvmType.Object &&
                StringUtilities.equals(t.getInternalName(), s.getInternalName()) &&
                visit(t.getDeclaringType(), s.getDeclaringType()) &&
                containsTypes(getTypeArguments(t), getTypeArguments(s))) {

                return true;
            }

            return false;
        }

        @Override
        public Boolean visitCompoundType(final CompoundTypeReference t, final TypeReference s) {
            if (!s.isCompoundType()) {
                return false;
            }

            if (!visit(getSuperType(t), getSuperType(s))) {
                return false;
            }

            final HashSet<TypeReference> set = new HashSet<>();

            for (final TypeReference i : getInterfaces(t)) {
                set.add(i);
            }

            for (final TypeReference i : getInterfaces(s)) {
                if (!set.remove(i)) {
                    return false;
                }
            }

            return set.isEmpty();
        }

        @Override
        public Boolean visitGenericParameter(final GenericParameter t, final TypeReference s) {
            if (s instanceof GenericParameter) {
                //
                // Type substitution does not preserve type variable types; check that type variable
                // bounds are indeed the same.
                //
                return areSameGenericParameters(t, (GenericParameter) s);
            }

            //
            // Special case for s == ? super X, where upper(s) == u; check that u == t.
            //

            return s.hasSuperBound() &&
                   !s.hasExtendsBound() &&
                   visit(t, getUpperBound(s));
        }

        @Override
        public Boolean visitNullType(final TypeReference t, final TypeReference s) {
            return t == s;
        }

        @Override
        public Boolean visitParameterizedType(final TypeReference t, final TypeReference s) {
            return visitClassType(t, s);
        }

        @Override
        public Boolean visitPrimitiveType(final PrimitiveType t, final TypeReference s) {
            return t.getSimpleType() == s.getSimpleType();
        }

        @Override
        public Boolean visitRawType(final TypeReference t, final TypeReference s) {
            return s.getSimpleType() == JvmType.Object &&
                   !s.isGenericType() &&
                   StringUtilities.equals(t.getInternalName(), s.getInternalName());
        }

        @Override
        public Boolean visitWildcard(final WildcardType t, final TypeReference s) {
            if (s.isWildcardType()) {
                if (t.isUnbounded()) {
                    return s.isUnbounded();
                }

                if (t.hasExtendsBound()) {
                    return s.hasExtendsBound() &&
                           visit(getUpperBound(t), getUpperBound(s));
                }

                if (t.hasSuperBound()) {
                    return s.hasSuperBound() &&
                           visit(getLowerBound(t), getLowerBound(s));
                }
            }

            return Boolean.FALSE;
        }
    }

    final static class LooseSameTypeVisitor extends SameTypeVisitor {
        @Override
        boolean areSameGenericParameters(final GenericParameter gp1, final GenericParameter gp2) {
            final TypeReference ub1 = getUpperBound(gp1);
            final TypeReference ub2 = getUpperBound(gp2);

            if (ub1 == gp1) {
                return ub2 == gp2;
            }

            return visit(ub1, ub2);
        }

        @Override
        protected boolean containsTypes(final List<? extends TypeReference> t1, final List<? extends TypeReference> t2) {
            return containsTypeEquivalent(t1, t2);
        }
    }

    final static class StrictSameTypeVisitor extends SameTypeVisitor {
        @Override
        boolean areSameGenericParameters(final GenericParameter gp1, final GenericParameter gp2) {
            if (gp1 == null) {
                return gp2 == null;
            }

            if (gp2 == null) {
                return false;
            }

            final IGenericParameterProvider o1 = gp1.getOwner();
            final IGenericParameterProvider o2 = gp2.getOwner();

            if (o1 instanceof TypeDefinition) {
                return o2 instanceof TypeDefinition &&
                       visit((TypeReference) o1, (TypeReference) o2);
            }

            if (o1 instanceof MethodDefinition) {
                if (o2 instanceof MethodDefinition) {
                    final TypeDefinition dt1 = ((MethodDefinition) o1).getDeclaringType();
                    final TypeDefinition dt2 = ((MethodDefinition) o2).getDeclaringType();

                    if (!visit(dt1, dt2)) {
                        return false;
                    }

                    return StringUtilities.equals(
                        ((MethodDefinition) o1).getErasedSignature(),
                        ((MethodDefinition) o2).getErasedSignature()
                    );
                }
            }

            return false;
        }

        @Override
        protected boolean containsTypes(final List<? extends TypeReference> t1, final List<? extends TypeReference> t2) {
            return areSameTypes(t1, t2, true);
        }

        @Override
        public Boolean visitWildcard(final WildcardType t, final TypeReference s) {
            if (s.isWildcardType()) {
                if (t.isUnbounded()) {
                    return s.isUnbounded();
                }

                if (t.hasExtendsBound()) {
                    return s.hasExtendsBound() &&
                           isSameType(t.getExtendsBound(), s.getExtendsBound());
                }

                return s.hasSuperBound() &&
                       isSameType(t.getSuperBound(), s.getSuperBound());
            }

            return false;
        }
    }

    private final static DefaultTypeVisitor<Void, List<TypeReference>> INTERFACES_VISITOR =
        new DefaultTypeVisitor<Void, List<TypeReference>>() {
            @Override
            public List<TypeReference> visitClassType(final TypeReference t, final Void ignored) {
                final TypeDefinition r = t.resolve();

                if (r == null) {
                    return Collections.emptyList();
                }

                final List<TypeReference> interfaces = r.getExplicitInterfaces();

                if (r.isGenericDefinition()) {
                    if (t.isGenericDefinition()) {
                        return interfaces;
                    }

                    if (isRawType(t)) {
                        return eraseRecursive(interfaces);
                    }

                    final List<? extends TypeReference> formal = getTypeArguments(r);
                    final List<? extends TypeReference> actual = getTypeArguments(t);

                    final ArrayList<TypeReference> result = new ArrayList<>();
                    final Map<TypeReference, TypeReference> mappings = new HashMap<>();

                    for (int i = 0, n = formal.size(); i < n; i++) {
                        mappings.put(formal.get(i), actual.get(i));
                    }

                    for (int i = 0, n = interfaces.size(); i < n; i++) {
                        result.add(substituteGenericArguments(interfaces.get(i), mappings));
                    }

                    return result;
                }

                return interfaces;
            }

            @Override
            public List<TypeReference> visitWildcard(final WildcardType t, final Void ignored) {
                if (t.hasExtendsBound()) {
                    final TypeReference bound = t.getExtendsBound();
                    final TypeDefinition resolvedBound = bound.resolve();

                    if (resolvedBound != null) {
                        if (resolvedBound.isInterface()) {
                            return Collections.singletonList(bound);
                        }
                        if (resolvedBound.isCompoundType()) {
                            visit(bound, null);
                        }
                    }

                    return visit(bound, null);
                }

                return Collections.emptyList();
            }

            @Override
            public List<TypeReference> visitGenericParameter(final GenericParameter t, final Void ignored) {
                if (t.hasExtendsBound()) {
                    final TypeReference bound = t.getExtendsBound();
                    final TypeDefinition resolvedBound = bound.resolve();

                    if (resolvedBound != null) {
                        if (resolvedBound.isInterface()) {
                            return Collections.singletonList(bound);
                        }
                        if (resolvedBound.isCompoundType()) {
                            visit(bound, null);
                        }
                    }

                    return visit(bound, null);
                }

                return Collections.emptyList();
            }
        };

    // </editor-fold>
}
