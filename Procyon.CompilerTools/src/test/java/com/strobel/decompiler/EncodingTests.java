package com.strobel.decompiler;

import org.junit.Test;

public class EncodingTests extends DecompilerTest {
    static class UnicodeTest {
        static String \ufe4f\u2167;
        static final transient short x\u03a7x = 5;

        private static String __\u0130\u00dfI(final UnicodeTest x) {
            return UnicodeTest.\ufe4f\u2167;
        }

        static void test() {
            System.out.println(__\u0130\u00dfI(null));
            System.out.println("\0\u000fu\\\"\ff'\rr'\nn \u0123\u1234O\uffffF");
        }

        static {
            UnicodeTest.\ufe4f\u2167 = "\ufeff\ud800\ud8d8\udffd";
        }
    }

    @Test
    public void testUnicodeIdentifierEscaping() {
        verifyOutput(
            UnicodeTest.class,
            defaultSettings(),
            "static class UnicodeTest {\n" +
            "    static String \\ufe4f\\u2167;\n" +
            "    static final transient short x\\u03a7x = 5;\n" +
            "    private static String __\\u0130\\u00dfI(final UnicodeTest x) {\n" +
            "        return UnicodeTest.\\ufe4f\\u2167;\n" +
            "    }\n" +
            "    static void test() {\n" +
            "        System.out.println(__\\u0130\\u00dfI(null));\n" +
            "        System.out.println(\"\\0\\u000fu\\\\\\\"\\ff'\\rr'\\nn \\u0123\\u1234O\\uffffF\");\n" +
            "    }\n" +
            "    static {\n" +
            "        UnicodeTest.\\ufe4f\\u2167 = \"\\ufeff\\ud800\\ud8d8\\udffd\";\n" +
            "    }\n" +
            "}"
        );
    }
}
