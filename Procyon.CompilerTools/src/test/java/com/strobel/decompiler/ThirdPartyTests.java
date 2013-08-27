package com.strobel.decompiler;

import org.junit.Test;

public class ThirdPartyTests extends DecompilerTest {
    @Test
    public void testDupX2Pop2() throws Throwable {
        verifyOutput(
            Class.forName("Hello"),
            defaultSettings(),
            "public class Hello {\n" +
            "    public static void main(final String[] array) {\n" +
            "        System.out.println(\"Goodbye world\");\n" +
            "    }\n" +
            "}"
        );
    }

    @Test
    public void testOptimizedVariables() throws Throwable {
        verifyOutput(
            Class.forName("SootOptimizationTest"),
            defaultSettings(),
            "public class SootOptimizationTest {\n" +
            "    public static void f(final short n) {\n" +
            "        boolean b;\n" +
            "        Drawable drawable;\n" +
            "        if (n > 10) {\n" +
            "            final Rectangle rectangle = new Rectangle(n, n);\n" +
            "            b = rectangle.isFat();\n" +
            "            drawable = rectangle;\n" +
            "        }\n" +
            "        else {\n" +
            "            final Circle circle = new Circle(n);\n" +
            "            b = circle.isFat();\n" +
            "            takeMyBoolean(b);\n" +
            "            drawable = circle;\n" +
            "        }\n" +
            "        if (!b) {\n" +
            "            drawable.draw();\n" +
            "        }\n" +
            "    }\n" +
            "    public static void takeMyBoolean(final boolean b) {\n" +
            "        System.out.println(b);\n" +
            "    }\n" +
            "}"
        );
    }

