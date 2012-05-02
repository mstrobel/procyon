package com.strobel.expressions;

/**
 * Represents an expression that has a constant value.
 *
 * @author Mike Strobel
 */
public class ConstantExpression extends Expression {
    private final Object _value;

    public ConstantExpression(final Object value) {
        _value = value;
    }

    public final Object getValue() {
        return _value;
    }

    @Override
    public Class getType() {
        if (_value == null) {
            return Object.class;
        }
        return _value.getClass();
    }

    @Override
    public ExpressionType getNodeType() {
        return ExpressionType.Constant;
    }

    @Override
    protected Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitConstant(this);
    }

    static ConstantExpression make(final Object value, final Class type) {
        if ((value == null && type == Object.class) || (value != null && value.getClass() == type)) {
            return new ConstantExpression(value);
        }
        else {
            return new TypedConstantExpression(value, type);
        }
    }
}

class TypedConstantExpression extends ConstantExpression {
    private final Class _type;

    TypedConstantExpression(final Object value, final Class type) {
        super(value);
        _type = type;
    }

    @Override
    public final Class getType() {
        return _type;
    }
}