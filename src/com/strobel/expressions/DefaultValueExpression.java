package com.strobel.expressions;

import com.strobel.reflection.Type;

/**
 * @author Mike Strobel
 */
public final class DefaultValueExpression extends Expression {
    private final Type _type;

    DefaultValueExpression(final Type type) {
        _type = type;
    }

    @Override
    public ExpressionType getNodeType() {
        return ExpressionType.DefaultValue;
    }

    @Override
    public Type getType() {
        return _type;
    }

    @Override
    protected Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitDefaultValue(this);
    }
}
