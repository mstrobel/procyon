package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import javax.lang.model.type.TypeKind;

/**
 * @author Mike Strobel
 */
final class CapturedType<T> extends GenericParameter<T> implements ICapturedType {
    final static String CapturedName = "<captured wildcard>";

    private final Type<?> _wildcard;

    CapturedType(final Type<?> declaringType, final Type<?> upperBound, final Type<?> lowerBound, final Type<?> wildcard) {
        super(CapturedName, declaringType, upperBound, lowerBound, -1);

        if (!wildcard.isWildcardType()) {
            throw new IllegalArgumentException("Argument 'wildcard' must be a wildcard type.");
        }

        _wildcard = VerifyArgument.notNull(wildcard, "wildcard");
    }

    @Override
    public Type<?> getWildcard() {
        return _wildcard;
    }

    @Override
    public boolean isGenericParameter() {
        return false;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.WILDCARD;
    }

    @Override
    public boolean isWildcardType() {
        return true;
    }
}
