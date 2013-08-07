package com.strobel.decompiler;

import org.junit.Test;

public class AssertTests extends DecompilerTest {
    private static class A {
        public void test(final String s) {
            assert s.equals("foo");
        }
    }

    @Test
    public void testSimpleAssert() throws Throwable {
        verifyOutput(
            A.class,
            createSettings(OPTION_INCLUDE_NESTED),
            "private static class A {\n" +
            "    public void test(final String s) {\n" +
            "        assert s.equals(\"foo\");\n" +
            "    }\n" +
            "}\n"
        );
    }
}
