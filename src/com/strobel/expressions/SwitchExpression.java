package com.strobel.expressions;

import com.strobel.core.ReadOnlyList;
import com.strobel.core.VerifyArgument;
import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.Type;

/**
 * @author strobelm
 */
public final class SwitchExpression extends Expression {
    private final Type _type;
    private final Expression _switchValue;
    private final ReadOnlyList<SwitchCase> _cases;
    private final Expression _defaultBody;
    private final MethodInfo _comparison;

    public SwitchExpression(
        final Type type,
        final Expression switchValue,
        final Expression defaultBody,
        final MethodInfo comparison,
        final ReadOnlyList<SwitchCase> cases) {

        _type = VerifyArgument.notNull(type, "type");
        _switchValue = VerifyArgument.notNull(switchValue, "switchValue");
        _defaultBody = defaultBody;
        _comparison = comparison;
        _cases = VerifyArgument.notEmpty(cases, "cases");
    }

    public final Expression getSwitchValue() {
        return _switchValue;
    }

    public final ReadOnlyList<SwitchCase> getCases() {
        return _cases;
    }

    public final Expression getDefaultBody() {
        return _defaultBody;
    }

    public final MethodInfo getComparison() {
        return _comparison;
    }

    @Override
    public final Type getType() {
        return _type;
    }

    @Override
    public final ExpressionType getNodeType() {
        return ExpressionType.Switch;
    }

    @Override
    protected final Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitSwitch(this);
    }

    public final SwitchExpression update(
        final Expression switchValue,
        final ReadOnlyList<SwitchCase> cases,
        final Expression defaultBody) {

        if (switchValue == _switchValue && cases == _cases && defaultBody == _defaultBody) {
            return this;
        }

        return Expression.makeSwitch(_type, switchValue, defaultBody, _comparison, cases);
    }
}
