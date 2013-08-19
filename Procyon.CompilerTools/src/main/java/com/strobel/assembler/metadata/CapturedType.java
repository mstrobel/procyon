package com.strobel.assembler.metadata;

import com.strobel.core.VerifyArgument;

public final class CapturedType extends TypeReference implements ICapturedType {
    private final TypeReference _superBound;
    private final TypeReference _extendsBound;
    private final WildcardType _wildcard;

    CapturedType(final TypeReference superBound, final TypeReference extendsBound, final WildcardType wildcard) {
        _superBound = superBound != null ? superBound : BuiltinTypes.Bottom;
        _extendsBound = extendsBound != null ? extendsBound : BuiltinTypes.Object;
        _wildcard = VerifyArgument.notNull(wildcard, "wildcard");
    }

    @Override
    public final WildcardType getWildcard() {
        return _wildcard;
    }

    @Override
    public final TypeReference getExtendsBound() {
        return _extendsBound;
    }

    @Override
    public final TypeReference getSuperBound() {
        return _superBound;
    }

    @Override
    public final boolean hasExtendsBound() {
        return _extendsBound != null &&
               !MetadataHelper.isSameType(_extendsBound, BuiltinTypes.Object);
    }

    @Override
    public final boolean hasSuperBound() {
        return _superBound != BuiltinTypes.Bottom;
    }

    @Override
    public final boolean isBoundedType() {
        return true;
    }

    @Override
    public String getSimpleName() {
        return "capture of " + _wildcard.getSimpleName();
    }

    @Override
    public final <R, P> R accept(final TypeMetadataVisitor<P, R> visitor, final P parameter) {
        return visitor.visitCapturedType(this, parameter);
    }

    @Override
    protected final StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
       return _wildcard.appendName(sb.append("capture of "), fullName, dottedName);
    }
}
