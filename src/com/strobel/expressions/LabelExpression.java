package com.strobel.expressions;

import com.strobel.reflection.Type;

/**
 * @author Mike Strobel
 */
public final class LabelExpression extends Expression {
    private final Expression _defaultValue;
    private final LabelTarget _target;

    public LabelExpression(final LabelTarget target, final Expression defaultValue) {
        _target = target;
        _defaultValue = defaultValue;
    }

    public final Expression getDefaultValue() {
        return _defaultValue;
    }

    public final LabelTarget getTarget() {
        return _target;
    }

    @Override
    public final ExpressionType getNodeType() {
        return ExpressionType.Label;
    }

    @Override
    public final Type<?> getType() {
        return _target.getType();
    }

    @Override
    protected final Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitLabel(this);
    }

    public final LabelExpression update(final LabelTarget target, final Expression defaultValue) {
        if (target == _target && defaultValue == _defaultValue) {
            return this;
        }
        return label(target, defaultValue);
    }
}
