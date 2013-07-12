package com.strobel.decompiler.languages.java.ast;

import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;

public class LabeledStatement extends Statement {
    public LabeledStatement() {
    }

    public LabeledStatement(final String name) {
        setLabel(name);
    }

    public LabeledStatement(final String name, final Statement statement) {
        setLabel(name);
        setStatement(statement);
    }

    public final String getLabel() {
        return getChildByRole(Roles.LABEL).getName();
    }

    public final void setLabel(final String value) {
        setChildByRole(Roles.LABEL, Identifier.create(value));
    }

    public final Identifier getLabelToken() {
        return getChildByRole(Roles.LABEL);
    }

    public final void setLabelToken(final Identifier value) {
        setChildByRole(Roles.LABEL, value);
    }

    public final JavaTokenNode getColonToken() {
        return getChildByRole(Roles.COLON);
    }

    public final Statement getStatement() {
        return getChildByRole(Roles.EMBEDDED_STATEMENT);
    }

    public final void setStatement(final Statement value) {
        setChildByRole(Roles.EMBEDDED_STATEMENT, value);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitLabeledStatement(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof LabeledStatement) {
            final LabeledStatement otherStatement = (LabeledStatement) other;

            return matchString(getLabel(), otherStatement.getLabel()) &&
                   getStatement().matches(otherStatement.getStatement(), match);
        }

        return false
    ;}
}
