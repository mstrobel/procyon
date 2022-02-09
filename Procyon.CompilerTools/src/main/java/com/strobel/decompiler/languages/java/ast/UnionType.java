package com.strobel.decompiler.languages.java.ast;

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;

public class UnionType extends AstType {
    public final static TokenRole UNION_TOKEN = new TokenRole("|", TokenRole.FLAG_OPERATOR);

    public UnionType(final AstType... alternatives) {
        for (final AstType ifType : alternatives) {
            addChild(ifType, Roles.TYPE);
        }
    }

    public final AstNodeCollection<AstType> getAlternatives() {
        return getChildrenByRole(Roles.TYPE);
    }

    @Override
    public TypeReference toTypeReference() {
        final AstNodeCollection<AstType> alternatives = getAlternatives();
        final TypeReference[] alternativeTypes = alternatives.isEmpty() ? TypeReference.EMPTY_REFERENCES : new TypeReference[alternatives.size()];

        int i = 0;

        for (final AstType alternative : alternatives) {
            alternativeTypes[i++] = alternative.toTypeReference();
        }

        return com.strobel.assembler.metadata.UnionType.of(alternativeTypes);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitUnionType(this, data);
    }

    @Override
    public String toString() {
        final AstNodeCollection<AstType> alternatives = getAlternatives();
        final StringBuilder sb = new StringBuilder();

        boolean needToken = false;

        for (final AstType alternative : alternatives) {
            if (needToken) {
                sb.append(" | ");
            }
            sb.append(alternative);
            needToken = true;
        }

        return sb.toString();
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        return other instanceof UnionType &&
               getAlternatives().matches(((UnionType) other).getAlternatives(), match);
    }
}
