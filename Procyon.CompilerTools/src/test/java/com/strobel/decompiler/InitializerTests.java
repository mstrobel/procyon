package com.strobel.decompiler;

import org.junit.Test;

public class InitializerTests extends DecompilerTest {
    private static class A {
        final int i = 42;
    }

    @Test
    public void testConstantNotInitializedInConstructor() throws Throwable {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
            "    final int i = 42;\n" +
            "}\n"
        );
    }
}
