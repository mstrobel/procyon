package com.strobel.decompiler.languages.java.utilities;

import com.strobel.annotations.NotNull;
import com.strobel.annotations.Nullable;
import com.strobel.assembler.metadata.*;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.semantics.ResolveResult;
import com.strobel.functions.Function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.strobel.core.CollectionUtilities.*;

public final class RedundantCastUtility {
    @NotNull
    public static List<CastExpression> getRedundantCastsInside(final Function<AstNode, ResolveResult> resolver, final AstNode site) {
        VerifyArgument.notNull(resolver, "resolver");

        if (site == null) {
            return Collections.emptyList();
        }

        final CastCollector visitor = new CastCollector(resolver);

        site.acceptVisitor(visitor, null);

        return new ArrayList<>(visitor.getFoundCasts());
    }

    public static boolean isCastRedundant(final Function<AstNode, ResolveResult> resolver, final CastExpression cast) {
        AstNode parent = skipParenthesesUp(cast.getParent());

        if (parent == null) {
            return false;
        }

        if (parent.getRole() == Roles.ARGUMENT || parent.isReference()) {
            parent = parent.getParent();
        }

        final IsRedundantVisitor visitor = new IsRedundantVisitor(resolver, false);

        parent.acceptVisitor(visitor, null);

        return visitor.isRedundant();
    }

    public static void removeCast(final CastExpression castExpression) {
        if (castExpression == null || castExpression.isNull()) {
            return;
        }

        Expression operand = castExpression.getExpression();

        if (operand instanceof ParenthesizedExpression) {
            operand = ((ParenthesizedExpression) operand).getExpression();
        }

        if (operand == null || operand.isNull()) {
            return;
        }

        AstNode toBeReplaced = castExpression;
        AstNode parent = castExpression.getParent();

        while (parent instanceof ParenthesizedExpression) {
            toBeReplaced = parent;
            parent = parent.getParent();
        }

        toBeReplaced.replaceWith(operand);
    }

    @Nullable
    private static Expression removeParentheses(final Expression e) {
        Expression result = e;

        while (result instanceof ParenthesizedExpression) {
            result = ((ParenthesizedExpression) result).getExpression();
        }

        return result;
    }

    @Nullable
    private static AstNode skipParenthesesUp(final AstNode e) {
        AstNode result = e;

        while (result instanceof ParenthesizedExpression) {
            result = result.getParent();
        }

        return result;
    }

    private static class CastCollector extends IsRedundantVisitor {
        private final Set<CastExpression> _foundCasts = new HashSet<>();

        CastCollector(final Function<AstNode, ResolveResult> resolver) {
            super(resolver, true);
        }

        private Set<CastExpression> getFoundCasts() {
            return _foundCasts;
        }

        @Override
        public Void visitAnonymousObjectCreationExpression(final AnonymousObjectCreationExpression node, final Void data) {
            for (final Expression argument : node.getArguments()) {
                argument.acceptVisitor(this, data);
            }
            return null;
        }

        @Override
        public Void visitTypeDeclaration(final TypeDeclaration typeDeclaration, final Void _) {
            return null;
        }

        @Override
        public Void visitMethodDeclaration(final MethodDeclaration node, final Void _) {
            return null;
        }

        @Override
        public Void visitConstructorDeclaration(final ConstructorDeclaration node, final Void _) {
            return null;
        }

        @Override
        protected void addToResults(@NotNull final CastExpression cast, final boolean force) {
            if (force || !isTypeCastSemantic(cast)) {
                _foundCasts.add(cast);
            }
        }
    }

    private static class IsRedundantVisitor extends DepthFirstAstVisitor<Void, Void> {
        private final boolean _isRecursive;
        private final Function<AstNode, ResolveResult> _resolver;

        private boolean _isRedundant;

        IsRedundantVisitor(final Function<AstNode, ResolveResult> resolver, final boolean recursive) {
            _isRecursive = recursive;
            _resolver = resolver;
        }

        public final boolean isRedundant() {
            return _isRedundant;
        }

        // <editor-fold defaultstate="collapsed" desc="Visitor Overrides">

