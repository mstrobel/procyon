/*
 * SwitchExpression.java
 *
 * Copyright (c) 2012 Mike Strobel
 *
 * This source code is based on the Dynamic Language Runtime from Microsoft,
 *   Copyright (c) Microsoft Corporation.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

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
    private final SwitchOptions _options;

    public SwitchExpression(
        final Type type,
        final Expression switchValue,
        final Expression defaultBody,
        final MethodInfo comparison,
        final ReadOnlyList<SwitchCase> cases,
        final SwitchOptions options) {

        _type = VerifyArgument.notNull(type, "type");
        _switchValue = VerifyArgument.notNull(switchValue, "switchValue");
        _defaultBody = defaultBody;
        _comparison = comparison;
        _cases = VerifyArgument.notEmpty(cases, "cases");
        _options = options != null ? options : SwitchOptions.Default;
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

    public final SwitchOptions getOptions() {
        return _options;
    }

    @Override
    public final Type<?> getType() {
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
        final Expression defaultBody,
        final SwitchOptions options) {

        if (switchValue == _switchValue && options == _options && cases == _cases && defaultBody == _defaultBody) {
            return this;
        }

        return Expression.makeSwitch(_type, switchValue, _options, defaultBody, _comparison, cases);
    }
}
