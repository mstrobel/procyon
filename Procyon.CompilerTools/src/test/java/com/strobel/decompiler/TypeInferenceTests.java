package com.strobel.decompiler;

import org.junit.Test;

public class TypeInferenceTests extends DecompilerTest {
    private static class A {
        public void test(final String[] array) {
            boolean b = array.length > 0;
            boolean b2 = array.length < 2;

            int n = 0;

            while (n++ <= 127) {
                if (!(b ^ b2)) {
                    n ^= array.length;
                    break;
                }
                b &= true;
                b2 |= false;
            }

            System.out.println(n);
            System.out.println(b);
            System.out.println(b2);
        }
    }

    private static class B {
        @SuppressWarnings("LocalCanBeFinal")
        public strictfp void test() {
            double n = 9007199254740992d;
            double n2 = n * n;
            double n3 = 9007199254740991d;
            double n4 = n2 % n3;

            System.out.println(n4);
            System.out.println(n4 == 1d);
            System.out.println(n4 * 2.7182818459);
        }
    }

    @Test
    public void testBooleanInference() throws Throwable {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
            "    public void test(final String[] array) {\n" +
            "        boolean b = array.length > 0;\n" +
            "        boolean b2 = array.length < 2;\n" +
            "        int n = 0;\n" +
            "        while (n++ <= 127) {\n" +
            "            if (!(b ^ b2)) {\n" +
            "                n ^= array.length;\n" +
            "                break;\n" +
            "            }\n" +
            "            b &= true;\n" +
            "            b2 |= false;\n" +
            "        }\n" +
            "        System.out.println(n);\n" +
            "        System.out.println(b);\n" +
            "        System.out.println(b2);\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testDoubleVariables() throws Throwable {
        verifyOutput(
            B.class,
            defaultSettings(),
            "private static class B {\n" +
            "    public strictfp void test() {\n" +
            "        final double n = 9007199254740992d;\n" +
            "        final double n2 = n * n;\n" +
            "        final double n3 = 9007199254740991d;\n" +
            "        final double n4 = n2 % n3;\n" +
            "        System.out.println(n4);\n" +
            "        System.out.println(n4 == 1d);\n" +
            "        System.out.println(n4 * 2.7182818459);\n" +
            "    }\n" +
            "}\n"
        );
    }
}