        @Override
        protected Void visitChildren(final AstNode node, final Void data) {
            if (_isRecursive) {
                return super.visitChildren(node, data);
            }
            return null;
        }

        @Override
        public Void visitAssignmentExpression(final AssignmentExpression node, final Void data) {
            processPossibleTypeCast(node.getRight(), getType(node.getLeft()));
            return super.visitAssignmentExpression(node, data);
        }

        @Override
        public Void visitVariableDeclaration(final VariableDeclarationStatement node, final Void data) {
            final TypeReference leftType = getType(node.getType());

            if (leftType != null) {
                for (final VariableInitializer initializer : node.getVariables()) {
                    processPossibleTypeCast(initializer.getInitializer(), leftType);
                }
            }

            return super.visitVariableDeclaration(node, data);
        }

        @Override
        public Void visitReturnStatement(final ReturnStatement node, final Void data) {
            final MethodDeclaration methodDeclaration = firstOrDefault(node.getAncestors(MethodDeclaration.class));

            if (methodDeclaration != null && !methodDeclaration.isNull()) {
                final TypeReference returnType = getType(methodDeclaration.getReturnType());
                final Expression returnValue = node.getExpression();

                if (returnType != null && returnValue != null && !returnValue.isNull()) {
                    processPossibleTypeCast(returnValue, returnType);
                }
            }

            return super.visitReturnStatement(node, data);
        }

        @Override
        public Void visitBinaryOperatorExpression(final BinaryOperatorExpression node, final Void data) {
            final TypeReference leftType = getType(node.getLeft());
            final TypeReference rightType = getType(node.getRight());

            processBinaryExpressionOperand(node.getLeft(), rightType, node.getOperator());
            processBinaryExpressionOperand(node.getRight(), leftType, node.getOperator());

            return super.visitBinaryOperatorExpression(node, data);
        }

        @Override
        public Void visitInvocationExpression(final InvocationExpression node, final Void data) {
            processCall(node);
            return super.visitInvocationExpression(node, data);
        }

        @Override
        public Void visitObjectCreationExpression(final ObjectCreationExpression node, final Void data) {
            for (final Expression argument : node.getArguments()) {
                argument.acceptVisitor(this, data);
            }
            processCall(node);
            return null;
        }

        @Override
        public Void visitAnonymousObjectCreationExpression(final AnonymousObjectCreationExpression node, final Void data) {
            for (final Expression argument : node.getArguments()) {
                argument.acceptVisitor(this, data);
            }
            processCall(node);
            return null;
        }

