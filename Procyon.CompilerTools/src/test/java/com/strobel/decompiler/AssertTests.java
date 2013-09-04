package com.strobel.decompiler;

import org.junit.Test;

public class AssertTests extends DecompilerTest {
    private static class A {
        public void test(final String s) {
            assert s.equals("foo");
        }
    }

    private static class B {
        public void test(final String s) {
            assert s.equals("foo") : "Expected 'foo'.";
        }
    }

    private static class C {
        public void test(final String s) {
            assert s.equals("foo") : "Expected 'foo', got: " + s;
        }
    }

    @Test
    public void testSimpleAssert() throws Throwable {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
            "    public void test(final String s) {\n" +
            "        assert s.equals(\"foo\");\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testAssertWithLiteralMessage() throws Throwable {
        verifyOutput(
            B.class,
            defaultSettings(),
            "private static class B {\n" +
            "    public void test(final String s) {\n" +
            "        assert s.equals(\"foo\") : \"Expected 'foo'.\";\n" +
            "    }\n" +
            "}\n"
        );
    }
    @Test
    public void testAssertWithExpressionMessage() throws Throwable {
        verifyOutput(
            C.class,
            defaultSettings(),
            "private static class C {\n" +
            "    public void test(final String s) {\n" +
            "        assert s.equals(\"foo\") : \"Expected 'foo', got: \" + s;\n" +
            "    }\n" +
            "}\n"
        );
    }
}
