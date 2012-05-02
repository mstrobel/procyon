package com.strobel.expressions;

import com.strobel.util.ContractUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author Mike Strobel
 */
public class MethodCallExpression extends Expression implements IArgumentProvider {
    private final Method _method;

    MethodCallExpression(final Method method) {
        _method = method;
    }

    public final Method getMethod() {
        return _method;
    }

    public Expression getTarget() {
        return null;
    }

    public final List<Expression> getArguments() {
        return getOrMakeArguments();
    }

    List<Expression> getOrMakeArguments() {
        throw ContractUtils.unreachable();
    }

    @Override
    public int getArgumentCount() {
        throw ContractUtils.unreachable();
    }

    @Override
    public Expression getArgument(final int index) {
        throw ContractUtils.unreachable();
    }

    MethodCallExpression rewrite(final Expression target, final List<Expression> arguments) {
        throw ContractUtils.unreachable();
    }
}

final class MethodCallExpressionN extends MethodCallExpression {
    private List<Expression> _arguments;

    MethodCallExpressionN(final Method method, final List<Expression> arguments) {
        super(method);
        _arguments = arguments;
    }

    @Override
    public final int getArgumentCount() {
        return _arguments.size();
    }

    @Override
    public final Expression getArgument(final int index) {
        return _arguments.get(index);
    }

    @Override
    final List<Expression> getOrMakeArguments() {
        return (_arguments = ensureUnmodifiable(_arguments));
    }

    @Override
    final MethodCallExpression rewrite(final Expression target, final List<Expression> arguments) {
        assert target == null;
        assert arguments == null || arguments.size() == _arguments.size();

        return call(getMethod(), arguments != null ? arguments : _arguments);
    }
}

final class InstanceMethodCallExpressionN extends MethodCallExpression {
    private final Expression _target;
    private List<Expression> _arguments;

    InstanceMethodCallExpressionN(final Method method, final Expression target, final List<Expression> arguments) {
        super(method);
        _target = target;
        _arguments = arguments;
    }

    @Override
    public final Expression getTarget() {
        return _target;
    }

    @Override
    public final int getArgumentCount() {
        return _arguments.size();
    }

    @Override
    public final Expression getArgument(final int index) {
        return _arguments.get(index);
    }

    @Override
    final List<Expression> getOrMakeArguments() {
        return (_arguments = ensureUnmodifiable(_arguments));
    }

    @Override
    final MethodCallExpression rewrite(final Expression target, final List<Expression> arguments) {
        assert target != null;
        assert arguments == null || arguments.size() == _arguments.size();

        return call(target, getMethod(), arguments != null ? arguments : _arguments);
    }
}