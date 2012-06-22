package com.strobel.core;

/**
 * @author strobelm
 */
public final class HashUtilities {
    private static final int NullHashCode = 0x61E04917;
    private static final int CombinedHashOffset = 5;

    private HashUtilities() {
        throw new UnsupportedOperationException();
    }

    public static int combineHashCodes(final int... hashes) {
        int hash = 0;

        for (final int h : hashes) {
            hash <<= CombinedHashOffset;
            hash ^= h;
        }

        return hash;
    }

    public static int combineHashCodes(final Object... objects) {
        int hash = 0;

        for (final Object o : objects) {
            int entryHash = NullHashCode;

            if (o != null) {
                if (o instanceof Object[]) {
                    entryHash = combineHashCodes((Object[])o);
                }
                else {
                    entryHash = o.hashCode();
                }
            }

            hash <<= CombinedHashOffset;
            hash ^= entryHash;
        }

        return hash;
    }

    public static int combineHashCodes(final int hash1, final int hash2) {
        return (hash1 << CombinedHashOffset) ^ hash2;
    }

    public static int combineHashCodes(final int hash1, final int hash2, final int hash3) {
        return (((hash1 << CombinedHashOffset)
                 ^ hash2) << CombinedHashOffset)
               ^ hash3;
    }

    public static int combineHashCodes(final int hash1, final int hash2, final int hash3, final int hash4) {
        return (((((hash1 << CombinedHashOffset)
                   ^ hash2) << CombinedHashOffset)
                 ^ hash3) << CombinedHashOffset)
               ^ hash4;
    }

    public static int combineHashCodes(
        final int hash1,
        final int hash2,
        final int hash3,
        final int hash4,
        final int hash5) {

        return (((((((hash1 << CombinedHashOffset)
                     ^ hash2) << CombinedHashOffset)
                   ^ hash3) << CombinedHashOffset)
                 ^ hash4) << CombinedHashOffset)
               ^ hash5;
    }

    public static int combineHashCodes(
        final int hash1,
        final int hash2,
        final int hash3,
        final int hash4,
        final int hash5,
        final int hash6) {

        return (((((((((hash1 << CombinedHashOffset)
                       ^ hash2) << CombinedHashOffset)
                     ^ hash3) << CombinedHashOffset)
                   ^ hash4) << CombinedHashOffset)
                 ^ hash5) << CombinedHashOffset)
               ^ hash6;
    }

    public static int combineHashCodes(
        final int hash1,
        final int hash2,
        final int hash3,
        final int hash4,
        final int hash5,
        final int hash6,
        final int hash7) {

        return (((((((((((hash1 << CombinedHashOffset)
                         ^ hash2) << CombinedHashOffset)
                       ^ hash3) << CombinedHashOffset)
                     ^ hash4) << CombinedHashOffset)
                   ^ hash5) << CombinedHashOffset)
                 ^ hash6) << CombinedHashOffset)
               ^ hash7;
    }

    public static int combineHashCodes(
        final int hash1,
        final int hash2,
        final int hash3,
        final int hash4,
        final int hash5,
        final int hash6,
        final int hash7,
        final int hash8) {

        return (((((((((((((hash1 << CombinedHashOffset)
                           ^ hash2) << CombinedHashOffset)
                         ^ hash3) << CombinedHashOffset)
                       ^ hash4) << CombinedHashOffset)
                     ^ hash5) << CombinedHashOffset)
                   ^ hash6) << CombinedHashOffset)
                 ^ hash7) << CombinedHashOffset)
               ^ hash8;
    }

    public static int combineHashCodes(final Object o1, final Object o2) {
        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode()
        );
    }

    public static int combineHashCodes(final Object o1, final Object o2, final Object o3) {
        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode(),
            o3 == null ? NullHashCode : o3.hashCode()
        );
    }

    public static int combineHashCodes(final Object o1, final Object o2, final Object o3, final Object o4) {
        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode(),
            o3 == null ? NullHashCode : o3.hashCode(),
            o4 == null ? NullHashCode : o4.hashCode()
        );
    }

    public static int combineHashCodes(
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5) {

        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode(),
            o3 == null ? NullHashCode : o3.hashCode(),
            o4 == null ? NullHashCode : o4.hashCode(),
            o5 == null ? NullHashCode : o5.hashCode()
        );
    }

    public static int combineHashCodes(
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5,
        final Object o6) {

        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode(),
            o3 == null ? NullHashCode : o3.hashCode(),
            o4 == null ? NullHashCode : o4.hashCode(),
            o5 == null ? NullHashCode : o5.hashCode(),
            o6 == null ? NullHashCode : o6.hashCode()
        );
    }

    public static int combineHashCodes(
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5,
        final Object o6,
        final Object o7) {

        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode(),
            o3 == null ? NullHashCode : o3.hashCode(),
            o4 == null ? NullHashCode : o4.hashCode(),
            o5 == null ? NullHashCode : o5.hashCode(),
            o6 == null ? NullHashCode : o6.hashCode(),
            o7 == null ? NullHashCode : o7.hashCode()
        );
    }

    public static int combineHashCodes(
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5,
        final Object o6,
        final Object o7,
        final Object o8) {

        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode(),
            o3 == null ? NullHashCode : o3.hashCode(),
            o4 == null ? NullHashCode : o4.hashCode(),
            o5 == null ? NullHashCode : o5.hashCode(),
            o6 == null ? NullHashCode : o6.hashCode(),
            o7 == null ? NullHashCode : o7.hashCode(),
            o8 == null ? NullHashCode : o8.hashCode()
        );
    }
}
