/*
 * JavaOutputVisitor.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.java;

import static java.lang.String.format;

public final class JavaOutputVisitor {
    public static String convertCharacter(final char ch) {
        switch (ch) {
            case '\\':
                return "\\\\";
            case '\0':
                return "\\0";
            case '\b':
                return "\\b";
            case '\f':
                return "\\f";
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\t':
                return "\\t";

            default:
                if (Character.isISOControl(ch) ||
                    Character.isSurrogate(ch) ||
                    Character.isWhitespace(ch) && ch != ' ') {

                    return format("\\u%1$04x", (int) ch);
                }
                else {
                    return String.valueOf(ch);
                }
        }
    }

    public static String convertString(final String s) {
        return convertString(s, false);
    }

    public static String convertString(final String s, final boolean quote) {
        final StringBuilder sb = new StringBuilder(Math.min(16, s.length()));

        if (quote) {
            sb.append('"');
        }

        for (int i = 0, n = s.length(); i < n; i++) {
            sb.append(convertCharacter(s.charAt(i)));
        }

        if (quote) {
            sb.append('"');
        }

        return sb.toString();
    }
}
