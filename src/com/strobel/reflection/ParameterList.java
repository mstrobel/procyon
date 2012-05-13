package com.strobel.reflection;

import com.strobel.core.ReadOnlyList;
import com.strobel.core.VerifyArgument;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class ParameterList extends ReadOnlyList<ParameterInfo> {
    private final static ParameterList EMPTY = new ParameterList();

    public static ParameterList empty() {
        return EMPTY;
    }

    public ParameterList(final List<ParameterInfo> elements) {
        super(ParameterInfo.class, VerifyArgument.noNullElements(elements, "elements"));
    }

    public ParameterList(final ParameterInfo... elements) {
        super(elements);
    }

    public ParameterList(final ParameterInfo[] elements, final int offset, final int length) {
        super(elements, offset, length);
    }
}
