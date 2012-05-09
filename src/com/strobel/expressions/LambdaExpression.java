package com.strobel.expressions;

import com.strobel.reflection.MethodBuilder;
import com.strobel.reflection.Type;

/**
 * @author Mike Strobel
 */
public final class LambdaExpression<T> extends Expression {
    private final String _name;
    private final Expression _body;
    private final ParameterExpressionList _parameters;
    private final Type<T> _interfaceType;
    private final boolean _tailCall;
    private final Type _returnType;

    LambdaExpression(
        final Type<T> interfaceType,
        final String name,
        final Expression body,
        final boolean tailCall,
        final ParameterExpressionList parameters) {

        assert interfaceType != null;

        _name = name;
        _body = body;
        _tailCall = tailCall;
        _parameters = parameters;
        _interfaceType = interfaceType;
        _returnType = _interfaceType.getMethods().get(0).getReturnType();
    }

    @Override
    public final Type<T> getType() {
        return _interfaceType;
    }

    @Override
    public ExpressionType getNodeType() {
        return ExpressionType.Lambda;
    }

    public final String getName() {
        return _name;
    }

    public final Expression getBody() {
        return _body;
    }

    public final ParameterExpressionList getParameters() {
        return _parameters;
    }

    public final Type getReturnType() {
        return _returnType;
    }

    public final boolean isTailCall() {
        return _tailCall;
    }

    public final LambdaExpression<T> update(final Expression body, final ParameterExpressionList parameters) {
        if (body == _body && parameters == _parameters) {
            return this;
        }
        return Expression.lambda(_interfaceType, getName(), body, isTailCall(), parameters);
    }

    @Override
    protected Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitLambda(this);
    }

    @SuppressWarnings("unchecked")
    final LambdaExpression<T> accept(final StackSpiller spiller) {
        return (LambdaExpression<T>)spiller.rewrite(this);
    }

    public final T compile() {
        return compileDelegate().getTarget();
    }

    public final Delegate<T> compileDelegate() {
        return LambdaCompiler.compile(this);
    }

    public final void compileToMethod(final MethodBuilder methodBuilder) {
        LambdaCompiler.compile(this, methodBuilder);
    }
}
