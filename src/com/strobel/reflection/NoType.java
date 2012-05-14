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
