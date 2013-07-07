package com.strobel.decompiler;

import org.junit.Test;

public class BoxingTests extends DecompilerTest {
    @SuppressWarnings("InfiniteRecursion")
    private static class A {
        void a(final Integer i) {
            this.a(i);
            this.b(i);
            this.c(i);
        }

        void b(final int i) {
            this.a(i);
            this.b(i);
            this.c(i);
        }

        void c(final double d) {
            this.c(d);
            this.d(d);
        }

        void d(final Double d) {
            this.c(d);
            this.d(d);
        }

        void e(final Short s) {
            this.b(s);
            this.c(s);
            this.e(s);
            this.f(s);
        }

        void f(final short s) {
            this.b(s);
            this.c(s);
            this.e(s);
            this.f(s);
        }
    }

    @Test
    public void testImplicitBoxingTranslation() throws Throwable {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
            "    void a(final Integer i) {\n" +
            "        this.a(i);\n" +
            "        this.b(i);\n" +
            "        this.c((double)i);\n" +
            "    }\n" +
            "    void b(final int i) {\n" +
            "        this.a(i);\n" +
            "        this.b(i);\n" +
            "        this.c((double)i);\n" +
            "    }\n" +
            "    void c(final double d) {\n" +
            "        this.c(d);\n" +
            "        this.d(d);\n" +
            "    }\n" +
            "    void d(final Double d) {\n" +
            "        this.c(d);\n" +
            "        this.d(d);\n" +
            "    }\n" +
            "    void e(final Short s) {\n" +
            "        this.b((int)s);\n" +
            "        this.c((double)s);\n" +
            "        this.e(s);\n" +
            "        this.f(s);\n" +
            "    }\n" +
            "    void f(final short s) {\n" +
            "        this.b((int)s);\n" +
            "        this.c((double)s);\n" +
            "        this.e(s);\n" +
            "        this.f(s);\n" +
            "    }\n" +
            "}\n"
        );
    }
}
