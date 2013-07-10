package com.strobel.decompiler;

import org.junit.Test;

public class BoxingTests extends DecompilerTest {
    @SuppressWarnings("InfiniteRecursion")
    private static class A {
        void a(final Integer i) {
            this.a(i);
            this.b(i);
            this.c(i);
            this.d((double) i);
            this.e((short) i.intValue());
            this.f((short) i.intValue());
        }

        void b(final int i) {
            this.a(i);
            this.b(i);
            this.c((double) i);
            this.d((double) i);
            this.e((short) i);
            this.f((short) i);
        }

        void c(final double d) {
            this.a((int) d);
            this.b((int) d);
            this.c(d);
            this.d(d);
            this.e((short) d);
            this.f((short) d);
        }

        void d(final Double d) {
            this.a((int) d.doubleValue());
            this.b((int) d.doubleValue());
            this.c(d);
            this.d(d);
            this.e((short) d.doubleValue());
            this.f((short) d.doubleValue());
        }

        void e(final Short s) {
            this.a((int) s);
            this.b(s);
            this.c(s);
            this.d((double) s);
            this.e(s);
            this.f(s);
        }

        void f(final short s) {
            this.a((int) s);
            this.b((int) s);
            this.c((double) s);
            this.d((double) s);
            this.e(s);
            this.f(s);
        }
    }

    private static class B {
        boolean test(final Integer n) {
            return Integer.valueOf(n.intValue()) != null;
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
            "        this.c(i);\n" +
            "        this.d((double)i);\n" +
            "        this.e((short)i.intValue());\n" +
            "        this.f((short)i.intValue());\n" +
            "    }\n" +
            "    void b(final int i) {\n" +
            "        this.a(i);\n" +
            "        this.b(i);\n" +
            "        this.c((double)i);\n" +
            "        this.d((double)i);\n" +
            "        this.e((short)i);\n" +
            "        this.f((short)i);\n" +
            "    }\n" +
            "    void c(final double d) {\n" +
            "        this.a((int)d);\n" +
            "        this.b((int)d);\n" +
            "        this.c(d);\n" +
            "        this.d(d);\n" +
            "        this.e((short)d);\n" +
            "        this.f((short)d);\n" +
            "    }\n" +
            "    void d(final Double d) {\n" +
            "        this.a((int)d.doubleValue());\n" +
            "        this.b((int)d.doubleValue());\n" +
            "        this.c(d);\n" +
            "        this.d(d);\n" +
            "        this.e((short)d.doubleValue());\n" +
            "        this.f((short)d.doubleValue());\n" +
            "    }\n" +
            "    void e(final Short s) {\n" +
            "        this.a((int)s);\n" +
            "        this.b(s);\n" +
            "        this.c(s);\n" +
            "        this.d((double)s);\n" +
            "        this.e(s);\n" +
            "        this.f(s);\n" +
            "    }\n" +
            "    void f(final short s) {\n" +
            "        this.a((int)s);\n" +
            "        this.b((int)s);\n" +
            "        this.c((double)s);\n" +
            "        this.d((double)s);\n" +
            "        this.e(s);\n" +
            "        this.f(s);\n" +
            "    }\n" +
            "}"
        );
    }

    @Test
    public void testExceptionalUnboxingNotOmitted() throws Exception {
        verifyOutput(
           B.class,
           defaultSettings(),
           "private static class B {\n" +
           "    boolean test(final Integer n) {\n" +
           "        return Integer.valueOf(n) != null;\n" +
           "    }\n" +
           "}\n"
        );
    }
}
