package com.strobel.reflection;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class FieldList extends MemberList<FieldInfo> {
    private final static FieldList EMPTY = new FieldList();

    public static FieldList empty() {
        return EMPTY;
    }

    public FieldList(final List<? extends FieldInfo> elements) {
        super(FieldInfo.class, elements);
    }

    public FieldList(final FieldInfo... elements) {
        super(FieldInfo.class, elements);
    }

    public FieldList(final FieldInfo[] elements, final int offset, final int length) {
        super(FieldInfo.class, elements, offset, length);
    }

    @Override
    public FieldList subList(final int fromIndex, final int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size());

        final int offset = getOffset() + fromIndex;
        final int length = toIndex - fromIndex;

        if (length == 0) {
            return empty();
        }

        return new FieldList(getElements(), offset, length);
    }
}
