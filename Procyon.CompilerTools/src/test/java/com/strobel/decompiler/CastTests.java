package com.strobel.decompiler;

import org.junit.Test;

public class CastTests extends DecompilerTest {
    @SuppressWarnings("RedundantCast")
    private static final class A {
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

    private static final class B {
        public static short test(final short a, final short b) {
            final short c = (short) (a + b);
            System.out.println(c);
            return c;
        }
    }

    @Test
    public void testPrimitiveComparisonCastAnalysis() {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static final class A {\n" +
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

    @Test
    public void testShortIntegerAdditionRetainsCast() {
        verifyOutput(
            B.class,
            defaultSettings(),
            "private static final class B {\n" +
            "    public static short test(final short a, final short b) {\n" +
            "        final short c = (short)(a + b);\n" +
            "        System.out.println(c);\n" +
            "        return c;\n" +
            "    }\n" +
            "}"
        );
    }
}


