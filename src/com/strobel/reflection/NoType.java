package com.strobel.reflection;

import javax.lang.model.type.TypeKind;

/**
 * @author Mike Strobel
 */
final class NoType extends Type {
    @Override
    public TypeKind getKind() {
        return TypeKind.NONE;
    }

    @Override
    public final Class getErasedClass() {
        return null;
    }

    @Override
    public boolean isAssignableFrom(final Type type) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object accept(final TypeVisitor visitor, final Object parameter) {
        return visitor.visitType(this, parameter);
    }

    @Override
    public final MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public final Type getDeclaringType() {
        return null;
    }

    @Override
    public final int getModifiers() {
        return 0;
    }

    @Override
    protected final StringBuilder _appendClassName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        return sb.append("<any>");
    }

    @Override
    protected final StringBuilder _appendClassDescription(final StringBuilder sb) {
        return sb.append("<any>");
    }

    @Override
    public final StringBuilder appendBriefDescription(final StringBuilder sb) {
        return sb.append("<any>");
    }

    @Override
    public final StringBuilder appendSimpleDescription(final StringBuilder sb) {
        return sb.append("<any>");
    }
}
