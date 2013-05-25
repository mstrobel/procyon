/*
 * ArrayCreationTests.java
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

public class ArrayCreationTests extends DecompilerTest {
    private static class A {
        public int[] test() {
            return new int[3];
        }
    }

    private static class B {
        public int[][] test() {
            return new int[][] { { 1, 2, 3 }, { 4, 5, 6 } };
        }
    }

    private static class C {
        public int[][] test() {
            return new int[3][2];
        }
    }

    @Test
    public void testSimpleArrayCreation() {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
            "    public int[] test() {\n" +
            "        return new int[3];\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testJaggedArrayInitialization() {
        verifyOutput(
            B.class,
            defaultSettings(),
            "private static class B {\n" +
            "    public int[][] test() {\n" +
            "        return new int[][] { { 1, 2, 3 }, { 4, 5, 6 } };\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testMultiDimensionalArrayCreation() {
        verifyOutput(
            C.class,
            defaultSettings(),
            "private static class C {\n" +
            "    public int[][] test() {\n" +
            "        return new int[3][2];\n" +
            "    }\n" +
            "}\n"
        );
    }
}
