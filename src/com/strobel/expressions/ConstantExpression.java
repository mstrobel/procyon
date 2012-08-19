package com.strobel.expressions;

import com.strobel.reflection.Type;
import com.strobel.reflection.Types;
import com.strobel.util.TypeUtils;

/**
 * Represents an expression that has a constant value.
 *
 * @author Mike Strobel
 */
public class ConstantExpression extends Expression {
    private final Object _value;

    ConstantExpression(final Object value) {
        _value = value;
    }

    public final Object getValue() {
        return _value;
    }

    @Override
    public Type getType() {
        if (_value == null) {
            return Types.Object;
        }
        final Type<?> valueType = Type.getType(_value);
        return TypeUtils.getUnderlyingPrimitiveOrSelf(valueType);
    }

    @Override
    public ExpressionType getNodeType() {
        return ExpressionType.Constant;
    }

    @Override
    protected Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitConstant(this);
    }

    static ConstantExpression make(final Object value, final Type type) {
        if ((value == null && type == Types.Object) || (value != null && Type.of(value.getClass()) == type)) {
            return new ConstantExpression(value);
        }
        else {
            return new TypedConstantExpression(value, type);
        }
    }
}

class TypedConstantExpression extends ConstantExpression {
    private final Type _type;

    TypedConstantExpression(final Object value, final Type type) {
        super(value);
        _type = type;
    }

    @Override
    public final Type getType() {
        return _type;
    }
}