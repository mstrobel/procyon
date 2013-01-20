package com.strobel.assembler.metadata;

import com.strobel.reflection.SimpleType;

/**
 * @author Mike Strobel
 */
public final class Wildcard extends TypeReference {
    private final static Wildcard UNBOUNDED = new Wildcard(BuiltinTypes.Object, BuiltinTypes.Bottom);

    private final TypeReference _bound;
    private final boolean _hasSuperBound;

    private String _name;

    private Wildcard(final TypeReference extendsBound, final TypeReference superBound) {
        _hasSuperBound = superBound != BuiltinTypes.Bottom;
        _bound = _hasSuperBound ? superBound : extendsBound;
    }

    @Override
    public TypeReference getDeclaringType() {
        return null;
    }

    @Override
    public SimpleType getSimpleType() {
        return SimpleType.Wildcard;
    }

    // <editor-fold defaultstate="collapsed" desc="Type Attributes">

    @Override
    public long getFlags() {
        return Flags.PUBLIC;
    }

    @Override
    public String getName() {
        if (_name == null) {
            _name = appendSimpleDescription(new StringBuilder()).toString();
        }
        return _name;
    }

    @Override
    public boolean isWildcardType() {
        return true;
    }

    @Override
    public boolean isBoundedType() {
        return true;
    }

    @Override
    public boolean isUnbound() {
        return !_hasSuperBound &&
               _bound == BuiltinTypes.Object;
    }

    @Override
    public boolean hasExtendsBound() {
        return !_hasSuperBound;
    }

    @Override
    public boolean hasSuperBound() {
        return _hasSuperBound;
    }

    @Override
    public TypeReference getSuperBound() {
        return _hasSuperBound ? _bound : BuiltinTypes.Bottom;
    }

    @Override
    public TypeReference getExtendsBound() {
        return _hasSuperBound ? BuiltinTypes.Object : _bound;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Name and Signature Formatting">

    @Override
    protected StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        return appendSimpleDescription(sb);
    }

    @Override
    public StringBuilder appendSignature(final StringBuilder sb) {
        if (_hasSuperBound) {
            return _bound.appendSignature(sb.append('-'));
        }
        if (_bound != BuiltinTypes.Object) {
            return _bound.appendSignature(sb.append('+'));
        }
        return sb.append('*');
    }

    @Override
    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        if (_hasSuperBound) {
            sb.append("? super ");
            if (_bound.isGenericParameter()) {
                return sb.append(_bound.getFullName());
            }
            return _bound.appendErasedDescription(sb);
        }

        if (_bound == BuiltinTypes.Object) {
            return sb.append("?");
        }

        sb.append("? extends ");

        if (_bound.isGenericParameter()) {
            return sb.append(_bound.getFullName());
        }

        return _bound.appendErasedDescription(sb);
    }

    @Override
    public StringBuilder appendSimpleDescription(final StringBuilder sb) {
        if (_hasSuperBound) {
            sb.append("? super ");
            if (_bound.isGenericParameter()) {
                return sb.append(_bound.getName());
            }
            return _bound.appendSimpleDescription(sb);
        }

        if (_bound == BuiltinTypes.Object) {
            return sb.append("?");
        }

        sb.append("? extends ");

        if (_bound.isGenericParameter()) {
            return sb.append(_bound.getName());
        }

        return _bound.appendSimpleDescription(sb);
    }

    @Override
    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        return appendBriefDescription(sb);
    }

    @Override
    public StringBuilder appendDescription(final StringBuilder sb) {
        return appendBriefDescription(sb);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Factory Methods">

    public static Wildcard unbounded() {
        return UNBOUNDED;
    }

    public static Wildcard makeSuper(final TypeReference superBound) {
        return new Wildcard(BuiltinTypes.Object, superBound);
    }

    public static Wildcard makeExtends(final TypeReference extendsBound) {
        return new Wildcard(extendsBound, BuiltinTypes.Bottom);
    }

    // </editor-fold>
}
