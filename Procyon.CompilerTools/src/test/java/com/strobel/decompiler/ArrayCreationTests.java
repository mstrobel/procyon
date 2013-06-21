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

    private static class D {
        public int[][] test() {
            return new int[10][];
        }
    }

    private static class E {
        public int[][][][] test() {
            return new int[3][4][5][];
        }
    }

    private static class F {
        public void test() {
            final Object[] a = { null, null, new Object() };
            final int[] b = { 1, 2, 3 };
            final int[][] c = { { 1, 2, 3 }, { 4, 5, 6 } };
            final int[][][] d = { { { 1, 2, 3, 4 }, { 1, 2, 3, 4 } } };
            final byte[] e = { 100 };
            final double[][][] g = new double[3][4][5];
            final byte[][][][] h = new byte[3][4][5][6];
            final byte[][][][] i = new byte[3][4][5][];
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

    @Test
    public void testJaggedArrayCreationWithSingleDimensionSize() {
        verifyOutput(
            D.class,
            defaultSettings(),
            "private static class D {\n" +
            "    public int[][] test() {\n" +
            "        return new int[10][];\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testMultiDimensionalArrayCreationWithPartialDimensions() {
        verifyOutput(
            E.class,
            defaultSettings(),
            "private static class E {\n" +
            "    public int[][][][] test() {\n" +
            "        return new int[3][4][5][];\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testArrayVariableInitializers() {
        verifyOutput(
            F.class,
            defaultSettings(),
            "private static class F {\n" +
            "    public void test() {\n" +
            "        final Object[] a = { null, null, new Object() };\n" +
            "        final int[] b = { 1, 2, 3 };\n" +
            "        final int[][] c = { { 1, 2, 3 }, { 4, 5, 6 } };\n" +
            "        final int[][][] d = { { { 1, 2, 3, 4 }, { 1, 2, 3, 4 } } };\n" +
            "        final byte[] e = { 100 };\n" +
            "        final double[][][] g = new double[3][4][5];\n" +
            "        final byte[][][][] h = new byte[3][4][5][6];\n" +
            "        final byte[][][][] i = new byte[3][4][5][];\n" +
            "    }\n" +
            "}\n"
        );
    }
}
