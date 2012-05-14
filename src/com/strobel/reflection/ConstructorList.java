package com.strobel.reflection;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class ConstructorList extends MemberList<ConstructorInfo> {
    private final static ConstructorList EMPTY = new ConstructorList();

    public static ConstructorList empty() {
        return EMPTY;
    }

    public ConstructorList(final List<? extends ConstructorInfo> elements) {
        super(ConstructorInfo.class, elements);
    }

    public ConstructorList(final ConstructorInfo... elements) {
        super(ConstructorInfo.class, elements);
    }

    public ConstructorList(final ConstructorInfo[] elements, final int offset, final int length) {
        super(ConstructorInfo.class, elements, offset, length);
    }

    @Override
    public ConstructorList subList(final int fromIndex, final int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size());

        final int offset = getOffset() + fromIndex;
        final int length = toIndex - fromIndex;

        if (length == 0) {
            return empty();
        }

        return new ConstructorList(getElements(), offset, length);
    }
}
