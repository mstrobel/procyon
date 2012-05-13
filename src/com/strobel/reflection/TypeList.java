package com.strobel.reflection;

import java.util.List;

/**
 * @author Mike Strobel
 */
 public class TypeList extends MemberList<Type> {
    private final static TypeList EMPTY = new TypeList();

    public static TypeList empty() {
        return EMPTY;
    }

    TypeList(final List<? extends Type> elements) {
        super(Type.class, elements);
    }

    TypeList(final Type... elements) {
        super(Type.class, elements);
    }

    TypeList(final Type[] elements, final int offset, final int length) {
        super(Type.class, elements, offset, length);
    }
    
    protected boolean hasOpenTypeParameters() {
        for (int i = 0, n = this.size(); i < n; i++) {
            if (this.get(i).isGenericParameter()) {
                return true;
            }
        }
        return false;
    }
}