        @Override
        public Void visitCastExpression(final CastExpression node, final Void data) {
            final Expression operand = node.getExpression();

            if (operand == null || operand.isNull()) {
                return null;
            }

            final TypeReference topCastType = getType(node);

            if (topCastType == null) {
                return null;
            }

            final Expression e = removeParentheses(operand);

            if (e instanceof CastExpression) {
                final CastExpression innerCast = (CastExpression) e;
                final TypeReference innerCastType = getType(innerCast.getType());

                if (innerCastType == null) {
                    return null;
                }

                final Expression innerOperand = innerCast.getExpression();
                final TypeReference innerOperandType = getType(innerOperand);

                if (!innerCastType.isPrimitive()) {
                    if (innerOperandType != null && MetadataHelper.isConvertible(innerOperandType, topCastType)) {
                        addToResults(innerCast, false);
                    }
                }
                else {
                    final ConversionType valueToInner = MetadataHelper.getNumericConversionType(innerCastType, innerOperandType);
                    final ConversionType outerToInner = MetadataHelper.getNumericConversionType(innerCastType, topCastType);

                    if (outerToInner == ConversionType.IDENTITY) {
                        if (valueToInner == ConversionType.IDENTITY) {
                            //
                            // T t; (T)(T)t => t
                            //
                            addToResults(node, false);
                            addToResults(innerCast, true);
                        }
                        else {
                            //
                            // (T)(T)x => (T)x
                            //
                            addToResults(innerCast, true);
                        }
                    }
                    else if (outerToInner == ConversionType.IMPLICIT) {
                        final ConversionType valueToOuter = MetadataHelper.getNumericConversionType(topCastType, innerOperandType);

                        if (valueToOuter != ConversionType.NONE) {
                            //
                            // If V -> T is equivalent to U -> T (assumed if T -> U is an implicit/non-narrowing conversion):
                            // V v; (T)(U)v => (T)v
                            //
                            addToResults(innerCast, true);
                        }
                    }
                    else if (valueToInner == ConversionType.IMPLICIT &&
                             MetadataHelper.getNumericConversionType(topCastType, innerOperandType) == ConversionType.IMPLICIT) {

                        addToResults(innerCast, true);
                    }
                }
            }
            else {
                final AstNode parent = node.getParent();

                if (parent instanceof ConditionalExpression) {
                    //
                    // Branches need to be of the same type.
                    //

                    final TypeReference operandType = getType(operand);
                    final TypeReference conditionalType = getType(parent);

                    if (!MetadataHelper.isSameType(operandType, conditionalType, true)) {
                        if (!checkResolveAfterRemoveCast(parent)) {
                            return null;
                        }

                        final Expression thenExpression = ((ConditionalExpression) parent).getTrueExpression();
                        final Expression elseExpression = ((ConditionalExpression) parent).getFalseExpression();
                        final Expression opposite = (thenExpression == node) ? elseExpression : thenExpression;
                        final TypeReference oppositeType = getType(opposite);

                        if (oppositeType == null || !MetadataHelper.isSameType(conditionalType, oppositeType, true)) {
                            return null;
                        }
                    }
                    else if (topCastType.isPrimitive() && !operandType.isPrimitive()) {
                        //
                        // The types on both sides would be the same, but the existing cast forces the conditional
                        // type to evaluate to an unboxed primitive, which has a side effect.
                        //
                        return null;
                    }
                }
                else if (parent instanceof SynchronizedStatement && getType(e) instanceof PrimitiveType) {
                    return null;
                }
                else if (e instanceof LambdaExpression || e instanceof MethodGroupExpression) {
                    if (parent instanceof ParenthesizedExpression &&
                        parent.getParent() != null &&
                        parent.getParent().isReference()) {

                        return null;
                    }

                    //
                    // TODO: Implement getFunctionalInterfaceType().
                    //

                    final DynamicCallSite callSite = e.getUserData(Keys.DYNAMIC_CALL_SITE);

                    if (callSite == null) {
                        return null;
                    }

                    final MethodReference method = (MethodReference) callSite.getBootstrapArguments().get(0);
                    final TypeReference functionalInterfaceType = method.getDeclaringType();

                    if (!MetadataHelper.isAssignableFrom(topCastType, functionalInterfaceType, false)) {
                        return null;
                    }
                }

                processAlreadyHasTypeCast(node);
            }

            return super.visitCastExpression(node, data);
        }

        // </editor-fold>

        // <editor-fold defaultstate="collapsed" desc="Helper Methods">

        protected TypeReference getType(final AstNode node) {
            final ResolveResult result = _resolver.apply(node);
            return result != null ? result.getType() : null;
        }

        @NotNull
        protected List<TypeReference> getTypes(final AstNodeCollection<? extends AstNode> nodes) {
            if (nodes == null || nodes.isEmpty()) {
                return Collections.emptyList();
            }

            final List<TypeReference> types = new ArrayList<>();

            for (final AstNode node : nodes) {
                final TypeReference nodeType = getType(node);

                if (nodeType == null) {
                    return Collections.emptyList();
                }

                types.add(nodeType);
            }

            return types;
        }

        protected void processPossibleTypeCast(final Expression rightExpression, @Nullable final TypeReference leftType) {
            if (leftType == null) {
                return;
            }

            final Expression r = removeParentheses(rightExpression);

            if (r instanceof CastExpression) {
                final Expression castOperand = ((CastExpression) r).getExpression();

                if (castOperand != null && !castOperand.isNull()) {
                    final TypeReference operandType = getType(castOperand);

                    if (operandType != null &&
                        MetadataHelper.isAssignableFrom(leftType, operandType, false)) {

                        addToResults((CastExpression) r, false);
                    }
                }
            }
        }

        protected void addToResults(@NotNull final CastExpression cast, final boolean force) {
            if (force || !isTypeCastSemantic(cast)) {
                _isRedundant = true;
            }
        }

