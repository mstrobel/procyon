package com.strobel.expressions;

import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;
import com.strobel.util.TypeUtils;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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

    /**
     * Returns the node type of this {@link Expression}.
     * @return the {@link ExpressionType} that represents this expression.
     */
    public ExpressionType getNodeType() {
        throw Error.extensionMustOverride("Expression.getNodeType()");
    }

    /**
     * Gets the static type of the expression that this {@link Expression} represents.
     * @return the {@link Class} that represents the static type of the expression.
     */
    public Class getType() {
        throw Error.extensionMustOverride("Expression.getType()");
    }

    /**
     * Indicates that the node can be reduced to a simpler node. If this returns {@code true},
     * {@code reduce()} can be called to produce the reduced form.
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FACTORY METHODS                                                                                                    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static MemberExpression makeMemberAccess(final Expression target, final AccessibleObject member) {
        VerifyArgument.notNull(member, "member");

        if (member instanceof Field) {
            return Expression.field(target, (Field)member);
        }

        throw Error.memberNotField(member);
    }

    public static MemberExpression field(final Expression target, final Field field) {
        return new FieldExpression(target, field);
    }

    //
    // CONSTANTS
    //

    public static ConstantExpression constant(final Object value) {
        return ConstantExpression.make(value, value == null ? Object.class : value.getClass());
    }

    public static ConstantExpression constant(final Object value, final Class type) {
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

    public static ParameterExpression parameter(final Class type) {
        return parameter(type, null);
    }

    public static ParameterExpression variable(final Class type) {
        return variable(type, null);
    }

    public static ParameterExpression parameter(final Class type, final String name) {
        VerifyArgument.notNull(type, "type");

        if (type == Void.TYPE) {
            throw Error.argumentCannotBeOfTypeVoid();
        }

        return ParameterExpression.make(type, name);
    }

    public static ParameterExpression variable(final Class type, final String name) {
        VerifyArgument.notNull(type, "type");

        if (type == Void.TYPE) {
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
        final Class type) {

        return makeUnary(unaryType, operand, type, null);
    }

    public static UnaryExpression makeUnary(
        final ExpressionType unaryType,
        final Expression operand,
        final Class type,
        final Method method) {

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

    public static UnaryExpression negate(final Expression expression, final Method method) {
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

    public static UnaryExpression not(final Expression expression, final Method method) {
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

    public static UnaryExpression isFalse(final Expression expression, final Method method) {
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

    public static UnaryExpression isTrue(final Expression expression, final Method method) {
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

    public static UnaryExpression onesComplement(final Expression expression, final Method method) {
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

        if (!array.getType().isArray() || !Array.class.isAssignableFrom(array.getType())) {
            throw Error.argumentMustBeArray();
        }

        return new UnaryExpression(ExpressionType.ArrayLength, array, Integer.TYPE, null);
    }

    public static UnaryExpression convert(final Expression expression, final Class type) {
        return convert(expression, type, null);
    }

    public static UnaryExpression convert(final Expression expression, final Class type, final Method method) {
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
        return makeThrow(expression, Void.class);
    }

    public static UnaryExpression makeThrow(final Expression value, final Class type) {
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

    public static UnaryExpression unaryPlus(final Expression expression, final Method method) {
        verifyCanRead(expression, "expression");

        if (method == null) {
            if (TypeUtils.isArithmetic(expression.getType())) {
                return new UnaryExpression(ExpressionType.UnaryPlus, expression, expression.getType(), null);
            }
            return getMethodBasedUnaryOperatorOrThrow(ExpressionType.UnaryPlus, "abs", expression);
        }

        return getMethodBasedUnaryOperator(ExpressionType.UnaryPlus, expression, method);
    }

    public static UnaryExpression unbox(final Expression expression, final Class type) {
        verifyCanRead(expression, "expression");
        VerifyArgument.notNull(type, "type");
        if (!TypeUtils.isAutoUnboxed(type) && type != Object.class) {
            throw Error.invalidUnboxType();
        }
        return new UnaryExpression(ExpressionType.Unbox, expression, type, null);
    }

    public static UnaryExpression increment(final Expression expression) {
        return negate(expression, null);
    }

    public static UnaryExpression increment(final Expression expression, final Method method) {
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

    public static UnaryExpression decrement(final Expression expression, final Method method) {
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

    public static UnaryExpression preIncrementAssign(final Expression expression, final Method method) {
        return makeOpAssignUnary(ExpressionType.PreIncrementAssign, expression, method);
    }

    public static UnaryExpression postIncrementAssign(final Expression expression) {
        return makeOpAssignUnary(ExpressionType.PostIncrementAssign, expression, null);
    }

    public static UnaryExpression postIncrementAssign(final Expression expression, final Method method) {
        return makeOpAssignUnary(ExpressionType.PostIncrementAssign, expression, method);
    }

    public static UnaryExpression preDecrementAssign(final Expression expression) {
        return makeOpAssignUnary(ExpressionType.PreDecrementAssign, expression, null);
    }

    public static UnaryExpression preDecrementAssign(final Expression expression, final Method method) {
        return makeOpAssignUnary(ExpressionType.PreDecrementAssign, expression, method);
    }

    public static UnaryExpression postDecrementAssign(final Expression expression) {
        return makeOpAssignUnary(ExpressionType.PostDecrementAssign, expression, null);
    }

    public static UnaryExpression postDecrementAssign(final Expression expression, final Method method) {
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
        VerifyArgument.notNull(expressions, "expressions");

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
                return new BlockN(ensureUnmodifiable(arrayToList(expressions)));
        }
    }

    public static BlockExpression block(final List<Expression> expressions) {
        return block(Collections.<ParameterExpression>emptyList(), expressions);
    }

    public static BlockExpression block(final ParameterExpression[] variables, final Expression... expressions) {
        VerifyArgument.notNull(expressions, "expressions");
        return block(arrayToList(variables), arrayToList(expressions));
    }

    public static BlockExpression block(final List<ParameterExpression> variables, final Expression... expressions) {
        VerifyArgument.notNull(expressions, "expressions");
        return block(variables, arrayToList(expressions));
    }

    public static BlockExpression block(final List<ParameterExpression> variables, final List<Expression> expressions) {
        VerifyArgument.notNull(expressions, "expressions");
        final List<Expression> expressionsList = ensureUnmodifiable(expressions);
        VerifyArgument.notEmpty(expressionsList, "expressions");
        verifyCanRead(expressionsList, "expressions");

        return block(
            expressionsList.get(expressionsList.size() - 1).getType(),
            ensureUnmodifiable(variables),
            expressionsList
        );
    }

    public static BlockExpression block(final Class type, final Expression... expressions) {
        VerifyArgument.notNull(expressions, "expressions");
        return block(type, arrayToList(expressions));
    }

    public static BlockExpression block(final Class type, final List<Expression> expressions) {
        VerifyArgument.notNull(expressions, "expressions");
        return block(type, Collections.<ParameterExpression>emptyList(), expressions);
    }

    public static BlockExpression block(final Class type, final ParameterExpression[] variables, final Expression... expressions) {
        VerifyArgument.notNull(expressions, "expressions");
        return block(type, arrayToList(variables), arrayToList(expressions));
    }

    public static BlockExpression block(final Class type, final List<ParameterExpression> variables, final Expression... expressions) {
        VerifyArgument.notNull(expressions, "expressions");
        return block(type, variables, arrayToList(expressions));
    }

    public static BlockExpression block(final Class type, final List<ParameterExpression> variables, final List<Expression> expressions) {
        VerifyArgument.notNull(type, "type");
        VerifyArgument.notEmpty(expressions, "expressions");

        final List<Expression> expressionList = ensureUnmodifiable(expressions);
        final List<ParameterExpression> variableList = ensureUnmodifiable(variables);

        verifyCanRead(expressionList, "expressions");
        validateVariables(variableList, "variables");

        final Expression last = expressionList.get(expressionList.size() - 1);

        if (type != Void.TYPE) {
            if (!type.isAssignableFrom(last.getType())) {
                throw Error.argumentTypesMustMatch();
            }
        }

        if (type != last.getType()) {
            return new ScopeWithType(variableList, expressionList, type);
        }
        else {
            if (expressionList.size() == 1) {
                return new Scope1(variableList, expressionList.get(0));
            }
            else {
                return new ScopeN(variableList, expressionList);
            }
        }
    }

    //
    // BINARY EXPRESSIONS
    //

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
        final Method method) {

        return makeBinary(binaryType, left, right, method, null);
    }

    public static BinaryExpression makeBinary(
        final ExpressionType binaryType,
        final Expression left,
        final Expression right,
        final Method method,
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
/*
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
*/
            default:
                throw Error.unhandledBinary(binaryType);
        }
    }

    public static BinaryExpression add(final Expression left, final Expression right) {
        return add(left, right, null);
    }

    public static BinaryExpression add(final Expression left, final Expression right, final Method method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Class leftType = left.getType();
            final Class rightType = right.getType();

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

    public static BinaryExpression subtract(final Expression left, final Expression right, final Method method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Class leftType = left.getType();
            final Class rightType = right.getType();

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

    public static BinaryExpression multiply(final Expression left, final Expression right, final Method method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Class leftType = left.getType();
            final Class rightType = right.getType();

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

    public static BinaryExpression divide(final Expression left, final Expression right, final Method method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Class leftType = left.getType();
            final Class rightType = right.getType();

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

    public static BinaryExpression modulo(final Expression left, final Expression right, final Method method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Class leftType = left.getType();
            final Class rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isArithmetic(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.Modulo, left, right, leftType);
            }

            return getMethodBasedBinaryOperatorOrThrow(ExpressionType.Modulo, "modulo", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.Modulo, left, right, method);
    }

    public static BinaryExpression and(final Expression left, final Expression right) {
        return and(left, right, null);
    }

    public static BinaryExpression and(final Expression left, final Expression right, final Method method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Class leftType = left.getType();
            final Class rightType = right.getType();

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

    public static BinaryExpression or(final Expression left, final Expression right, final Method method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            final Class leftType = left.getType();
            final Class rightType = right.getType();

            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) && TypeUtils.isIntegralOrBoolean(leftType)) {
                return new SimpleBinaryExpression(ExpressionType.Or, left, right, leftType);
            }

            return getMethodBasedBinaryOperatorOrThrow(ExpressionType.Or, "or", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.Or, left, right, method);
    }

    public static BinaryExpression andAlso(final Expression left, final Expression right) {
        return andAlso(left, right, null);
    }

    public static BinaryExpression andAlso(final Expression left, final Expression right, final Method method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        final Class leftType = left.getType();
        final Class rightType = right.getType();

        Class returnType;

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.hasIdentityPrimitiveOrBoxingConversion(left.getType(), Boolean.TYPE)) {

                return new SimpleBinaryExpression(ExpressionType.AndAlso, left, right, leftType);
            }

/*
            final Method opMethod = getBinaryOperatorMethod(ExpressionType.AndAlso, leftType, rightType, "and");

            if (opMethod != null) {
                validateUserDefinedConditionalLogicOperator(ExpressionType.AndAlso, leftType, rightType, opMethod);

                returnType = opMethod.getReturnType();

                if (TypeUtils.isAutoUnboxed(leftType) &&
                    TypeUtils.areEquivalent(returnType, TypeUtils.getUnderlyingPrimitive(leftType))) {

                    returnType = leftType;
                }

                return new MethodBinaryExpression(ExpressionType.AndAlso, left, right, returnType, method);
            }
*/

            throw Error.binaryOperatorNotDefined(ExpressionType.AndAlso, leftType, rightType);
        }

/*
        validateUserDefinedConditionalLogicOperator(ExpressionType.AndAlso, leftType, rightType, method);

        returnType = method.getReturnType();

        if (TypeUtils.isAutoUnboxed(leftType) &&
            TypeUtils.areEquivalent(returnType, TypeUtils.getUnderlyingPrimitive(leftType))) {

            returnType = leftType;
        }

        return new MethodBinaryExpression(ExpressionType.AndAlso, left, right, returnType, method);
*/
        throw Error.binaryOperatorNotDefined(ExpressionType.AndAlso, leftType, rightType);
    }

    public static BinaryExpression orElse(final Expression left, final Expression right) {
        return orElse(left, right, null);
    }

    public static BinaryExpression orElse(final Expression left, final Expression right, final Method method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        final Class leftType = left.getType();
        final Class rightType = right.getType();

        Class returnType;

        if (method == null) {
            if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(leftType, rightType) &&
                TypeUtils.hasIdentityPrimitiveOrBoxingConversion(left.getType(), Boolean.TYPE)) {

                return new SimpleBinaryExpression(ExpressionType.OrElse, left, right, leftType);
            }

/*
            final Method opMethod = getBinaryOperatorMethod(ExpressionType.OrElse, leftType, rightType, "and");

            if (opMethod != null) {
                validateUserDefinedConditionalLogicOperator(ExpressionType.OrElse, leftType, rightType, opMethod);

                returnType = opMethod.getReturnType();

                if (TypeUtils.isAutoUnboxed(leftType) &&
                    TypeUtils.areEquivalent(returnType, TypeUtils.getUnderlyingPrimitive(leftType))) {

                    returnType = leftType;
                }

                return new MethodBinaryExpression(ExpressionType.OrElse, left, right, returnType, method);
            }
*/

            throw Error.binaryOperatorNotDefined(ExpressionType.OrElse, leftType, rightType);
        }

