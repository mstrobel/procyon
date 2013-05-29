/*
 * GenericParameterBuilderList.java
 *
 * Copyright (c) 2012 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.reflection.emit;

import com.strobel.annotations.NotNull;
import com.strobel.reflection.MemberList;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class GenericParameterBuilderList extends MemberList<GenericParameterBuilder> {
    private final static GenericParameterBuilderList EMPTY = new GenericParameterBuilderList();

    @SuppressWarnings("unchecked")
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

    @NotNull
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
