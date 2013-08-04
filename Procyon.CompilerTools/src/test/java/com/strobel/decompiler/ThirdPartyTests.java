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
            "}\n"
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
            "            n = (rectangle.isFat() ? 1 : 0);\n" +
            "            drawable = rectangle;\n" +
            "        }\n" +
            "        else {\n" +
            "            final Circle circle = new Circle(n);\n" +
            "            n = (circle.isFat() ? 1 : 0);\n" +
            "            drawable = circle;\n" +
            "        }\n" +
            "        if (n == 0) {\n" +
            "            drawable.draw();\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }
}
