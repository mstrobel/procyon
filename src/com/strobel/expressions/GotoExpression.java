package com.strobel.expressions;

import com.strobel.reflection.Type;

/**
 * @author Mike Strobel
 */
public final class GotoExpression extends Expression {
    private final GotoExpressionKind _kind;
    private final Expression _value;
    private final LabelTarget _target;
    private final Type _type;

    GotoExpression(final GotoExpressionKind kind, final LabelTarget target, final Expression value, final Type type) {
        _kind = kind;
        _target = target;
        _value = value;
        _type = type;
    }

    public final GotoExpressionKind getKind() {
        return _kind;
    }

    public final Expression getValue() {
        return _value;
    }

    public final LabelTarget getTarget() {
        return _target;
    }

    @Override
    public final ExpressionType getNodeType() {
        return ExpressionType.Goto;
    }

    @Override
    public final Type getType() {
        return _type;
    }

    @Override
    protected final Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitGoto(this);
    }

    public final GotoExpression update(final LabelTarget target, final Expression value) {
        if (target == _target && value == _value) {
            return this;
        }
        return makeGoto(_kind, target, value, _type);
    }
}
