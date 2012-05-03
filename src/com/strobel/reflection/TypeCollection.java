package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public final class TypeCollection extends MemberCollection<Type> {
    private final static TypeCollection EMPTY = new TypeCollection();

    public static TypeCollection empty() {
        return EMPTY;
    }

    public TypeCollection(final Type... elements) {
        super(Type.class, elements);
    }

    public TypeCollection(final Type[] elements, final int offset, final int length) {
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
