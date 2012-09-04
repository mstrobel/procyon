package com.strobel.expressions;

import com.strobel.core.ReadOnlyList;
import com.strobel.reflection.Type;

/**
 * @author Mike Strobel
 */
public final class TryExpression extends Expression {
    private final Type _type;
    private final Expression _body;
    private final ReadOnlyList<CatchBlock> _handlers;
    private final Expression _finallyBlock;

    public TryExpression(
        final Type type,
        final Expression body,
        final ReadOnlyList<CatchBlock> handlers,
        final Expression finallyBlock) {

        _type = type;
        _body = body;
        _handlers = handlers;
        _finallyBlock = finallyBlock;
    }

    public final Expression getBody() {
        return _body;
    }

    public final ReadOnlyList<CatchBlock> getHandlers() {
        return _handlers;
    }

    public final Expression getFinallyBlock() {
        return _finallyBlock;
    }

    @Override
    public final ExpressionType getNodeType() {
        return ExpressionType.Try;
    }

    @Override
    public final Type<?> getType() {
        return _type;
    }

    @Override
    protected final Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitTry(this);
    }

    public final TryExpression update(
        final Expression body,
        final ReadOnlyList<CatchBlock> handlers,
        final Expression finallyBlock) {

        if (body == _body && handlers == _handlers && finallyBlock == _finallyBlock) {
            return this;
        }

        return makeTry(getType(), body, handlers, finallyBlock);
    }
}
