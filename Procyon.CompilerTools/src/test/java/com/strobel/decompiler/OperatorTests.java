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
}
