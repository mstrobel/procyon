package com.strobel.expressions;

import java.util.Arrays;
import java.util.List;

/**
 * @author Mike Strobel
 */
public final class InvocationExpression extends Expression implements IArgumentProvider {
    private List<Expression> _arguments;
    private final Expression _lambda;
    private final Class _returnType;

    InvocationExpression(final Expression lambda, final List<Expression> arguments, final Class returnType) {
        _lambda = lambda;
        _arguments = arguments;
        _returnType = returnType;
    }

    @Override
    public final Class getType() {
        return _returnType;
    }

    @Override
    public final ExpressionType getNodeType() {
        return ExpressionType.Invoke;
    }

    public Expression getExpression() {
        return _lambda;
    }

    public List<Expression> getArguments() {
        return (_arguments = ensureUnmodifiable(_arguments));
    }

    @Override
    public int getArgumentCount() {
        return _arguments.size();
    }

    @Override
    public Expression getArgument(final int index) {
        return _arguments.get(index);
    }

    public InvocationExpression update(final Expression lambda, final List<Expression> arguments) {
        if (lambda == _lambda && arguments == _arguments) {
            return this;
        }
        return invoke(lambda, arguments);
    }

    @Override
    protected Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitInvocation(this);
    }

    InvocationExpression rewrite(final Expression lambda, final Expression[] arguments) {
        assert lambda != null;
        assert arguments == null || arguments.length == _arguments.size();

        return Expression.invoke(lambda, arguments != null ? Arrays.asList(arguments) : _arguments);
    }
}
