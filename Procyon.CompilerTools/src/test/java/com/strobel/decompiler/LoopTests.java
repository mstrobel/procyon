/*
 * LoopTests.java
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

import java.util.ArrayList;
import java.util.List;

public class LoopTests extends DecompilerTest {
    private static final class StringList extends ArrayList<String> {}

    private static class A {
        public void test(final Iterable<String> items) {
            for (final String item : items) {
                System.out.println(item);
            }
        }
    }

    private static class B {
        public void test(final List<String> items) {
            for (final String item : items) {
                System.out.println(item);
            }
        }
    }

    private static class C {
        public void test(final StringList items) {
            for (final String item : items) {
                System.out.println(item);
            }
        }
    }

    private static class D {
        public void test(final String[] items) {
            for (final String item : items) {
                System.out.println(item);
            }
        }
    }

    private static class E {
        public void test(final List<String> items) {
            for (int i = 0, n = items.size(); i < n; i++) {
                final String item = items.get(i);
                System.out.println(item);
            }
        }
    }

    private static class F {
        public void test(final String[] items) {
            for (int i = 0; i < items.length; i++) {
                final String item = items[i];
                System.out.println(item);
            }
        }
    }

    private static class G {
        public void test(final String[] items) {
            if (items == null || items.length == 0) {
                return;
            }

            int i = 0;

            do {
                System.out.println(items[i]);
                ++i;
            }
            while (i < items.length);
        }
    }

    @SuppressWarnings("LocalCanBeFinal")
    private static class H {
        public void test(final List<Integer> items) {
            for (int item : items) {
                System.out.println(item);
            }
        }
    }

    @Test
    public void testEnhancedForInIterable() {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A\n" +
            "{\n" +
            "    public void test(final Iterable<String> items) {\n" +
            "        for (String item : items) {\n" +
            "            System.out.println(item);\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testEnhancedForInList() {
        verifyOutput(
            B.class,
            defaultSettings(),
            "private static class B\n" +
            "{\n" +
            "    public void test(final List<String> items) {\n" +
            "        for (String item : items) {\n" +
            "            System.out.println(item);\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testEnhancedForInNonGenericCollection() {
        verifyOutput(
            C.class,
            defaultSettings(),
            "private static class C\n" +
            "{\n" +
            "    public void test(final StringList items) {\n" +
            "        for (String item : items) {\n" +
            "            System.out.println(item);\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testEnhancedForInArray() {
        verifyOutput(
            D.class,
            defaultSettings(),
            "private static class D\n" +
            "{\n" +
            "    public void test(final String[] items) {\n" +
            "        for (String item : items) {\n" +
            "            System.out.println(item);\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testForWithList() {
        verifyOutput(
            E.class,
            defaultSettings(),
            "private static class E {\n" +
            "    public void test(final List<String> items) {\n" +
            "        for (int i = 0, n = items.size(); i < n; ++i) {\n" +
            "            final String item = (String)items.get(i);\n" +
            "            System.out.println(item);\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testForWithArray() {
        verifyOutput(
            F.class,
            defaultSettings(),
            "private static class F {\n" +
            "    public void test(final String[] items) {\n" +
            "        for (int i = 0; i < items.length; ++i) {\n" +
            "            final String item = items[i];\n" +
            "            System.out.println(item);\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testDoWhileLoop() {
        verifyOutput(
            G.class,
            defaultSettings(),
            "private static class G {\n" +
            "    public void test(final String[] items) {\n" +
            "        if (items == null || items.length == 0) {\n" +
            "            return;\n" +
            "        }\n" +
            "        int i = 0;\n" +
            "        do {\n" +
            "            System.out.println(items[i]);\n" +
            "        }\n" +
            "        while (++i < items.length);\n" +
            "    }\n" +
            "}\n"
        );
    }
    @Test
    public void testUnboxingEnhancedForLoop() {
        verifyOutput(
            H.class,
            defaultSettings(),
            "private static class H {\n" +
            "    public void test(final List<Integer> items) {\n" +
            "        for (int item : items) {\n" +
            "            System.out.println(item);\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }
}
