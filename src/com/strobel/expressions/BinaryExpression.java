package com.strobel.expressions;

import com.strobel.util.TypeUtils;

import java.lang.reflect.Method;

/**
 * @author Mike Strobel
 */
public class BinaryExpression extends Expression {
    private final Expression _left;
    private final Expression _right;

    BinaryExpression(final Expression left, final Expression right) {
        _left = left;
        _right = right;
    }

    public final Expression getRight() {
        return _right;
    }

    public final Expression getLeft() {
        return _left;
    }

    public Method getMethod() {
        return null;
    }

    public LambdaExpression getConversion() {
        return null;
    }

    @Override
    public boolean canReduce() {
        return isOpAssignment(getNodeType());
    }

    @Override
    public Expression reduce() {
        // Only reduce OpAssignment expressions.
        if (isOpAssignment(getNodeType())) {
            switch (_left.getNodeType()) {
                case MemberAccess:
                    return reduceMember();
                default:
                    return reduceVariable();
            }
        }
        return this;
    }

    public BinaryExpression update(final Expression left, final LambdaExpression conversion, final Expression right) {
        if (left == getLeft() && right == getRight() && conversion == getConversion()) {
            return this;
        }
        if (isReferenceComparison()) {
            if (getNodeType() == ExpressionType.Equal) {
                return Expression.referenceEqual(left, right);
            }
            else {
                return Expression.referenceNotEqual(left, right);
            }
        }

        return Expression.makeBinary(getNodeType(), left, right, getMethod(), conversion);
    }

    boolean isLiftedLogical() {
        final Class left = _left.getType();
        final Class right = _right.getType();
        final Method method = getMethod();
        final ExpressionType kind = getNodeType();

        return (kind == ExpressionType.AndAlso || kind == ExpressionType.OrElse) &&
               TypeUtils.areEquivalent(right, left) &&
               TypeUtils.isAutoUnboxed(left) &&
               method != null &&
               TypeUtils.areEquivalent(method.getReturnType(), TypeUtils.getUnderlyingPrimitive(left));
    }

    boolean isReferenceComparison() {
        final Class left = _left.getType();
        final Class right = _right.getType();
        final Method method = getMethod();
        final ExpressionType kind = getNodeType();

        return (kind == ExpressionType.Equal || kind == ExpressionType.NotEqual) &&
               method == null &&
               !left.isPrimitive() &&
               !right.isPrimitive();
    }

    private Expression reduceVariable() {
        // v (op)= r => v = v (op) r
        final ExpressionType op = getBinaryOpFromAssignmentOp(getNodeType());

        Expression r = Expression.makeBinary(op, _left, _right, getMethod());

        final LambdaExpression conversion = getConversion();

        if (conversion != null) {
            r = Expression.invoke(conversion, r);
        }

        return Expression.assign(_left, r);
    }

    private Expression reduceMember() {
        final MemberExpression member = (MemberExpression)_left;

        if (member.getTarget() == null) {
            // Static member; reduce the same as variable
            return reduceVariable();
        }
        else {
            // left.b (op)= r => temp1 = left; temp2 = temp1.b (op) r;  temp1.b = temp2; temp2
            final ParameterExpression temp1 = variable(member.getTarget().getType(), "temp1");

            // 1. temp1 = left
            final Expression temp1EqualsLeft = Expression.assign(temp1, member.getTarget());

            // 2. temp2 = temp1.b (op) r
            final ExpressionType op = getBinaryOpFromAssignmentOp(getNodeType());

            Expression temp2EqualsTemp1Member = Expression.makeBinary(
                op,
                Expression.makeMemberAccess(temp1, member.getMember()),
                _right,
                getMethod()
            );

            final LambdaExpression conversion = getConversion();

            if (conversion != null) {
                temp2EqualsTemp1Member = Expression.invoke(conversion, temp2EqualsTemp1Member);
            }

            final ParameterExpression temp2 = variable(temp2EqualsTemp1Member.getType(), "temp2");
            temp2EqualsTemp1Member = Expression.assign(temp2, temp2EqualsTemp1Member);

            // 3. temp1.b = temp2

            final Expression temp1MemberEqualsTemp2 = Expression.assign(
                Expression.makeMemberAccess(temp1, member.getMember()),
                temp2
            );

            return Expression.block(
                new ParameterExpression[]{temp1, temp2},
                temp1EqualsLeft,
                temp2EqualsTemp1Member,
                temp1MemberEqualsTemp2,
                temp2
            );
        }
    }

    private static boolean isOpAssignment(final ExpressionType operation) {
        switch (operation) {
            case AddAssign:
            case SubtractAssign:
            case MultiplyAssign:
            case DivideAssign:
            case ModuloAssign:
            case AndAssign:
            case OrAssign:
            case RightShiftAssign:
            case LeftShiftAssign:
            case ExclusiveOrAssign:
                return true;
        }
        return false;
    }

