package com.strobel.reflection;

import javax.lang.model.type.TypeKind;
import java.lang.reflect.Modifier;

/**
 * @author Mike Strobel
 */
public final class CompoundType<T> extends Type<T> {
    private final TypeList _interfaces;
    private final Type<T> _baseType;

    CompoundType(final TypeList interfaces, final Type<T> baseType) {
        _interfaces = interfaces;
        _baseType = baseType;
    }

    @Override
    public <P, R> R accept(final TypeVisitor<P, R> visitor, final P parameter) {
        return visitor.visitTypeParameter(this, parameter);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.TYPEVAR;
    }

    @Override
    public Class<T> getErasedClass() {
        return _baseType.getErasedClass();
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
        return Modifier.PUBLIC | Modifier.ABSTRACT;
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }

    @Override
    public TypeList getInterfaces() {
        return _interfaces;
    }

    @Override
    public boolean isGenericParameter() {
        return true;
    }

    @Override
    public Type<?> getUpperBound() {
        return _baseType;
    }

    @Override
    public Type<?> getLowerBound() {
        return super.getLowerBound();
    }

    @Override
    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        final Type<T> baseType = _baseType;
        final TypeList interfaces = _interfaces;

        StringBuilder s = sb;

        if (baseType != Types.Object) {
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
    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        final Type<T> baseType = _baseType;
        final TypeList interfaces = _interfaces;

        StringBuilder s = sb;

        if (baseType != Types.Object) {
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
    public StringBuilder appendFullDescription(final StringBuilder sb) {
        return appendBriefDescription(sb);
    }

    @Override
    public StringBuilder appendSignature(final StringBuilder sb) {
        return super.appendSignature(sb);
    }

    @Override
    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        return super.appendErasedSignature(sb);
    }
}