    @Test
    public void testOddsAndEnds() throws Throwable {
        verifyOutput(
            Class.forName("OddsAndEnds"),
            defaultSettings(),
            "public final strictfp class OddsAndEnds {\n" +
            "    private static strictfp void test(final float n, final Object o) {\n" +
            "        synchronized (o) {\n" +
            "            final long n2 = (long)n;\n" +
            "            if (o instanceof Long) {\n" +
            "                final long longValue = (long)o;\n" +
            "                if (longValue <= n2) {\n" +
            "                    System.out.println((float)(-longValue) % -n);\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "    public static strictfp void main(String[] array) {\n" +
            "        if (array != null) {\n" +
            "            final String[] array2 = array;\n" +
            "            try {\n" +
            "                final ArrayList list;\n" +
            "                System.out.println(list = (ArrayList)(Object)array2);\n" +
            "                array = (String[])list.toArray(new String[0]);\n" +
            "            }\n" +
            "            catch (ClassCastException ex) {\n" +
            "                array = array;\n" +
            "            }\n" +
            "        }\n" +
            "        test(42.24f, array);\n" +
            "        test(4.224f, Long.valueOf(array[0]));\n" +
            "        test(-0.0f, main(999999999L));\n" +
            "    }\n" +
            "    public static strictfp int main(final Object o) {\n" +
            "        final boolean b = false;\n" +
            "        final boolean b2 = true;\n" +
            "        final boolean b3 = o == null == b;\n" +
            "        final boolean b4 = b2 ? (b ? b2 : (b ? b2 : b)) : (b2 ? b : b2);\n" +
            "        final boolean b5 = (b ? b2 : b3) ? b4 : (b3 ? b2 : b);\n" +
            "        return ((Number)o).shortValue();\n" +
            "    }\n" +
            "}"
        );
    }

    @Test
    public void testSwitchKrakatau() throws Throwable {
        verifyOutput(
            Class.forName("Switch"),
            defaultSettings(),
            "public class Switch {\n" +
            "    public static strictfp void main(final String[] array) {\n" +
            "        int n = -1;\n" +
            "        switch (array.length % -5) {\n" +
            "            case 3: {\n" +
            "                n += 3;\n" +
            "            }\n" +
            "            case 1: {\n" +
            "                if (--n == -2) {\n" +
            "                    break;\n" +
            "                }\n" +
            "            }\n" +
            "            case 0: {\n" +
            "                n += n << n;\n" +
            "                break;\n" +
            "            }\n" +
            "            default: {\n" +
            "                n ^= 180146176;\n" +
            "            }\n" +
            "            case 4: {\n" +
            "                n *= 4;\n" +
            "                break;\n" +
            "            }\n" +
            "        }\n" +
            "        System.out.println(n);\n" +
            "        System.out.println(i(array.length));\n" +
            "    }\n" +
            "    public static int i(int n) {\n" +
            "        switch (n) {\n" +
            "            case 1:\n" +
            "            case 3: {\n" +
            "                throw null;\n" +
            "            }\n" +
            "            case 2: {\n" +
            "                n += 4;\n" +
            "                break;\n" +
            "            }\n" +
            "        }\n" +
            "        return -n;\n" +
            "    }\n" +
            "}"
        );
    }

    @Test
    public void testWhileLoopsKrakatau() throws Throwable {
        verifyOutput(
            Class.forName("WhileLoops"),
            defaultSettings(),
            "public class WhileLoops {\n" +
            "    static int x;\n" +
            "    static int i;\n" +
            "    static int i2;\n" +
            "    public static void main(final String[] array) {\n" +
            "        boolean b = array.length > 0;\n" +
            "        boolean b2 = array.length < 2;\n" +
            "        WhileLoops.x = 42;\n" +
            "        Label_0088: {\n" +
            "            Label_0073: {\n" +
            "                while (true) {\n" +
            "                    ++WhileLoops.x;\n" +
            "                    if (WhileLoops.x > 127) {\n" +
            "                        break Label_0073;\n" +
            "                    }\n" +
            "                    if (b ^ b2) {\n" +
            "                        break;\n" +
            "                    }\n" +
            "                    b &= true;\n" +
            "                    b2 |= false;\n" +
            "                }\n" +
            "                WhileLoops.x ^= array.length;\n" +
            "                break Label_0088;\n" +
            "            }\n" +
            "            WhileLoops.x = (WhileLoops.x ^ -1 ^ -1) >>> 3;\n" +
            "        }\n" +
            "        System.out.println(WhileLoops.x);\n" +
            "        System.out.println(b);\n" +
            "        System.out.println(b2);\n" +
            "        try {\n" +
            "            main(array[0]);\n" +
            "        }\n" +
            "        catch (IllegalArgumentException ex) {}\n" +
            "    }\n" +
            "    private static int foo() {\n" +
            "        --WhileLoops.i;\n" +
            "        return 4369;\n" +
            "    }\n" +
            "    private static void main(final String s) {\n" +
            "        final int intValue = Integer.valueOf(s);\n" +
            "        Block_4: {\n" +
            "            while (true) {\n" +
            "                if (WhileLoops.i2 < 0) {\n" +
            "                    if (intValue <= 1111 || WhileLoops.i <= foo()) {\n" +
            "                        break;\n" +
            "                    }\n" +
            "                }\n" +
            "                else {\n" +
            "                    WhileLoops.i = intValue;\n" +
            "                    if (++WhileLoops.i == 10) {\n" +
            "                        break Block_4;\n" +
            "                    }\n" +
            "                    if (++WhileLoops.i != 20) {\n" +
            "                        if (++WhileLoops.i == 30) {\n" +
            "                            break Block_4;\n" +
            "                        }\n" +
            "                        if (++WhileLoops.i == 50) {}\n" +
            "                    }\n" +
            "                    WhileLoops.i2 = WhileLoops.i - WhileLoops.i * WhileLoops.i;\n" +
            "                }\n" +
            "            }\n" +
            "            ++WhileLoops.i;\n" +
            "        }\n" +
            "        System.out.println(WhileLoops.i);\n" +
            "    }\n" +
            "}"
        );
    }

    @Test
    public void testSkipJsrKrakatau() throws Throwable {
        verifyOutput(
            Class.forName("SkipJSR"),
            defaultSettings(),
            "public class SkipJSR {\n" +
            "    public static void main(final String[] array) {\n" +
            "        int n = 1 + array.length;\n" +
            "        final double[] array2 = { n };\n" +
            "        ++n;\n" +
            "        System.out.println(array2[0] / (n + array.length + array.length));\n" +
            "    }\n" +
            "}"
        );
    }

    @Test
    public void testArgumentTypesKrakatau() throws Throwable {
        verifyOutput(
            Class.forName("ArgumentTypes"),
            defaultSettings(),
            "public class ArgumentTypes {\n" +
            "    public static int main(final boolean b) {\n" +
            "        return b ? 1 : 0;\n" +
            "    }\n" +
            "    public static boolean main(final int n) {\n" +
            "        return n <= 42;\n" +
            "    }\n" +
            "    public static char main(final char c) {\n" +
            "        return c ^ '*';\n" +
            "    }\n" +
            "    public static String main(final Object o) {\n" +
            "        if (o instanceof boolean[]) {\n" +
            "            return Arrays.toString((boolean[])o);\n" +
            "        }\n" +
            "        if (o instanceof String[]) {\n" +
            "            return null;\n" +
            "        }\n" +
            "        if (o instanceof int[]) {\n" +
            "            return \"\" + ((int[])o)[0];\n" +
            "        }\n" +
            "        return Arrays.toString((byte[])o);\n" +
            "    }\n" +
            "    public static void main(final String[] array) {\n" +
            "        final int intValue = Integer.decode(array[0]);\n" +
            "        final boolean booleanValue = Boolean.valueOf(array[1]);\n" +
            "        System.out.println(main(intValue));\n" +
            "        System.out.println(main(booleanValue));\n" +
            "        final byte[] array2 = { 1, 2, 3, 45, 6 };\n" +
            "        final boolean[] array3 = { false, true, false };\n" +
            "        System.out.println(main((Object)array));\n" +
            "        System.out.println(main(array3));\n" +
            "        System.out.println(main(array2));\n" +
            "        final char c = 'C';\n" +
            "        System.out.println(c);\n" +
            "        System.out.println((int)c);\n" +
            "    }\n" +
            "    public static byte[] main(final byte[][] array) {\n" +
            "        if (array.length > 0) {\n" +
            "            return array[0];\n" +
            "        }\n" +
            "        return null;\n" +
            "    }\n" +
            "}"
        );
    }
}

