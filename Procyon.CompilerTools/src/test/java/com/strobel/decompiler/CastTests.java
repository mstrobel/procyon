package com.strobel.decompiler;

import org.junit.Test;

public class CastTests extends DecompilerTest {
    private static class A {
        public static void test() {
            final Character c1 = '1';
            final Character c2 = '2';
            final Object o = '3';

            // A cast on either operand is required to force a primitive comparison; not redundant.
            System.out.println((char) c1 == c2);
            System.out.println(c1 == (char) c2);

            // If one operand is a primitive, and the other is a wrapper, the wrapper need not be cast.
            System.out.println((char) c1 == '*');
            System.out.println('*' == (char) c1);

            // The cast on the Object is required to force a primitive comparison; not redundant.
            System.out.println((char) o == '*');
            System.out.println('*' == (char) o);

            // The cast on the Object is required to force a primitive comparison; not redundant.
            System.out.println((Character) o == '*');
            System.out.println('*' == (Character) o);

            // The cast on the Object triggers an implicit unboxing of the wrapper; not redundant.
            System.out.println((char) o == c1);
            System.out.println(c1 == (char) o);

            // A cast on the Object is required for a primitive comparison, but the wrapper cast is redundant.
            System.out.println((char) o == (char) c1);
            System.out.println((char) c1 == (char) o);

            // Although a reference comparison, the cast on the wrapper has a side effect; not redundant.
            System.out.println(o == (char) c1);
            System.out.println((char) c1 == o);
        }
    }

    @Test
    public void testPrimitiveComparisonCastAnalysis() {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
            "    public static void test() {\n" +
            "        final Character c1 = '1';\n" +
            "        final Character c2 = '2';\n" +
            "        final Object o = '3';\n" +
            "        System.out.println(c1 == (char)c2);\n" +
            "        System.out.println(c1 == (char)c2);\n" +
            "        System.out.println(c1 == '*');\n" +
            "        System.out.println('*' == c1);\n" +
            "        System.out.println((char)o == '*');\n" +
            "        System.out.println('*' == (char)o);\n" +
            "        System.out.println((char)o == '*');\n" +
            "        System.out.println('*' == (char)o);\n" +
            "        System.out.println((char)o == c1);\n" +
            "        System.out.println(c1 == (char)o);\n" +
            "        System.out.println((char)o == c1);\n" +
            "        System.out.println(c1 == (char)o);\n" +
            "        System.out.println(o == Character.valueOf(c1));\n" +
            "        System.out.println(Character.valueOf(c1) == o);\n" +
            "    }\n" +
            "}"
        );
    }
}


