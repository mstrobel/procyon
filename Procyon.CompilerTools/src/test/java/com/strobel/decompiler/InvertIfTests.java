/*
 * InvertIfTests.java
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

package com.strobel.decompiler;

import org.junit.Test;

public class InvertIfTests extends DecompilerTest {
    private class T {
        void main(final String[] args) {
            if (args.length == 0) {
                System.out.println("no arg");
            } else {
                if (args.length == 1) {
                    System.out.println("1 arg");
                } else {
                    throw new RuntimeException("too many args");
                }
            }
        }
    }

    private class U {
        void test(final Object o) {
            if (o instanceof String) {
                System.out.println(o);
            } else {
                if (o instanceof CharSequence) {
                    System.out.println(o);
                } else {
                    throw new RuntimeException();
                }
            }
        }
    }

    @Test
    public void testInvertIfTest() {
        verifyOutput(
            T.class,
            defaultSettings(),
            "private class T\n" +
            "{\n" +
            "    void main(final String[] args) {\n" +
            "        if (args.length == 0) {\n" +
            "            System.out.println(\"no arg\");\n" +
            "        } else {\n" +
            "            if (args.length == 1) {\n" +
            "                System.out.println(\"1 arg\");\n" +
            "            } else {\n" +
            "                throw new RuntimeException(\"too many args\");\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}"
        );
    }

    @Test
    public void testInvertIfDoubleNegationTest() {
        verifyOutput(
            U.class,
            defaultSettings(),
            "private class U\n" +
            "{\n" +
            "    void test(final Object o) {\n" +
            "        if (o instanceof String) {\n" +
            "            System.out.println(o);\n" +
            "        } else {\n" +
            "            if (o instanceof CharSequence) {\n" +
            "                System.out.println(o);\n" +
            "            } else {\n" +
            "                throw new RuntimeException();\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}"
        );
    }
}