    private static ExpressionType getBinaryOpFromAssignmentOp(final ExpressionType operator) {
        assert isOpAssignment(operator);

        switch (operator) {
            case AddAssign:
                return ExpressionType.Add;
            case SubtractAssign:
                return ExpressionType.Subtract;
            case MultiplyAssign:
                return ExpressionType.Multiply;
            case DivideAssign:
                return ExpressionType.Divide;
            case ModuloAssign:
                return ExpressionType.Modulo;
            case AndAssign:
                return ExpressionType.And;
            case OrAssign:
                return ExpressionType.Or;
            case RightShiftAssign:
                return ExpressionType.RightShift;
            case LeftShiftAssign:
                return ExpressionType.LeftShift;
            case ExclusiveOrAssign:
                return ExpressionType.ExclusiveOr;
            default:
                throw Error.invalidOperator(operator);
        }
    }
}

/**
 * Optimized representation of simple logical expressions:
 * {@code && || == != > < >= <=}
 */
class LogicalBinaryExpression extends BinaryExpression {
    private final ExpressionType _operator;

    public LogicalBinaryExpression(final ExpressionType operator, final Expression left, final Expression right) {
        super(left, right);
        _operator = operator;
    }

    @Override
    public final Class getType() {
        return Boolean.TYPE;
    }

    @Override
    public final ExpressionType getNodeType() {
        return _operator;
    }
}

final class CompareMethodBasedLogicalBinaryExpression extends BinaryExpression {
    private final ExpressionType _operator;
    private final Method _method;

    public CompareMethodBasedLogicalBinaryExpression(
        final ExpressionType operator,
        final Expression left,
        final Expression right,
        final Method method) {

        super(left, right);

        _operator = operator;
        _method = method;
    }

    @Override
    public final Class getType() {
        return Boolean.TYPE;
    }

    @Override
    public final ExpressionType getNodeType() {
        return _operator;
    }

    @Override
    public Method getMethod() {
        return _method;
    }

    @Override
    public boolean canReduce() {
        return super.canReduce();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Expression reduce() {
        return super.reduce();    //To change body of overridden methods use File | Settings | File Templates.
    }
}

/**
 * Optimized assignment node; only holds onto children.
 */
final class AssignBinaryExpression extends BinaryExpression {
    public AssignBinaryExpression(final Expression left, final Expression right) {
        super(left, right);
    }

    @Override
    public final Class getType() {
        return getLeft().getType();
    }

    @Override
    public final ExpressionType getNodeType() {
        return ExpressionType.Assign;
    }
}

/**
 * Coalesce with conversion.  This is not a frequently used node, but
 * rather we want to save every other BinaryExpression from holding onto the
 * null conversion lambda.
 */
final class CoalesceConversionBinaryExpression extends BinaryExpression {
    private final LambdaExpression _conversion;

    CoalesceConversionBinaryExpression(final Expression left, final Expression right, final LambdaExpression conversion) {
        super(left, right);
        _conversion = conversion;
    }

    @Override
    public final LambdaExpression getConversion() {
        return _conversion;
    }

    @Override
    public final Class getType() {
        return getRight().getType();
    }

    @Override
    public final ExpressionType getNodeType() {
        return ExpressionType.Coalesce;
    }
}

final class OpAssignMethodConversionBinaryExpression extends MethodBinaryExpression {
    private final LambdaExpression _conversion;

    OpAssignMethodConversionBinaryExpression(
        final ExpressionType nodeType,
        final Expression left,
        final Expression right,
        final Class type,
        final Method method,
        final LambdaExpression conversion) {

        super(nodeType, left, right, type, method);

        _conversion = conversion;
    }

    @Override
    public final LambdaExpression getConversion() {
        return _conversion;
    }
}

class SimpleBinaryExpression extends BinaryExpression {
    private final ExpressionType _nodeType;
    private final Class _type;

    SimpleBinaryExpression(
        final ExpressionType nodeType,
        final Expression left,
        final Expression right,
        final Class type) {

        super(left, right);

        _nodeType = nodeType;
        _type = type;
    }

    @Override
    public final ExpressionType getNodeType() {
        return _nodeType;
    }

    @Override
    public final Class getType() {
        return _type;
    }
}

class MethodBinaryExpression extends SimpleBinaryExpression {
    private final Method _method;

    public MethodBinaryExpression(
        final ExpressionType operator,
        final Expression left,
        final Expression right,
        final Class type,
        final Method method) {

        super(operator, left, right, type);
        _method = method;
    }

    @Override
    public final Method getMethod() {
        return _method;
    }
}

