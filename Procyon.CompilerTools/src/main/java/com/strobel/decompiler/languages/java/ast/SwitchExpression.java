package com.strobel.decompiler.languages.java.ast;

import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.Role;

public class SwitchExpression extends Expression{
    public final static SwitchExpression NULL = new NullSwitchExpression();

    public final static Role<SwitchExpressionArm> ARM_ROLE = new Role<>("SwitchExpressionArm", SwitchExpressionArm.class, SwitchExpressionArm.NULL);
    public final static Role<Expression> GOVERNING_EXPRESSION_ROLE = new Role<>("GoverningExpression", Expression.class, Expression.NULL);

    public SwitchExpression() {
        super(MYSTERY_OFFSET);
    }

    public SwitchExpression(final int offset) {
        super(offset);
    }
    
    public final Expression getGoverningExpression() {
        return getChildByRole(GOVERNING_EXPRESSION_ROLE);
    }

    public final void setGoverningExpression(final Expression value) {
        setChildByRole(GOVERNING_EXPRESSION_ROLE, value);
    }

    public final AstNodeCollection<SwitchExpressionArm> getArms() {
        return getChildrenByRole(ARM_ROLE);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitSwitchExpression(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof SwitchExpression) {
            final SwitchExpression otherStatement = (SwitchExpression) other;

            return !otherStatement.isNull() &&
                   getGoverningExpression().matches(otherStatement.getGoverningExpression(), match) &&
                   getArms().matches(otherStatement.getArms(), match);
        }

        return false;
    }

    private static final class NullSwitchExpression extends SwitchExpression {
        public NullSwitchExpression() {
            super(Expression.MYSTERY_OFFSET);
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
