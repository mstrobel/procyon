package com.strobel.assembler.metadata;

import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class ArrayType extends TypeReference {
    private final TypeReference _elementType;

    private String _internalName;
    private String _fullName;
    private String _simpleName;

    ArrayType(final TypeReference elementType) {
        _elementType = VerifyArgument.notNull(elementType, "elementType");

        setName(elementType.getName() + "[]");
    }

    @Override
    public String getPackageName() {
        return _elementType.getPackageName();
    }

    public String getSimpleName() {
        if (_simpleName == null) {
            _simpleName = _elementType.getSimpleName() + "[]";
        }
        return _simpleName;
    }

    public String getFullName() {
        if (_fullName == null) {
            _fullName = _elementType.getFullName() + "[]";
        }
        return _fullName;
    }

    public String getInternalName() {
        if (_internalName == null) {
            _internalName = "[" + _elementType.getInternalName();
        }
        return _internalName;
    }

    @Override
    public final boolean isArray() {
        return true;
    }

    @Override
    public final TypeReference getElementType() {
        return _elementType;
    }

    @Override
    public final TypeReference getUnderlyingType() {
        return _elementType;
    }

    @Override
    public final StringBuilder appendSignature(final StringBuilder sb) {
        sb.append('[');
        return _elementType.appendSignature(sb);
    }

    @Override
    public final StringBuilder appendErasedSignature(final StringBuilder sb) {
        return _elementType.appendErasedSignature(sb.append('['));
    }

    public final StringBuilder appendBriefDescription(final StringBuilder sb) {
        return _elementType.appendBriefDescription(sb).append("[]");
    }

    public final StringBuilder appendSimpleDescription(final StringBuilder sb) {
        return _elementType.appendSimpleDescription(sb).append("[]");
    }

    public final StringBuilder appendDescription(final StringBuilder sb) {
        return appendBriefDescription(sb);
    }

    public static ArrayType create(final TypeReference elementType) {
        return new ArrayType(elementType);
    }

    @Override
    public final TypeDefinition resolve() {
        final TypeDefinition resolvedElementType = _elementType.resolve();

        if (resolvedElementType != null && !resolvedElementType.equals(_elementType))
            return resolvedElementType;

        return super.resolve();
    }
}
