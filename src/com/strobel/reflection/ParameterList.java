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

    private TypeList _parameterTypes;

    public TypeList getParameterTypes() {
        if (_parameterTypes == null) {
            synchronized (this) {
                if (_parameterTypes == null) {
                    final Type<?>[] types = new Type<?>[size()];
                    for (int i = 0, n = size(); i < n; i++) {
                        types[i] = get(i).getParameterType();
                    }
                    _parameterTypes = new TypeList(types);
                }
            }
        }
        return _parameterTypes;
    }

    public ParameterList(final List<ParameterInfo> elements) {
        super(ParameterInfo.class, VerifyArgument.noNullElements(elements, "elements"));
    }

    public ParameterList(final ParameterInfo... elements) {
        super(VerifyArgument.noNullElements(elements, "elements"));
    }

    public ParameterList(final ParameterInfo[] elements, final int offset, final int length) {
        super(VerifyArgument.noNullElements(elements, offset, length, "elements"), offset, length);
    }
}
