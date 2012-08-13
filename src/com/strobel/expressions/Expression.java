package com.strobel.expressions;

import com.strobel.core.ReadOnlyList;
import com.strobel.core.VerifyArgument;
import com.strobel.reflection.*;
import com.strobel.util.ContractUtils;
import com.strobel.util.TypeUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

/**
 * The base type for all nodes in Expression Trees.
 * @author Mike Strobel
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class Expression {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // INSTANCE MEMBERS                                                                                                   //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected Expression() {}

    /**
     * Returns the node type of this {@link Expression}.
     * @return the {@link ExpressionType} that represents this expression.
     */
    public ExpressionType getNodeType() {
        throw Error.extensionMustOverride("Expression.getNodeType()");
    }

    /**
     * Gets the static type of the expression that this {@link Expression} represents.
     * @return the {@link Type} that represents the static type of the expression.
     */
    public com.strobel.reflection.Type getType() {
        throw Error.extensionMustOverride("Expression.getType()");
    }

    /**
     * Indicates that the node can be reduced to a simpler node. If this returns {@code true},
     * {@code reduce()} can be called to produce the reduced form.
     * @return {@code true} if the node can be reduced; otherwise, {@code false}.
     */
    public boolean canReduce() {
        return false;
    }

    /**
     * Reduces this node to a simpler expression.  If {@code canReduce()} returns {@code true},
     * this should return a valid expression.  This method is allowed to return another node
     * which itself must be reduced.
     * @return the reduced expression.
     */
    public Expression reduce() {
        if (canReduce()) {
            throw Error.reducibleMustOverride("Expression.reduce()");
        }
        return this;
    }

    /**
     * Reduces this node to a simpler expression.  If {@code canReduce()} returns {@code true},
     * this should return a valid expression.  This method is allowed to return another node
     * which itself must be reduced.  Unlike {@code reduce()}, this method checks that the
     * reduced node satisfies certain invariants.
     * @return the reduced expression.
     */
    public final Expression reduceAndCheck() {
        if (!canReduce()) {
            throw Error.mustBeReducible();
        }

        final Expression newNode = reduce();

        if (newNode == null || newNode == this) {
            throw Error.mustReduceToDifferent();
        }

        if (!getType().isAssignableFrom(newNode.getType())) {
            throw Error.reducedNotCompatible();
        }

        return newNode;
    }

    /**
     * Reduces the expression to a known node type (i.e. not an Extension node or simply
     * returns the expression if it is already a known type.
     * @return the reduced expression.
     */
    public final Expression reduceExtensions() {
        Expression node = this;

        while (node.getNodeType() == ExpressionType.Extension) {
            node = node.reduceAndCheck();
        }

        return node;
    }

    /**
     * Dispatches to the specific visit method for this node type.  For example,
     * {@link BinaryExpression} will call into {@code ExpressionVisitor.visitBinary()}.
     * @param visitor the visitor to visit this node.
     * @return the result of visiting this node.
     */
    protected Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitExtension(this);
    }

    /**
     * Reduces the node and then calls the visitor on the reduced expression.  Throws an
     * exception if the node isn't reducible.
     * @param visitor an expression visitor
     * @return the expression being visited, or an expression which should replace it in the tree.
     */
    protected Expression visitChildren(final ExpressionVisitor visitor) {
        if (!canReduce()) {
            throw Error.mustBeReducible();
        }
        return visitor.visit(reduceAndCheck());
    }

    @Override
    public String toString() {
        return ExpressionStringBuilder.expressionToString(this);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FACTORY METHODS                                                                                                    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Expression empty() {
        return new DefaultValueExpression(PrimitiveTypes.Void);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LABELS                                                                                                             //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static LabelTarget label() {
        return label(PrimitiveTypes.Void, null);
    }

    public static LabelTarget label(final String name) {
        return label(PrimitiveTypes.Void, name);
    }

    public static LabelTarget label(final Type type) {
        return label(type, null);
    }

    public static LabelTarget label(final Type type, final String name) {
        VerifyArgument.notNull(type, "type");

        return new LabelTarget(type, name);
    }

    public static LabelExpression label(final LabelTarget target) {
        VerifyArgument.notNull(target, "target");

        if (target.getType() != PrimitiveTypes.Void) {
            return label(target, defaultValue(target.getType()));
        }

        return label(target, null);
    }

    public static LabelExpression label(final LabelTarget target, final Expression defaultValue) {
        validateGoto(target, defaultValue, "label", "defaultValue");
        return new LabelExpression(target, defaultValue);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // GOTO, BREAK, CONTINUE, AND RETURN EXPRESSIONS                                                                      //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static GotoExpression makeBreak(final LabelTarget target) {
        return makeGoto(GotoExpressionKind.Break, target, null, PrimitiveTypes.Void);
    }

    public static GotoExpression makeBreak(final LabelTarget target, final Expression value) {
        return makeGoto(GotoExpressionKind.Break, target, value, PrimitiveTypes.Void);
    }

    public static GotoExpression makeBreak(final LabelTarget target, final Type type) {
        return makeGoto(GotoExpressionKind.Break, target, null, type);
    }

    public static GotoExpression makeBreak(final LabelTarget target, final Expression value, final Type type) {
        return makeGoto(GotoExpressionKind.Break, target, value, type);
    }

    public static GotoExpression makeContinue(final LabelTarget target) {
        return makeGoto(GotoExpressionKind.Continue, target, null, PrimitiveTypes.Void);
    }

    public static GotoExpression makeContinue(final LabelTarget target, final Type type) {
        return makeGoto(GotoExpressionKind.Continue, target, null, type);
    }

    public static GotoExpression makeReturn(final LabelTarget target) {
        return makeGoto(GotoExpressionKind.Return, target, null, PrimitiveTypes.Void);
    }

    public static GotoExpression makeReturn(final LabelTarget target, final Type type) {
        return makeGoto(GotoExpressionKind.Return, target, null, type);
    }

    public static GotoExpression makeReturn(final LabelTarget target, final Expression value) {
        return makeGoto(GotoExpressionKind.Return, target, value, PrimitiveTypes.Void);
    }

    public static GotoExpression makeReturn(final LabelTarget target, final Expression value, final Type type) {
        return makeGoto(GotoExpressionKind.Return, target, value, type);
    }

    public static GotoExpression makeGoto(final LabelTarget target) {
        return makeGoto(GotoExpressionKind.Goto, target, null, PrimitiveTypes.Void);
    }

    public static GotoExpression makeGoto(final LabelTarget target, final Type type) {
        return makeGoto(GotoExpressionKind.Goto, target, null, type);
    }

    public static GotoExpression makeGoto(final LabelTarget target, final Expression value) {
        return makeGoto(GotoExpressionKind.Goto, target, value, PrimitiveTypes.Void);
    }

    public static GotoExpression makeGoto(final LabelTarget target, final Expression value, final Type type) {
        return makeGoto(GotoExpressionKind.Goto, target, value, type);
    }

    public static GotoExpression makeGoto(final GotoExpressionKind kind, final LabelTarget target, final Expression value, final Type type) {
        validateGoto(target, value, "target", "value");
        return new GotoExpression(kind, target, value, type);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LOOP EXPRESSIONS                                                                                                   //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static LoopExpression loop(final Expression body) {
        return loop(body, null, null);
    }

    public static LoopExpression loop(final Expression body, final LabelTarget breakLabel) {
        return loop(body, breakLabel, null);
    }

    public static LoopExpression loop(final Expression body, final LabelTarget breakLabel, final LabelTarget continueLabel) {
        verifyCanRead(body, "body");

        if (continueLabel != null && continueLabel.getType() != PrimitiveTypes.Void) {
            throw Error.labelTypeMustBeVoid();
        }

        return new LoopExpression(body, breakLabel, continueLabel);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // NEW EXPRESSIONS                                                                                                    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static NewExpression makeNew(final ConstructorInfo constructor) {
        return makeNew(constructor, ExpressionList.empty());
    }

    public static NewExpression makeNew(final ConstructorInfo constructor, final Expression... parameters) {
        return makeNew(constructor, arrayToList(parameters));
    }

    public static NewExpression makeNew(final ConstructorInfo constructor, final ExpressionList<? extends Expression> parameters) {
        VerifyArgument.notNull(constructor, "constructor");
        VerifyArgument.notNull(constructor.getDeclaringType(), "constructor.getDeclaringType()");

        final ExpressionList<? extends Expression> arguments = validateArgumentTypes(
            constructor,
            ExpressionType.New,
            parameters
        );

        return new NewExpression(constructor, arguments);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // NEW ARRAY EXPRESSIONS                                                                                              //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static NewArrayExpression newArrayInit(final Type type, final Expression... initializers) {
        return newArrayInit(type, arrayToList(initializers));
    }

    public static NewArrayExpression newArrayInit(final Type type, final ExpressionList<? extends Expression> initializers) {
        VerifyArgument.notNull(type, "type");
        VerifyArgument.noNullElements(initializers, "initializers");

        if (type.isEquivalentTo(PrimitiveTypes.Void)) {
            throw Error.argumentCannotBeOfTypeVoid();
        }

        for (int i = 0, n = initializers.size(); i < n; i++) {
            final Expression item = initializers.get(i);

            verifyCanRead(item, "initializers");

            if (!TypeUtils.areReferenceAssignable(type, item.getType())) {
                throw Error.expressionTypeCannotInitializeArrayType(item.getType(), type);
            }
        }

        return NewArrayInitExpression.make(ExpressionType.NewArrayInit, type, initializers);
    }

    public static NewArrayExpression newArrayBounds(final Type type, final Expression dimension) {
        VerifyArgument.notNull(type, "type");
        VerifyArgument.notNull(dimension, "dimension");

        verifyCanRead(dimension, "dimension");

        if (type.isEquivalentTo(PrimitiveTypes.Void)) {
            throw Error.argumentCannotBeOfTypeVoid();
        }

        if (!TypeUtils.isIntegral(type)) {
            throw Error.argumentMustBeIntegral();
        }

        final Expression convertedDimension;

        if (TypeUtils.getUnderlyingPrimitiveOrSelf(type) != PrimitiveTypes.Integer) {
            convertedDimension = convert(dimension, PrimitiveTypes.Integer);
        }
        else {
            convertedDimension = dimension;
        }

        return NewArrayInitExpression.make(
            ExpressionType.NewArrayBounds,
            type,
            new ExpressionList<>(convertedDimension)
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // TRY/CATCH EXPRESSIONS                                                                                              //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static CatchBlock makeCatch(final Type type, final Expression body) {
        return makeCatch(type, null, body, null);
    }

    public static CatchBlock makeCatch(final ParameterExpression variable, final Expression body) {
        VerifyArgument.notNull(variable, "variable");
        return makeCatch(variable.getType(), variable, body, null);
    }

    public static CatchBlock makeCatch(final Type type, final Expression body, final Expression filter) {
        return makeCatch(type, null, body, filter);
    }

    public static CatchBlock makeCatch(final ParameterExpression variable, final Expression body, final Expression filter) {
        VerifyArgument.notNull(variable, "variable");
        return makeCatch(variable.getType(), variable, body, filter);
    }

    public static CatchBlock makeCatch(
        final Type type,
        final ParameterExpression variable,
        final Expression body,
        final Expression filter) {

        VerifyArgument.notNull(type, "type");

        if (variable != null && !TypeUtils.areEquivalent(variable.getType(), type)) {
            throw Error.catchVariableMustBeCompatibleWithCatchType(type, variable.getType());
        }

        verifyCanRead(body, "body");

        if (filter != null) {
            verifyCanRead(filter, "filter");
            if (!TypeUtils.hasIdentityPrimitiveOrBoxingConversion(filter.getType(), PrimitiveTypes.Boolean)) {
                throw Error.argumentMustBeBoolean();
            }
        }

        return new CatchBlock(type, variable, body, filter);
    }

    public static TryExpression tryFinally(final Expression body, final Expression finallyBlock) {
        return makeTry(null, body, finallyBlock);
    }

    public static TryExpression tryCatch(final Expression body, final CatchBlock... handlers) {
        return makeTry(null, body, null, handlers);
    }

    public static TryExpression tryCatchFinally(final Expression body, final Expression finallyBlock, final CatchBlock... handlers) {
        return makeTry(null, body, finallyBlock, handlers);
    }

    public static TryExpression makeTry(final Type type, final Expression body, final Expression finallyBlock, final CatchBlock... handlers) {
        final ReadOnlyList<CatchBlock> catchBlocks;

        if (handlers != null) {
            catchBlocks = new ReadOnlyList<>(handlers);
        }
        else {
            VerifyArgument.noNullElements(handlers, "handlers");
            catchBlocks = ReadOnlyList.emptyList();
        }

        return makeTry(type, body, catchBlocks, finallyBlock);
    }

    public static TryExpression makeTry(
        final Type type,
        final Expression body,
        final ReadOnlyList<CatchBlock> catchBlocks,
        final Expression finallyBlock) {

        verifyCanRead(body, "body");
        VerifyArgument.noNullElements(catchBlocks, "catchBlocks");
        validateTryAndCatchHaveSameType(type, body, catchBlocks);

        if (finallyBlock != null) {
            verifyCanRead(finallyBlock, "finallyBlock");
        }
        else if (catchBlocks.isEmpty()) {
            throw Error.tryMustHaveCatchOrFinally();
        }

        return new TryExpression(
            type != null ? type : body.getType(),
            body,
            catchBlocks,
            finallyBlock
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // RUNTIME VARIABLES EXPRESSIONS                                                                                      //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static RuntimeVariablesExpression runtimeVariables(final ParameterExpression... variables) {
        return runtimeVariables(arrayToList(variables));
    }

    public static RuntimeVariablesExpression runtimeVariables(final ParameterExpressionList variables) {
        VerifyArgument.noNullElements(variables, "variables");

        return new RuntimeVariablesExpression(variables);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // CONDITIONAL EXPRESSIONS                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ConditionalExpression condition(
        final Expression test,
        final Expression ifTrue,
        final Expression ifFalse) {

        if (!TypeUtils.hasIdentityPrimitiveOrBoxingConversion(test.getType(), PrimitiveTypes.Boolean)) {
            throw Error.argumentMustBeBoolean();
        }

        if (!TypeUtils.hasIdentityPrimitiveOrBoxingConversion(ifTrue.getType(), ifFalse.getType())) {
            throw Error.argumentTypesMustMatch();
        }

        return condition(test, ifTrue, ifFalse, ifTrue.getType());
    }

    public static ConditionalExpression condition(
        final Expression test,
        final Expression ifTrue,
        final Expression ifFalse,
        final Type type) {

        verifyCanRead(test, "test");
        verifyCanRead(ifTrue, "ifTrue");
        verifyCanRead(ifFalse, "ifFalse");

        VerifyArgument.notNull(type, "type");

        if (!TypeUtils.hasIdentityPrimitiveOrBoxingConversion(test.getType(), PrimitiveTypes.Boolean)) {
            throw Error.argumentMustBeBoolean();
        }

        if (type != PrimitiveTypes.Void &&
            (!TypeUtils.areReferenceAssignable(type, ifTrue.getType()) ||
             !TypeUtils.areReferenceAssignable(type, ifFalse.getType()))) {

            throw Error.argumentTypesMustMatch();
        }

        return ConditionalExpression.make(test, ifTrue, ifFalse, type);
    }

    public static ConditionalExpression ifThen(
        final Expression test,
        final Expression ifTrue) {

        return condition(test, ifTrue, empty(), PrimitiveTypes.Void);
    }

    public static ConditionalExpression ifThenElse(
        final Expression test,
        final Expression ifTrue,
        final Expression ifFalse) {

        return condition(test, ifTrue, ifFalse, PrimitiveTypes.Void);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MEMBER ACCESS EXPRESSIONS                                                                                          //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static MemberExpression makeMemberAccess(final Expression target, final MemberInfo member) {
        VerifyArgument.notNull(member, "member");

        if (member instanceof FieldInfo) {
            return Expression.field(target, (FieldInfo)member);
        }

        throw Error.memberNotField(member);
    }

    public static MemberExpression field(final Expression target, final FieldInfo field) {
        return new FieldExpression(target, field);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // CONSTANT EXPRESSIONS                                                                                              //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ConstantExpression constant(final Object value) {
        return ConstantExpression.make(value, value == null ? Types.Object : Type.of(value.getClass()));
    }

    public static ConstantExpression constant(final Object value, final Type type) {
        VerifyArgument.notNull(type, "type");

        if (value == null && type.isPrimitive()) {
            throw Error.argumentTypesMustMatch();
        }

        if (value != null && !type.isInstance(value)) {
            throw Error.argumentTypesMustMatch();
        }

        return ConstantExpression.make(value, type);
    }

    //
    // PARAMETERS AND VARIABLES
    //

    public static ParameterExpressionList parameters(final ParameterExpression... parameters) {
        return new ParameterExpressionList(parameters);
    }

    public static ParameterExpression parameter(final Type type) {
        return parameter(type, null);
    }

    public static ParameterExpression variable(final Type type) {
        return variable(type, null);
    }

    public static ParameterExpression parameter(final Type type, final String name) {
        VerifyArgument.notNull(type, "type");

        if (type == PrimitiveTypes.Void) {
            throw Error.argumentCannotBeOfTypeVoid();
        }

        return ParameterExpression.make(type, name);
    }

    public static ParameterExpression variable(final Type type, final String name) {
        VerifyArgument.notNull(type, "type");

        if (type == PrimitiveTypes.Void) {
            throw Error.argumentCannotBeOfTypeVoid();
        }

        return ParameterExpression.make(type, name);
    }

    //
    // UNARY EXPRESSIONS
    //

    public static UnaryExpression makeUnary(
        final ExpressionType unaryType,
        final Expression operand,
        final Type type) {

        return makeUnary(unaryType, operand, type, null);
    }

    public static UnaryExpression makeUnary(
        final ExpressionType unaryType,
        final Expression operand,
        final Type type,
        final MethodInfo method) {

        switch (unaryType) {
            case Negate:
                return negate(operand, method);
            case Not:
                return not(operand, method);
            case IsFalse:
                return isFalse(operand, method);
            case IsTrue:
                return isTrue(operand, method);
            case OnesComplement:
                return onesComplement(operand, method);
            case ArrayLength:
                return arrayLength(operand);
            case Convert:
                return convert(operand, type, method);
            case Throw:
                return makeThrow(operand, type);
            case UnaryPlus:
                return unaryPlus(operand, method);
            case Unbox:
                return unbox(operand, type);
            case Increment:
                return increment(operand, method);
            case Decrement:
                return decrement(operand, method);
            case PreIncrementAssign:
                return preIncrementAssign(operand, method);
            case PostIncrementAssign:
                return postIncrementAssign(operand, method);
            case PreDecrementAssign:
                return preDecrementAssign(operand, method);
            case PostDecrementAssign:
                return postDecrementAssign(operand, method);
            default:
                throw Error.unhandledUnary(unaryType);
        }
    }

    public static UnaryExpression negate(final Expression expression) {
        return negate(expression, null);
    }

    public static UnaryExpression negate(final Expression expression, final MethodInfo method) {
        verifyCanRead(expression, "expression");

        if (method == null) {
            if (TypeUtils.isArithmetic(expression.getType())) {
                return new UnaryExpression(ExpressionType.Negate, expression, expression.getType(), null);
            }
            return getMethodBasedUnaryOperatorOrThrow(ExpressionType.Negate, "negate", expression);
        }

        return getMethodBasedUnaryOperator(ExpressionType.Negate, expression, method);
    }

    public static UnaryExpression not(final Expression expression) {
        return not(expression, null);
    }

    public static UnaryExpression not(final Expression expression, final MethodInfo method) {
        verifyCanRead(expression, "expression");

        if (method == null) {
            if (TypeUtils.isIntegralOrBoolean(expression.getType())) {
                return new UnaryExpression(ExpressionType.Not, expression, expression.getType(), null);
            }
            throw Error.unaryOperatorNotDefined(ExpressionType.Not, expression.getType());
        }

        return getMethodBasedUnaryOperator(ExpressionType.Not, expression, method);
    }

    public static UnaryExpression isFalse(final Expression expression) {
        return isFalse(expression, null);
    }

    public static UnaryExpression isFalse(final Expression expression, final MethodInfo method) {
        verifyCanRead(expression, "expression");

        if (method == null) {
            if (TypeUtils.isBoolean(expression.getType())) {
                return new UnaryExpression(ExpressionType.IsFalse, expression, expression.getType(), null);
            }
            throw Error.unaryOperatorNotDefined(ExpressionType.IsFalse, expression.getType());
        }

        return getMethodBasedUnaryOperator(ExpressionType.IsFalse, expression, method);
    }

    public static UnaryExpression isTrue(final Expression expression) {
        return isTrue(expression, null);
    }

    public static UnaryExpression isTrue(final Expression expression, final MethodInfo method) {
        verifyCanRead(expression, "expression");

        if (method == null) {
            if (TypeUtils.isBoolean(expression.getType())) {
                return new UnaryExpression(ExpressionType.IsTrue, expression, expression.getType(), null);
            }
            throw Error.unaryOperatorNotDefined(ExpressionType.IsTrue, expression.getType());
        }

        return getMethodBasedUnaryOperator(ExpressionType.IsTrue, expression, method);
    }

    public static UnaryExpression onesComplement(final Expression expression) {
        return not(expression, null);
    }

    public static UnaryExpression onesComplement(final Expression expression, final MethodInfo method) {
        verifyCanRead(expression, "expression");

        if (method == null) {
            if (TypeUtils.isIntegral(expression.getType())) {
                return new UnaryExpression(ExpressionType.OnesComplement, expression, expression.getType(), null);
            }
            return getMethodBasedUnaryOperatorOrThrow(ExpressionType.OnesComplement, "not", expression);
        }

        return getMethodBasedUnaryOperator(ExpressionType.OnesComplement, expression, method);
    }

    public static UnaryExpression arrayLength(final Expression array) {
        VerifyArgument.notNull(array, "array");

        if (!array.getType().isArray()) {
            throw Error.argumentMustBeArray();
        }

        return new UnaryExpression(ExpressionType.ArrayLength, array, PrimitiveTypes.Integer, null);
    }

    public static UnaryExpression convert(final Expression expression, final Type type) {
        return convert(expression, type, null);
    }

    public static UnaryExpression convert(final Expression expression, final Type type, final MethodInfo method) {
        verifyCanRead(expression, "expression");

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(expression.getType(), type) ||
                TypeUtils.hasReferenceConversion(expression.getType(), type)) {

                return new UnaryExpression(ExpressionType.Convert, expression, type, null);
            }

            return getMethodBasedCoercionOrThrow(ExpressionType.Convert, expression, type);
        }

        return getMethodBasedCoercionOperator(ExpressionType.Convert, expression, type, method);
    }

    public static UnaryExpression makeThrow(final Expression expression) {
        return makeThrow(expression, PrimitiveTypes.Void);
    }

    public static UnaryExpression makeThrow(final Expression value, final Type type) {
        VerifyArgument.notNull(type, "type");

        if (value != null) {
            verifyCanRead(value, "value");

            if (value.getType().isPrimitive()) {
                throw Error.argumentMustNotHaveValueType();
            }
        }

        return new UnaryExpression(ExpressionType.Throw, value, type, null);
    }

    public static UnaryExpression unaryPlus(final Expression expression) {
        return negate(expression, null);
    }

    public static UnaryExpression unaryPlus(final Expression expression, final MethodInfo method) {
        verifyCanRead(expression, "expression");

        if (method == null) {
            if (TypeUtils.isArithmetic(expression.getType())) {
                return new UnaryExpression(ExpressionType.UnaryPlus, expression, expression.getType(), null);
            }
            return getMethodBasedUnaryOperatorOrThrow(ExpressionType.UnaryPlus, "abs", expression);
        }

        return getMethodBasedUnaryOperator(ExpressionType.UnaryPlus, expression, method);
    }

    public static UnaryExpression unbox(final Expression expression, final Type type) {
        verifyCanRead(expression, "expression");
        VerifyArgument.notNull(type, "type");
        if (!TypeUtils.isAutoUnboxed(type) && type != Types.Object) {
            throw Error.invalidUnboxType();
        }
        return new UnaryExpression(ExpressionType.Unbox, expression, type, null);
    }

    public static UnaryExpression increment(final Expression expression) {
        return negate(expression, null);
    }

    public static UnaryExpression increment(final Expression expression, final MethodInfo method) {
        verifyCanRead(expression, "expression");

        if (method == null) {
            if (TypeUtils.isArithmetic(expression.getType())) {
                return new UnaryExpression(ExpressionType.Increment, expression, expression.getType(), null);
            }
            return getMethodBasedUnaryOperatorOrThrow(ExpressionType.Increment, "increment", expression);
        }

        return getMethodBasedUnaryOperator(ExpressionType.Increment, expression, method);
    }

    public static UnaryExpression decrement(final Expression expression) {
        return negate(expression, null);
    }

    public static UnaryExpression decrement(final Expression expression, final MethodInfo method) {
        verifyCanRead(expression, "expression");

        if (method == null) {
            if (TypeUtils.isArithmetic(expression.getType())) {
                return new UnaryExpression(ExpressionType.Decrement, expression, expression.getType(), null);
            }
            return getMethodBasedUnaryOperatorOrThrow(ExpressionType.Decrement, "decrement", expression);
        }

        return getMethodBasedUnaryOperator(ExpressionType.Decrement, expression, method);
    }

    public static UnaryExpression preIncrementAssign(final Expression expression) {
        return makeOpAssignUnary(ExpressionType.PreIncrementAssign, expression, null);
    }

    public static UnaryExpression preIncrementAssign(final Expression expression, final MethodInfo method) {
        return makeOpAssignUnary(ExpressionType.PreIncrementAssign, expression, method);
    }

    public static UnaryExpression postIncrementAssign(final Expression expression) {
        return makeOpAssignUnary(ExpressionType.PostIncrementAssign, expression, null);
    }

    public static UnaryExpression postIncrementAssign(final Expression expression, final MethodInfo method) {
        return makeOpAssignUnary(ExpressionType.PostIncrementAssign, expression, method);
    }

    public static UnaryExpression preDecrementAssign(final Expression expression) {
        return makeOpAssignUnary(ExpressionType.PreDecrementAssign, expression, null);
    }

    public static UnaryExpression preDecrementAssign(final Expression expression, final MethodInfo method) {
        return makeOpAssignUnary(ExpressionType.PreDecrementAssign, expression, method);
    }

    public static UnaryExpression postDecrementAssign(final Expression expression) {
        return makeOpAssignUnary(ExpressionType.PostDecrementAssign, expression, null);
    }

    public static UnaryExpression postDecrementAssign(final Expression expression, final MethodInfo method) {
        return makeOpAssignUnary(ExpressionType.PostDecrementAssign, expression, method);
    }

    //
    // BLOCK EXPRESSIONS
    //

    public static BlockExpression block(final Expression arg0, final Expression arg1) {
        verifyCanRead(arg0, "arg0");
        verifyCanRead(arg1, "arg1");

        return new Block2(arg0, arg1);
    }

    public static BlockExpression block(final Expression arg0, final Expression arg1, final Expression arg2) {
        verifyCanRead(arg0, "arg0");
        verifyCanRead(arg1, "arg1");
        verifyCanRead(arg2, "arg2");

        return new Block3(arg0, arg1, arg2);
    }

    public static BlockExpression block(
        final Expression arg0,
        final Expression arg1,
        final Expression arg2,
        final Expression arg3) {

        verifyCanRead(arg0, "arg0");
        verifyCanRead(arg1, "arg1");
        verifyCanRead(arg2, "arg2");
        verifyCanRead(arg3, "arg3");

        return new Block4(arg0, arg1, arg2, arg3);
    }

    public static BlockExpression block(
        final Expression arg0,
        final Expression arg1,
        final Expression arg2,
        final Expression arg3,
        final Expression arg4) {

        verifyCanRead(arg0, "arg0");
        verifyCanRead(arg1, "arg1");
        verifyCanRead(arg2, "arg2");
        verifyCanRead(arg3, "arg3");
        verifyCanRead(arg4, "arg4");

        return new Block5(arg0, arg1, arg2, arg3, arg4);
    }

    public static BlockExpression block(final Expression... expressions) {
        VerifyArgument.notEmpty(expressions, "expressions");
        VerifyArgument.noNullElements(expressions, "expressions");

        switch (expressions.length) {
            case 2:
                return block(expressions[0], expressions[1]);
            case 3:
                return block(expressions[0], expressions[1], expressions[2]);
            case 4:
                return block(expressions[0], expressions[1], expressions[2], expressions[3]);
            case 5:
                return block(expressions[0], expressions[1], expressions[2], expressions[3], expressions[4]);
            default:
                VerifyArgument.notEmpty(expressions, "expressions");
                verifyCanRead(expressions, "expressions");
                return new BlockN(arrayToList(expressions));
        }
    }

    public static BlockExpression block(final ExpressionList<? extends Expression> expressions) {
        return block(ParameterExpressionList.empty(), expressions);
    }

    public static BlockExpression block(final ParameterExpression[] variables, final Expression... expressions) {
        VerifyArgument.notEmpty(expressions, "expressions");

        return block(arrayToList(variables), arrayToList(expressions));
    }

    public static BlockExpression block(final ParameterExpressionList variables, final Expression... expressions) {
        VerifyArgument.notEmpty(expressions, "expressions");

        return block(variables, arrayToList(expressions));
    }

    public static BlockExpression block(final ParameterExpressionList variables, final ExpressionList<? extends Expression> expressions) {
        VerifyArgument.notEmpty(expressions, "expressions");
        VerifyArgument.noNullElements(expressions, "expressions");

        verifyCanRead(expressions, "expressions");

        return block(
            expressions.get(0).getType(),
            variables,
            expressions
        );
    }

    public static BlockExpression block(final Type type, final Expression... expressions) {
        VerifyArgument.notEmpty(expressions, "expressions");

        return block(type, ParameterExpressionList.empty(), arrayToList(expressions));
    }

    public static BlockExpression block(final Type type, final ExpressionList<? extends Expression> expressions) {
        VerifyArgument.notEmpty(expressions, "expressions");
        VerifyArgument.noNullElements(expressions, "expressions");

        return block(type, ParameterExpressionList.empty(), expressions);
    }

    public static BlockExpression block(final Type type, final ParameterExpression[] variables, final Expression... expressions) {
        VerifyArgument.notEmpty(expressions, "expressions");

        return block(type, arrayToList(variables), arrayToList(expressions));
    }

    public static BlockExpression block(final Type type, final ParameterExpressionList variables, final Expression... expressions) {
        VerifyArgument.notEmpty(expressions, "expressions");

        return block(type, variables, arrayToList(expressions));
    }

    public static BlockExpression block(final Type type, final ParameterExpressionList variables, final ExpressionList<? extends Expression> expressions) {
        VerifyArgument.notNull(type, "type");
        VerifyArgument.notEmpty(expressions, "expressions");
        VerifyArgument.noNullElements(expressions, "expressions");

        verifyCanRead(expressions, "expressions");
        validateVariables(variables, "variables");

        final Expression last = expressions.get(expressions.size() - 1);

        if (type != PrimitiveTypes.Void) {
            if (!type.isAssignableFrom(last.getType())) {
                throw Error.argumentTypesMustMatch();
            }
        }

        if (type != last.getType()) {
            return new ScopeWithType(variables, expressions, type);
        }
        else {
            if (expressions.size() == 1) {
                return new Scope1(variables, expressions.get(0));
            }
            else {
                return new ScopeN(variables, expressions);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // BINARY EXPRESSIONS                                                                                                 //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static BinaryExpression makeBinary(
        final ExpressionType binaryType,
        final Expression left,
        final Expression right) {

        return makeBinary(binaryType, left, right, null, null);
    }

    public static BinaryExpression makeBinary(
        final ExpressionType binaryType,
        final Expression left,
        final Expression right,
        final MethodInfo method) {

        return makeBinary(binaryType, left, right, method, null);
    }

    public static BinaryExpression makeBinary(
        final ExpressionType binaryType,
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        switch (binaryType) {
            case Add:
                return add(left, right, method);
            case Subtract:
                return subtract(left, right, method);
            case Multiply:
                return multiply(left, right, method);
            case Divide:
                return divide(left, right, method);
            case Modulo:
                return modulo(left, right, method);
            case And:
                return and(left, right, method);
            case AndAlso:
                return andAlso(left, right, method);
            case Or:
                return or(left, right, method);
            case OrElse:
                return orElse(left, right, method);
            case LessThan:
                return lessThan(left, right, method);
            case LessThanOrEqual:
                return lessThanOrEqual(left, right, method);
            case GreaterThan:
                return greaterThan(left, right, method);
            case GreaterThanOrEqual:
                return greaterThanOrEqual(left, right, method);
            case Equal:
                return equal(left, right, method);
            case NotEqual:
                return notEqual(left, right, method);
            case ExclusiveOr:
                return exclusiveOr(left, right, method);
            case ArrayIndex:
                return arrayIndex(left, right);
            case RightShift:
                return rightShift(left, right, method);
            case UnsignedRightShift:
                return unsignedRightShift(left, right, method);
            case LeftShift:
                return leftShift(left, right, method);
            case Assign:
                return assign(left, right);
            case AddAssign:
                return addAssign(left, right, method, conversion);
            case AndAssign:
                return andAssign(left, right, method, conversion);
            case DivideAssign:
                return divideAssign(left, right, method, conversion);
            case ExclusiveOrAssign:
                return exclusiveOrAssign(left, right, method, conversion);
            case LeftShiftAssign:
                return leftShiftAssign(left, right, method, conversion);
            case ModuloAssign:
                return moduloAssign(left, right, method, conversion);
            case MultiplyAssign:
                return multiplyAssign(left, right, method, conversion);
            case OrAssign:
                return orAssign(left, right, method, conversion);
            case RightShiftAssign:
                return rightShiftAssign(left, right, method, conversion);
            case UnsignedRightShiftAssign:
                return unsignedRightShiftAssign(left, right, method, conversion);
            case SubtractAssign:
                return subtractAssign(left, right, method, conversion);
            default:
                throw Error.unhandledBinary(binaryType);
        }
    }

    public static BinaryExpression add(final Expression left, final Expression right) {
        return add(left, right, null);
    }

    public static BinaryExpression add(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Type leftType = left.getType();
            final Type rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isArithmetic(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.Add, left, right, leftType);
            }

            return getMethodBasedBinaryOperatorOrThrow(ExpressionType.Add, "add", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.Add, left, right, method);
    }

    public static BinaryExpression subtract(final Expression left, final Expression right) {
        return subtract(left, right, null);
    }

    public static BinaryExpression subtract(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Type leftType = left.getType();
            final Type rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isArithmetic(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.Subtract, left, right, leftType);
            }

            return getMethodBasedBinaryOperatorOrThrow(ExpressionType.Subtract, "subtract", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.Subtract, left, right, method);
    }

    public static BinaryExpression multiply(final Expression left, final Expression right) {
        return multiply(left, right, null);
    }

    public static BinaryExpression multiply(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Type leftType = left.getType();
            final Type rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isArithmetic(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.Multiply, left, right, leftType);
            }

            return getMethodBasedBinaryOperatorOrThrow(ExpressionType.Multiply, "multiply", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.Multiply, left, right, method);
    }

    public static BinaryExpression divide(final Expression left, final Expression right) {
        return divide(left, right, null);
    }

    public static BinaryExpression divide(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Type leftType = left.getType();
            final Type rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isArithmetic(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.Divide, left, right, leftType);
            }

            return getMethodBasedBinaryOperatorOrThrow(ExpressionType.Divide, "divide", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.Divide, left, right, method);
    }

    public static BinaryExpression modulo(final Expression left, final Expression right) {
        return modulo(left, right, null);
    }

    public static BinaryExpression modulo(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Type leftType = left.getType();
            final Type rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isArithmetic(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.Modulo, left, right, leftType);
            }

            return getMethodBasedBinaryOperatorOrThrow(ExpressionType.Modulo, "modulo", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.Modulo, left, right, method);
    }

    public static BinaryExpression leftShift(final Expression left, final Expression right) {
        return leftShift(left, right, null);
    }

    public static BinaryExpression leftShift(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Type leftType = left.getType();
            final Type rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isArithmetic(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.LeftShift, left, right, leftType);
            }

            return getMethodBasedBinaryOperatorOrThrow(ExpressionType.LeftShift, "shiftLeft", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.LeftShift, left, right, method);
    }

    public static BinaryExpression rightShift(final Expression left, final Expression right) {
        return rightShift(left, right, null);
    }

    public static BinaryExpression rightShift(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Type leftType = left.getType();
            final Type rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isArithmetic(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.RightShift, left, right, leftType);
            }

            return getMethodBasedBinaryOperatorOrThrow(ExpressionType.RightShift, "shiftRight", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.RightShift, left, right, method);
    }

    public static BinaryExpression unsignedRightShift(final Expression left, final Expression right) {
        return unsignedRightShift(left, right, null);
    }

    public static BinaryExpression unsignedRightShift(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Type leftType = left.getType();
            final Type rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isArithmetic(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.RightShift, left, right, leftType);
            }

            throw Error.binaryOperatorNotDefined(ExpressionType.UnsignedRightShift, left.getType(), right.getType());
        }

        return getMethodBasedBinaryOperator(ExpressionType.RightShift, left, right, method);
    }

    public static BinaryExpression and(final Expression left, final Expression right) {
        return and(left, right, null);
    }

    public static BinaryExpression and(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Type leftType = left.getType();
            final Type rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isIntegralOrBoolean(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.And, left, right, leftType);
            }

            return getMethodBasedBinaryOperatorOrThrow(ExpressionType.And, "and", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.And, left, right, method);
    }

    public static BinaryExpression or(final Expression left, final Expression right) {
        return or(left, right, null);
    }

    public static BinaryExpression or(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Type leftType = left.getType();
            final Type rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isIntegralOrBoolean(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.Or, left, right, leftType);
            }

            return getMethodBasedBinaryOperatorOrThrow(ExpressionType.Or, "or", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.Or, left, right, method);
    }

    public static BinaryExpression exclusiveOr(final Expression left, final Expression right) {
        return exclusiveOr(left, right, null);
    }

    public static BinaryExpression exclusiveOr(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Type leftType = left.getType();
            final Type rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isIntegralOrBoolean(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.ExclusiveOr, left, right, leftType);
            }

            return getMethodBasedBinaryOperatorOrThrow(ExpressionType.ExclusiveOr, "xor", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.ExclusiveOr, left, right, method);
    }

    public static BinaryExpression andAlso(final Expression left, final Expression right) {
        return andAlso(left, right, null);
    }

    public static BinaryExpression andAlso(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        Type returnType;

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.hasIdentityPrimitiveOrBoxingConversion(left.getType(), PrimitiveTypes.Boolean)) {

                return new SimpleBinaryExpression(ExpressionType.AndAlso, left, right, leftType);
            }

            throw Error.binaryOperatorNotDefined(ExpressionType.AndAlso, leftType, rightType);
        }

        throw Error.binaryOperatorNotDefined(ExpressionType.AndAlso, leftType, rightType);
    }

    public static BinaryExpression orElse(final Expression left, final Expression right) {
        return orElse(left, right, null);
    }

    public static BinaryExpression orElse(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        Type returnType;

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.hasIdentityPrimitiveOrBoxingConversion(left.getType(), PrimitiveTypes.Boolean)) {

                return new SimpleBinaryExpression(ExpressionType.OrElse, left, right, leftType);
            }

            throw Error.binaryOperatorNotDefined(ExpressionType.OrElse, leftType, rightType);
        }

        throw Error.binaryOperatorNotDefined(ExpressionType.OrElse, leftType, rightType);
    }

    public static BinaryExpression lessThan(final Expression left, final Expression right) {
        return lessThan(left, right, null);
    }

    public static BinaryExpression lessThan(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            return getComparisonOperator(ExpressionType.LessThan, left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.LessThan, left, right, method);
    }

    public static BinaryExpression lessThanOrEqual(final Expression left, final Expression right) {
        return lessThanOrEqual(left, right, null);
    }

    public static BinaryExpression lessThanOrEqual(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            return getComparisonOperator(ExpressionType.LessThanOrEqual, left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.LessThanOrEqual, left, right, method);
    }

    public static BinaryExpression greaterThan(final Expression left, final Expression right) {
        return greaterThan(left, right, null);
    }

    public static BinaryExpression greaterThan(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            return getComparisonOperator(ExpressionType.GreaterThan, left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.GreaterThan, left, right, method);
    }

    public static BinaryExpression greaterThanOrEqual(final Expression left, final Expression right) {
        return greaterThanOrEqual(left, right, null);
    }

    public static BinaryExpression greaterThanOrEqual(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            return getComparisonOperator(ExpressionType.GreaterThanOrEqual, left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.GreaterThanOrEqual, left, right, method);
    }

    public static BinaryExpression equal(final Expression left, final Expression right) {
        return equal(left, right, null);
    }

    public static BinaryExpression equal(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            return getEqualityComparisonOperator(ExpressionType.Equal, "equals", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.Equal, left, right, method);
    }

    public static BinaryExpression notEqual(final Expression left, final Expression right) {
        return equal(left, right, null);
    }

    public static BinaryExpression notEqual(final Expression left, final Expression right, final MethodInfo method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            return getEqualityComparisonOperator(ExpressionType.NotEqual, null, left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.NotEqual, left, right, method);
    }

    public static BinaryExpression referenceEqual(final Expression left, final Expression right) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (TypeUtils.hasReferenceEquality(left.getType(), right.getType())) {
            return new LogicalBinaryExpression(ExpressionType.Equal, left, right);
        }

        throw Error.referenceEqualityNotDefined(left.getType(), right.getType());
    }

    public static BinaryExpression referenceNotEqual(final Expression left, final Expression right) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (TypeUtils.hasReferenceEquality(left.getType(), right.getType())) {
            return new LogicalBinaryExpression(ExpressionType.NotEqual, left, right);
        }

        throw Error.referenceEqualityNotDefined(left.getType(), right.getType());
    }

    public static BinaryExpression arrayIndex(final Expression array, final Expression index) {
        verifyCanRead(array, "array");
        verifyCanRead(index, "index");

        if (!TypeUtils.hasIdentityPrimitiveOrBoxingConversion(index.getType(), PrimitiveTypes.Integer)) {
            throw Error.argumentMustBeArrayIndexType();
        }

        final Type arrayType = array.getType();

        if (!arrayType.isArray()) {
            throw Error.argumentMustBeArray();
        }

        return new SimpleBinaryExpression(ExpressionType.ArrayIndex, array, index, arrayType.getElementType());
    }

    public static BinaryExpression assign(final Expression left, final Expression right) {
        verifyCanWrite(left, "left");
        verifyCanRead(right, "right");

        if (!TypeUtils.areReferenceAssignable(left.getType(), right.getType())) {
            throw Error.expressionTypeDoesNotMatchAssignment(right.getType(), left.getType());
        }

        return new AssignBinaryExpression(left, right);
    }

    public static BinaryExpression addAssign(final Expression left, final Expression right) {
        return addAssign(left, right, null, null);
    }

    public static BinaryExpression addAssign(final Expression left, final Expression right, final MethodInfo method) {
        return addAssign(left, right, method, null);
    }

    public static BinaryExpression addAssign(
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        verifyCanRead(left, "left");
        verifyCanWrite(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.isArithmetic(leftType)) {
                // conversion is not supported for binary ops on arithmetic types without operator overloading
                if (conversion != null) {
                    throw Error.conversionIsNotSupportedForArithmeticTypes();
                }
                return new SimpleBinaryExpression(ExpressionType.AddAssign, left, right, leftType);
            }
            return getMethodBasedAssignOperatorOrThrow(ExpressionType.AddAssign, "add", left, right, conversion);
        }

        return getMethodBasedAssignOperator(ExpressionType.AddAssign, left, right, method, conversion);
    }

    public static BinaryExpression subtractAssign(final Expression left, final Expression right) {
        return subtractAssign(left, right, null, null);
    }

    public static BinaryExpression subtractAssign(final Expression left, final Expression right, final MethodInfo method) {
        return subtractAssign(left, right, method, null);
    }

    public static BinaryExpression subtractAssign(
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        verifyCanRead(left, "left");
        verifyCanWrite(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.isArithmetic(leftType)) {
                // conversion is not supported for binary ops on arithmetic types without operator overloading
                if (conversion != null) {
                    throw Error.conversionIsNotSupportedForArithmeticTypes();
                }
                return new SimpleBinaryExpression(ExpressionType.SubtractAssign, left, right, leftType);
            }
            return getMethodBasedAssignOperatorOrThrow(ExpressionType.SubtractAssign, "subtract", left, right, conversion);
        }

        return getMethodBasedAssignOperator(ExpressionType.SubtractAssign, left, right, method, conversion);
    }

    public static BinaryExpression multiplyAssign(final Expression left, final Expression right) {
        return multiplyAssign(left, right, null, null);
    }

    public static BinaryExpression multiplyAssign(final Expression left, final Expression right, final MethodInfo method) {
        return multiplyAssign(left, right, method, null);
    }

    public static BinaryExpression multiplyAssign(
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        verifyCanRead(left, "left");
        verifyCanWrite(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.isArithmetic(leftType)) {
                // conversion is not supported for binary ops on arithmetic types without operator overloading
                if (conversion != null) {
                    throw Error.conversionIsNotSupportedForArithmeticTypes();
                }
                return new SimpleBinaryExpression(ExpressionType.MultiplyAssign, left, right, leftType);
            }
            return getMethodBasedAssignOperatorOrThrow(ExpressionType.MultiplyAssign, "multiply", left, right, conversion);
        }

        return getMethodBasedAssignOperator(ExpressionType.MultiplyAssign, left, right, method, conversion);
    }

    public static BinaryExpression divideAssign(final Expression left, final Expression right) {
        return divideAssign(left, right, null, null);
    }

    public static BinaryExpression divideAssign(final Expression left, final Expression right, final MethodInfo method) {
        return divideAssign(left, right, method, null);
    }

    public static BinaryExpression divideAssign(
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        verifyCanRead(left, "left");
        verifyCanWrite(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.isArithmetic(leftType)) {
                // conversion is not supported for binary ops on arithmetic types without operator overloading
                if (conversion != null) {
                    throw Error.conversionIsNotSupportedForArithmeticTypes();
                }
                return new SimpleBinaryExpression(ExpressionType.DivideAssign, left, right, leftType);
            }
            return getMethodBasedAssignOperatorOrThrow(ExpressionType.DivideAssign, "divide", left, right, conversion);
        }

        return getMethodBasedAssignOperator(ExpressionType.DivideAssign, left, right, method, conversion);
    }

    public static BinaryExpression moduloAssign(final Expression left, final Expression right) {
        return moduloAssign(left, right, null, null);
    }

    public static BinaryExpression moduloAssign(final Expression left, final Expression right, final MethodInfo method) {
        return moduloAssign(left, right, method, null);
    }

    public static BinaryExpression moduloAssign(
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        verifyCanRead(left, "left");
        verifyCanWrite(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.isArithmetic(leftType)) {
                // conversion is not supported for binary ops on arithmetic types without operator overloading
                if (conversion != null) {
                    throw Error.conversionIsNotSupportedForArithmeticTypes();
                }
                return new SimpleBinaryExpression(ExpressionType.ModuloAssign, left, right, leftType);
            }
            return getMethodBasedAssignOperatorOrThrow(ExpressionType.ModuloAssign, "modulo", left, right, conversion);
        }

        return getMethodBasedAssignOperator(ExpressionType.ModuloAssign, left, right, method, conversion);
    }

    public static BinaryExpression leftShiftAssign(final Expression left, final Expression right) {
        return leftShiftAssign(left, right, null, null);
    }

    public static BinaryExpression leftShiftAssign(final Expression left, final Expression right, final MethodInfo method) {
        return leftShiftAssign(left, right, method, null);
    }

    public static BinaryExpression leftShiftAssign(
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        verifyCanRead(left, "left");
        verifyCanWrite(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.isArithmetic(leftType)) {
                // conversion is not supported for binary ops on arithmetic types without operator overloading
                if (conversion != null) {
                    throw Error.conversionIsNotSupportedForArithmeticTypes();
                }
                return new SimpleBinaryExpression(ExpressionType.LeftShiftAssign, left, right, leftType);
            }
            return getMethodBasedAssignOperatorOrThrow(ExpressionType.LeftShiftAssign, "shiftLeft", left, right, conversion);
        }

        return getMethodBasedAssignOperator(ExpressionType.LeftShiftAssign, left, right, method, conversion);
    }

    public static BinaryExpression rightShiftAssign(final Expression left, final Expression right) {
        return rightShiftAssign(left, right, null, null);
    }

    public static BinaryExpression rightShiftAssign(final Expression left, final Expression right, final MethodInfo method) {
        return rightShiftAssign(left, right, method, null);
    }

    public static BinaryExpression rightShiftAssign(
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        verifyCanRead(left, "left");
        verifyCanWrite(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.isArithmetic(leftType)) {
                // conversion is not supported for binary ops on arithmetic types without operator overloading
                if (conversion != null) {
                    throw Error.conversionIsNotSupportedForArithmeticTypes();
                }
                return new SimpleBinaryExpression(ExpressionType.RightShiftAssign, left, right, leftType);
            }
            return getMethodBasedAssignOperatorOrThrow(ExpressionType.RightShiftAssign, "rightShift", left, right, conversion);
        }

        return getMethodBasedAssignOperator(ExpressionType.RightShiftAssign, left, right, method, conversion);
    }

    public static BinaryExpression unsignedRightShiftAssign(final Expression left, final Expression right) {
        return unsignedRightShiftAssign(left, right, null, null);
    }

    public static BinaryExpression unsignedRightShiftAssign(final Expression left, final Expression right, final MethodInfo method) {
        return unsignedRightShiftAssign(left, right, method, null);
    }

    public static BinaryExpression unsignedRightShiftAssign(
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        verifyCanRead(left, "left");
        verifyCanWrite(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.isArithmetic(leftType)) {
                // conversion is not supported for binary ops on arithmetic types without operator overloading
                if (conversion != null) {
                    throw Error.conversionIsNotSupportedForArithmeticTypes();
                }
                return new SimpleBinaryExpression(ExpressionType.UnsignedRightShiftAssign, left, right, leftType);
            }

            throw Error.binaryOperatorNotDefined(ExpressionType.UnsignedRightShiftAssign, left.getType(), right.getType());
        }

        return getMethodBasedAssignOperator(ExpressionType.UnsignedRightShiftAssign, left, right, method, conversion);
    }

    public static BinaryExpression orAssign(final Expression left, final Expression right) {
        return orAssign(left, right, null, null);
    }

    public static BinaryExpression orAssign(final Expression left, final Expression right, final MethodInfo method) {
        return orAssign(left, right, method, null);
    }

    public static BinaryExpression orAssign(
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        verifyCanRead(left, "left");
        verifyCanWrite(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.isIntegralOrBoolean(leftType)) {
                // conversion is not supported for binary ops on arithmetic types without operator overloading
                if (conversion != null) {
                    throw Error.conversionIsNotSupportedForArithmeticTypes();
                }
                return new SimpleBinaryExpression(ExpressionType.OrAssign, left, right, leftType);
            }

            return getMethodBasedAssignOperatorOrThrow(ExpressionType.OrAssign, "or", left, right, conversion);
        }

        return getMethodBasedAssignOperator(ExpressionType.OrAssign, left, right, method, conversion);
    }

    public static BinaryExpression andAssign(final Expression left, final Expression right) {
        return andAssign(left, right, null, null);
    }

    public static BinaryExpression andAssign(final Expression left, final Expression right, final MethodInfo method) {
        return andAssign(left, right, method, null);
    }

    public static BinaryExpression andAssign(
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        verifyCanRead(left, "left");
        verifyCanWrite(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.isIntegralOrBoolean(leftType)) {
                // conversion is not supported for binary ops on arithmetic types without operator overloading
                if (conversion != null) {
                    throw Error.conversionIsNotSupportedForArithmeticTypes();
                }
                return new SimpleBinaryExpression(ExpressionType.AndAssign, left, right, leftType);
            }

            return getMethodBasedAssignOperatorOrThrow(ExpressionType.AndAssign, "and", left, right, conversion);
        }

        return getMethodBasedAssignOperator(ExpressionType.AndAssign, left, right, method, conversion);
    }

    public static BinaryExpression exclusiveOrAssign(final Expression left, final Expression right) {
        return exclusiveOrAssign(left, right, null, null);
    }

    public static BinaryExpression exclusiveOrAssign(final Expression left, final Expression right, final MethodInfo method) {
        return exclusiveOrAssign(left, right, method, null);
    }

    public static BinaryExpression exclusiveOrAssign(
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        verifyCanRead(left, "left");
        verifyCanWrite(left, "left");
        verifyCanRead(right, "right");

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.isIntegralOrBoolean(leftType)) {
                // conversion is not supported for binary ops on arithmetic types without operator overloading
                if (conversion != null) {
                    throw Error.conversionIsNotSupportedForArithmeticTypes();
                }
                return new SimpleBinaryExpression(ExpressionType.ExclusiveOrAssign, left, right, leftType);
            }

            return getMethodBasedAssignOperatorOrThrow(ExpressionType.ExclusiveOrAssign, "xor", left, right, conversion);
        }

        return getMethodBasedAssignOperator(ExpressionType.ExclusiveOrAssign, left, right, method, conversion);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // TYPE BINARY EXPRESSIONS                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static TypeBinaryExpression instanceOf(final Expression expression, final Type type) {
        verifyCanRead(expression, "expression");
        VerifyArgument.notNull(type, "type");

        verifyTypeBinaryExpressionOperand(expression,  type);

        return new TypeBinaryExpression(expression, type, ExpressionType.InstanceOf);
    }

    public static TypeBinaryExpression typeEqual(final Expression expression, final Type type) {
        verifyCanRead(expression, "expression");
        VerifyArgument.notNull(type, "type");

        verifyTypeBinaryExpressionOperand(expression,  type);

        return new TypeBinaryExpression(expression, type, ExpressionType.TypeEqual);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LAMBDA EXPRESSIONS                                                                                                 //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static <T> LambdaExpression<T> lambda(
        final Type<T> interfaceType,
        final Expression body,
        final ParameterExpression... parameters) {

        return lambda(interfaceType, null, body, false, arrayToList(parameters));
    }

    public static <T> LambdaExpression<T> lambda(
        final Type<T> interfaceType,
        final Expression body,
        final boolean tailCall,
        final ParameterExpression... parameters) {

        return lambda(interfaceType, null, body, tailCall, arrayToList(parameters));
    }

    public static <T> LambdaExpression<T> lambda(
        final Type<T> interfaceType,
        final String name,
        final Expression body,
        final boolean tailCall,
        final ParameterExpression... parameters) {

        return lambda(interfaceType, name, body, false, arrayToList(parameters));
    }

    public static <T> LambdaExpression<T> lambda(
        final Type<T> interfaceType,
        final Expression body,
        final ParameterExpressionList parameters) {

        return lambda(interfaceType, null, body, false, parameters);
    }

    public static <T> LambdaExpression<T> lambda(
        final Type<T> interfaceType,
        final Expression body,
        final boolean tailCall,
        final ParameterExpressionList parameters) {

        return lambda(interfaceType, null, body, tailCall, parameters);
    }

    public static <T> LambdaExpression<T> lambda(
        final Type<T> interfaceType,
        final String name,
        final Expression body,
        final boolean tailCall,
        final ParameterExpressionList parameters) {

        VerifyArgument.notNull(interfaceType, "interfaceType");
        VerifyArgument.notNull(body, "body");
        VerifyArgument.noNullElements(parameters, "parameters");

        validateLambdaArgs(interfaceType, body, parameters);

        return new LambdaExpression<>(interfaceType, name, body, tailCall, parameters);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // INVOKE EXPRESSIONS                                                                                                 //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static InvocationExpression invoke(final LambdaExpression expression, final Expression... arguments) {
        VerifyArgument.noNullElements(arguments, "arguments");
        return invoke(expression, new ExpressionList<>(arguments));
    }

    public static InvocationExpression invoke(final LambdaExpression expression, final ExpressionList<? extends Expression> arguments) {
        verifyCanRead(expression, "expression");

        final MethodInfo method = getInvokeMethod(expression);

        return new InvocationExpression(expression, arguments, method.getReturnType());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // METHOD CALL EXPRESSIONS                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static MethodCallExpression call(final MethodInfo method, final Expression... arguments) {
        return call(null, method, arrayToList(arguments));
    }

    public static MethodCallExpression call(final MethodInfo method, final ExpressionList<? extends Expression> arguments) {
        return call(null, method, arguments);
    }

    public static MethodCallExpression call(final Expression target, final MethodInfo method, final Expression... arguments) {
        return call(target, method, arrayToList(arguments));
    }

    public static MethodCallExpression call(
        final Expression target,
        final MethodInfo method,
        final ExpressionList<? extends Expression> arguments) {

        VerifyArgument.notNull(method, "method");

        validateStaticOrInstanceMethod(target, method);

        final ExpressionList<?> argumentList = validateArgumentTypes(method, ExpressionType.Call, arguments);

        if (target == null) {
            return new MethodCallExpressionN(method, argumentList);
        }

        return new InstanceMethodCallExpressionN(method, target, arguments);
    }

    public static MethodCallExpression call(
        final Expression target,
        final String methodName,
        final TypeList typeArguments,
        final Expression... arguments) {

        return call(target, methodName, typeArguments, arrayToList(arguments));
    }

    public static MethodCallExpression call(
        final Expression target,
        final String methodName,
        final TypeList typeArguments,
        final ExpressionList<? extends Expression> arguments) {

        VerifyArgument.notNull(target, "target");
        VerifyArgument.notNull(methodName, "methodName");

        final MethodInfo resolvedMethod = findMethod(
            target.getType(),
            methodName,
            typeArguments,
            arguments,
            InstanceMethodBindingFlags
        );

        return call(
            target,
            resolvedMethod,
            arguments
        );
    }

    public static MethodCallExpression call(
        final Type declaringType,
        final String methodName,
        final TypeList typeArguments,
        final Expression... arguments) {

        return call(declaringType, methodName, typeArguments, arrayToList(arguments));
    }

    public static MethodCallExpression call(
        final Type declaringType,
        final String methodName,
        final TypeList typeArguments,
        final ExpressionList<? extends Expression> arguments) {

        VerifyArgument.notNull(declaringType, "declaringType");
        VerifyArgument.notNull(methodName, "methodName");

        final MethodInfo resolvedMethod = findMethod(
            declaringType,
            methodName,
            typeArguments,
            arguments,
            StaticMethodBindingFlags
        );

        return call(
            resolvedMethod,
            arguments
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DEFAULT VALUE EXPRESSIONS                                                                                          //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static DefaultValueExpression defaultValue(final Type type) {
        VerifyArgument.notNull(type, "type");
        return new DefaultValueExpression(type);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // SWITCH EXPRESSIONS                                                                                                 //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static SwitchCase switchCase(final Expression body, final Expression... testValues) {
        return switchCase(body, arrayToList(testValues));
    }

    public static SwitchCase switchCase(
        final Expression body,
        final ExpressionList<? extends Expression> testValues) {

        verifyCanRead(body, "body");
        verifyCanRead(testValues, "testValues");

        VerifyArgument.notEmpty(testValues, "testValues");

        return new SwitchCase(body, testValues);
    }

    public static SwitchExpression makeSwitch(final Expression switchValue, final SwitchCase... cases) {
        return makeSwitch(switchValue, null, null, arrayToReadOnlyList(cases));
    }

    public static SwitchExpression makeSwitch(
        final Expression switchValue,
        final Expression defaultBody,
        final SwitchCase... cases) {

        return makeSwitch(switchValue, defaultBody, null, arrayToReadOnlyList(cases));
    }

    public static SwitchExpression makeSwitch(
        final Expression switchValue,
        final Expression defaultBody,
        final MethodInfo comparison,
        final SwitchCase... cases) {

        return makeSwitch(switchValue, defaultBody, comparison, arrayToReadOnlyList(cases));
    }

    public static SwitchExpression makeSwitch(
        final Type type,
        final Expression switchValue,
        final Expression defaultBody,
        final MethodInfo comparison,
        final SwitchCase... cases) {

        return makeSwitch(type, switchValue, defaultBody, comparison, arrayToReadOnlyList(cases));
    }

    public static SwitchExpression makeSwitch(
        final Expression switchValue,
        final Expression defaultBody,
        final MethodInfo comparison,
        final ReadOnlyList<SwitchCase> cases) {

        return makeSwitch(null, switchValue, defaultBody, comparison, cases);
    }

    public static SwitchExpression makeSwitch(
        final Type type,
        final Expression switchValue,
        final Expression defaultBody,
        final MethodInfo comparison,
        final ReadOnlyList<SwitchCase> cases) {

        verifyCanRead(switchValue, "switchValue");

        if (switchValue.getType() == PrimitiveTypes.Void) {
            throw Error.argumentCannotBeOfTypeVoid();
        }

        VerifyArgument.notEmpty(cases, "cases");
        VerifyArgument.noNullElements(cases, "cases");

        final boolean customType = type != null;
        final Type resultType = type != null ? type : cases.get(0).getBody().getType();
        final MethodInfo actualComparison;

        if (comparison != null) {
            final ParameterList parameters = comparison.getParameters();

            if (parameters.size() != 2) {
                throw Error.incorrectNumberOfMethodCallArguments(comparison);
            }

            // Validate that the switch value's type matches the comparison method's
            // left hand side parameter type.
            final ParameterInfo leftArg = parameters.get(0);
            final ParameterInfo rightArg = parameters.get(1);

            for (int i = 0, n = cases.size(); i < n; i++) {
                final SwitchCase c = cases.get(i);

                validateSwitchCaseType(c.getBody(), customType, resultType, "cases");

                final ExpressionList<? extends Expression> testValues = c.getTestValues();

                for (int j = 0, m = testValues.size(); j < m; j++) {
                    // When a comparison method is provided, test values can have different type but have to
                    // be reference assignable to the right hand side parameter of the method.
                    final Type rightOperandType = testValues.get(j).getType();

                    if (!parameterIsAssignable(rightArg.getParameterType(), rightOperandType)) {
                        throw Error.testValueTypeDoesNotMatchComparisonMethodParameter(
                            rightOperandType,
                            rightArg.getParameterType()
                        );
                    }
                }
            }

            actualComparison = comparison;
        }
        else {
            // When comparison method is not present, all the test values must have
            // the same type. Use the first test value's type as the baseline.
            final Expression firstTestValue = cases.get(0).getTestValues().get(0);
            for (int i = 0, n = cases.size(); i < n; i++) {
                final SwitchCase c = cases.get(i);

                validateSwitchCaseType(c.getBody(), customType, resultType, "cases");

                final ExpressionList<? extends Expression> testValues = c.getTestValues();

                // When no comparison method is provided, require all test values to have the same type.
                for (int j = 0, m = testValues.size(); j < m; j++) {
                    if (!TypeUtils.areReferenceAssignable(firstTestValue.getType(), testValues.get(j).getType())) {
                        throw Error.allTestValuesMustHaveTheSameType();
                    }
                }
            }

            // Now we need to validate that switchValue.Type and testValueType make sense in an
            // Equal node. Fortunately, Equal throws a reasonable error, so just call it.
            final BinaryExpression equal = equal(switchValue, firstTestValue, comparison);

            // Get the comparison function from equals node.
            actualComparison = equal.getMethod();
        }

        if (defaultBody == null) {
            if (resultType != PrimitiveTypes.Void) {
                throw Error.defaultBodyMustBeSupplied();
            }
        }
        else {
            validateSwitchCaseType(defaultBody, customType, resultType, "defaultBody");
        }

        // if we have a non-boolean user-defined equals, we don't want it.
        if (comparison != null &&
            TypeUtils.hasIdentityPrimitiveOrBoxingConversion(comparison.getReturnType(), PrimitiveTypes.Boolean)) {

            throw Error.equalityMustReturnBoolean(comparison);
        }

        return new SwitchExpression(resultType, switchValue, defaultBody, actualComparison, cases);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // HELPER METHODS                                                                                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Class UNMODIFIABLE_LIST_CLASS;

    static {
        Class unmodifiableListClass = null;

        final Class<?>[] declaredClasses = Collections.class.getDeclaredClasses();

        for (final Class clazz : declaredClasses) {
            if (clazz.getName().equals("UnmodifiableCollection")) {
                unmodifiableListClass = clazz;
                break;
            }
        }

        UNMODIFIABLE_LIST_CLASS = unmodifiableListClass;
    }

    static <T extends Expression> ExpressionList<T> arrayToList(final T[] expressions) {
        if (expressions == null || expressions.length == 0) {
            return ExpressionList.empty();
        }

        VerifyArgument.noNullElements(expressions, "expressions");

        return new ExpressionList<>(expressions);
    }

    static <T> ReadOnlyList<T> arrayToReadOnlyList(final T[] items) {
        if (items == null || items.length == 0) {
            return ReadOnlyList.emptyList();
        }

        VerifyArgument.noNullElements(items, "items");

        return new ReadOnlyList<>(items);
    }

    static ParameterExpressionList arrayToList(final ParameterExpression[] parameters) {
        if (parameters == null || parameters.length == 0) {
            return ParameterExpressionList.empty();
        }

        VerifyArgument.noNullElements(parameters, "parameters");

        return new ParameterExpressionList(parameters);
    }

    private static void verifyCanRead(final Expression expression, final String parameterName) {
        VerifyArgument.notNull(expression, parameterName);

        // All expression types are currently readable.
    }

    private static void verifyCanRead(final Iterable<? extends Expression> items, final String parameterName) {
        if (items == null) {
            return;
        }

        if (items instanceof List) {
            final List<? extends Expression> list = (List<? extends Expression>)items;
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, count = list.size(); i < count; i++) {
                verifyCanRead(list.get(i), parameterName);
            }
        }

        for (final Expression item : items) {
            verifyCanRead(item, parameterName);
        }
    }

    private static void verifyCanRead(final Expression[] items, final String parameterName) {
        if (items == null) {
            return;
        }

        for (final Expression item : items) {
            verifyCanRead(item, parameterName);
        }
    }

    private static void verifyCanWrite(final Expression expression, final String parameterName) {
        VerifyArgument.notNull(expression, "expression");

        boolean canWrite = false;

        switch (expression.getNodeType()) {

            case MemberAccess:
                final MemberExpression memberExpression = (MemberExpression)expression;
                if (memberExpression.getMember() instanceof FieldInfo) {
                    final FieldInfo field = (FieldInfo)memberExpression.getMember();
                    canWrite = !field.isEnumConstant() && !field.isFinal();
                }
                break;

            case Parameter:
                canWrite = true;
                break;
        }

        if (!canWrite) {
            throw Error.expressionMustBeWriteable(parameterName);
        }
    }

    private static void verifyCanWrite(final Iterable<? extends Expression> items, final String parameterName) {
        if (items == null) {
            return;
        }

        if (items instanceof List) {
            final List<? extends Expression> list = (List<? extends Expression>)items;
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, count = list.size(); i < count; i++) {
                verifyCanWrite(list.get(i), parameterName);
            }
        }

        for (final Expression item : items) {
            verifyCanWrite(item, parameterName);
        }
    }

    static void validateVariables(final ParameterExpressionList varList, final String collectionName) {
        if (varList.isEmpty()) {
            return;
        }

        final int count = varList.size();
        final HashSet<ParameterExpression> set = new HashSet<>(count);

        for (int i = 0; i < count; i++) {
            final ParameterExpression v = varList.get(i);
            if (v == null) {
                throw new IllegalArgumentException(format("%s[%s] is null.", collectionName, i));
            }
            if (set.contains(v)) {
                throw Error.duplicateVariable(v);
            }
            set.add(v);
        }
    }

    private static UnaryExpression getMethodBasedUnaryOperator(
        final ExpressionType unaryType,
        final Expression operand,
        final MethodInfo method) {

        validateOperator(method);

        final ParameterList parameters = method.getParameters();

        if (parameters.size() != 0) {
            throw Error.incorrectNumberOfMethodCallArguments(method);
        }

        final Type returnType = method.getReturnType();

        if (TypeUtils.areReferenceAssignable(operand.getType(), returnType)) {
            return new UnaryExpression(unaryType, operand, returnType, method);
        }

        if (TypeUtils.isAutoUnboxed(operand.getType()) &&
            TypeUtils.areReferenceAssignable(TypeUtils.getUnderlyingPrimitive(operand.getType()), returnType)) {

            return new UnaryExpression(
                unaryType,
                operand,
                returnType,
                method
            );
        }

        throw Error.methodBasedOperatorMustHaveValidReturnType(unaryType, method);
    }

    private static UnaryExpression getMethodBasedUnaryOperatorOrThrow(
        final ExpressionType unaryType,
        final String methodName,
        final Expression operand) {

        final UnaryExpression u = getMethodBasedUnaryOperator(unaryType, methodName, operand);

        if (u != null) {
            validateOperator(u.getMethod());
            return u;
        }

        throw Error.unaryOperatorNotDefined(unaryType, operand.getType());
    }

    private static UnaryExpression getMethodBasedUnaryOperator(
        final ExpressionType unaryType,
        final String methodName,
        final Expression operand) {

        final Type operandType = operand.getType();

        assert !operandType.isPrimitive();

        final MethodInfo method = operandType.getMethod(methodName);

        return new UnaryExpression(unaryType, operand, method.getReturnType(), method);
    }

    private static void validateOperator(final MethodInfo method) {
        assert method != null;

        if (method.isStatic()) {
            throw Error.operatorMethodMustNotBeStatic(method);
        }

        final Type returnType = method.getReturnType();

        if (returnType == PrimitiveTypes.Void) {
            throw Error.operatorMethodMustNotReturnVoid(method);
        }

        final ParameterList parameters = method.getParameters();

        if (parameters.size() != 0) {
            throw Error.operatorMethodParametersMustMatchReturnValue(method);
        }

        if (TypeUtils.areReferenceAssignable(method.getDeclaringType(), returnType)) {
            throw Error.methodBasedOperatorMustHaveValidReturnType(method);
        }
    }

    private static UnaryExpression getMethodBasedCoercionOrThrow(
        final ExpressionType coercionType,
        final Expression expression,
        final Type convertToType) {

        final UnaryExpression u = getMethodBasedCoercion(coercionType, expression, convertToType);

        if (u != null) {
            return u;
        }

        throw Error.coercionOperatorNotDefined(expression.getType(), convertToType);
    }

    private static UnaryExpression getMethodBasedCoercion(
        final ExpressionType coercionType,
        final Expression expression,
        final Type convertToType) {

        final MethodInfo method = TypeUtils.getCoercionMethod(expression.getType(), convertToType);

        if (method != null) {
            return new UnaryExpression(coercionType, expression, convertToType, method);
        }
        else {
            return null;
        }
    }

    private static UnaryExpression getMethodBasedCoercionOperator(
        final ExpressionType unaryType,
        final Expression operand,
        final Type convertToType,
        final MethodInfo method) {

        assert method != null;

        validateOperator(method);

        final ParameterList parameters = method.getParameters();

        if (parameters.size() != 0) {
            throw Error.incorrectNumberOfMethodCallArguments(method);
        }

        final Type returnType = method.getReturnType();

        if (TypeUtils.areReferenceAssignable(convertToType, returnType)) {
            return new UnaryExpression(unaryType, operand, returnType, method);
        }

        // check for auto(un)boxing call
        if (TypeUtils.isAutoUnboxed(convertToType) && returnType.isPrimitive() &&
            TypeUtils.areEquivalent(returnType, TypeUtils.getUnderlyingPrimitive(convertToType))) {

            return new UnaryExpression(unaryType, operand, convertToType, method);
        }

        throw Error.methodBasedOperatorMustHaveValidReturnType(unaryType, method);
    }

    private static UnaryExpression makeOpAssignUnary(final ExpressionType kind, final Expression expression, final MethodInfo method) {
        verifyCanRead(expression, "expression");
        verifyCanWrite(expression, "expression");

        final UnaryExpression result;

        if (method == null) {
            if (TypeUtils.isArithmetic(expression.getType())) {
                return new UnaryExpression(kind, expression, expression.getType(), null);
            }

            final String methodName;

            if (kind == ExpressionType.PreIncrementAssign || kind == ExpressionType.PostIncrementAssign) {
                methodName = "increment";
            }
            else {
                methodName = "decrement";
            }

            result = getMethodBasedUnaryOperatorOrThrow(kind, methodName, expression);
        }
        else {
            result = getMethodBasedUnaryOperator(kind, expression, method);
        }

        // Return type must be assignable back to the operand type
        if (!TypeUtils.areReferenceAssignable(expression.getType(), result.getType())) {
            throw Error.methodBasedOperatorMustHaveValidReturnType(kind, method);
        }

        return result;
    }

    static boolean parameterIsAssignable(final Type parameterType, final Type argumentType) {
        return argumentType.isPrimitive() && parameterType == Types.Object ||
               parameterType.isAssignableFrom(argumentType);
    }

    static MethodInfo getMethodValidated(
        final Type type,
        final String name,
        final Set<BindingFlags> bindingFlags,
        final CallingConvention callingConvention,
        final Type... parameterTypes) {

        final MethodInfo method = type.getMethod(name, bindingFlags, callingConvention, parameterTypes);

        return methodArgumentsMatch(method, parameterTypes) ? method : null;
    }

    static boolean methodArgumentsMatch(
        final MethodInfo method,
        final Type... argumentTypes) {

        if (method == null || argumentTypes == null) {
            return false;
        }

        final ParameterList parameters = method.getParameters();
        final int parameterCount = parameters.size();

        if (parameterCount != argumentTypes.length) {
            return false;
        }

        for (int i = 0; i < parameterCount; i++) {
            if (!parameterIsAssignable(parameters.get(i).getParameterType(), argumentTypes[i])) {
                return false;
            }
        }

        return true;
    }

    static <T> List<T> ensureUnmodifiable(final List<T> list) {
        if (UNMODIFIABLE_LIST_CLASS.isInstance(list)) {
            return list;
        }
        return Collections.unmodifiableList(list);
    }

    @SuppressWarnings("unchecked")
    static <T extends Expression> T returnObject(final Class<T> clazz, final Object objectOrCollection) {
        if (clazz.isInstance(objectOrCollection)) {
            return (T)objectOrCollection;
        }
        return ((ExpressionList<T>)objectOrCollection).get(0);
    }

    private static void verifyTypeBinaryExpressionOperand(final Expression expression, final Type type) {
        if (expression.getType().isPrimitive()) {
            throw Error.primitiveCannotBeTypeBinaryOperand();
        }

        if (type.isPrimitive()) {
            throw Error.primitiveCannotBeTypeBinaryType();
        }
    }

    static MethodInfo getInvokeMethod(final Expression expression) {
        final Type interfaceType = expression.getType();
        final MethodList methods = interfaceType.getMethods();

        if (!interfaceType.isInterface() || methods.size() != 1) {
            throw Error.expressionTypeNotInvokable(interfaceType);
        }

        return methods.get(0);
    }

    private static BinaryExpression getEqualityComparisonOperator(
        final ExpressionType binaryType,
        final String opName,
        final Expression left,
        final Expression right
    ) {

        // Known comparison: numeric types, booleans, object, enums
        if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(left.getType(), right.getType()) &&
            (TypeUtils.isArithmetic(left.getType()) ||
             left.getType() == Types.Object ||
             TypeUtils.isBoolean(left.getType()) ||
             left.getType().isEnum())) {

            return new LogicalBinaryExpression(binaryType, left, right);
        }

        // look for user defined operator
        final BinaryExpression b = getMethodBasedBinaryOperator(binaryType, opName, left, right);

        if (b != null) {
            return b;
        }

        if (TypeUtils.hasBuiltInEqualityOperator(left.getType(), right.getType()) ||
            isNullComparison(left, right)) {

            return new LogicalBinaryExpression(binaryType, left, right);
        }

        throw Error.binaryOperatorNotDefined(binaryType, left.getType(), right.getType());
    }

    private static MethodInfo getBinaryOperatorMethod(
        final ExpressionType binaryType,
        final Type leftType,
        final Type rightType,
        final String name) {

        final MethodInfo method = getMethodValidated(
            leftType,
            name,
            BindingFlags.PublicInstance,
            CallingConvention.Standard,
            rightType
        );

        if (method != null || TypeUtils.areEquivalent(leftType, rightType)) {
            return method;
        }

        return getMethodValidated(
            rightType,
            name,
            BindingFlags.PublicInstance,
            CallingConvention.Standard,
            leftType
        );
    }

    private static MethodInfo getBinaryOperatorStaticMethod(
        final ExpressionType binaryType,
        final Type leftType,
        final Type rightType,
        final String name) {

        final MethodInfo method = getMethodValidated(
            leftType,
            name,
            BindingFlags.PublicStatic,
            CallingConvention.Standard,
            leftType,
            rightType
        );

        if (method != null || TypeUtils.areEquivalent(leftType, rightType)) {
            return method;
        }

        return getMethodValidated(
            rightType,
            name,
            BindingFlags.PublicStatic,
            CallingConvention.Standard,
            leftType,
            rightType
        );
    }

    private static BinaryExpression getMethodBasedBinaryOperator(
        final ExpressionType binaryType,
        final String name,
        final Expression left,
        final Expression right) {

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (name != null) {
            // try exact match first
            MethodInfo method = getBinaryOperatorStaticMethod(binaryType, leftType, rightType, name);

            if (method != null) {
                return new MethodBinaryExpression(binaryType, left, right, method.getReturnType(), method);
            }

            method = getBinaryOperatorMethod(binaryType, leftType, rightType, name);

            if (method != null) {
                return new MethodBinaryExpression(binaryType, left, right, method.getReturnType(), method);
            }

            // try auto(un)boxing call
            if (TypeUtils.isAutoUnboxed(rightType)) {
                final Type unboxedRightType = TypeUtils.getUnderlyingPrimitive(rightType);

                method = getBinaryOperatorMethod(binaryType, leftType, unboxedRightType, name);

                if (method != null) {
                    return new MethodBinaryExpression(binaryType, left, right, method.getReturnType(), method);
                }
            }
            else if (rightType.isPrimitive()) {
                if (TypeUtils.isAutoUnboxed(rightType)) {
                    final Type boxedRightType = TypeUtils.getBoxedType(rightType);

                    method = getBinaryOperatorMethod(binaryType, leftType, boxedRightType, name);

                    if (method != null) {
                        return new MethodBinaryExpression(binaryType, left, right, method.getReturnType(), method);
                    }
                }
            }
        }

        switch (binaryType) {
            case Equal:
            case NotEqual:
                return getEqualsMethodBasedBinaryOperator(binaryType, left, right);

            case GreaterThan:
            case GreaterThanOrEqual:
            case LessThan:
            case LessThanOrEqual:
                return getCompareMethodBasedBinaryOperator(binaryType, left, right);
        }

        return null;
    }

    private static BinaryExpression getCompareMethodBasedBinaryOperator(
        final ExpressionType binaryType,
        final Expression left,
        final Expression right) {

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        MethodInfo method;

        if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType)) {
            final Type comparable = Type.of(Comparable.class);
            final Type leftComparable = comparable.makeGenericType(leftType);

            if (leftType.implementsInterface(leftComparable)) {
                method = getMethodValidated(
                    leftType,
                    "compareTo",
                    BindingFlags.PublicInstance,
                    CallingConvention.Standard,
                    leftType
                );

                if (method != null) {
                    return new CompareMethodBasedLogicalBinaryExpression(binaryType, left, right, method);
                }
            }

            final Type rightComparable = comparable.makeGenericType(leftType);

            if (rightType.implementsInterface(rightComparable)) {
                method = getMethodValidated(
                    rightType,
                    "compareTo",
                    BindingFlags.PublicInstance,
                    CallingConvention.Standard,
                    rightType
                );

                if (method != null) {
                    return new CompareMethodBasedLogicalBinaryExpression(binaryType, left, right, method);
                }
            }
        }

        return null;
    }

    private static BinaryExpression getEqualsMethodBasedBinaryOperator(
        final ExpressionType binaryType,
        final Expression left,
        final Expression right) {

        return new EqualsMethodBasedLogicalBinaryExpression(binaryType, left, right, null);
    }

    private static BinaryExpression getMethodBasedBinaryOperator(
        final ExpressionType binaryType,
        final Expression left,
        final Expression right,
        final MethodInfo method) {

        assert method != null;

        final ParameterList parameters = method.getParameters();

        if (parameters.size() != 1) {
            throw Error.incorrectNumberOfMethodCallArguments(method);
        }

        final Type returnType = method.getReturnType();
        final Type parameterType = parameters.get(0).getParameterType();
        final Type rightType = right.getType();

        if (parameterIsAssignable(parameters.get(0).getParameterType(), rightType)) {
            return new MethodBinaryExpression(binaryType, left, right, returnType, method);
        }

        throw Error.methodBasedOperatorMustHaveValidReturnType(binaryType, method);
    }

    private static BinaryExpression getStaticMethodBasedBinaryOperator(
        final ExpressionType binaryType,
        final Expression left,
        final Expression right,
        final MethodInfo method) {

        assert method != null;

        final ParameterList parameters = method.getParameters();

        if (parameters.size() != 2) {
            throw Error.incorrectNumberOfMethodCallArguments(method);
        }

        final Type returnType = method.getReturnType();
        final Type leftParameterType = parameters.get(0).getParameterType();
        final Type rightParameterType = parameters.get(1).getParameterType();

        final Type leftType = left.getType();
        final Type rightType = right.getType();

        if (parameterIsAssignable(parameters.get(0).getParameterType(), leftType) &&
            parameterIsAssignable(parameters.get(1).getParameterType(), rightType)) {

            return new MethodBinaryExpression(binaryType, left, right, returnType, method);
        }

        throw Error.methodBasedOperatorMustHaveValidReturnType(binaryType, method);
    }

    private static BinaryExpression getMethodBasedBinaryOperatorOrThrow(
        final ExpressionType binaryType,
        final String name,
        final Expression left,
        final Expression right) {

        final BinaryExpression b = Expression.getMethodBasedBinaryOperator(binaryType, name, left, right);

        if (b != null) {
            return b;
        }

        throw Error.binaryOperatorNotDefined(binaryType, left.getType(), right.getType());
    }

    private static BinaryExpression getMethodBasedAssignOperator(
        final ExpressionType binaryType,
        final String name,
        final Expression left,
        final Expression right,
        final LambdaExpression conversion) {

        final MethodInfo method = getBinaryOperatorMethod(binaryType, left.getType(), right.getType(), name);

        if (method != null) {
            return new MethodBinaryExpression(binaryType, left, right, method.getReturnType(), method);
        }

        return null;
    }

    private static BinaryExpression getMethodBasedAssignOperatorOrThrow(
        final ExpressionType binaryType,
        final String name,
        final Expression left,
        final Expression right,
        final LambdaExpression conversion) {

        BinaryExpression b = getMethodBasedBinaryOperatorOrThrow(binaryType, name, left, right);

        if (conversion == null) {
            if (!TypeUtils.areReferenceAssignable(left.getType(), right.getType())) {
                throw Error.methodBasedOperatorMustHaveValidReturnType(binaryType, b.getMethod());
            }
        }
        else {
            validateOpAssignConversionLambda(conversion, b.getLeft(), b.getMethod(), b.getNodeType());

            b = new OpAssignMethodConversionBinaryExpression(
                b.getNodeType(),
                b.getLeft(),
                b.getRight(),
                b.getLeft().getType(),
                b.getMethod(),
                conversion
            );
        }

        return b;
    }

    private static BinaryExpression getMethodBasedAssignOperator(
        final ExpressionType binaryType,
        final Expression left,
        final Expression right,
        final MethodInfo method,
        final LambdaExpression conversion) {

        BinaryExpression b = getMethodBasedBinaryOperator(binaryType, left, right, method);

        if (conversion == null) {
            if (!TypeUtils.areReferenceAssignable(left.getType(), right.getType())) {
                throw Error.methodBasedOperatorMustHaveValidReturnType(binaryType, b.getMethod());
            }
        }
        else {
            validateOpAssignConversionLambda(conversion, b.getLeft(), b.getMethod(), b.getNodeType());

            b = new OpAssignMethodConversionBinaryExpression(
                b.getNodeType(),
                b.getLeft(),
                b.getRight(),
                b.getLeft().getType(),
                b.getMethod(),
                conversion
            );
        }

        return b;
    }

    private static void validateOpAssignConversionLambda(
        final LambdaExpression conversion,
        final Expression left,
        final MethodInfo method,
        final ExpressionType nodeType) {

        final Type interfaceType = conversion.getType();
        final MethodInfo invokeMethod = getInvokeMethod(conversion);
        final ParameterList parameters = invokeMethod.getParameters();

        if (parameters.size() != 1) {
            throw Error.incorrectNumberOfMethodCallArguments(invokeMethod);
        }

        if (!TypeUtils.hasIdentityPrimitiveOrBoxingConversion(invokeMethod.getReturnType(), left.getType())) {
            throw Error.operandTypesDoNotMatchParameters(nodeType, invokeMethod);
        }

        if (method != null) {
            if (!TypeUtils.hasIdentityPrimitiveOrBoxingConversion(
                parameters.get(0).getParameterType(),
                method.getReturnType()
            )) {

                throw Error.overloadOperatorTypeDoesNotMatchConversionType(nodeType, method);
            }
        }
    }

    private static BinaryExpression getComparisonOperator(
        final ExpressionType binaryType,
        final Expression left,
        final Expression right) {

        if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(left.getType(), right.getType()) &&
            TypeUtils.isArithmetic(left.getType())) {

            return new LogicalBinaryExpression(binaryType, left, right);
        }

        final BinaryExpression b = getCompareMethodBasedBinaryOperator(binaryType, left, right);

        if (b != null) {
            return null;
        }

        throw Error.binaryOperatorNotDefined(binaryType, left.getType(), right.getType());
    }

    private static boolean isNullConstant(final Expression e) {
        return e instanceof ConstantExpression &&
               ((ConstantExpression)e).getValue() == null;
    }

    private static boolean isNullComparison(final Expression left, final Expression right) {
        // Do we have x==null, x!=null, null==x or null!=x where x is a reference type but not null?
        return isNullConstant(left) && !isNullConstant(right) && !right.getType().isPrimitive() ||
               isNullConstant(right) && !isNullConstant(left) && !left.getType().isPrimitive();
    }

    private static void validateStaticOrInstanceMethod(final Expression instance, final MethodInfo method) {
        if (method.isStatic()) {
            if (instance != null) {
                throw Error.targetInvalidForStaticMethodCall(method);
            }
        }
        else {
            if (instance == null) {
                throw Error.targetRequiredForNonStaticMethodCall(method);
            }
            verifyCanRead(instance, "instance");
            validateCallTargetType(instance.getType(), method);
        }
    }

    private static void validateCallTargetType(final Type targetType, final MethodInfo method) {
        if (!TypeUtils.isValidInvocationTargetType(method, targetType)) {
            throw Error.targetAndMethodTypeMismatch(method, targetType);
        }
    }

    private static <T extends Expression> ExpressionList<T> validateArgumentTypes(
        final MethodBase method,
        final ExpressionType nodeKind,
        final ExpressionList<T> arguments) {

        assert nodeKind == ExpressionType.Invoke ||
               nodeKind == ExpressionType.Call ||
               nodeKind == ExpressionType.New;

        final ParameterList parameters = method.getParameters();

        validateArgumentCount(method, nodeKind, arguments.size(), parameters);

        T[] newArgs = null;

        for (int i = 0, n = parameters.size(); i < n; i++) {
            final Type parameterType = parameters.get(i).getParameterType();
            final T arg = validateOneArgument(method, nodeKind, arguments.get(i), parameterType);

            if (arg != arguments.get(i)) {
                if (newArgs == null) {
                    newArgs = arguments.toArray();
                }
                newArgs[i] = arg;
            }
        }

        if (newArgs != null) {
            return arguments.newInstance(newArgs);
        }

        return arguments;
    }

    private static <T extends Expression> T validateOneArgument(
        final MethodBase method,
        final ExpressionType nodeKind,
        final T arg,
        final Type parameterType) {

        verifyCanRead(arg, "arguments");

        final Type argType = arg.getType();

        if (!TypeUtils.areReferenceAssignable(parameterType, argType)) {
            switch (nodeKind) {
                case New:
                    throw Error.expressionTypeDoesNotMatchConstructorParameter(argType, parameterType);
                case Invoke:
                    throw Error.expressionTypeDoesNotMatchParameter(argType, parameterType);
                case Call:
                    throw Error.expressionTypeDoesNotMatchMethodParameter(argType, parameterType, method);
                default:
                    throw ContractUtils.unreachable();
            }
        }

        return arg;
    }

    private static void validateArgumentCount(
        final MethodBase method,
        final ExpressionType nodeKind,
        final int count,
        final ParameterList parameterTypes) {

        if (parameterTypes.size() == count) {
            return;
        }

        // Throw the right error for the node we were given
        switch (nodeKind) {
            case New:
                throw Error.incorrectNumberOfConstructorArguments();
            case Invoke:
                throw Error.incorrectNumberOfLambdaArguments();
            case Call:
                throw Error.incorrectNumberOfMethodCallArguments(method);
            default:
                throw ContractUtils.unreachable();
        }
    }

    private static <T> void validateLambdaArgs(
        final Type<T> interfaceType,
        final Expression body,
        final ParameterExpressionList parameters) {

        VerifyArgument.notNull(interfaceType, "interfaceType");
        verifyCanRead(body, "body");

        final MethodList methods = interfaceType.getMethods(BindingFlags.PublicInstance);

        if (!interfaceType.isInterface() || methods.size() != 1) {
            throw Error.lambdaTypeMustBeSingleMethodInterface();
        }

        final MethodInfo method = methods.get(0);
        final ParameterList methodParameters = method.getParameters();

        if (methodParameters.size() > 0) {
            if (parameters.size() != methodParameters.size()) {
                throw Error.incorrectNumberOfLambdaArguments();
            }

            final Set<ParameterExpression> set = new HashSet<>(parameters.size());

            for (int i = 0, n = methodParameters.size(); i < n; i++) {
                final ParameterExpression pex = parameters.get(i);
                final ParameterInfo pi = methodParameters.get(i);

                verifyCanRead(pex, "parameters");

                final Type pType = pi.getParameterType();

                if (!TypeUtils.areReferenceAssignable(pex.getType(), pType)) {
                    throw Error.parameterExpressionNotValidForDelegate(pex.getType(), pType);
                }

                if (set.contains(pex)) {
                    throw Error.duplicateVariable(pex);
                }

                set.add(pex);
            }
        }
        else if (parameters.size() > 0) {
            throw Error.incorrectNumberOfLambdaDeclarationParameters();
        }

        if (method.getReturnType() != PrimitiveTypes.Void &&
            !TypeUtils.areReferenceAssignable(method.getReturnType(), body.getType())) {

            throw Error.expressionTypeDoesNotMatchReturn(body.getType(), method.getReturnType());
        }
    }

    private static void validateGoto(
        final LabelTarget target,
        final Expression value,
        final String targetParameter,
        final String valueParameter) {

        VerifyArgument.notNull(target, targetParameter);

        if (value == null) {
            if (target.getType() != PrimitiveTypes.Void) {
                throw Error.labelMustBeVoidOrHaveExpression();
            }
        }
    }

    private static void validateGotoType(final Type expectedType, final Expression value, final String valueParameter) {
        verifyCanRead(value, valueParameter);

        if (expectedType != PrimitiveTypes.Void &&
            !TypeUtils.areReferenceAssignable(expectedType, value.getType())) {

            throw Error.expressionTypeDoesNotMatchLabel(value.getType(), expectedType);
        }
    }

    private static void validateTryAndCatchHaveSameType(
        final Type type,
        final Expression tryBody,
        final ReadOnlyList<CatchBlock> handlers) {

        // Type unification ... all parts must be reference assignable to "type"
        if (type != null) {
            if (type != PrimitiveTypes.Void) {
                if (!TypeUtils.areReferenceAssignable(type, tryBody.getType())) {
                    throw Error.argumentTypesMustMatch();
                }

                for (int i = 0, n = handlers.size(); i < n; i++) {
                    final CatchBlock cb = handlers.get(i);
                    if (!TypeUtils.areReferenceAssignable(type, cb.getBody().getType())) {
                        throw Error.argumentTypesMustMatch();
                    }
                }
            }
        }
        else if (tryBody == null || tryBody.getType() == PrimitiveTypes.Void) {
            //The body of every try block must be null or have void type.
            for (int i = 0, n = handlers.size(); i < n; i++) {
                final Expression catchBody = handlers.get(i).getBody();
                if (catchBody != null && catchBody.getType() != PrimitiveTypes.Void) {
                    throw Error.bodyOfCatchMustHaveSameTypeAsBodyOfTry();
                }
            }
        }
        else {
            //Body of every catch must have the same type of body of try.
            final Type tryType = tryBody.getType();
            for (int i = 0, n = handlers.size(); i < n; i++) {
                final Expression catchBody = handlers.get(i).getBody();
                if (catchBody == null || !TypeUtils.areEquivalent(catchBody.getType(), tryType)) {
                    throw Error.bodyOfCatchMustHaveSameTypeAsBodyOfTry();
                }
            }
        }
    }

    private static void validateSwitchCaseType(
        final Expression caseBody,
        final boolean customType,
        final Type resultType,
        final String parameterName) {

        if (customType) {
            if (resultType != PrimitiveTypes.Void) {
                if (!TypeUtils.areReferenceAssignable(resultType, caseBody.getType())) {
                    throw Error.argumentTypesMustMatch();
                }
            }
        }
        else {
            if (!TypeUtils.hasIdentityPrimitiveOrBoxingConversion(resultType, caseBody.getType())) {
                throw Error.allCaseBodiesMustHaveSameType();
            }
        }
    }

    private final static Set<BindingFlags> StaticMethodBindingFlags = BindingFlags.set(
        BindingFlags.Static,
        BindingFlags.Public,
        BindingFlags.NonPublic,
        BindingFlags.FlattenHierarchy
    );

    private final static Set<BindingFlags> InstanceMethodBindingFlags = BindingFlags.set(
        BindingFlags.Instance,
        BindingFlags.Public,
        BindingFlags.NonPublic,
        BindingFlags.FlattenHierarchy
    );

    private static MethodInfo findMethod(
        final Type type,
        final String methodName,
        final TypeList typeArguments,
        final ExpressionList<? extends Expression> arguments,
        final Set<BindingFlags> flags) {

        final MemberList members = type.findMembers(
            MemberType.methods(),
            flags,
            Type.FilterNameIgnoreCase,
            methodName
        );

        if (members == null || members.size() == 0) {
            throw Error.methodDoesNotExistOnType(methodName, type);
        }

        MethodInfo method;

        final int bestMethodIndex = findBestMethod(members, typeArguments, arguments);

        if (bestMethodIndex == -1) {
            if (typeArguments != null && typeArguments.size() > 0) {
                throw Error.genericMethodWithArgsDoesNotExistOnType(methodName, type);
            }
            else {
                throw Error.methodWithArgsDoesNotExistOnType(methodName, type);
            }
        }

        if (bestMethodIndex == -2) {
            throw Error.methodWithMoreThanOneMatch(methodName, type);
        }

        final MethodInfo bestMatch = (MethodInfo)members.get(bestMethodIndex);

        if (!typeArguments.isEmpty()) {
            return applyTypeArgs(bestMatch, typeArguments);
        }

        return bestMatch;
    }

    private static int findBestMethod(
        final MemberList<?> methods,
        final TypeList typeArgs,
        final ExpressionList<? extends Expression> arguments) {

        int count = 0;
        int bestMethodIndex = -1;
        MethodInfo bestMethod = null;

        for (int i = 0, n = methods.size(); i < n; i++) {
            final MethodInfo method = applyTypeArgs((MethodInfo)methods.get(i), typeArgs);
            if (method != null && isCompatible(method, arguments)) {
                // Favor public over non-public methods.
                if (bestMethod == null || (!bestMethod.isPublic() && method.isPublic())) {
                    bestMethodIndex = i;
                    bestMethod = method;
                    count = 1;
                }
                else {
                    // Only count it as additional method if they both public or both non-public.
                    if (bestMethod.isPublic() == method.isPublic()) {
                        count++;
                    }
                }
            }
        }

        if (count > 1)
            return -2;

        return bestMethodIndex;
    }

    private static boolean isCompatible(final MethodBase m, final ExpressionList<? extends Expression> arguments) {
        VerifyArgument.noNullElements(arguments, "arguments");

        final ParameterList parameters = m.getParameters();

        if (parameters.size() != arguments.size()) {
            return false;
        }

        for (int i = 0, n = arguments.size(); i < n; i++) {
            final Expression argument = arguments.get(i);
            final Type argumentType = argument.getType();
            final Type parameterType = parameters.get(i).getParameterType();

            if (!TypeUtils.areReferenceAssignable(parameterType, argumentType) &&
                !(TypeUtils.isSameOrSubType(Type.of(LambdaExpression.class), parameterType) &&
                  parameterType.isAssignableFrom(argument.getType()))) {

                return false;
            }
        }
        return true;
    }

    private static MethodInfo applyTypeArgs(final MethodInfo m, final TypeList typeArgs) {
        if (typeArgs == null || typeArgs.size() == 0) {
            if (!m.isGenericMethodDefinition()) {
                return m;
            }
        }
        else if (m.isGenericMethodDefinition()) {
            final TypeList genericParameters = m.getGenericMethodParameters();
            if (genericParameters.size() == typeArgs.size()) {
                return m.makeGenericMethod(typeArgs);
            }
        }
        return null;
    }
}
