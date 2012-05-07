package com.strobel.expressions;

import com.strobel.core.VerifyArgument;

/**
 * @author strobelm
 */
public final class SwitchCase {
    private final ExpressionList<? extends Expression> _testValues;
    private final Expression _body;

    SwitchCase(final Expression body, final ExpressionList<? extends Expression> testValues) {
        _body = VerifyArgument.notNull(body, "body");
        _testValues = VerifyArgument.notNull(testValues, "testValues");
    }

    public final ExpressionList<? extends Expression> getTestValues() {
        return _testValues;
    }

    public final Expression getBody() {
        return _body;
    }

    @Override
    public final String toString() {
        return ExpressionStringBuilder.switchCaseToString(this);
    }

    public final SwitchCase update(final ExpressionList<? extends Expression> testValues, final Expression body) {
        if (testValues == _testValues && body == _body) {
            return this;
        }
        return Expression.switchCase(body, testValues);
    }
}
