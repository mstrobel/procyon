package com.strobel.decompiler.languages.java.ast;

import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;

public final class OuterTypeReferenceExpression extends Expression {
    public final static TokenRole THIS_KEYWORD_ROLE = new TokenRole("this", TokenRole.FLAG_KEYWORD);

    public OuterTypeReferenceExpression() {
    }

    public OuterTypeReferenceExpression(final AstType outerType) {
        setOuterType(outerType);
    }

    public final AstType getOuterType() {
        return getChildByRole(Roles.TYPE);
    }

    public final void setOuterType(final AstType value) {
        setChildByRole(Roles.TYPE, value);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitOuterTypeReferenceExpression(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        return other instanceof OuterTypeReferenceExpression &&
               getOuterType().matches(((OuterTypeReferenceExpression) other).getOuterType(), match);
    }
}
