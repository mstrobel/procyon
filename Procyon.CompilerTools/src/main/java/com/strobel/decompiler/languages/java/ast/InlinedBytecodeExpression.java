package com.strobel.decompiler.languages.java.ast;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.ast.AstCode;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.Role;

public class InlinedBytecodeExpression extends Expression {
    public final static Role<Expression> OPERAND_ROLE = new Role<>("Operand", Expression.class, Expression.NULL);
    public final static InlinedBytecodeExpression NULL = new NullInlinedBytecodeExpression();

    private final AstCode _code;

    public InlinedBytecodeExpression(final int offset, final AstCode code, final Object... operands) {
        super(offset);

        _code = VerifyArgument.notNull(code, "code");

        if (ArrayUtilities.isNullOrEmpty(operands) || operands.length == 1 && operands[0] == null) {
            return;
        }

        final AstNodeCollection<Expression> operandNodes = getOperands();

        for (final Object operand : operands) {
            if (operand != null) {
                operandNodes.add(operand instanceof Expression ? (Expression) operand : new BytecodeConstant(operand));
            }
            else {
                operandNodes.add(BytecodeConstant.NULL);
            }
        }
    }

    public InlinedBytecodeExpression(final AstCode code, final Object... operands) {
        this(MYSTERY_OFFSET, code, operands);
    }

    public final AstCode getCode() {
        return _code;
    }

    public final AstNodeCollection<Expression> getOperands() {
        return getChildrenByRole(OPERAND_ROLE);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitInlinedBytecode(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof InlinedBytecodeExpression) {
            final InlinedBytecodeExpression otherCode = (InlinedBytecodeExpression) other;

            return !otherCode.isNull() &&
                   getCode() == otherCode.getCode() &&
                   getOperands().matches(otherCode.getOperands(), match);
        }

        return false;
    }

    private static final class NullInlinedBytecodeExpression extends InlinedBytecodeExpression {
        public NullInlinedBytecodeExpression() {
            super(AstCode.Nop);
        }

        @Override
        public final boolean isNull() {
            return true;
        }

        @Override
        public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
            return null;
        }

        @Override
        public boolean matches(final INode other, final Match match) {
            return other == null || other.isNull();
        }
    }
}

