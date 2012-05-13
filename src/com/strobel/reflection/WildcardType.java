package com.strobel.reflection;

import com.strobel.core.VerifyArgument;
import com.sun.tools.javac.code.Flags;

/**
 * @author Mike Strobel
 */
final class WildcardType extends Type {
    private final TypeList _extendsBounds;
    private final Type _superBound;
    private Class<?> _erasedClass;

    WildcardType(final Type extendsBound, final Type superBound) {
        _extendsBounds = list(VerifyArgument.notNull(extendsBound, "extendsBound"));
        _superBound = VerifyArgument.notNull(superBound, "superBound");
    }

    @Override
    public String getName() {
        if (_superBound != NoType) {
            return "? super " + _superBound.getName();
        }

        final Type extendsBound = _extendsBounds.get(0);

        if (extendsBound == Types.Object) {
            return "?";
        }

        return "? extends " + extendsBound.getName();
    }

    @Override
    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        if (_superBound != NoType) {
            return _superBound.appendBriefDescription(sb.append("? super "));
        }

        final Type extendsBound = _extendsBounds.get(0);

        if (extendsBound == Types.Object) {
            return sb.append("?");
        }

        return extendsBound.appendBriefDescription(sb.append("? extends "));
    }

    @Override
    public StringBuilder appendFullDescription(final StringBuilder sb) {
        if (_superBound != NoType) {
            return _superBound.appendFullDescription(sb.append("? super "));
        }

        final Type extendsBound = _extendsBounds.get(0);

        if (extendsBound == Types.Object) {
            return sb.append("?");
        }

        return extendsBound.appendFullDescription(sb.append("? extends "));
    }

    private Class<?> resolveErasedClass() {
        if (!_extendsBounds.isEmpty()) {
            return _extendsBounds.get(0).getErasedClass();
        }

        return Object.class;
    }

    @Override
    public Type getLowerBound() {
        return _superBound;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object accept(final TypeVisitor visitor, final Object parameter) {
        return visitor.visitWildcard(this, parameter);
    }

    @Override
    public TypeList getGenericParameterConstraints() {
        return _extendsBounds;
    }

    @Override
    public Class<?> getErasedClass() {
        if (_erasedClass == null) {
            synchronized (CACHE_LOCK) {
                if (_erasedClass == null) {
                    _erasedClass = resolveErasedClass();
                }
            }
        }
        return _erasedClass;
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public Type getDeclaringType() {
        return null;
    }

    @Override
    int getModifiers() {
        return Flags.PUBLIC;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final WildcardType that = (WildcardType)o;

        return _extendsBounds.get(0).equals(that._extendsBounds.get(0)) &&
               _superBound.equals(that._superBound);
    }

    @Override
    public int hashCode() {
        int result = _extendsBounds.get(0).hashCode();
        result = 31 * result + _superBound.hashCode();
        return result;
    }
}
