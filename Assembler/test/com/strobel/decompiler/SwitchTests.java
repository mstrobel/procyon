/*
 * SwitchTests.java
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

public class SwitchTests extends DecompilerTest {
    private static class A {
        public void test(final int i) {
            switch (i) {
                case 1:
                    System.out.print("1");
                    break;
                case 0:
                    System.out.print("0");
                    break;
                case -1:
                    System.out.print("-1");
                    break;
                default:
                    System.out.print("Bad Value");
                    break;
            }
            System.out.println("after switch");
        }
    }

    private static class B {
        public void test(final int i) {
            switch (i) {
                case -2:
                case -3:
                case 1:
                    System.out.print("1");
                case 0:
                    System.out.print("0");
                case -1:
                    System.out.print("-1");
                default:
                    System.out.print("end of fall through");
            }
            System.out.println("after switch");
        }
    }

    private static class C {
        public void test(final String s) {
            switch (s.toLowerCase()) {
                case "1":
                case "2":
                case "3":
                    System.out.println(s);
                    break;

                //
                // Include two strings with a hash code collision ("Aa", "BB").
                //
                case "Aa":
                case "BB":
                    System.out.println(s.toUpperCase());
                    break;

                default:
                    System.out.println(s);
                    break;
            }
            System.out.println("after switch");
        }
    }

    @Test
    public void testSimpleSwitch() {
        verifyOutput(
            A.class,
            createSettings(OPTION_FLATTEN_SWITCH_BLOCKS),
            "private static class A {\n" +
            "    public void test(int i) {\n" +
            "        switch (i) {\n" +
            "            case -1:\n" +
            "                System.out.print(\"-1\");\n" +
            "                break;\n" +
            "            case 0:\n" +
            "                System.out.print(\"0\");\n" +
            "                break;\n" +
            "            case 1:\n" +
            "                System.out.print(\"1\");\n" +
            "                break;\n" +
            "            default:\n" +
            "                System.out.print(\"Bad Value\");\n" +
            "                break;\n" +
            "        }\n" +
            "        System.out.println(\"after switch\");\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testSwitchFallThrough() {
        //
        // The decompiler is expected to forgo the 'default' label in cases where all
        // case blocks have edges connecting to the default block in a CFG.  This is
        // normally the desired behavior, though in the rare cases of total fall-through,
        // it results in slightly different code.
        //

        verifyOutput(
            B.class,
            createSettings(OPTION_FLATTEN_SWITCH_BLOCKS),
            "private static class B {\n" +
            "    public void test(int i) {\n" +
            "        switch (i) {\n" +
            "            case -3:\n" +
            "            case -2:\n" +
            "            case 1:\n" +
            "                System.out.print(\"1\");\n" +
            "            case 0:\n" +
            "                System.out.print(\"0\");\n" +
            "            case -1:\n" +
            "                System.out.print(\"-1\");\n" +
            "                break;\n" +
            "        }\n" +
            "        System.out.print(\"end of fall through\");\n" +
            "        System.out.println(\"after switch\");\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testStringSwitch() {
        verifyOutput(
            C.class,
            createSettings(OPTION_FLATTEN_SWITCH_BLOCKS),
            "private static class C {\n" +
            "    public void test(String s) {\n" +
            "        final String lowerCase = s.toLowerCase();\n" +
            "        switch (lowerCase) {\n" +
            "            case \"1\":\n" +
            "            case \"2\":\n" +
            "            case \"3\":\n" +
            "                System.out.println(s);\n" +
            "                break;\n" +
            "            case \"Aa\":\n" +
            "            case \"BB\":\n" +
            "                System.out.println(s.toUpperCase());\n" +
            "                break;\n" +
            "            default:\n" +
            "                System.out.println(s);\n" +
            "                break;\n" +
            "        }\n" +
            "        System.out.println(\"after switch\");\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testEnumSwitch() {
        verifyOutput(
            SwitchTests$D.class,
            createSettings(OPTION_FLATTEN_SWITCH_BLOCKS),
            "final class SwitchTests$D {\n" +
            "    public void test(Color color) {\n" +
            "        switch (color) {\n" +
            "            case BLUE:\n" +
            "                System.out.println(\"blue\");\n" +
            "                break;\n" +
            "            case RED:\n" +
            "                System.out.println(\"red\");\n" +
            "                break;\n" +
            "            default:\n" +
            "                System.out.println(\"other\");\n" +
            "                break;\n" +
            "        }\n" +
            "        System.out.println(\"after switch\");\n" +
            "    }\n" +
            "}\n"
        );
    }
}

//
// For the moment, we need to declare this test class out here to ensure the generated
// SwitchMap type is in scope.
//
final class SwitchTests$D {
    public enum Color {
        RED,
        BLUE;
    }

    public void test(final Color color) {
        switch (color) {
            case BLUE:
                System.out.println("blue");
                break;
            case RED:
                System.out.println("red");
                break;
            default:
                System.out.println("other");
                break;
        }
        System.out.println("after switch");
    }
}
