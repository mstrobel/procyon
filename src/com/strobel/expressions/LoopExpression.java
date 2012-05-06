package com.strobel.expressions;

import com.strobel.reflection.PrimitiveTypes;
import com.strobel.reflection.Type;

/**
 * @author Mike Strobel
 */
public final class LoopExpression extends Expression {
    private final Expression _body;
    private final LabelTarget _breakLabel;
    private final LabelTarget _continueLabel;

    LoopExpression(final Expression body, final LabelTarget breakLabel, final LabelTarget continueLabel) {
        _body = body;
        _breakLabel = breakLabel;
        _continueLabel = continueLabel;
    }

    public final Expression getBody() {
        return _body;
    }

    public final LabelTarget getBreakLabel() {
        return _breakLabel;
    }

    public final LabelTarget getContinueLabel() {
        return _continueLabel;
    }

    @Override
    public final ExpressionType getNodeType() {
        return ExpressionType.Loop;
    }

    @Override
    public final Type getType() {
        return _breakLabel == null ? PrimitiveTypes.Void : _breakLabel.getType();
    }

    @Override
    protected final Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitLoop(this);
    }

    public final LoopExpression update(final LabelTarget breakLabel, final LabelTarget continueLabel, final Expression body) {
        if (breakLabel == _breakLabel && continueLabel == _continueLabel && body == _body) {
            return this;
        }
        return loop(body, breakLabel, continueLabel);
    }
}
