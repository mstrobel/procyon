package com.strobel.decompiler;

import org.junit.Ignore;
import org.junit.Test;

public class EnumTests extends DecompilerTest {
    private enum A {
        X {
            public void f() {
                System.out.println(this.name().toLowerCase());
            }
        },
        Y {
            public void f() {
                System.out.println("y");
            }
        };

        public abstract void f();
    }

    @Test
    public void testEnumWithAnonymousClassValues() {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private enum A {\n" +
            "    X {\n" +
            "        public void f() {\n" +
            "            System.out.println(this.name().toLowerCase());\n" +
            "        }\n" +
            "    },\n" +
            "    Y {\n" +
            "        public void f() {\n" +
            "            System.out.println(\"y\");\n" +
            "        }\n" +
            "    };\n" +
            "    public abstract void f();\n" +
            "}\n"
        );
    }
}