/*
        validateUserDefinedConditionalLogicOperator(ExpressionType.OrElse, leftType, rightType, method);

        returnType = method.getReturnType();

        if (TypeUtils.isAutoUnboxed(leftType) &&
            TypeUtils.areEquivalent(returnType, TypeUtils.getUnderlyingPrimitive(leftType))) {

            returnType = leftType;
        }

        return new MethodBinaryExpression(ExpressionType.OrElse, left, right, returnType, method);
*/
        throw Error.binaryOperatorNotDefined(ExpressionType.OrElse, leftType, rightType);
    }

    public static BinaryExpression assign(final Expression left, final Expression right) {
        throw ContractUtils.unreachable();
    }

    public static BinaryExpression equal(final Expression left, final Expression right, final Method method) {
        verifyCanRead(left, "left");
        verifyCanRead(right, "right");

        if (method == null) {
            return getEqualityComparisonOperator(ExpressionType.Equal, "equals", left, right);
        }

        return getMethodBasedBinaryOperator(ExpressionType.Equal, left, right, method);
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LAMBDA EXPRESSIONS                                                                                                 //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // INVOKE EXPRESSIONS                                                                                                 //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static InvocationExpression invoke(final Expression expression, final Expression... arguments) {
        return invoke(expression, arrayToList(arguments));
    }

    public static InvocationExpression invoke(final Expression expression, final List<Expression> arguments) {
        verifyCanRead(expression, "expression");

        final List<Expression> args = ensureUnmodifiable(arguments);
        final Method method = getInvokeMethod(expression);

        return new InvocationExpression(expression, args, method.getReturnType());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // METHOD CALL EXPRESSIONS                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static MethodCallExpression call(final Method method, final Expression... arguments) {
        return call(null, method, arrayToList(arguments));
    }

    public static MethodCallExpression call(final Method method, final List<Expression> arguments) {
        return call(null, method, arguments);
    }

    public static MethodCallExpression call(final Expression target, final Method method, final Expression... arguments) {
        return call(target, method, arrayToList(arguments));
    }

    public static MethodCallExpression call(final Expression target, final Method method, final List<Expression> arguments) {
        VerifyArgument.notNull(method, "method");

        List<Expression> argumentList = ensureUnmodifiable(arguments);

        validateStaticOrInstanceMethod(target, method);
        argumentList = validateArgumentTypes(method, ExpressionType.Call, argumentList);

        if (target == null) {
            return new MethodCallExpressionN(method, argumentList);
        }

        return new InstanceMethodCallExpressionN(method, target, arguments);
    }

    public static MethodCallExpression call(
        final Expression target,
        final String methodName,
        final Class[] typeArguments,
        final Expression... arguments) {

        return call(target, methodName, typeArguments, arrayToList(arguments));
    }

    public static MethodCallExpression call(
        final Expression target,
        final String methodName,
        final Class[] typeArguments,
        final List<Expression> arguments) {

        VerifyArgument.notNull(target, "target");
        VerifyArgument.notNull(methodName, "methodName");

        final Method resolvedMethod = MethodBinder.findMethod(
            target.getType(),
            methodName,
            typeArguments,
            arguments,
            0
        );

        return call(
            target,
            resolvedMethod,
            arguments
        );
    }

    public static MethodCallExpression call(
        final Class declaringType,
        final String methodName,
        final Class[] typeArguments,
        final Expression... arguments) {

        return call(declaringType, methodName, typeArguments, arrayToList(arguments));
    }

    public static MethodCallExpression call(
        final Class declaringType,
        final String methodName,
        final Class[] typeArguments,
        final List<Expression> arguments) {

        VerifyArgument.notNull(declaringType, "declaringType");
        VerifyArgument.notNull(methodName, "methodName");

        final Method resolvedMethod = MethodBinder.findMethod(
            declaringType,
            methodName,
            typeArguments,
            arguments,
            0
        );

        return call(
            resolvedMethod,
            arguments
        );
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

    private static <T extends Expression> List<T> arrayToList(final T[] expressions) {
        if (expressions == null || expressions.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(expressions);
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
                if (memberExpression.getMember() instanceof Field) {
                    final Field field = (Field)memberExpression.getMember();
                    canWrite = !field.isEnumConstant() &&
                               (field.getModifiers() & Modifier.FINAL) == 0;
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

    static void validateVariables(final List<ParameterExpression> varList, final String collectionName) {
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
        final Method method) {

        validateOperator(method);

        final Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length != 0) {
            throw Error.incorrectNumberOfMethodCallArguments(method);
        }

        final Class<?> returnType = method.getReturnType();

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

        final Class operandType = operand.getType();

        assert !operandType.isPrimitive();

        final Method method;

        try {
            method = operandType.getMethod(methodName);
            return new UnaryExpression(unaryType, operand, method.getReturnType(), method);
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static void validateOperator(final Method method) {
        assert method != null;

        if ((method.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
            throw Error.operatorMethodMustNotBeStatic(method);
        }

        final Class<?> returnType = method.getReturnType();

        if (returnType == Void.TYPE) {
            throw Error.operatorMethodMustNotReturnVoid(method);
        }

        final Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length != 0) {
            throw Error.operatorMethodParametersMustMatchReturnValue(method);
        }

        if (TypeUtils.areReferenceAssignable(method.getDeclaringClass(), returnType)) {
            throw Error.methodBasedOperatorMustHaveValidReturnType(method);
        }
    }

    private static UnaryExpression getMethodBasedCoercionOrThrow(
        final ExpressionType coercionType,
        final Expression expression,
        final Class convertToType) {

        final UnaryExpression u = getMethodBasedCoercion(coercionType, expression, convertToType);

        if (u != null) {
            return u;
        }

        throw Error.coercionOperatorNotDefined(expression.getType(), convertToType);
    }

    private static UnaryExpression getMethodBasedCoercion(
        final ExpressionType coercionType,
        final Expression expression,
        final Class convertToType) {

        final Method method = TypeUtils.getCoercionMethod(expression.getType(), convertToType);

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
        final Class convertToType,
        final Method method) {

        assert method != null;

        validateOperator(method);

        final Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length != 0) {
            throw Error.incorrectNumberOfMethodCallArguments(method);
        }

        final Class<?> returnType = method.getReturnType();

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

    private static UnaryExpression makeOpAssignUnary(final ExpressionType kind, final Expression expression, final Method method) {
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

    static boolean parameterIsAssignable(final Class parameterType, final Class argumentType) {
        return argumentType.isPrimitive() && parameterType == Object.class ||
               parameterType.isAssignableFrom(argumentType);
    }

    static <T> List<T> ensureUnmodifiable(final List<T> list) {
        if (UNMODIFIABLE_LIST_CLASS.isInstance(list)) {
            return list;
        }
        return Collections.unmodifiableList(list);
    }

    @SuppressWarnings("unchecked")
    static <T> T returnObject(final Class<T> clazz, final Object objectOrCollection) {
        if (clazz.isInstance(objectOrCollection)) {
            return (T)objectOrCollection;
        }
        return ((List<T>)objectOrCollection).get(0);
    }

    static Method getInvokeMethod(final Expression expression) {
        final Class interfaceType = expression.getType();
        final Method[] methods = interfaceType.getMethods();

        if (!interfaceType.isInterface() || methods.length != 1) {
            throw Error.expressionTypeNotInvokable(interfaceType);
        }

        try {
            return interfaceType.getMethod("Invoke");
        }
        catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static BinaryExpression getEqualityComparisonOperator(
        final ExpressionType binaryType,
        final String opName,
        final Expression left,
        final Expression right) {

        // Known comparison: numeric types, booleans, object, enums
        if (left.getType() == right.getType() && (TypeUtils.isArithmetic(left.getType()) ||
                                                  left.getType() == Object.class ||
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

    private static Method getBinaryOperatorMethod(
        final ExpressionType binaryType,
        final Class leftType,
        final Class rightType,
        final String name) {

        try {
            final Method method = leftType.getMethod(name, rightType);

            if (TypeUtils.areReferenceAssignable(leftType, method.getReturnType())) {
                return method;
            }
        }
        catch (NoSuchMethodException ignored) {
        }

        return null;
    }

    private static BinaryExpression getMethodBasedBinaryOperator(
        final ExpressionType binaryType,
        final String name,
        final Expression left,
        final Expression right) {

        final Class leftType = left.getType();
        final Class rightType = right.getType();

        // try exact match first
        Method method = getBinaryOperatorMethod(binaryType, leftType, rightType, name);

        if (method != null) {
            return new MethodBinaryExpression(binaryType, left, right, method.getReturnType(), method);
        }

        // try auto(un)boxing call
        if (TypeUtils.isAutoUnboxed(rightType)) {
            final Class unboxedRightType = TypeUtils.getUnderlyingPrimitive(rightType);

            method = getBinaryOperatorMethod(binaryType, leftType, unboxedRightType, name);

            if (method != null) {
                return new MethodBinaryExpression(binaryType, left, right, method.getReturnType(), method);
            }
        }
        else if (rightType.isPrimitive()) {
            if (TypeUtils.isAutoUnboxed(rightType)) {
                final Class boxedRightType = TypeUtils.getBoxedType(rightType);

                method = getBinaryOperatorMethod(binaryType, leftType, boxedRightType, name);

                if (method != null) {
                    return new MethodBinaryExpression(binaryType, left, right, method.getReturnType(), method);
                }
            }
        }

        return null;
    }

    private static BinaryExpression getMethodBasedBinaryOperator(
        final ExpressionType binaryType,
        final Expression left,
        final Expression right,
        final Method method) {

        assert method != null;

        final Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length != 1) {
            throw Error.incorrectNumberOfMethodCallArguments(method);
        }

        final Class<?> returnType = method.getReturnType();

        if (TypeUtils.areReferenceAssignable(left.getType(), returnType)) {
            final Class rightType = right.getType();

            if (parameterIsAssignable(parameterTypes[0], rightType)) {
                return new MethodBinaryExpression(binaryType, left, right, returnType, method);
            }

            // Check for auto(un)boxing call
            if ((TypeUtils.isAutoUnboxed(rightType) &&
                 parameterIsAssignable(parameterTypes[0], TypeUtils.getUnderlyingPrimitive(rightType))) ||
                (returnType.isPrimitive() &&
                 parameterIsAssignable(parameterTypes[0], TypeUtils.getBoxedType(returnType)))) {

                return new MethodBinaryExpression(binaryType, left, right, returnType, method);
            }
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

    private static boolean isNullConstant(final Expression e) {
        return e instanceof ConstantExpression &&
               ((ConstantExpression)e).getValue() == null;
    }

    private static boolean isNullComparison(final Expression left, final Expression right) {
        // Do we have x==null, x!=null, null==x or null!=x where x is a reference type but not null?
        return isNullConstant(left) && !isNullConstant(right) && !right.getType().isPrimitive() ||
               isNullConstant(right) && !isNullConstant(left) && !left.getType().isPrimitive();
    }

    private static void validateStaticOrInstanceMethod(final Expression instance, final Method method) {
        if (Modifier.isStatic(method.getModifiers())) {
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

    private static void validateCallTargetType(final Class targetType, final Method method) {
        if (!TypeUtils.isValidInvocationTargetType(method, targetType)) {
            throw Error.targetAndMethodTypeMismatch(method, targetType);
        }
    }

    private static List<Expression> validateArgumentTypes(
        final Method method,
        final ExpressionType nodeKind,
        final List<Expression> arguments) {

        assert  nodeKind == ExpressionType.Invoke ||
                nodeKind == ExpressionType.Call ||
                nodeKind == ExpressionType.New;

        final Class<?>[] parameterTypes = method.getParameterTypes();

        validateArgumentCount(method, nodeKind, arguments.size(), parameterTypes);

        Expression[] newArgs = null;

        for (int i = 0, n = parameterTypes.length; i < n; i++) {
            final Class<?> parameterType = parameterTypes[i];
            final Expression arg = validateOneArgument(method, nodeKind, arguments.get(i), parameterType);

            if (newArgs == null && arg != arguments.get(i)) {
                newArgs = new Expression[arguments.size()];
                for (int j = 0; j < i; j++) {
                    newArgs[j] = arguments.get(j);
                }
            }
            if (newArgs != null) {
                newArgs[i] = arg;
            }
        }

        if (newArgs != null) {
            return ensureUnmodifiable(arrayToList(newArgs));
        }

        return arguments;
    }

    private static Expression validateOneArgument(
        final Method method,
        final ExpressionType nodeKind,
        final Expression arg,
        final Class<?> parameterType) {

        verifyCanRead(arg, "arguments");

        final Class argType = arg.getType();

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
        final Method method,
        final ExpressionType nodeKind,
        final int count,
        final Class<?>[] parameterTypes) {

        if (parameterTypes.length == count) {
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
}
