/*
 * InnerClassTests.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler;

import com.strobel.annotations.NotNull;
import com.strobel.functions.Function;
import org.junit.Test;

import java.util.Iterator;

@SuppressWarnings({ "UnusedDeclaration", "UnnecessaryLocalVariable" })
public class InnerClassTests extends DecompilerTest {
    private class T {
        void test() {
            final T t = new T();
            final A a = t.new A();
            final A.B b = a.new B(a);
            final A.D d = a.new D(a, b);
            final A.D d2 = new Object() {
                class Inner {
                    A.D getD() {
                        final T t = new T();
                        final A a = t.new A();
                        final A.B b = a.new B(a);
                        final A.D d = a.new D(a, b);
                        return d;
                    }
                }
            }.new Inner().getD();
        }

        public class A extends T {
            public A() {
            }

            public class B extends A {
                public B(final A a) {
                    a.super();
                }

                public class C extends B {
                    public C(final A a) {
                        a.super(a);
                    }
                }
            }

            public class D extends B.C {
                public D(final A a, final B b) {
                    b.super(a);
                }
            }
        }
    }

    private static class A {
        private boolean x;

        public Iterable<String> test(final boolean b) {
            return new Iterable<String>() {
                private final boolean y = b;

                @NotNull
                @Override
                public Iterator<String> iterator() {
                    return new Iterator<String>() {
                        @Override
                        public boolean hasNext() {
                            return x && y;
                        }

                        @Override
                        public String next() {
                            return null;
                        }

                        @Override
                        public void remove() {
                        }
                    };
                }
            };
        }
    }

    private static class B {
        private boolean x;

        public Iterable<String> test(final boolean b) {
            final class MethodScopedIterable implements Iterable<String> {
                private final boolean y = b;

                @NotNull
                @Override
                public Iterator<String> iterator() {
                    return new Iterator<String>() {
                        @Override
                        public boolean hasNext() {
                            return B.this.x && MethodScopedIterable.this.y;
                        }

                        @Override
                        public String next() {
                            return null;
                        }

                        @Override
                        public void remove() {
                        }
                    };
                }
            }

            System.out.println(new MethodScopedIterable());

            return new MethodScopedIterable();
        }
    }

    private static class C {
        public static void test() {
            final C c = null;
            c.new A(6);
        }

        class A {
            int j;

            A(final int j) {
                super();
                this.j = j;
            }
        }
    }

    private static class D {
        final int k;

        D(final int k) {
            super();
            this.k = k;
        }

        public static void test() {
            final int n;

            final int value = new D(2 + (n = 3)) {
                int get() {
                    return this.k + n;
                }
            }.get();

            if (value != 8) {
                throw new Error("value = " + value);
            }
        }
    }

    static interface E {
        public static final I i = new I() {};
    }

    static interface I {}

    private static class F {
        public static void test() {
            final Object o = new Error() {};
            System.out.println(o.getClass().isAnonymousClass());
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private static class G {
        private static int x;
        private int y;

        public class A {
            private int z;

            public A(final int z) {
                super();
                G.x += this.z * G.this.y;
                this.z = z + 1 + G.this.y;
                G.this.y += this.z * G.x;
            }
        }
    }

    @SuppressWarnings("UnnecessaryBoxing")
    private static class H {
        private static final Runnable runnable;

        public static void test() {
            H.runnable.run();
        }

        static {
            runnable = new Runnable() {
                private final Integer mCount = new Integer(2);

                public void run() {
                    System.out.println("Runnable: mCount = " + this.mCount);
                }
            };
        }
    }

    private static class J {
        Integer f(final int x, final Function<Integer, Integer> h) {
            return h.apply(x);
        }

        private static final class F implements Function<Integer, Integer> {
            @Override
            public Integer apply(final Integer x) {
                return x * 3;
            }
        }
    }

    @SuppressWarnings("UnusedParameters")
    private static class K {
        private static int f(final int x) {
            return x / 2;
        }

        private void g(final int x) {
            f(x * 2);
        }

        Object h() {
            return new Object() {
                void j() {
                    f(16);
                    K.this.g(8);
                }
            };
        }
    }

    @Test
    public void testComplexInnerClassRelations() {
        //
        // NOTE: Currently, this test will fail if T is compiled with Eclipse.  Apparently the Eclipse
        //       compiler chooses not to emit debug info for variables which are never referenced.
        //
        // TODO: Compile test code manually to ensure the expected compiler is used.
        //

        verifyOutput(
            T.class,
            defaultSettings(),
            "private class T\n" +
            "{\n" +
            "    void test() {\n" +
            "        final T t = new T();\n" +
            "        final A a = t.new A();\n" +
            "        final A.B b = a.new B(a);\n" +
            "        final A.D d = a.new D(a, b);\n" +
            "        final A.D d2 = new Object() {\n" +
            "            class Inner\n" +
            "            {\n" +
            "                A.D getD() {\n" +
            "                    final T t = new T();\n" +
            "                    final A a = t.new A();\n" +
            "                    final A.B b = a.new B(a);\n" +
            "                    final A.D d = a.new D(a, b);\n" +
            "                    return d;\n" +
            "                }\n" +
            "            }\n" +
            "        }.new Inner().getD();\n" +
            "    }\n" +
            "}"
        );

        verifyOutput(
            T.A.class,
            defaultSettings(),
            "public class A extends T\n" +
            "{\n" +
            "}"
        );

        verifyOutput(
            T.A.B.class,
            defaultSettings(),
            "public class B extends A\n" +
            "{\n" +
            "    public B(final A a) {\n" +
            "        a.super();\n" +
            "    }\n" +
            "}"
        );

        verifyOutput(
            T.A.B.C.class,
            defaultSettings(),
            "public class C extends B\n" +
            "{\n" +
            "    public C(final A a) {\n" +
            "        a.super(a);\n" +
            "    }\n" +
            "}"
        );

        verifyOutput(
            T.A.D.class,
            defaultSettings(),
            "public class D extends B.C\n" +
            "{\n" +
            "    public D(final A a, final B b) {\n" +
            "        b.super(a);\n" +
            "    }\n" +
            "}"
        );
    }

    @Test
    public void testAnonymousLocalClassCreation() {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
            "    private boolean x;\n" +
            "    public Iterable<String> test(final boolean b) {\n" +
            "        return new Iterable<String>() {\n" +
            "            private final boolean y = b;\n" +
            "            @NotNull\n" +
            "            public Iterator<String> iterator() {\n" +
            "                return new Iterator<String>() {\n" +
            "                    public boolean hasNext() {\n" +
            "                        return A.this.x && Iterable.this.y;\n" +
            "                    }\n" +
            "                    public String next() {\n" +
            "                        return null;\n" +
            "                    }\n" +
            "                    public void remove() {\n" +
            "                    }\n" +
            "                };\n" +
            "            }\n" +
            "        };\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testNamedLocalClassCreation() {
        verifyOutput(
            B.class,
            createSettings(OPTION_INCLUDE_NESTED),
            "private static class B {\n" +
            "    private boolean x;\n" +
            "    public Iterable<String> test(final boolean b) {\n" +
            "        final class MethodScopedIterable implements Iterable<String> {\n" +
            "            private final boolean y = b;\n" +
            "            @NotNull\n" +
            "            public Iterator<String> iterator() {\n" +
            "                return new Iterator<String>() {\n" +
            "                    public boolean hasNext() {\n" +
            "                        return B.this.x && MethodScopedIterable.this.y;\n" +
            "                    }\n" +
            "                    public String next() {\n" +
            "                        return null;\n" +
            "                    }\n" +
            "                    public void remove() {\n" +
            "                    }\n" +
            "                };\n" +
            "            }\n" +
            "        }\n" +
            "        System.out.println(new MethodScopedIterable());\n" +
            "        return new MethodScopedIterable();\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testNullQualifiedInnerClassCreation() {
        verifyOutput(
            C.class,
            createSettings(OPTION_INCLUDE_NESTED),
            "private static class C {\n" +
            "    public static void test() {\n" +
            "        final C c = null;\n" +
            "        c.new A(6);\n" +
            "    }\n" +
            "    class A {\n" +
            "        int j;\n" +
            "        A(final int j) {\n" +
            "            super();\n" +
            "            this.j = j;\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testAnonymousClassWithAssignmentAsConstructorParameter() {
        verifyOutput(
            D.class,
            createSettings(OPTION_INCLUDE_NESTED),
            "private static class D {\n" +
            "    final int k;\n" +
            "    D(final int k) {\n" +
            "        super();\n" +
            "        this.k = k;\n" +
            "    }\n" +
            "    public static void test() {\n" +
            "        final int n;\n" +
            "        final int value = new D(2 + (n = 3)) {\n" +
            "            int get() {\n" +
            "                return this.k + n;\n" +
            "            }\n" +
            "        }.get();\n" +
            "        if (value != 8) {\n" +
            "            throw new Error(\"value = \" + value);\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testAnonymousClassInInterface() {
        verifyOutput(
            E.class,
            defaultSettings(),
            "interface E {\n" +
            "    public static final I i = new I() {};\n" +
            "}\n"
        );
    }

    @Test
    public void testEmptyAnonymousClass() {
        verifyOutput(
            F.class,
            defaultSettings(),
            "private static class F {\n" +
            "    public static void test() {\n" +
            "        final Object o = new Error() {};\n" +
            "        System.out.println(o.getClass().isAnonymousClass());\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testInnerClassSetsOuterClassField() {
        verifyOutput(
            G.class,
            createSettings(OPTION_INCLUDE_NESTED),
            "private static class G {\n" +
            "    private static int x;\n" +
            "    private int y;\n" +
            "    public class A {\n" +
            "        private int z;\n" +
            "        public A(final int z) {\n" +
            "            super();\n" +
            "            G.x += this.z * G.this.y;\n" +
            "            this.z = z + 1 + G.this.y;\n" +
            "            G.this.y += this.z * G.x;\n" +
            "        }\n" +
            "    }\n" +
            "}"
        );
    }

    @Test
    public void testInnerClassSyntheticConstructorInitializersInlined() {
        verifyOutput(
            H.class,
            createSettings(OPTION_INCLUDE_NESTED),
            "private static class H {\n" +
            "    private static final Runnable runnable;\n" +
            "    public static void test() {\n" +
            "        H.runnable.run();\n" +
            "    }\n" +
            "    static {\n" +
            "        runnable = new Runnable() {\n" +
            "            private final Integer mCount = new Integer(2);\n" +
            "            public void run() {\n" +
            "                System.out.println(\"Runnable: mCount = \" + this.mCount);\n" +
            "            }\n" +
            "        };\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testInnerClassSyntheticPrivateBridge() {
        verifyOutput(
            J.class,
            createSettings(OPTION_INCLUDE_NESTED),
            "private static class J {\n" +
            "    Integer f(final int x, final Function<Integer, Integer> h) {\n" +
            "        return h.apply(x);\n" +
            "    }\n" +
            "    private static final class F implements Function<Integer, Integer> {\n" +
            "        public Integer apply(final Integer x) {\n" +
            "            return x * 3;\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testInnerClassSyntheticCallWrappers() {
        verifyOutput(
            K.class,
            createSettings(OPTION_INCLUDE_NESTED),
            "private static class K {\n" +
            "    private static int f(final int x) {\n" +
            "        return x / 2;\n" +
            "    }\n" +
            "    private void g(final int x) {\n" +
            "        f(x * 2);\n" +
            "    }\n" +
            "    Object h() {\n" +
            "        return new Object() {\n" +
            "            void j() {\n" +
            "                f(16);\n" +
            "                K.this.g(8);\n" +
            "            }\n" +
            "        };\n" +
            "    }\n" +
            "}\n"
        );
    }
}
