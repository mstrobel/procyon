package com.strobel.core;

/**
 * @author Mike Strobel
 */
public final class StringEx {
    private StringEx() {}

    public static boolean isNullOrEmpty(final String s) {
        return s == null || s.length() == 0;
    }

    public static boolean isNullOrWhitespace(final String s) {
        if (isNullOrEmpty(s)) {
            return true;
        }
        for (int i = 0, length = s.length(); i < length; i++) {
            final char ch = s.charAt(i);
            if (!Character.isWhitespace(ch)) {
                return false;
            }
        }
        return true;
    }

    public static boolean startsWithIgnoreCase(final String s, final String prefix) {
        return startsWithIgnoreCase(s, prefix, 0);
    }

    public static boolean startsWithIgnoreCase(final String s, final String prefix, final int offset) {
        final int sourceLength = VerifyArgument.notNull(s, "s").length();
        final int prefixLength = VerifyArgument.notNull(prefix, "prefix").length();

        final int n = sourceLength - offset;

        if (prefixLength > n) {
            return false;
        }

        for (int i = 0; i < prefixLength; i++) {
            final char c1 = s.charAt(offset + i);
            final char c2 = prefix.charAt(i);
            if (c1 != c2) {
                if (Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                    return false;
                }
            }
        }

        return true;
    }
}
