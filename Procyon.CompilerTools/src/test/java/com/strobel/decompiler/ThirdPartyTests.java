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
            "            drawable = new Rectangle(n, n);\n" +
            "            n = (((Rectangle)drawable).isFat() ? 1 : 0);\n" +
            "            drawable = drawable;\n" +
            "        }\n" +
            "        else {\n" +
            "            drawable = new Circle(n);\n" +
            "            n = (((Circle)drawable).isFat() ? 1 : 0);\n" +
            "            drawable = drawable;\n" +
            "        }\n" +
            "        if (n == 0) {\n" +
            "            drawable.draw();\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }
}
