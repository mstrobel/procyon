package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
final class HierarchicType {
    private final boolean _isMixin;
    private final Type _type;
    private final int _priority;

    public HierarchicType(final Type type, final boolean mixin, final int priority) {
        _type = type;
        _isMixin = mixin;
        _priority = priority;
    }

    public Type<?> getType() { return _type; }

    public Class<?> getErasedType() { return getType().getErasedClass(); }

    public boolean isMixin() { return _isMixin; }

    public int getPriority() { return _priority; }

    @Override
    public String toString() { return getType().toString(); }

    @Override
    public int hashCode() { return getType().hashCode(); }

    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        final HierarchicType other = (HierarchicType)o;
        return getType().equals(other.getType());
    }
}
