/*
 * InnerClassMadness.java
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

public class Evil {
    public static void main(final String[] args) {
        new Evil().new A().new D(new Evil().new A(), new Evil().new A().new B(new Evil().new A()), "one", 123);
    }

    public class A extends Evil {
        public A() {
        }

        public class B extends A {
            public B(final A a) {
                a.super();
            }

            public class C extends B {
                public C(final A a, final String x, final int y) {
                    a.super(a);
                }
            }
        }

        public class D extends B.C {
            public D(final A a, final B b, final String x, final int y) {
                b.super(a, x, y);
            }
        }
    }
}
