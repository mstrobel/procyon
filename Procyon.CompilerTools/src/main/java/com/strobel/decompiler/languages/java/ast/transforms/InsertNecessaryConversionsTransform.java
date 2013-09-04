/*
 * InsertNecessaryConversionsTransform.java
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

package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.assembler.metadata.ConversionType;
import com.strobel.assembler.metadata.IMethodSignature;
import com.strobel.assembler.metadata.JvmType;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.Predicates;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.languages.java.utilities.RedundantCastUtility;
import com.strobel.decompiler.languages.java.utilities.TypeUtilities;
import com.strobel.decompiler.semantics.ResolveResult;
import com.strobel.functions.Function;

import static com.strobel.core.CollectionUtilities.firstOrDefault;

public class InsertNecessaryConversionsTransform extends ContextTrackingVisitor<Void> {
    private final static ConvertTypeOptions NO_IMPORT_OPTIONS;

    static {
        NO_IMPORT_OPTIONS = new ConvertTypeOptions();
        NO_IMPORT_OPTIONS.setAddImports(false);
    }

    private final JavaResolver _resolver;

    public InsertNecessaryConversionsTransform(final DecompilerContext context) {
        super(context);
        _resolver = new JavaResolver(context);
    }

    @Override
    public Void visitCastExpression(final CastExpression node, final Void data) {
        super.visitCastExpression(node, data);

        final Expression operand = node.getExpression();
        final ResolveResult targetResult = _resolver.apply(node.getType());

        if (targetResult == null || targetResult.getType() == null) {
            return null;
        }

        final ResolveResult valueResult = _resolver.apply(operand);

        if (valueResult == null || valueResult.getType() == null) {
            return null;
        }

        final ConversionType conversionType = MetadataHelper.getConversionType(targetResult.getType(), valueResult.getType());

        if (conversionType == ConversionType.NONE) {
            addCastForAssignment(node.getType(), node.getExpression());
        }

        if (RedundantCastUtility.isCastRedundant(_resolver, node)) {
            RedundantCastUtility.removeCast(node);
        }

        return null;
    }

    @Override
    public Void visitMemberReferenceExpression(final MemberReferenceExpression node, final Void data) {
        super.visitMemberReferenceExpression(node, data);

        MemberReference member = node.getUserData(Keys.MEMBER_REFERENCE);

        if (member == null && node.getParent() != null && node.getRole() == Roles.TARGET_EXPRESSION) {
            member = node.getParent().getUserData(Keys.MEMBER_REFERENCE);
        }

        if (member == null) {
            return null;
        }
        final AstBuilder astBuilder = context.getUserData(Keys.AST_BUILDER);

        if (astBuilder == null) {
            return null;
        }

        final ResolveResult valueResult = _resolver.apply(node.getTarget());

        TypeReference declaringType = member.getDeclaringType();

        if (valueResult != null &&
            valueResult.getType() != null) {

            if (MetadataHelper.isAssignableFrom(declaringType, valueResult.getType())) {
                return null;
            }

            if (valueResult.getType().isGenericType() &&
                (declaringType.isGenericType() ||
                 MetadataHelper.isRawType(declaringType))) {

                final TypeReference asSuper = MetadataHelper.asSuper(declaringType, valueResult.getType());

                if (asSuper != null) {
                    declaringType = asSuper;
                }
            }
        }

        addCastForAssignment(astBuilder.convertType(declaringType, NO_IMPORT_OPTIONS), node.getTarget());

        return null;
    }

    @Override
    public Void visitAssignmentExpression(final AssignmentExpression node, final Void data) {
        super.visitAssignmentExpression(node, data);

        addCastForAssignment(node.getLeft(), node.getRight());

        return null;
    }

    @Override
    public Void visitVariableDeclaration(final VariableDeclarationStatement node, final Void data) {
        super.visitVariableDeclaration(node, data);

        for (final VariableInitializer initializer : node.getVariables()) {
            addCastForAssignment(node, initializer.getInitializer());
        }

        return null;
    }

    @Override
    public Void visitReturnStatement(final ReturnStatement node, final Void data) {
        super.visitReturnStatement(node, data);

        final AstNode function = firstOrDefault(
            node.getAncestors(),
            Predicates.or(
                Predicates.<AstNode>instanceOf(MethodDeclaration.class),
                Predicates.<AstNode>instanceOf(LambdaExpression.class)
            )
        );

        if (function == null) {
            return null;
        }

        final AstType left;

        if (function instanceof MethodDeclaration) {
            left = ((MethodDeclaration) function).getReturnType();
        }
        else {
            final TypeReference expectedType = TypeUtilities.getExpectedTypeByParent(_resolver, (Expression) function);

            if (expectedType == null) {
                return null;
            }

            final AstBuilder astBuilder = context.getUserData(Keys.AST_BUILDER);

            if (astBuilder == null) {
                return null;
            }

            final IMethodSignature method = TypeUtilities.getLambdaSignature((LambdaExpression) function);

            if (method == null) {
                return null;
            }

            left = astBuilder.convertType(method.getReturnType(), NO_IMPORT_OPTIONS);
        }

        final Expression right = node.getExpression();

        addCastForAssignment(left, right);

        return null;
    }

    private boolean addCastForAssignment(final AstNode left, final Expression right) {
        final ResolveResult targetResult = _resolver.apply(left);

        if (targetResult == null || targetResult.getType() == null) {
            return false;
        }

        final ResolveResult valueResult = _resolver.apply(right);

        if (valueResult == null || valueResult.getType() == null) {
            return false;
        }

        final ConversionType conversionType = MetadataHelper.getConversionType(targetResult.getType(), valueResult.getType());

        AstNode replacement = null;

        if (conversionType == ConversionType.EXPLICIT || conversionType == ConversionType.EXPLICIT_TO_UNBOXED) {
            final AstBuilder astBuilder = context.getUserData(Keys.AST_BUILDER);

            if (astBuilder == null) {
                return false;
            }

            final ConvertTypeOptions convertTypeOptions = new ConvertTypeOptions();

            convertTypeOptions.setAllowWildcards(false);

            final AstType castToType = astBuilder.convertType(targetResult.getType(), convertTypeOptions);

            replacement = right.replaceWith(
                new Function<AstNode, Expression>() {
                    @Override
                    public Expression apply(final AstNode e) {
                        return new CastExpression(castToType, right);
                    }
                }
            );
        }
        else if (conversionType == ConversionType.NONE) {
            if (valueResult.getType().getSimpleType() == JvmType.Boolean &&
                targetResult.getType().getSimpleType() != JvmType.Boolean &&
                targetResult.getType().getSimpleType().isNumeric()) {

                replacement = convertBooleanToNumeric(right);

                if (targetResult.getType().getSimpleType().bitWidth() < 32) {
                    final AstBuilder astBuilder = context.getUserData(Keys.AST_BUILDER);

                    if (astBuilder != null) {
                        replacement = replacement.replaceWith(
                            new Function<AstNode, AstNode>() {
                                @Override
                                public AstNode apply(final AstNode input) {
                                    return new CastExpression(astBuilder.convertType(targetResult.getType()), (Expression) input);
                                }
                            }
                        );
                    }
                }
            }
            else if (targetResult.getType().getSimpleType() == JvmType.Boolean &&
                     valueResult.getType().getSimpleType() != JvmType.Boolean &&
                     valueResult.getType().getSimpleType().isNumeric()) {

                replacement = right.replaceWith(
                    new Function<AstNode, AstNode>() {
                        @Override
                        public AstNode apply(final AstNode input) {
                            return new BinaryOperatorExpression(
                                right,
                                BinaryOperatorType.INEQUALITY,
                                new PrimitiveExpression(JavaPrimitiveCast.cast(valueResult.getType().getSimpleType(), 0))
                            );
                        }
                    }
                );
            }
            else {
                final AstBuilder astBuilder = context.getUserData(Keys.AST_BUILDER);

                if (astBuilder != null) {
                    replacement = right.replaceWith(
                        new Function<AstNode, AstNode>() {
                            @Override
                            public AstNode apply(final AstNode input) {
                                return new CastExpression(astBuilder.convertType(BuiltinTypes.Object), right);
                            }
                        }
                    );
                }
            }
        }

        if (replacement != null) {
            recurse(replacement);
            return true;
        }

        return false;
    }

    @Override
    public Void visitBinaryOperatorExpression(final BinaryOperatorExpression node, final Void data) {
        super.visitBinaryOperatorExpression(node, data);

        switch (node.getOperator()) {
            case GREATER_THAN:
            case GREATER_THAN_OR_EQUAL:
            case LESS_THAN:
            case LESS_THAN_OR_EQUAL:
            case ADD:
            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case MODULUS:
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
            case UNSIGNED_SHIFT_RIGHT: {
                final Expression left = node.getLeft();
                final Expression right = node.getRight();

                final ResolveResult leftResult = _resolver.apply(left);
                final ResolveResult rightResult = _resolver.apply(right);

                if (leftResult != null && leftResult.getType() == BuiltinTypes.Boolean) {
                    convertBooleanToNumeric(left);
                }

                if (rightResult != null && rightResult.getType() == BuiltinTypes.Boolean) {
                    convertBooleanToNumeric(right);
                }

                break;
            }
        }

        return null;
    }

    private Expression convertBooleanToNumeric(final Expression operand) {
        final boolean invert;

        Expression e = operand;

        if (e instanceof UnaryOperatorExpression &&
            ((UnaryOperatorExpression) e).getOperator() == UnaryOperatorType.NOT) {

            final Expression inner = ((UnaryOperatorExpression) e).getExpression();

            inner.remove();
            e.replaceWith(inner);
            e = inner;
            invert = true;
        }
        else {
            invert = false;
        }

        return (Expression) e.replaceWith(
            new Function<AstNode, AstNode>() {
                @Override
                public AstNode apply(final AstNode input) {
                    return new ConditionalExpression(
                        (Expression) input,
                        new PrimitiveExpression(invert ? 0 : 1),
                        new PrimitiveExpression(invert ? 1 : 0)
                    );
                }
            }
        );
    }

    private void recurse(final AstNode replacement) {
        final AstNode parent = replacement.getParent();

        if (parent != null) {
            parent.acceptVisitor(this, null);
        }
        else {
            replacement.acceptVisitor(this, null);
        }
    }
}
