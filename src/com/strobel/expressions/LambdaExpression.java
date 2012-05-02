package com.strobel.expressions;

import com.strobel.reflection.emit.MethodBuilder;

import java.util.List;

/**
 * @author Mike Strobel
 */
public class LambdaExpression extends Expression {
    private final String _name;
    private final Expression _body;
    private final List<ParameterExpression> _parameters;
    private final Class _interfaceType;
    private final boolean _tailCall;
    private final Class<?> _returnType;

    LambdaExpression(
        final Class interfaceType,
        final String name,
        final Expression body,
        final boolean tailCall,
        final List<ParameterExpression> parameters) {

        assert interfaceType != null;

        _name = name;
        _body = body;
        _tailCall = tailCall;
        _parameters = parameters;
        _interfaceType = interfaceType;
        _returnType = _interfaceType.getMethods()[0].getReturnType();
    }

    @Override
    public final Class getType() {
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

    public final List<ParameterExpression> getParameters() {
        return _parameters;
    }

    public final Class getReturnType() {
        return _returnType;
    }

    public final boolean isTailCall() {
        return _tailCall;
    }

    public final Delegate compile() {
        return LambdaCompiler.compileDelegate(this);
    }

    public final void CompileToMethod(final MethodBuilder methodBuilder) {
        LambdaCompiler.compile(this, methodBuilder);
    }
}
