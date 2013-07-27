package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.assembler.metadata.ConversionType;
import com.strobel.assembler.metadata.JvmType;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.semantics.ResolveResult;
import com.strobel.functions.Function;

public class InsertNecessaryCastsTransform extends ContextTrackingVisitor<Void> {
    private final JavaResolver _resolver;

    public InsertNecessaryCastsTransform(final DecompilerContext context) {
        super(context);
        _resolver = new JavaResolver(context);
    }

    @Override
    public Void visitCastExpression(final CastExpression node, final Void data) {
        super.visitCastExpression(node, data);

        final ResolveResult targetResult = _resolver.apply(node.getType());

        if (targetResult == null || targetResult.getType() == null) {
            return null;
        }

        final Expression value = node.getExpression();
        final ResolveResult valueResult = _resolver.apply(value);

        if (valueResult == null || valueResult.getType() == null) {
            return null;
        }

        final ConversionType conversionType = MetadataHelper.getConversionType(targetResult.getType(), valueResult.getType());

        if (conversionType == ConversionType.NONE) {
            final AstBuilder astBuilder = context.getUserData(Keys.AST_BUILDER);

            if (astBuilder != null) {
                value.replaceWith(
                    new Function<AstNode, AstNode>() {
                        @Override
                        public AstNode apply(final AstNode input) {
                            return new CastExpression(astBuilder.convertType(BuiltinTypes.Object), value);
                        }
                    }
                );
            }
        }

        return null;
    }

    @Override
    public Void visitAssignmentExpression(final AssignmentExpression node, final Void data) {
        super.visitAssignmentExpression(node, data);

        final Expression left = node.getLeft();
        final Expression right = node.getRight();

        final ResolveResult targetResult = _resolver.apply(left);

        if (targetResult == null || targetResult.getType() == null) {
            return null;
        }

        final ResolveResult valueResult = _resolver.apply(right);

        if (valueResult == null || valueResult.getType() == null) {
            return null;
        }

        final ConversionType conversionType = MetadataHelper.getConversionType(targetResult.getType(), valueResult.getType());

        if (conversionType == ConversionType.EXPLICIT || conversionType == ConversionType.EXPLICIT_TO_UNBOXED) {
            final AstBuilder astBuilder = context.getUserData(Keys.AST_BUILDER);

            if (astBuilder == null) {
                return null;
            }

            final ConvertTypeOptions convertTypeOptions = new ConvertTypeOptions();

            convertTypeOptions.setAllowWildcards(false);

            final AstType castToType = astBuilder.convertType(targetResult.getType(), convertTypeOptions);

            right.replaceWith(
                new Function<AstNode, Expression>() {
                    @Override
                    public Expression apply(final AstNode e) {
                        return new CastExpression(castToType, right);
                    }
                }
            );
        }

        if (valueResult.getType().getSimpleType() == JvmType.Boolean &&
            targetResult.getType().getSimpleType() != JvmType.Boolean &&
            targetResult.getType().getSimpleType().isNumeric()) {

            right.replaceWith(
                new Function<AstNode, AstNode>() {
                    @Override
                    public AstNode apply(final AstNode input) {
                        return new ConditionalExpression(
                            right,
                            new PrimitiveExpression(JavaPrimitiveCast.cast(targetResult.getType().getSimpleType(), 1)),
                            new PrimitiveExpression(JavaPrimitiveCast.cast(targetResult.getType().getSimpleType(), 0))
                        );
                    }
                }
            );

            node.acceptVisitor(this, data);
        }

        return null;
    }

    @Override
    public Void visitMemberReferenceExpression(final MemberReferenceExpression node, final Void data) {
        super.visitMemberReferenceExpression(node, data);

        final Expression target = node.getTarget();

        if (target == null || target.isNull()) {
            return null;
        }

        MemberReference member = node.getUserData(Keys.MEMBER_REFERENCE);

        if (member == null && node.getParent() != null) {
            member = node.getParent().getUserData(Keys.MEMBER_REFERENCE);
        }

        if (member == null) {
            return null;
        }

        final ResolveResult targetResult = _resolver.apply(target);

        if (targetResult == null || targetResult.getType() == null) {
            return null;
        }

        final TypeReference declaringType = member.getDeclaringType();
        final TypeDefinition resolvedDeclaringType = declaringType.resolve();

        final boolean isSubType = MetadataHelper.isSubType(
            targetResult.getType(),
            resolvedDeclaringType != null ? resolvedDeclaringType : declaringType
        );

        if (isSubType) {
            return null;
        }

        final AstBuilder astBuilder = context.getUserData(Keys.AST_BUILDER);

        if (astBuilder == null) {
            return null;
        }

        TypeReference castType = MetadataHelper.asSubType(targetResult.getType(), declaringType);

        if (castType == null) {

            if (resolvedDeclaringType != null &&
                resolvedDeclaringType.isGenericDefinition() &&
                member.containsGenericParameters()) {

                final int wildcardCount = resolvedDeclaringType.getGenericParameters().size();
                final TypeReference[] typeArguments = new TypeReference[wildcardCount];

                for (int i = 0; i < typeArguments.length; i++) {
                    typeArguments[i] = com.strobel.assembler.metadata.WildcardType.unbounded();
                }

                castType = resolvedDeclaringType.makeGenericType(typeArguments);
            }
            else {
                castType = declaringType;
            }
        }

        final AstType astType = astBuilder.convertType(castType);

        final AstNode replacement = target.replaceWith(
            new Function<AstNode, AstNode>() {
                @Override
                public AstNode apply(final AstNode n) {
                    return new CastExpression(astType, target);
                }
            }
        );

        return replacement.acceptVisitor(this, data);
    }
}

