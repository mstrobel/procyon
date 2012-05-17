package com.strobel.reflection;

import com.sun.tools.javac.code.Flags;

import javax.lang.model.type.TypeKind;

/**
 * @author Mike Strobel
 */
final class NullType extends Type {
    @Override
    public TypeKind getKind() {
        return TypeKind.NULL;
    }

    @Override
    public final Class getErasedClass() {
        return null;
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
    final int getModifiers() {
        return Flags.PUBLIC;
    }

    @Override
    protected final StringBuilder _appendClassName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        return sb.append("<nulltype>");
    }

    @Override
    protected final StringBuilder _appendClassDescription(final StringBuilder sb) {
        return sb.append("<nulltype>");
    }

    @Override
    public final StringBuilder appendBriefDescription(final StringBuilder sb) {
        return sb.append("<nulltype>");
    }

    @Override
    public final StringBuilder appendSimpleDescription(final StringBuilder sb) {
        return sb.append("<nulltype>");
    }
}

