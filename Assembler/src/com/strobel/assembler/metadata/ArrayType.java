package com.strobel.assembler.metadata;

import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class ArrayType extends TypeSpecification {
    private final TypeReference _elementType;
    private final String _name;

    ArrayType(final TypeReference elementType) {
        _elementType = VerifyArgument.notNull(elementType, "elementType");
        _name = elementType.getName() + "[]";
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public TypeReference getElementType() {
        return _elementType;
    }

    @Override
    public TypeReference getUnderlyingType() {
        return _elementType;
    }

    @Override
    public StringBuilder appendSignature(final StringBuilder sb) {
        sb.append('[');
        return _elementType.appendSignature(sb);
    }

    @Override
    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        return _elementType.appendErasedSignature(sb.append('['));
    }

    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        return _elementType.appendBriefDescription(sb).append("[]");
    }

    public StringBuilder appendSimpleDescription(final StringBuilder sb) {
        return _elementType.appendSimpleDescription(sb).append("[]");
    }

    @Override
    public TypeReference getDeclaringType() {
        return null;
    }

    @Override
    public long getFlags() {
        return Flags.PUBLIC;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public String getPackageName() {
        return _elementType.getPackageName();
    }

    public StringBuilder appendDescription(final StringBuilder sb) {
        return appendBriefDescription(sb);
    }

    public static ArrayType create(final TypeReference elementType) {
        return new ArrayType(elementType);
    }

    @Override
    public TypeReference resolve() {
        final TypeReference resolvedElementType = _elementType.resolve();

        if (resolvedElementType != null && !resolvedElementType.equals(_elementType))
            return resolvedElementType.makeArrayType();

        return super.resolve();
    }
}
