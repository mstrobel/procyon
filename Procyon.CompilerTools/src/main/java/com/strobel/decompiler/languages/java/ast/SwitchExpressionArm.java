package com.strobel.decompiler.languages.java.ast;

import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.Role;

public class SwitchExpressionArm extends AstNode {
    public final static SwitchExpressionArm NULL = new NullSwitchExpressionArm();

    public final static Role<Expression> VALUE_ROLE = new Role<>("Value", Expression.class, Expression.NULL);
    public final static TokenRole CASE_KEYWORD_ROLE = CaseLabel.CASE_KEYWORD_ROLE;
    public final static TokenRole DEFAULT_KEYWORD_ROLE = CaseLabel.DEFAULT_KEYWORD_ROLE;
    public final static TokenRole ARROW_ROLE = new TokenRole("->", TokenRole.FLAG_OPERATOR);
    public final static TokenRole COLON_ROLE = new TokenRole(":", TokenRole.FLAG_DELIMITER);

    public final AstNodeCollection<Statement> getStatements() {
        return getChildrenByRole(Roles.EMBEDDED_STATEMENT);
    }

    public final AstNodeCollection<Expression> getValues() {
        return getChildrenByRole(VALUE_ROLE);
    }

    private boolean defaultCase;
    private boolean isClassicStyle;

    public boolean isDefaultCase() {
        return defaultCase;
    }

    public void setDefaultCase(final boolean defaultCase) {
        this.defaultCase = defaultCase;
    }

    public boolean isClassicStyle() {
        return isClassicStyle;
    }

    public void setClassicStyle(final boolean classicStyle) {
        isClassicStyle = classicStyle;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.UNKNOWN;
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitSwitchExpressionArm(this, data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Role<? extends SwitchExpressionArm> getRole() {
        return (Role<? extends SwitchExpressionArm>) super.getRole();
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof SwitchExpressionArm) {
            final SwitchExpressionArm otherSection = (SwitchExpressionArm) other;

            return !otherSection.isNull() &&
                   getValues().matches(otherSection.getValues(), match) &&
                   getStatements().matches(otherSection.getStatements(), match);
        }

        return false;
    }

    private static final class NullSwitchExpressionArm extends SwitchExpressionArm {
        public NullSwitchExpressionArm() {
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