        protected void processBinaryExpressionOperand(
            final Expression operand,
            final TypeReference otherType,
            final BinaryOperatorType op) {

            if (operand instanceof CastExpression) {
                final CastExpression cast = (CastExpression) operand;
                final Expression toCast = cast.getExpression();
                final TypeReference castType = getType(cast);
                final TypeReference innerType = getType(toCast);

                if (castType != null &&
                    innerType != null &&
                    TypeUtilities.isBinaryOperatorApplicable(op, innerType, otherType, false)) {

                    if (castType.isPrimitive() &&
                        !otherType.isPrimitive() &&
                        (op == BinaryOperatorType.EQUALITY || op == BinaryOperatorType.INEQUALITY)) {

                        if (!innerType.isPrimitive()) {
                            //
                            // Don't change an unboxing (in)equality operator to a reference (in)equality operator.
                            //
                            return;
                        }
                    }

                    addToResults(cast, false);
                }
            }
        }

        @SuppressWarnings("ConstantConditions")
        protected void processCall(@NotNull final Expression e) {
            final AstNodeCollection<Expression> arguments = e.getChildrenByRole(Roles.ARGUMENT);

            if (arguments.isEmpty()) {
                return;
            }

            MemberReference reference = e.getUserData(Keys.MEMBER_REFERENCE);

            if (reference == null && e.getParent() instanceof MemberReferenceExpression) {
                reference = e.getParent().getUserData(Keys.MEMBER_REFERENCE);
            }

            final MethodReference method;

            if (reference instanceof MethodReference) {
                method = (MethodReference) reference;
            }
            else {
                return;
            }

            Expression target = e.getChildByRole(Roles.TARGET_EXPRESSION);

            if (target instanceof MemberReferenceExpression) {
                target = target.getChildByRole(Roles.TARGET_EXPRESSION);
            }

            TypeReference targetType = getType(target);

            if (targetType == null) {
                targetType = method.getDeclaringType();
            }

            final List<MethodReference> candidates = MetadataHelper.findMethods(
                targetType,
                MetadataFilters.matchName(method.getName())
            );

            final MethodDefinition resolvedMethod = method.resolve();
            final List<TypeReference> originalTypes = new ArrayList<>();
            final List<ParameterDefinition> parameters = method.getParameters();
            final Expression lastArgument = arguments.lastOrNullObject();

            List<TypeReference> newTypes = null;
            int syntheticLeadingCount = 0;

            for (final ParameterDefinition parameter : parameters) {
                if (parameter.isSynthetic()) {
                    ++syntheticLeadingCount;
                    originalTypes.add(parameter.getParameterType());
                }
                else {
                    break;
                }
            }

            for (final Expression argument : arguments) {
                final TypeReference argumentType = getType(argument);

                if (argumentType == null) {
                    return;
                }

                originalTypes.add(argumentType);
            }

            int i = syntheticLeadingCount;

            for (Expression a = arguments.firstOrNullObject();
                 a != null && !a.isNull();
                 a = (Expression) a.getNextSibling(Roles.ARGUMENT), ++i) {

                final Expression arg = removeParentheses(a);

                if (!(arg instanceof CastExpression)) {
                    continue;
                }

                if (a == lastArgument &&
                    i == parameters.size() - 1 &&
                    resolvedMethod != null &&
                    resolvedMethod.isVarArgs()) {

                    //
                    // Do not mark cast to resolve ambiguity for calling varargs method
                    // with inexact argument.
                    //

                    continue;
                }

                final CastExpression cast = (CastExpression) arg;
                final Expression castOperand = cast.getExpression();
                final TypeReference castType = getType(cast);
                final TypeReference operandType = getType(castOperand);

                if (castType == null || operandType == null) {
                    continue;
                }

                if (castType.isPrimitive() && !operandType.isPrimitive()) {
                    final ParameterDefinition p = parameters.get(i);
                    final TypeReference parameterType = p.getParameterType();

                    if (!parameterType.isPrimitive()) {
                        //
                        // Don't mark a cast as redundant if it has a side effect (possible NullPointerException).
                        //
                        continue;
                    }
                }

                if (newTypes == null) {
                    newTypes = new ArrayList<>(originalTypes);
                }
                else {
                    newTypes.clear();
                    newTypes.addAll(originalTypes);
                }

                newTypes.set(i, operandType);

                final MethodBinder.BindResult result = MethodBinder.selectMethod(candidates, newTypes);

                if (result.isFailure() || result.isAmbiguous()) {
                    continue;
                }

                final boolean sameMethod = StringUtilities.equals(
                    method.getErasedSignature(),
                    result.getMethod().getErasedSignature()
                );

                if (sameMethod) {
                    addToResults(cast, false);
                }
            }

            for (final Expression arg : arguments) {
                if (arg instanceof CastExpression) {
                    final Expression castOperand = ((CastExpression) arg).getExpression();

                    if (castOperand != null) {
                        castOperand.acceptVisitor(this, null);
                    }
                }
                else {
                    arg.acceptVisitor(this, null);
                }
            }
        }

