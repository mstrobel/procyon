package com.strobel.decompiler;

import org.junit.Test;

public class EncodingTests extends DecompilerTest {
    private static final class A {
        static String \ufe4f\u2167;
        static final transient short x\u03a7x = 5;

        private static String __\u0130\u00dfI(final A x) {
            return A.\ufe4f\u2167;
        }

        static void test() {
            System.out.println(__\u0130\u00dfI(null));
            System.out.println("\"\0\u000fu\\\"\ff'\rr'\nn \u0123\u1234O\uffffF");
        }

        static {
            A.\ufe4f\u2167 = "\ufeff\ud800\ud8d8\udffd";
        }
    }

    private static final class B {
        void \u0442\u0435\u0441\u0442() {
            System.out.println('\u0434');
            System.out.println("\u042d\u0442\u043e \u043a\u043e\u0434\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0442\u0435\u0441\u0442.");
        }
    }

    @Test
    public void testUnicodeIdentifierEscaping() {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static final class A {\n" +
            "    static String \\ufe4f\\u2167;\n" +
            "    static final transient short x\\u03a7x = 5;\n" +
            "    private static String __\\u0130\\u00dfI(final A x) {\n" +
            "        return A.\\ufe4f\\u2167;\n" +
            "    }\n" +
            "    static void test() {\n" +
            "        System.out.println(__\\u0130\\u00dfI(null));\n" +
            "        System.out.println(\"\\\"\\0\\u000fu\\\\\\\"\\ff'\\rr'\\nn \\u0123\\u1234O\\uffffF\");\n" +
            "    }\n" +
            "    static {\n" +
            "        A.\\ufe4f\\u2167 = \"\\ufeff\\ud800\\ud8d8\\udffd\";\n" +
            "    }\n" +
            "}"
        );
    }

    @Test
    public void testCyrillicEscaped() {
        verifyOutput(
            B.class,
            defaultSettings(),
            "private static final class B {\n" +
            "    void \\u0442\\u0435\\u0441\\u0442() {\n" +
            "        System.out.println('\\u0434');\n" +
            "        System.out.println(\"\\u042d\\u0442\\u043e \\u043a\\u043e\\u0434\\u0438\\u0440\\u043e\\u0432\\u0430\\u043d\\u0438\\u0435 \\u0442\\u0435\\u0441\\u0442.\");\n" +
            "    }\n" +
            "}"
        );

    }

    @Test
    public void testCyrillicUnescaped() {
        verifyOutput(
            B.class,
            createSettings(OPTION_ENABLE_UNICODE_OUTPUT),
            "private static final class B {\n" +
            "    void тест() {\n" +
            "        System.out.println('д');\n" +
            "        System.out.println(\"Это кодирование тест.\");\n" +
            "    }\n" +
            "}"
        );

    }
}
