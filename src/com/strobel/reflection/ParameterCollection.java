package com.strobel.reflection;

import com.strobel.core.ReadOnlyCollection;

/**
 * @author Mike Strobel
 */
public final class ParameterCollection extends ReadOnlyCollection<ParameterInfo> {
    private final static ParameterCollection EMPTY = new ParameterCollection();

    public static ParameterCollection empty() {
        return EMPTY;
    }

    public ParameterCollection(final ParameterInfo... elements) {
        super(elements);
    }

    public ParameterCollection(final ParameterInfo[] elements, final int offset, final int length) {
        super(elements, offset, length);
    }
}
