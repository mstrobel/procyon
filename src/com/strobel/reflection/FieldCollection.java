package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public final class FieldCollection extends MemberCollection<FieldInfo> {
    private final static FieldCollection EMPTY = new FieldCollection();

    public static FieldCollection empty() {
        return EMPTY;
    }

    public FieldCollection(final FieldInfo... elements) {
        super(FieldInfo.class, elements);
    }

    public FieldCollection(final FieldInfo[] elements, final int offset, final int length) {
        super(FieldInfo.class, elements, offset, length);
    }
}
