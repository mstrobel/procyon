package com.strobel.decompiler;

@SuppressWarnings("UnusedDeclaration")
public class SyncTests extends DecompilerTest {
    private static class A {
        public String test(final Object o) {
            synchronized (o) {
                return "";
            }
        }
    }

    private static class B {
        public String test(final Object o, final Object p) {
            synchronized (o) {
                synchronized (p) {
                    return "";
                }
            }
        }
    }

    private static class C {
        public String test(final Object o, final Object p, final Object q) {
            synchronized (o) {
                synchronized (p) {
                    synchronized (q) {
                        return "";
                    }
                }
            }
        }
    }

    private static class D {
        public String test(final Object o, final Object p, final Object q, final Object r) {
            synchronized (o) {
                synchronized (p) {
                    synchronized (q) {
                        synchronized (r) {
                            return "";
                        }
                    }
                }
            }
        }
    }

    private static class E {
        public String test(final Object o, final Object p, final Object q, final Object r) {
            final String result;

            synchronized (o) {
                System.out.println("enter(o)");
                synchronized (p) {
                    System.out.println("enter(p)");
                    synchronized (q) {
                        System.out.println("enter(q)");
                        synchronized (r) {
                            System.out.println("enter(r)");
                            result = "";
                            System.out.println("exit(r)");
                        }
                        System.out.println("exit(q)");
                    }
                    System.out.println("exit(p)");
                }
                System.out.println("exit(o)");
            }

            return result;
        }
    }
}
