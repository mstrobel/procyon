package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public final class ConstructorList extends MemberList<ConstructorInfo> {
    private final static ConstructorList EMPTY = new ConstructorList();

    public static ConstructorList empty() {
        return EMPTY;
    }

    public ConstructorList(final ConstructorInfo... elements) {
        super(ConstructorInfo.class, elements);
    }

    public ConstructorList(final ConstructorInfo[] elements, final int offset, final int length) {
        super(ConstructorInfo.class, elements, offset, length);
    }
}
