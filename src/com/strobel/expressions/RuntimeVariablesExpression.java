package com.strobel.expressions;

import com.strobel.reflection.Type;

/**
 * @author Mike Strobel
 */
public final class RuntimeVariablesExpression extends Expression {
    private final ParameterExpressionList _variables;

    RuntimeVariablesExpression(final ParameterExpressionList variables) {
        _variables = variables;
    }

    public final ParameterExpressionList getVariables() {
        return _variables;
    }

    @Override
    public final Type getType() {
        return Type.of(IRuntimeVariables.class);
    }

    @Override
    public final ExpressionType getNodeType() {
        return ExpressionType.RuntimeVariables;
    }

    @Override
    protected Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitRuntimeVariables(this);
    }

    public final RuntimeVariablesExpression update(final ParameterExpressionList variables) {
        if (variables == getVariables()) {
            return this;
        }
        return runtimeVariables(variables);
    }
}
