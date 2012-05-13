package com.strobel.reflection;

import com.sun.tools.javac.code.Flags;

/**
 * @author Mike Strobel
 */
final class NullType extends Type {
    @Override
    public final Class getErasedClass() {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object accept(final TypeVisitor visitor, final Object parameter) {
        return visitor.visitUnknown(this, parameter);
    }

    @Override
    public final String getName() {
        return "<nulltype>";
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
    protected final StringBuilder _appendClassName(final StringBuilder sb, final boolean dottedName) {
        return sb.append(getName());
    }

    @Override
    protected final StringBuilder _appendClassDescription(final StringBuilder sb) {
        return sb.append(getName());
    }

    @Override
    public final StringBuilder appendBriefDescription(final StringBuilder sb) {
        return sb.append(getName());
    }

    @Override
    protected final FieldList resolveFields() {
        return FieldList.empty();
    }

    @Override
    protected final MethodList resolveInstanceMethods() {
        return MethodList.empty();
    }

    @Override
    protected final MethodList resolveStaticMethods() {
        return MethodList.empty();
    }

    @Override
    protected final ConstructorList resolveConstructors() {
        return ConstructorList.empty();
    }
}

final class NoType extends Type {
    @Override
    public final Class getErasedClass() {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object accept(final TypeVisitor visitor, final Object parameter) {
        return visitor.visitUnknown(this, parameter);
    }

    @Override
    public final String getName() {
        return "<any>";
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
        return 0;
    }

    @Override
    protected final StringBuilder _appendClassName(final StringBuilder sb, final boolean dottedName) {
        return sb.append(getName());
    }

    @Override
    protected final StringBuilder _appendClassDescription(final StringBuilder sb) {
        return sb.append(getName());
    }

    @Override
    public final StringBuilder appendBriefDescription(final StringBuilder sb) {
        return sb.append(getName());
    }

    @Override
    protected final FieldList resolveFields() {
        return FieldList.empty();
    }

    @Override
    protected final MethodList resolveInstanceMethods() {
        return MethodList.empty();
    }

    @Override
    protected final MethodList resolveStaticMethods() {
        return MethodList.empty();
    }

    @Override
    protected final ConstructorList resolveConstructors() {
        return ConstructorList.empty();
    }
}
