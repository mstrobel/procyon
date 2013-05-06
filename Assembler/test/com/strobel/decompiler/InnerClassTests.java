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

import org.junit.Test;

import java.util.Iterator;

@SuppressWarnings("UnusedDeclaration")
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

        public Iterable<String> test(final boolean z) {
            return new Iterable<String>() {
                private final boolean y = z;

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

        public Iterable<String> test(final boolean z) {
            final class MethodScopedIterable implements Iterable<String> {
                private final boolean y = z;

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
            "    public B(A a) {\n" +
            "        a.super();\n" +
            "    }\n" +
            "}"
        );

        verifyOutput(
            T.A.B.C.class,
            defaultSettings(),
            "public class C extends B\n" +
            "{\n" +
            "    public C(A a) {\n" +
            "        a.super(a);\n" +
            "    }\n" +
            "}"
        );

        verifyOutput(
            T.A.D.class,
            defaultSettings(),
            "public class D extends B.C\n" +
            "{\n" +
            "    public D(A a, B b) {\n" +
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
            "    public Iterable<String> test(boolean z) {\n" +
            "        return new Iterable<String>() {\n" +
            "            private final boolean y = z;\n" +
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
            "    public Iterable<String> test(boolean z) {\n" +
            "        final class MethodScopedIterable implements Iterable<String> {\n" +
            "            private final boolean y = z;\n" +
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
            "}\n");
    }
}