        protected void processAlreadyHasTypeCast(final CastExpression cast) {
            AstNode parent = cast.getParent();

            while (parent instanceof ParenthesizedExpression) {
                parent = parent.getParent();
            }

            if (parent == null ||
                cast.getRole() == Roles.ARGUMENT && !(parent instanceof IndexerExpression) ||
                parent instanceof ReturnStatement ||
                parent instanceof CastExpression ||
                parent instanceof BinaryOperatorExpression) {

                //
                // Null, or handled by ancestor.
                //

                return;
            }

            if (isTypeCastSemantic(cast)) {
                return;
            }

            final TypeReference castTo = getType(cast.getType());
            final Expression operand = cast.getExpression();

            TypeReference operandType = getType(operand);

            if (castTo == null || operandType == null) {
                return;
            }

            final TypeReference expectedType = TypeUtilities.getExpectedTypeByParent(_resolver, cast);

            if (expectedType != null) {
                operandType = expectedType;
            }

            if (operandType == BuiltinTypes.Null && castTo.isPrimitive()) {
                return;
            }

            if (parent.isReference()) {
                if (operandType.isPrimitive() && !castTo.isPrimitive()) {
                    //
                    // Explicit boxing.
                    //
                    return;
                }

                final TypeReference referenceType = getType(parent);

                if (!operandType.isPrimitive() &&
                    referenceType != null &&
                    !isCastRedundantInReferenceExpression(referenceType, operand)) {

                    return;
                }
            }

            if (arrayAccessAtTheLeftSideOfAssignment(parent)) {
                if (MetadataHelper.isAssignableFrom(operandType, castTo, false) &&
                    MetadataHelper.getArrayRank(operandType) == MetadataHelper.getArrayRank(castTo)) {

                    addToResults(cast, false);
                }
            }
            else if (MetadataHelper.isAssignableFrom(castTo, operandType, false)) {
                addToResults(cast, false);
            }
        }

        protected boolean arrayAccessAtTheLeftSideOfAssignment(final AstNode node) {
            final AssignmentExpression assignment = firstOrDefault(node.getAncestors(AssignmentExpression.class));

            if (assignment == null) {
                return false;
            }

            final Expression left = assignment.getLeft();

            return left.isAncestorOf(node) &&
                   left instanceof IndexerExpression;
        }

        @SuppressWarnings("UnusedParameters")
        protected boolean isCastRedundantInReferenceExpression(final TypeReference type, final Expression operand) {
            return false;
        }

