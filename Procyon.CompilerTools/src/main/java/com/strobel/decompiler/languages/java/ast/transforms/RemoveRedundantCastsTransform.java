package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.NumericConversionType;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.Predicate;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.Role;
import com.strobel.decompiler.semantics.ResolveResult;

import java.util.List;

import static com.strobel.core.CollectionUtilities.firstIndexWhere;

public class RemoveRedundantCastsTransform extends ContextTrackingVisitor<Void> {
    private final JavaResolver _resolver;

    public RemoveRedundantCastsTransform(final DecompilerContext context) {
        super(context);
        _resolver = new JavaResolver(context);
    }

    @Override
    public Void visitCastExpression(final CastExpression node, final Void data) {
        super.visitCastExpression(node, data);

        tryRemoveCast(node);

        return null;
    }

    private void tryRemoveCast(final CastExpression node) {
        final AstNode parent = node.getParent();
        final Expression value = node.getExpression();
        final ResolveResult valueResult = _resolver.apply(value);

        if (parent == null ||
            parent.isNull() ||
            value == null ||
            value.isNull() ||
            valueResult == null ||
            valueResult.getType() == null) {

            return;
        }

        final TypeReference sourceType = node.getType().toTypeReference();

        if (sourceType == null) {
            return;
        }

        final Role role = node.getRole();

        if (role == Roles.ARGUMENT) {
            tryRemoveCastForArgument(node, value, parent, sourceType, valueResult.getType());
            return;
        }

        if (parent instanceof CastExpression) {
            trySimplifyDoubleCast(node, value, (CastExpression) parent, sourceType, valueResult.getType());
            return;
        }
    }

    private void tryRemoveCastForArgument(
        final CastExpression node,
        final Expression value,
        final AstNode parent,
        final TypeReference castType,
        final TypeReference valueType) {

        final MemberReference member = parent.getUserData(Keys.MEMBER_REFERENCE);

        if (!(member instanceof MethodReference)) {
            return;
        }

        final MethodReference method = (MethodReference) member;
        final List<ParameterDefinition> parameters = method.getParameters();
        final AstNodeCollection<Expression> arguments = parent.getChildrenByRole(Roles.ARGUMENT);

        final int argumentPosition = firstIndexWhere(
            arguments,
            new Predicate<Expression>() {
                @Override
                public boolean test(final Expression e) {
                    return e == node;
                }
            }
        );

        if (argumentPosition < 0 || argumentPosition >= parameters.size()) {
            return;
        }

        final NumericConversionType valueToCast = MetadataHelper.getNumericConversionType(castType, valueType);

        if (valueToCast == NumericConversionType.IDENTITY) {
            //
            // T t; f((T)t) => f(t)
            //
            value.remove();
            node.replaceWith(value);
            return;
        }

/*
        final ParameterDefinition parameter = parameters.get(argumentPosition);
        final TypeReference targetType = parameter.getParameterType();
        final NumericConversionType castToTarget = MetadataHelper.getNumericConversionType(targetType, castType);

        if (castToTarget != NumericConversionType.IDENTITY) {
            return;
        }

        final NumericConversionType valueToTarget = MetadataHelper.getNumericConversionType(targetType, valueType);

        if (valueToTarget == NumericConversionType.IMPLICIT) {
            //
            // Given f(U u) and an implicit conversion of T -> U: T t; f((T)t) => f(t)
            //

            value.remove();
            node.replaceWith(value);
        }
*/
    }

    private void trySimplifyDoubleCast(
        final CastExpression node,
        final Expression value,
        final CastExpression parent,
        final TypeReference innerCastType,
        final TypeReference valueType) {

        final TypeReference outerCastType = parent.getType().toTypeReference();

        if (outerCastType == null) {
            return;
        }

        final NumericConversionType valueToInner = MetadataHelper.getNumericConversionType(innerCastType, valueType);
        final NumericConversionType outerToInner = MetadataHelper.getNumericConversionType(innerCastType, outerCastType);

        if (outerToInner == NumericConversionType.IDENTITY) {
            if (valueToInner == NumericConversionType.IDENTITY) {
                //
                // T t; (T)(T)t => t
                //
                value.remove();
                parent.replaceWith(value);
            }
            else {
                //
                // (T)(T)x => (T)x
                //
                value.remove();
                node.replaceWith(value);
            }
            return;
        }

        if (outerToInner != NumericConversionType.IMPLICIT) {
            return;
        }

        final NumericConversionType valueToOuter = MetadataHelper.getNumericConversionType(outerCastType, valueType);

        if (valueToOuter == NumericConversionType.NONE) {
            return;
        }

        //
        // IF V -> T is equivalent to U -> T (assumed if T -> U is an implicit/non-narrowing conversion):
        // V v; (T)(U)v => (T)v
        //

        value.remove();
        node.replaceWith(value);
    }
}
