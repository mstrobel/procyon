package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
final class Flags {
    private Flags() {}

    static boolean all(final int flags, final int mask) {
        return (flags & mask) == mask;
    }

    static boolean any(final int flags, final int mask) {
        return (flags & mask) != 0;
    }
}
