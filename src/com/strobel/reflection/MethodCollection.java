package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public final class MethodCollection extends MemberCollection<MethodInfo> {
    private final static MethodCollection EMPTY = new MethodCollection();

    public static MethodCollection empty() {
        return EMPTY;
    }

    public MethodCollection(final MethodInfo... elements) {
        super(elements);
    }

    public MethodCollection(final MethodInfo[] elements, final int offset, final int length) {
        super(elements, offset, length);
    }
}
