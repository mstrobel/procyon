package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
final class FlagUtilities {
    private FlagUtilities() {}

    static boolean all(final int flags, final int mask) {
        return (flags & mask) == mask;
    }

    static boolean any(final int flags, final int mask) {
        return (flags & mask) != 0;
    }
}
