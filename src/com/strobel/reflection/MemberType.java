package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public enum MemberType {

    Constructor(1),
    Field(2),
    Method(4),
    TypeInfo(8),
    Custom(16),
    NestedType(32),

    All(NestedType.mask | TypeInfo.mask | Method.mask | Field.mask | Constructor.mask);

    @SuppressWarnings("PackageVisibleField")
    final int mask;

    MemberType(final int mask) {
        this.mask = mask;
    }
}
