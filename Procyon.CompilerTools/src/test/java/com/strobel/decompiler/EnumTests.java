package com.strobel.decompiler;

import org.junit.Ignore;
import org.junit.Test;

public class EnumTests extends DecompilerTest {
    private static enum A {
        X {
            @Override
            public void f() {
                System.out.println(name());
            }
        };

        public abstract void f();
    }

    @Test
    @Ignore
    public void testEnumWithAnonymousClassValues() {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static enum A {\n" +
            "    X {\n" +
            "        @Override\n" +
            "        public void f() {\n" +
            "            System.out.println(name());\n" +
            "        }\n" +
            "    };\n" +
            "    public abstract void f();\n" +
            "}\n"
        );
    }
}
