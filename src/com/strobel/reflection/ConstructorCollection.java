package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public final class ConstructorCollection extends MemberCollection<ConstructorInfo> {
    private final static ConstructorCollection EMPTY = new ConstructorCollection();

    public static ConstructorCollection empty() {
        return EMPTY;
    }

    public ConstructorCollection(final ConstructorInfo... elements) {
        super(ConstructorInfo.class, elements);
    }

    public ConstructorCollection(final ConstructorInfo[] elements, final int offset, final int length) {
        super(ConstructorInfo.class, elements, offset, length);
    }
}
