package com.strobel.expressions;

import com.strobel.reflection.Type;
import com.strobel.reflection.Types;

/**
 * @author Mike Strobel
 */
public final class ConcatExpression extends Expression {
    private final ExpressionList<? extends Expression> _operands;

    public ConcatExpression(final ExpressionList<? extends Expression> operands) {
        _operands = operands;
    }

    public ExpressionList<? extends Expression> getOperands() {
        return _operands;
    }

    @Override
    public ExpressionType getNodeType() {
        return ExpressionType.Extension;
    }

    @Override
    public Type getType() {
        return Types.String;
    }

    @Override
    protected Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitConcat(this);
    }

    public final ConcatExpression update(final ExpressionList<? extends Expression> operands) {
        if (operands == _operands) {
            return this;
        }
        return concat(operands);
    }

    final ConcatExpression rewrite(final ExpressionList<? extends Expression> operands) {
        assert operands == null || operands.size() == _operands.size();
        return concat(operands);
    }

    @Override
    protected Expression visitChildren(final ExpressionVisitor visitor) {
        return update(visitor.visit(getOperands()));
    }

    @Override
    public boolean canReduce() {
        return true;
    }

    @Override
    public Expression reduce() {
        final ParameterExpression sb = variable(Types.StringBuilder);
        final ExpressionList<? extends Expression> operands = _operands;
        final Expression[] body = new Expression[operands.size() + 2];

        body[0] = assign(
            sb,
            makeNew(Types.StringBuilder.getConstructor())
        );

        for (int i = 0, n = operands.size(); i < n; i++) {
            body[i + 1] = call(sb, "append", operands.get(i));
        }

        body[body.length - 1] = call(sb, "toString");

        return block(
            new ParameterExpression[] { sb },
            body
        );
    }
}
