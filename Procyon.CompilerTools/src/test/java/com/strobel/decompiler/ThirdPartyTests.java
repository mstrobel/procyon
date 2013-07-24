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
}
