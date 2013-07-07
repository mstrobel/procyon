package com.strobel.decompiler;

import com.strobel.util.EmptyArrayCache;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

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
            double n = 9.007199254740992E15;
            double n2 = n * n;
            double n3 = 9.007199254740991E15;
            double n4 = n2 % n3;

            System.out.println(n4);
            System.out.println(n4 == 1.0);
            System.out.println(n4 * 2.7182818459);
        }
    }

    private interface C {
        public static final Integer[] EMPTY_ARRAY = EmptyArrayCache.fromElementType(Integer.class);
    }

    private interface D {
        public static final List<Integer> EMPTY_ARRAY = Arrays.asList(1, 2, 3, 4, 5);
    }

    private static class E {
        Integer[] f(final Integer[] array) {
            return array;
        }

        public void test() {
            this.f(new Integer[0]);
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
            "        final double n = 9.007199254740992E15;\n" +
            "        final double n2 = n * n;\n" +
            "        final double n3 = 9.007199254740991E15;\n" +
            "        final double n4 = n2 % n3;\n" +
            "        System.out.println(n4);\n" +
            "        System.out.println(n4 == 1.0);\n" +
            "        System.out.println(n4 * 2.7182818459);\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testClassArgument() throws Throwable {
        verifyOutput(
            C.class,
            defaultSettings(),
            "private interface C {\n" +
            "    public static final Integer[] EMPTY_ARRAY = (Integer[])EmptyArrayCache.fromElementType(Integer.class);\n" +
            "}\n"
        );
    }

    @Test
    public void testGenericArrayArgument() throws Throwable {
        verifyOutput(
            D.class,
            defaultSettings(),
            "private interface D {\n" +
            "    public static final List<Integer> EMPTY_ARRAY = Arrays.asList(new Integer[] { 1, 2, 3, 4, 5 });\n" +
            "}\n"
        );
    }

    @Test
    public void testMatchingArrayArgument() throws Throwable {
        verifyOutput(
            E.class,
            defaultSettings(),
            "private static class E {\n" +
            "    Integer[] f(final Integer[] array) {\n" +
            "        return array;\n" +
            "    }\n" +
            "    public void test() {\n" +
            "        this.f(new Integer[0]);\n" +
            "    }\n" +
            "}\n"
        );
    }
}
