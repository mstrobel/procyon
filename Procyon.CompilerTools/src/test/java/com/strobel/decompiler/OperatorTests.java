/*
 * OperatorTests.java
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

import java.util.Date;

public class OperatorTests extends DecompilerTest {
    private static class A {
        public String test(final String s, final char c, final byte b, final float n, final Date date) {
            return b + ":" + c + ":" + s + ":" + n + ":" + date;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class B {
        public void test() {
            String s = "";
            s += "james";
        }
    }


    @SuppressWarnings("UnusedAssignment")
    private static class C {
        public void test() {
            int n = 0;
            System.out.println(n++);
        }
    }

    private static class D {
        public void test(final int x) {
            if (x < 5 || x >= 2) {
                System.out.println("ifTrue");
            }
            else {
                System.out.println("ifFalse");
            }
        }
    }

    private static class E {
        public void test(final int x) {
            if (x > 2 && x <= 5) {
                System.out.println("ifTrue");
            }
            else {
                System.out.println("ifFalse");
            }
        }
    }

    private static class F {
        public void test(final int x) {
            System.out.println((x > 0) ? "positive" : ((x < 0) ? "negative" : "zero"));
        }
    }

    @Test
    public void testStringConcatenation() {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
            "    public String test(final String s, final char c, final byte b, final float n, final Date date) {\n" +
            "        return b + \":\" + c + \":\" + s + \":\" + n + \":\" + date;\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testStringConcatenationToExistingString() {
        verifyOutput(
            B.class,
            defaultSettings(),
            "private static class B {\n" +
            "    public void test() {\n" +
            "        String s = \"\";\n" +
            "        s = s + \"james\";\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testPostIncrementTransform() {
        verifyOutput(
            C.class,
            defaultSettings(),
            "private static class C {\n" +
            "    public void test() {\n" +
            "        int n = 0;\n" +
            "        System.out.println(n++);\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testLogicalOr() {
        verifyOutput(
            D.class,
            defaultSettings(),
            "private static class D {\n" +
            "    public void test(final int x) {\n" +
            "        if (x < 5 || x >= 2) {\n" +
            "            System.out.println(\"ifTrue\");\n" +
            "        }\n" +
            "        else {\n" +
            "            System.out.println(\"ifFalse\");\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testLogicalAnd() {
        verifyOutput(
            E.class,
            defaultSettings(),
            "private static class E {\n" +
            "    public void test(final int x) {\n" +
            "        if (x > 2 && x <= 5) {\n" +
            "            System.out.println(\"ifTrue\");\n" +
            "        }\n" +
            "        else {\n" +
            "            System.out.println(\"ifFalse\");\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testTernaryOperator() {
        verifyOutput(
            F.class,
            defaultSettings(),
            "private static class F {\n" +
            "    public void test(final int x) {\n" +
            "        System.out.println((x > 0) ? \"positive\" : ((x < 0) ? \"negative\" : \"zero\"));\n" +
            "    }\n" +
            "}\n"
        );
    }
}
