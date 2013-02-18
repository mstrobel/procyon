package com.strobel.assembler.metadata;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class CompoundTypeReference extends TypeReference {
    private final TypeReference _baseType;
    private final List<TypeReference> _interfaces;

    public CompoundTypeReference(final TypeReference baseType, final List<TypeReference> interfaces) {
        _baseType = baseType;
        _interfaces = interfaces;
    }

    @Override
    public TypeReference getDeclaringType() {
        return null;
    }

    @Override
    public String getSimpleName() {
        if (_baseType != null) {
            return _baseType.getSimpleName();
        }
        return _interfaces.get(0).getSimpleName();
    }

    @Override
    public String getName() {
        if (_baseType != null) {
            return _baseType.getName();
        }
        return _interfaces.get(0).getName();
    }

    @Override
    public String getFullName() {
        if (_baseType != null) {
            return _baseType.getFullName();
        }
        return _interfaces.get(0).getFullName();
    }

    @Override
    public String getInternalName() {
        if (_baseType != null) {
            return _baseType.getInternalName();
        }
        return _interfaces.get(0).getInternalName();
    }

    @Override
    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        final TypeReference baseType = _baseType;
        final List<TypeReference> interfaces = _interfaces;

        StringBuilder s = sb;

        if (baseType != null && !baseType.equals(BuiltinTypes.Object)) {
            s = baseType.appendBriefDescription(s);
            if (!interfaces.isEmpty()) {
                s.append(" & ");
            }
        }

        for (int i = 0, n = interfaces.size(); i < n; i++) {
            if (i != 0) {
                s.append(" & ");
            }
            s = interfaces.get(i).appendBriefDescription(s);
        }

        return s;
    }

    @Override
    public StringBuilder appendSimpleDescription(final StringBuilder sb) {
        final TypeReference baseType = _baseType;
        final List<TypeReference> interfaces = _interfaces;

        StringBuilder s = sb;

        if (baseType != BuiltinTypes.Object) {
            s = baseType.appendSimpleDescription(s);
            if (!interfaces.isEmpty()) {
                s.append(" & ");
            }
        }

        for (int i = 0, n = interfaces.size(); i < n; i++) {
            if (i != 0) {
                s.append(" & ");
            }
            s = interfaces.get(i).appendSimpleDescription(s);
        }

        return s;
    }

    @Override
    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        final TypeReference baseType = _baseType;
        final List<TypeReference> interfaces = _interfaces;

        StringBuilder s = sb;

        if (baseType != BuiltinTypes.Object) {
            s = baseType.appendErasedDescription(s);
            if (!interfaces.isEmpty()) {
                s.append(" & ");
            }
        }

        for (int i = 0, n = interfaces.size(); i < n; i++) {
            if (i != 0) {
                s.append(" & ");
            }
            s = interfaces.get(i).appendErasedDescription(s);
        }

        return s;
    }

    @Override
    public StringBuilder appendDescription(final StringBuilder sb) {
        return appendBriefDescription(sb);
    }

    @Override
    public StringBuilder appendSignature(final StringBuilder sb) {
        StringBuilder s = sb;

        if (_baseType != null && !_baseType.equals(BuiltinTypes.Object)) {
            s = _baseType.appendSignature(s);
        }

        if (_interfaces.isEmpty()) {
            return s;
        }

        for (final TypeReference interfaceType : _interfaces) {
            s.append(':');
            s = interfaceType.appendSignature(s);
        }

        return s;
    }

    @Override
    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        return super.appendErasedSignature(sb);
    }
}
