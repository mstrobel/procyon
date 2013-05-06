/*
 * OperatorTests.java
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

import java.util.Date;

public class OperatorTests extends DecompilerTest {
    private static class A {
        public String test(final String s, final char c, final byte b, final float f, final Date d) {
            return b + ":" + c + ":" + s + ":" + f + ":" + d;
        }
    }
    
    @Test
    public void testStringConcatenation() {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
            "    public String test(String s, char c, byte b, float f, Date d) {\n" +
            "        return b + \":\" + c + \":\" + s + \":\" + f + \":\" + d;\n" +
            "    }\n" +
            "}\n"
        );
    }
}
