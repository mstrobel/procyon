package com.strobel.expressions;

import com.strobel.core.StringUtilities;
import com.strobel.reflection.Type;

/**
 * Used to denote the target of a {@link GotoExpression}
 * @author Mike Strobel
 */
public final class LabelTarget {
    private final Type _type;
    private final String _name;

    LabelTarget(final Type type, final String name) {
        _type = type;
        _name = name;
    }

    public final String getName() {
        return _name;
    }

    public final Type getType() {
        return _type;
    }

    public final String toString() {
        return StringUtilities.isNullOrEmpty(_name) ? "UnnamedLabel" : _name;
    }
}
