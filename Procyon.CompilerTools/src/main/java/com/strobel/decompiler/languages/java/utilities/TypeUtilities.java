/*
 * TypeUtilities.java
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

package com.strobel.decompiler.languages.java.utilities;

import com.strobel.annotations.NotNull;
import com.strobel.annotations.Nullable;
import com.strobel.assembler.metadata.*;
import com.strobel.core.Comparer;
import com.strobel.core.Predicates;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.semantics.ResolveResult;
import com.strobel.functions.Function;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.strobel.core.CollectionUtilities.firstOrDefault;

public final class TypeUtilities {
    private static final String OBJECT_DESCRIPTOR = "java/lang/Object";
    private static final String STRING_DESCRIPTOR = "java/lang/String";

    private static final Map<JvmType, Integer> TYPE_TO_RANK_MAP;

    private static final int BYTE_RANK = 1;
    private static final int SHORT_RANK = 2;
    private static final int CHAR_RANK = 3;
    private static final int INT_RANK = 4;
    private static final int LONG_RANK = 5;
    private static final int FLOAT_RANK = 6;
    private static final int DOUBLE_RANK = 7;
    private static final int BOOL_RANK = 10;
    private static final int STRING_RANK = 100;
    private static final int MAX_NUMERIC_RANK = DOUBLE_RANK;

    static {
        final Map<JvmType, Integer> rankMap = new EnumMap<>(JvmType.class);

        rankMap.put(JvmType.Byte, BYTE_RANK);
        rankMap.put(JvmType.Short, SHORT_RANK);
        rankMap.put(JvmType.Character, CHAR_RANK);
        rankMap.put(JvmType.Integer, INT_RANK);
        rankMap.put(JvmType.Long, LONG_RANK);
        rankMap.put(JvmType.Float, FLOAT_RANK);
        rankMap.put(JvmType.Double, DOUBLE_RANK);
        rankMap.put(JvmType.Boolean, BOOL_RANK);

        TYPE_TO_RANK_MAP = Collections.unmodifiableMap(rankMap);
    }

    private static int getTypeRank(@NotNull final TypeReference type) {
        final TypeReference unboxedType = MetadataHelper.getUnderlyingPrimitiveTypeOrSelf(type);
        final Integer rank = TYPE_TO_RANK_MAP.get(unboxedType.getSimpleType());

        if (rank != null) {
            return rank;
        }

        if (StringUtilities.equals(type.getInternalName(), STRING_DESCRIPTOR)) {
            return STRING_RANK;
        }

        return Integer.MAX_VALUE;
    }

    public static boolean isBoolean(@NotNull final TypeReference type) {
        return MetadataHelper.getUnderlyingPrimitiveTypeOrSelf(type).getSimpleType() == JvmType.Boolean;
    }

    public static boolean isBinaryOperatorApplicable(
        @NotNull final BinaryOperatorType op,
        @NotNull final AstType lType,
        @NotNull final AstType rType,
        final boolean strict) {

        return isBinaryOperatorApplicable(
            op,
            VerifyArgument.notNull(lType, "lType").toTypeReference(),
            VerifyArgument.notNull(rType, "rType").toTypeReference(),
            strict
        );
    }

    public static boolean isBinaryOperatorApplicable(
        @NotNull final BinaryOperatorType op,
        @Nullable final TypeReference lType,
        @Nullable final TypeReference rType,
        final boolean strict) {

        if (lType == null || rType == null) {
            return true;
        }

        VerifyArgument.notNull(op, "op");

        final int lRank = getTypeRank(lType);
        final int rRank = getTypeRank(rType);

        final TypeReference lUnboxed = MetadataHelper.getUnderlyingPrimitiveTypeOrSelf(lType);
        final TypeReference rUnboxed = MetadataHelper.getUnderlyingPrimitiveTypeOrSelf(rType);

        int resultRank = BOOL_RANK;
        boolean isApplicable = false;

        switch (op) {
            case BITWISE_AND:
            case BITWISE_OR:
            case EXCLUSIVE_OR: {
                if (lUnboxed.isPrimitive() && rUnboxed.isPrimitive()) {
                    isApplicable = lRank <= LONG_RANK && rRank <= LONG_RANK ||
                                   isBoolean(lUnboxed) || isBoolean(rUnboxed);

                    resultRank = lRank <= LONG_RANK ? INT_RANK : BOOL_RANK;
                }
                break;
            }

            case LOGICAL_AND:
            case LOGICAL_OR: {
                if (lUnboxed.isPrimitive() && rUnboxed.isPrimitive()) {
                    isApplicable = isBoolean(lType) && isBoolean(rType);
                }
                break;
            }

            case GREATER_THAN:
            case GREATER_THAN_OR_EQUAL:
            case LESS_THAN:
            case LESS_THAN_OR_EQUAL: {
                if (lUnboxed.isPrimitive() && rUnboxed.isPrimitive()) {
                    isApplicable = lRank <= LONG_RANK && rRank <= LONG_RANK;
                    resultRank = INT_RANK;
                }
                break;
            }

            case EQUALITY:
            case INEQUALITY: {
                if (lUnboxed.isPrimitive() && rUnboxed.isPrimitive() &&
                    (lType.isPrimitive() || rType.isPrimitive())) {
                    isApplicable = lRank <= MAX_NUMERIC_RANK && rRank <= MAX_NUMERIC_RANK
                                   || lRank == BOOL_RANK && rRank == BOOL_RANK;
                }
                else {
                    if (lType.isPrimitive()) {
                        return MetadataHelper.isConvertible(lType, rType);
                    }
                    if (rType.isPrimitive()) {
                        return MetadataHelper.isConvertible(rType, lType);
                    }
                    isApplicable = MetadataHelper.isConvertible(lType, rType) ||
                                   MetadataHelper.isConvertible(rType, lType);
                }
                break;
            }

            case ADD: {
                if (StringUtilities.equals(lType.getInternalName(), STRING_DESCRIPTOR)) {
                    isApplicable = !rType.isVoid();
                    resultRank = STRING_RANK;
                    break;
                }
                else if (StringUtilities.equals(rType.getInternalName(), STRING_DESCRIPTOR)) {
                    isApplicable = !lType.isVoid();
                    resultRank = STRING_RANK;
                    break;
                }

                if (lUnboxed.isPrimitive() && rUnboxed.isPrimitive()) {
                    resultRank = Math.max(lRank, rRank);
                    isApplicable = lRank <= MAX_NUMERIC_RANK && rRank <= MAX_NUMERIC_RANK;
                }

                break;
            }

            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case MODULUS: {
                if (lUnboxed.isPrimitive() && rUnboxed.isPrimitive()) {
                    resultRank = Math.max(lRank, rRank);
                    isApplicable = lRank <= MAX_NUMERIC_RANK && rRank <= MAX_NUMERIC_RANK;
                }
                break;
            }

            case SHIFT_LEFT:
            case SHIFT_RIGHT:
            case UNSIGNED_SHIFT_RIGHT: {
                if (lUnboxed.isPrimitive() && rUnboxed.isPrimitive()) {
                    isApplicable = lRank <= LONG_RANK && rRank <= LONG_RANK;
                    resultRank = INT_RANK;
                }
                break;
            }
        }

        if (isApplicable && strict) {
            if (resultRank > MAX_NUMERIC_RANK) {
                isApplicable = lRank == resultRank ||
                               StringUtilities.equals(lType.getInternalName(), OBJECT_DESCRIPTOR);
            }
            else {
                isApplicable = lRank <= MAX_NUMERIC_RANK;
            }
        }

        return isApplicable;
    }

    @Nullable
    public static AstNode skipParenthesesUp(final AstNode e) {
        AstNode result = e;

        while (result instanceof ParenthesizedExpression) {
            result = result.getParent();
        }

        return result;
    }

    @Nullable
    public static AstNode skipParenthesesDown(final AstNode e) {
        AstNode result = e;

        while (result instanceof ParenthesizedExpression) {
            result = ((ParenthesizedExpression) result).getExpression();
        }

        return result;
    }

    @Nullable
    public static Expression skipParenthesesDown(final Expression e) {
        Expression result = e;

        while (result instanceof ParenthesizedExpression) {
            result = ((ParenthesizedExpression) result).getExpression();
        }

        return result;
    }

    private static boolean checkSameExpression(final Expression template, final Expression expression) {
        return Comparer.equals(template, skipParenthesesDown(expression));
    }

    private static TypeReference getType(@NotNull final Function<AstNode, ResolveResult> resolver, @NotNull final AstNode node) {
        final ResolveResult result = resolver.apply(node);
        return result != null ? result.getType() : null;
    }

    @Nullable
    public static TypeReference getExpectedTypeByParent(final Function<AstNode, ResolveResult> resolver, final Expression expression) {
        VerifyArgument.notNull(resolver, "resolver");
        VerifyArgument.notNull(expression, "expression");

        final AstNode parent = skipParenthesesUp(expression.getParent());

        if (expression.getRole() == Roles.CONDITION) {
            return CommonTypeReferences.Boolean;
        }

        if (parent instanceof VariableInitializer) {
            if (checkSameExpression(expression, ((VariableInitializer) parent).getInitializer())) {
                if (parent.getParent() instanceof VariableDeclarationStatement) {
                    return getType(resolver, parent.getParent());
                }
            }
        }
        else if (parent instanceof AssignmentExpression) {
            if (checkSameExpression(expression, ((AssignmentExpression) parent).getRight())) {
                return getType(resolver, ((AssignmentExpression) parent).getLeft());
            }
        }
        else if (parent instanceof ReturnStatement) {
            final LambdaExpression lambdaExpression = firstOrDefault(parent.getAncestors(LambdaExpression.class));

            if (lambdaExpression != null) {
                final DynamicCallSite callSite = lambdaExpression.getUserData(Keys.DYNAMIC_CALL_SITE);

                if (callSite == null) {
                    return null;
                }

                final MethodReference method = (MethodReference) callSite.getBootstrapArguments().get(0);

                return method.getDeclaringType();
            }
            else {
                final MethodDeclaration method = firstOrDefault(parent.getAncestors(MethodDeclaration.class));

                if (method != null) {
                    return getType(resolver, method.getReturnType());
                }
            }
        }
        else if (parent instanceof ConditionalExpression) {
            if (checkSameExpression(expression, ((ConditionalExpression) parent).getTrueExpression())) {
                return getType(resolver, ((ConditionalExpression) parent).getFalseExpression());
            }
            else if (checkSameExpression(expression, ((ConditionalExpression) parent).getFalseExpression())) {
                return getType(resolver, ((ConditionalExpression) parent).getTrueExpression());
            }
        }

        return null;
    }

    public static IMethodSignature getLambdaSignature(final MethodGroupExpression node) {
        return getLambdaSignatureCore(node);
    }

    public static IMethodSignature getLambdaSignature(final LambdaExpression node) {
        return getLambdaSignatureCore(node);
    }

    private static IMethodSignature getLambdaSignatureCore(final Expression node) {
        VerifyArgument.notNull(node, "node");

        final TypeReference lambdaType = node.getUserData(Keys.TYPE_REFERENCE);
        final DynamicCallSite callSite = node.getUserData(Keys.DYNAMIC_CALL_SITE);

        if (lambdaType == null) {
            if (callSite == null) {
                return null;
            }

            return (IMethodSignature) callSite.getBootstrapArguments().get(2);
        }

        final TypeDefinition resolvedType = lambdaType.resolve();

        if (resolvedType == null) {
            if (callSite == null) {
                return null;
            }

            return (IMethodSignature) callSite.getBootstrapArguments().get(2);
        }

        MethodReference functionMethod = null;

        final List<MethodReference> methods = MetadataHelper.findMethods(
            resolvedType,
            callSite != null ? MetadataFilters.matchName(callSite.getMethodName())
                             : Predicates.<MemberReference>alwaysTrue()
        );

        for (final MethodReference m : methods) {
            final MethodDefinition r = m.resolve();

            if (r != null && r.isAbstract() && !r.isStatic() && !r.isDefault()) {
                functionMethod = r;
                break;
            }
        }

        if (functionMethod != null) {
            final TypeReference asMemberOf = MetadataHelper.asSuper(functionMethod.getDeclaringType(), lambdaType);
            final TypeReference effectiveType = asMemberOf != null ? asMemberOf : lambdaType;

            if (MetadataHelper.isRawType(effectiveType)) {
                return MetadataHelper.erase(functionMethod);
            }

            functionMethod = MetadataHelper.asMemberOf(functionMethod, effectiveType);
        }

        return functionMethod;
    }
}