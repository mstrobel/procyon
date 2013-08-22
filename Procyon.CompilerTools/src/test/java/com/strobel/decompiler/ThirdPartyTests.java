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
            "    public static void f(short n) {\n" +
            "        Drawable drawable;\n" +
            "        if (n > 10) {\n" +
            "            final Rectangle rectangle = new Rectangle(n, n);\n" +
            "            n = (short)(rectangle.isFat() ? 1 : 0);\n" +
            "            drawable = rectangle;\n" +
            "        }\n" +
            "        else {\n" +
            "            final Circle circle = new Circle(n);\n" +
            "            n = (short)(circle.isFat() ? 1 : 0);\n" +
            "            takeMyBoolean(n != 0);\n" +
            "            drawable = circle;\n" +
            "        }\n" +
            "        if (n == 0) {\n" +
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
            "            try {\n" +
            "                final ArrayList list;\n" +
            "                System.out.println(list = (ArrayList)(Object)array);\n" +
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
            "        final int n = 0;\n" +
            "        final int n2 = 1;\n" +
            "        final int n3 = (((o == null) ? 1 : 0) == n) ? 1 : 0;\n" +
            "        final int n4 = (n2 != 0) ? ((n != 0) ? n2 : ((n != 0) ? n2 : n)) : ((n2 != 0) ? n : n2);\n" +
            "        final int n5 = ((n != 0) ? (n2 != 0) : (n3 != 0)) ? n4 : ((n3 != 0) ? n2 : n);\n" +
            "        return ((Number)o).shortValue();\n" +
            "    }\n" +
            "}"
        );
    }
}