        protected boolean checkResolveAfterRemoveCast(final AstNode parent) {
            final AstNode grandParent = parent.getParent();

            if (grandParent == null || parent.getRole() != Roles.ARGUMENT) {
                return true;
            }

            final TypeReference targetType;

            if (grandParent instanceof InvocationExpression) {
                targetType = getType(((InvocationExpression) parent).getTarget());
            }
            else {
                targetType = getType(grandParent);
            }

            if (targetType == null) {
                return false;
            }

            final Expression expression = (Expression) grandParent.clone();
            final AstNodeCollection<Expression> arguments = expression.getChildrenByRole(Roles.ARGUMENT);
            final List<TypeReference> argumentTypes = getTypes(arguments);

            if (argumentTypes.isEmpty()) {
                return arguments.isEmpty();
            }

            MemberReference memberReference = grandParent.getUserData(Keys.MEMBER_REFERENCE);

            if (!(memberReference instanceof MethodReference) && grandParent.getParent() != null) {
                memberReference = grandParent.getParent().getUserData(Keys.MEMBER_REFERENCE);
            }

            if (!(memberReference instanceof MethodReference)) {
                return false;
            }

            final MethodReference method = (MethodReference) memberReference;
            final MethodDefinition resolvedMethod = method.resolve();

            if (resolvedMethod == null) {
                return false;
            }

            final int argumentIndex = indexOf(arguments, (Expression) parent);
            final Expression toReplace = get(arguments, argumentIndex);

            if (toReplace instanceof ConditionalExpression) {
                final Expression trueExpression = ((ConditionalExpression) toReplace).getTrueExpression();
                final Expression falseExpression = ((ConditionalExpression) toReplace).getFalseExpression();

                if (trueExpression instanceof CastExpression) {
                    final Expression trueOperand = ((CastExpression) trueExpression).getExpression();
                    final TypeReference operandType = getType(trueOperand);

                    if (operandType != null) {
                        trueExpression.replaceWith(trueOperand);
                    }
                }
                else if (falseExpression instanceof CastExpression) {
                    final Expression falseOperand = ((CastExpression) falseExpression).getExpression();
                    final TypeReference operandType = getType(falseOperand);

                    if (operandType != null) {
                        falseExpression.replaceWith(falseOperand);
                    }
                }

                final TypeReference newArgumentType = getType(toReplace);

                if (newArgumentType == null) {
                    return false;
                }

                argumentTypes.set(argumentIndex, newArgumentType);
            }

            final List<MethodReference> candidates = MetadataHelper.findMethods(
                targetType,
                MetadataFilters.matchName(resolvedMethod.getName())
            );

            final MethodBinder.BindResult result = MethodBinder.selectMethod(candidates, argumentTypes);

            return !result.isFailure() &&
                   !result.isAmbiguous() &&
                   StringUtilities.equals(resolvedMethod.getErasedSignature(), result.getMethod().getErasedSignature());
        }

        public boolean isTypeCastSemantic(final CastExpression cast) {
            final Expression operand = cast.getExpression();

            if (operand.isNull()) {
                return false;
            }

            if (isInPolymorphicCall(cast)) {
                return true;
            }

            final TypeReference opType = getType(operand);
            final TypeReference castType = getType(cast.getType());

            if (opType == null || castType == null) {
                return false;
            }

            if (castType instanceof PrimitiveType) {
                if (opType instanceof PrimitiveType) {
                    final ConversionType conversionType = MetadataHelper.getNumericConversionType(castType, opType);

                    return conversionType != ConversionType.IDENTITY &&
                           conversionType != ConversionType.IMPLICIT;
                }

                final TypeReference unboxedOpType = MetadataHelper.getUnderlyingPrimitiveTypeOrSelf(opType);

                if (unboxedOpType.isPrimitive()) {
                    final ConversionType conversionType = MetadataHelper.getNumericConversionType(castType, unboxedOpType);

                    return conversionType != ConversionType.IDENTITY &&
                           conversionType != ConversionType.IMPLICIT;
                }
            }
            else if (castType instanceof IGenericInstance) {
                if (MetadataHelper.isRawType(opType)) {
                    return !MetadataHelper.isAssignableFrom(castType, opType, false);
                }
            }
            else if (MetadataHelper.isRawType(castType)) {
                if (opType instanceof IGenericInstance) {
                    return !MetadataHelper.isAssignableFrom(castType, opType, false);
                }
            }

            if (operand instanceof LambdaExpression || operand instanceof MethodGroupExpression) {
                final MetadataParser parser = new MetadataParser(IMetadataResolver.EMPTY);
                final TypeReference serializable = parser.parseTypeDescriptor("java/lang/Serializable");

                if (!castType.isPrimitive() && MetadataHelper.isSubType(castType, serializable)) {
                    return true;
                }

                if (castType instanceof CompoundTypeReference) {
                    boolean redundant = false;

                    final CompoundTypeReference compoundType = (CompoundTypeReference) castType;
                    final List<TypeReference> interfaces = compoundType.getInterfaces();

                    int start = 0;
                    TypeReference baseType = compoundType.getBaseType();

                    if (baseType == null) {
                        baseType = first(interfaces);
                        start = 1;
                    }

                    for (int i = start; i < interfaces.size(); i++) {
                        final TypeReference conjunct = interfaces.get(i);

                        if (MetadataHelper.isAssignableFrom(baseType, conjunct)) {
                            redundant = true;
                            break;
                        }
                    }

                    if (!redundant) {
                        return true;
                    }
                }
            }

            AstNode parent = cast.getParent();

            while (parent instanceof ParenthesizedExpression) {
                parent = parent.getParent();
            }

            if (parent instanceof BinaryOperatorExpression) {
                final BinaryOperatorExpression expression = (BinaryOperatorExpression) parent;

                Expression firstOperand = expression.getLeft();
                Expression otherOperand = expression.getRight();

                if (otherOperand.isAncestorOf(cast)) {
                    final Expression temp = otherOperand;
                    otherOperand = firstOperand;
                    firstOperand = temp;
                }

                if (firstOperand != null &&
                    otherOperand != null &&
                    wrapperCastChangeSemantics(firstOperand, otherOperand, operand)) {

                    return true;
                }
            }
            else if (parent instanceof ConditionalExpression) {
                if (opType.isPrimitive() && !(getType(parent) instanceof PrimitiveType)) {
                    final TypeReference expectedType = TypeUtilities.getExpectedTypeByParent(_resolver, (Expression) parent);

                    if (expectedType != null &&
                        MetadataHelper.getUnderlyingPrimitiveTypeOrSelf(expectedType).isPrimitive()) {

                        return true;
                    }
                }
            }

            return false;
        }

