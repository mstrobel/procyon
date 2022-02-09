package com.strobel.assembler.metadata;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public final class UnionType extends TypeReference implements IUnionType {
    private final List<TypeReference> _alternatives;
    private final String _simpleName;

    private UnionType(final TypeReference... alternatives) {
        _alternatives = ArrayUtilities.asUnmodifiableList(VerifyArgument.noNullElementsAndNotEmpty(alternatives, "alternatives"));
        _simpleName = appendName(new StringBuilder(), false, false).toString();
        setName(_simpleName);
    }

    public List<TypeReference> getAlternatives() {
        return _alternatives;
    }

    @Override
    public String getSimpleName() {
        return _simpleName;
    }

    @Override
    protected StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        for (int i = 0; i < _alternatives.size(); i++) {
            final TypeReference t = _alternatives.get(i);

            if (i > 0) {
                sb.append(" | ");
            }

            t.appendName(sb, fullName, dottedName);
        }
        return sb;
    }

    @Override
    public <R, P> R accept(final TypeMetadataVisitor<P, R> visitor, final P parameter) {
        return visitor.visitClassType(this, parameter);
    }

    @Override
    public boolean isUnionType() {
        return true;
    }

    public static TypeReference of(final TypeReference... types) {
        VerifyArgument.noNullElementsAndNotEmpty(types, "types");

        if (types.length == 1) {
            return types[0];
        }

        TypeReference[] alternatives = Arrays.copyOf(types, types.length);

        for (int i = 0; i < alternatives.length; i++) {
            final TypeReference alternative = alternatives[i];

            if (alternative instanceof IUnionType) {
                final TypeReference[] innerAlternatives = ((IUnionType) alternative).getAlternatives().toArray(EMPTY_REFERENCES);
                alternatives = ArrayUtilities.insert(ArrayUtilities.remove(alternatives, i), i, innerAlternatives);
            }
        }

        alternatives = new LinkedHashSet<>(Arrays.asList(alternatives)).toArray(EMPTY_REFERENCES);

        if (alternatives.length == 1) {
            return alternatives[0];
        }

        return new UnionType(alternatives);
    }
}
