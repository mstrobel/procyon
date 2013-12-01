package com.strobel.decompiler;

import org.junit.Test;

public class CallTests extends DecompilerTest {
    private static class A {
        void f() {
        }

        static class B extends A {
            void f() {
            }

            void g() {
                this.f();
                super.f();
            }
        }
    }

    @Test
    public void testSuperMethodCall() throws Throwable {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
            "    void f() {\n" +
            "    }\n" +
            "    static class B extends A {\n" +
            "        @Override\n" +
            "        void f() {\n" +
            "        }\n" +
            "        void g() {\n" +
            "            this.f();\n" +
            "            super.f();\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }
}
