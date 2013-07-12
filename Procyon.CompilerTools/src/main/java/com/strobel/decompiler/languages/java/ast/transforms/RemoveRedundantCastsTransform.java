package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.*;
import com.strobel.core.Predicate;
import com.strobel.core.Predicates;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.Role;
import com.strobel.decompiler.semantics.ResolveResult;

import java.util.ArrayList;
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

        final ConversionType valueToCast = MetadataHelper.getConversionType(castType, valueType);

        if (valueToCast == ConversionType.IDENTITY) {
            //
            // T t; f((T)t) => f(t)
            //
            value.remove();
            node.replaceWith(value);
            return;
        }

        final ParameterDefinition parameter = parameters.get(argumentPosition);
        final TypeReference targetType = parameter.getParameterType();
        final ConversionType castToTarget = MetadataHelper.getConversionType(targetType, castType);

        if (castToTarget != ConversionType.IDENTITY) {
            return;
        }

        final ConversionType valueToTarget = MetadataHelper.getConversionType(targetType, valueType);

        if (valueToTarget != ConversionType.IMPLICIT) {
            return;
        }

        int i = 0;
        int argumentIndex = -1;

        final List<TypeReference> argumentTypes = new ArrayList<>();

        for (final Expression argument : arguments) {
            if (argument == node) {
                argumentIndex = i;
            }

            final ResolveResult argumentResult = _resolver.apply(argument);

            if (argumentResult == null || argumentResult.getType() == null) {
                return;
            }

            ++i;
            argumentTypes.add(argumentResult.getType());
        }

        if (argumentIndex < 0) {
            return;
        }

        final TypeReference declaringType = method.getDeclaringType();

        final List<MethodReference> candidates = MetadataHelper.findMethods(
            declaringType,
            Predicates.and(
                MetadataFilters.<MethodReference>matchName(method.getName()),
                new Predicate<MethodReference>() {
                    @Override
                    public boolean test(final MethodReference m) {
                        final MethodDefinition r = m.resolve();
                        return r == null || !r.isBridgeMethod();
                    }
                }
            )
        );

        final MethodBinder.BindResult c1 = MethodBinder.selectMethod(candidates, argumentTypes);

        if (c1.isFailure() || c1.isAmbiguous()) {
            return;
        }

        argumentTypes.set(argumentIndex, valueType);

        final MethodBinder.BindResult c2 = MethodBinder.selectMethod(candidates, argumentTypes);

        if (c2.isFailure() ||
            c2.isAmbiguous() ||
            !StringUtilities.equals(c2.getMethod().getSignature(), c1.getMethod().getSignature())) {

            return;
        }

        //
        // Given f(U u) and an implicit conversion of T -> U: T t; f((T)t) => f(t)
        //

        value.remove();
        node.replaceWith(value);
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

        final ConversionType valueToInner = MetadataHelper.getNumericConversionType(innerCastType, valueType);
        final ConversionType outerToInner = MetadataHelper.getNumericConversionType(innerCastType, outerCastType);

        if (outerToInner == ConversionType.IDENTITY) {
            if (valueToInner == ConversionType.IDENTITY) {
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

        if (outerToInner != ConversionType.IMPLICIT) {
            return;
        }

        final ConversionType valueToOuter = MetadataHelper.getNumericConversionType(outerCastType, valueType);

        if (valueToOuter == ConversionType.NONE) {
            return;
        }

        //
        // If V -> T is equivalent to U -> T (assumed if T -> U is an implicit/non-narrowing conversion):
        // V v; (T)(U)v => (T)v
        //

        value.remove();
        node.replaceWith(value);
    }
}
