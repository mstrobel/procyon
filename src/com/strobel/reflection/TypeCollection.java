package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public final class TypeCollection extends MemberCollection<Type> {
    private final static TypeCollection EMPTY = new TypeCollection();

    public static TypeCollection empty() {
        return EMPTY;
    }

    public TypeCollection(final Type... elements) {
        super(elements);
    }

    public TypeCollection(final Type[] elements, final int offset, final int length) {
        super(elements, offset, length);
    }
}
