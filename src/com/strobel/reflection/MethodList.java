package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public final class MethodList extends MemberList<MethodInfo> {
    private final static MethodList EMPTY = new MethodList();

    public static MethodList empty() {
        return EMPTY;
    }

    public MethodList(final MethodInfo... elements) {
        super(MethodInfo.class, elements);
    }

    public MethodList(final MethodInfo[] elements, final int offset, final int length) {
        super(MethodInfo.class, elements, offset, length);
    }
}
