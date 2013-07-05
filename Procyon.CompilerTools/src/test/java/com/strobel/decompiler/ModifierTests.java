/*
 * ModifierTests.java
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

public class ModifierTests extends DecompilerTest {
    @SuppressWarnings({ "UnusedParameters", "FinalStaticMethod", "UnusedDeclaration" })
    strictfp static abstract class A {
        public static final int i = 0;
        protected byte b;
        float f;
        private transient volatile double d;

        public static synchronized final void m(final String... args) {
            for (char c = 32; c < 100; ++c) {
                System.out.println(c + " " + (int) c);
            }
        }
    }

    @Test
    public void testModifiers() {
        verifyOutput(
            A.class,
            defaultSettings(),
            "abstract static strictfp class A\n" +
            "{\n" +
            "    public static final int i = 0;\n" +
            "    protected byte b;\n" +
            "    float f;\n" +
            "    private transient volatile double d;\n" +
            "    public static final synchronized strictfp void m(final String... args) {\n" +
            "        for (char c = ' '; c < 'd'; c = (char)(c + '\\u0001')) {\n" +
            "            System.out.println(c + \" \" + c);\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }
}
