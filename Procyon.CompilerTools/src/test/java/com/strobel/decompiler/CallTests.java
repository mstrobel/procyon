package com.strobel.decompiler;

import org.junit.Ignore;
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

    private static class B {
        private void f(final String s) {
        }

        void g(final String s) {
        }

        class C extends B {
            private void f(final String s) {
            }

            void g(final String s) {
            }

            private void h() {
                B.this.f("B.f()");
                C.super.f("B.f()");
                C.this.f("C.f()");
                this.f("C.f()");
                super.f("B.f()");

                B.this.g("B.g()");
                C.super.g("B.g()");
                C.this.g("C.g()");
                this.g("C.g()");
                super.g("B.g()");
            }

            class D extends C {
                private void f(final String s) {
                }

                void g(final String s) {
                }

                private void h() {
                    B.this.f("B.f()");
                    C.this.f("C.f()");
                    C.super.f("B.f()");
                    D.super.f("C.f()");
                    D.this.f("D.f()");
                    super.f("C.f()");
                    this.f("D.f()");

                    B.this.g("B.g()");
                    C.this.g("C.g()");
                    C.super.g("B.g()");
                    D.super.g("C.g()");
                    D.this.g("D.g()");
                    super.g("C.g()");
                    this.g("D.g()");
                }
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

    @Test
    public void testOuterSuperMethodCalls() throws Throwable {
        verifyOutput(
            B.class,
            defaultSettings(),
            "private static class B {\n" +
            "    private void f(final String s) {\n" +
            "    }\n" +
            "    void g(final String s) {\n" +
            "    }\n" +
            "    class C extends B {\n" +
            "        private void f(final String s) {\n" +
            "        }\n" +
            "        @Override\n" +
            "        void g(final String s) {\n" +
            "        }\n" +
            "        private void h() {\n" +
            "            B.this.f(\"B.f()\");\n" +
            "            B.this.f(\"B.f()\");\n" +
            "            this.f(\"C.f()\");\n" +
            "            this.f(\"C.f()\");\n" +
            "            B.this.f(\"B.f()\");\n" +
            "            B.this.g(\"B.g()\");\n" +
            "            B.this.g(\"B.g()\");\n" +
            "            this.g(\"C.g()\");\n" +
            "            this.g(\"C.g()\");\n" +
            "            super.g(\"B.g()\");\n" +
            "        }\n" +
            "        class D extends C {\n" +
            "            private void f(final String s) {\n" +
            "            }\n" +
            "            @Override\n" +
            "            void g(final String s) {\n" +
            "            }\n" +
            "            private void h() {\n" +
            "                B.this.f(\"B.f()\");\n" +
            "                C.this.f(\"C.f()\");\n" +
            "                B.this.f(\"B.f()\");\n" +
            "                C.this.f(\"C.f()\");\n" +
            "                this.f(\"D.f()\");\n" +
            "                C.this.f(\"C.f()\");\n" +
            "                this.f(\"D.f()\");\n" +
            "                B.this.g(\"B.g()\");\n" +
            "                C.this.g(\"C.g()\");\n" +
            "                B.this.g(\"B.g()\");\n" +
            "                C.this.g(\"C.g()\");\n" +
            "                this.g(\"D.g()\");\n" +
            "                super.g(\"C.g()\");\n" +
            "                this.g(\"D.g()\");\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }
}
