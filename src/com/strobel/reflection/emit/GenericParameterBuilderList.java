package com.strobel.reflection.emit;

import com.strobel.reflection.MemberList;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class GenericParameterBuilderList extends MemberList<GenericParameterBuilder> {
    private final static GenericParameterBuilderList EMPTY = new GenericParameterBuilderList();

    public static GenericParameterBuilderList empty() {
        return EMPTY;
    }

    GenericParameterBuilderList(final List<? extends GenericParameterBuilder> elements) {
        super(GenericParameterBuilder.class, elements);
    }

    GenericParameterBuilderList(final GenericParameterBuilder... elements) {
        super(GenericParameterBuilder.class, elements);
    }

    GenericParameterBuilderList(final GenericParameterBuilder[] elements, final int offset, final int length) {
        super(GenericParameterBuilder.class, elements, offset, length);
    }

    @Override
    public GenericParameterBuilderList subList(final int fromIndex, final int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size());

        final int offset = getOffset() + fromIndex;
        final int length = toIndex - fromIndex;

        if (length == 0) {
            return empty();
        }

        return new GenericParameterBuilderList(getElements(), offset, length);
    }
}
