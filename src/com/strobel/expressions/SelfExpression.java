package com.strobel.expressions;

import com.strobel.reflection.Type;

/**
 * @author Mike Strobel
 */
final class SelfExpression extends ParameterExpression {
    private final Type<?> _type;

    SelfExpression(final Type<?> type) {
        super("this");
        _type = type;
    }

    @Override
    public Type getType() {
        return _type;
    }
}