        public boolean isInPolymorphicCall(final CastExpression cast) {
            //
            // See http://download.java.net/jdk7/docs/api/java/lang/invoke/MethodHandle.html#sigpoly
            //

            final Expression operand = cast.getExpression();

            if (operand instanceof InvocationExpression ||
                operand instanceof MemberReferenceExpression && operand.getParent() instanceof InvocationExpression ||
                operand instanceof ObjectCreationExpression) {

                if (isPolymorphicMethod(operand)) {
                    return true;
                }
            }

            return cast.getRole() == Roles.ARGUMENT &&
                   isPolymorphicMethod(skipParenthesesUp(cast.getParent()));
        }

        private static boolean isPolymorphicMethod(final AstNode expression) {
            if (expression == null) {
                return false;
            }

            MemberReference memberReference = expression.getUserData(Keys.MEMBER_REFERENCE);

            if (memberReference == null && expression.getParent() instanceof MemberReferenceExpression) {
                memberReference = expression.getParent().getUserData(Keys.MEMBER_REFERENCE);
            }

            if (memberReference != null) {
                final List<CustomAnnotation> annotations = memberReference.getAnnotations();

                for (final CustomAnnotation annotation : annotations) {
                    final String typeName = annotation.getAnnotationType().getInternalName();

                    if (StringUtilities.equals(typeName, "java.lang.invoke.MethodHandle.PolymorphicSignature")) {
                        return true;
                    }
                }
            }

            return false;
        }

        private boolean wrapperCastChangeSemantics(final Expression operand, final Expression otherOperand, final Expression toCast) {
            final TypeReference operandType = getType(operand);
            final TypeReference otherType = getType(otherOperand);
            final TypeReference castType = getType(toCast);

            final boolean isPrimitiveComparisonWithCast = operandType != null && operandType.isPrimitive() ||
                                                          otherType != null && otherType.isPrimitive();

            final boolean isPrimitiveComparisonWithoutCast = castType != null && castType.isPrimitive() ||
                                                             operandType != null && operandType.isPrimitive();

            //
            // Wrapper cast to primitive vs. wrapper comparison
            //

            return isPrimitiveComparisonWithCast != isPrimitiveComparisonWithoutCast;
        }

        // </editor-fold>
    }
}
