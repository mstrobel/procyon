package com.strobel.expressions;

import com.strobel.reflection.Type;

/**
 * @author Mike Strobel
 */
public final class CatchBlock {
    private final Type _test;
    private final ParameterExpression _variable;
    private final Expression _body;
    private final Expression _filter;

    CatchBlock(final Type test, final ParameterExpression variable, final Expression body, final Expression filter) {
        _test = test;
        _variable = variable;
        _body = body;
        _filter = filter;
    }

    public final Type getTest() {
        return _test;
    }

    public final ParameterExpression getVariable() {
        return _variable;
    }

    public final Expression getBody() {
        return _body;
    }

    public final Expression getFilter() {
        return _filter;
    }

    @Override
    public final String toString() {
        return ExpressionStringBuilder.catchBlockToString(this);
    }

    public final CatchBlock update(final ParameterExpression variable, final Expression filter, final Expression body) {
        if (variable == _variable && filter == _filter && body == _body) {
            return this;
        }
        return Expression.makeCatch(_test, variable, body, filter);
    }
}
