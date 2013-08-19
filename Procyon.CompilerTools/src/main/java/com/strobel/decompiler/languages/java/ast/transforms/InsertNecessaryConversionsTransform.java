package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.assembler.metadata.ConversionType;
import com.strobel.assembler.metadata.JvmType;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.languages.java.utilities.RedundantCastUtility;
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

        if (astBuilder == null)
            return null;

        addCastForAssignment(astBuilder.convertType(member.getDeclaringType(), NO_IMPORT_OPTIONS), node.getTarget());

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
        for (final VariableInitializer initializer : node.getVariables()) {
            addCastForAssignment(node, initializer.getInitializer());
        }
        return super.visitVariableDeclaration(node, data);
    }

    @Override
    public Void visitReturnStatement(final ReturnStatement node, final Void data) {
        super.visitReturnStatement(node, data);

        final MethodDeclaration method = firstOrDefault(node.getAncestors(MethodDeclaration.class));

        if (method == null) {
            return null;
        }

        final AstType left = method.getReturnType();
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

                replacement = right.replaceWith(
                    new Function<AstNode, AstNode>() {
                        @Override
                        public AstNode apply(final AstNode input) {
                            return new ConditionalExpression(
                                right,
                                new PrimitiveExpression(new PrimitiveExpression(1)),
                                new PrimitiveExpression(new PrimitiveExpression(0))
                            );
                        }
                    }
                );

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

