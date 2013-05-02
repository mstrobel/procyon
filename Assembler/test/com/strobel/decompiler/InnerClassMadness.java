package com.strobel.decompiler;

public class InnerClassMadness {

    public static void main(final String[] args) {
        final InnerClassMadness t = new InnerClassMadness();
        final A a = t.new A();
        final A.B b = a.new B(a);
        final A.D d = a.new D(a, b);
        final A.D d2 = new Object() {
            class Inner {
                A.D getD() {
                    final InnerClassMadness t = new InnerClassMadness();
                    final A a = t.new A();
                    final A.B b = a.new B(a);
                    final A.D d = a.new D(a, b);
                    return d;
                }
            }
        }.new Inner().getD();
    }

    public class A extends InnerClassMadness {
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


class Temp {
    public static void main(final String[] args) {
        new Temp().new A().new D(new Temp().new A(), new Temp().new A().new B(new Temp().new A()));
    }

    public class A extends Temp {
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