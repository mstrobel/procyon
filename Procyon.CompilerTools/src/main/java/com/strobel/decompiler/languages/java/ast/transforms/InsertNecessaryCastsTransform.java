package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.assembler.metadata.ConversionType;
import com.strobel.assembler.metadata.MetadataHelper;
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

        return null;
    }
}
