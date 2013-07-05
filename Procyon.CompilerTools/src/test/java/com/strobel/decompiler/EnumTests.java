package com.strobel.decompiler;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

@SuppressWarnings({ "UnnecessarySemicolon", "UnusedDeclaration" })
public class EnumTests extends DecompilerTest {
    private enum A {
        FOO,
        BAR,
        BAP;
    }

    private enum B {
        FOO(2),
        BAR(1),
        BAZ(5);

        private final int x;
        private static final Map<String, B> map;
        private static final B temp;

        private B(final int x) {
            this.x = x;
        }

        public static void test() {
            System.out.println(B.FOO);
        }

        static {
            map = Collections.emptyMap();
            temp = B.BAZ;
        }
    }

    private enum C {
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
    public void testSimpleEnum() {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private enum A {\n" +
            "    FOO,\n" +
            "    BAR,\n" +
            "    BAP;\n" +
            "}\n"
        );
    }

    @Test
    public void testEnumWithFieldsAndConstructor() {
        verifyOutput(
            B.class,
            defaultSettings(),
            "private enum B {\n" +
            "    FOO(2),\n" +
            "    BAR(1),\n" +
            "    BAZ(5);\n" +
            "    private final int x;\n" +
            "    private static final Map<String, B> map;\n" +
            "    private static final B temp;\n" +
            "    private B(final int x) {\n" +
            "        this.x = x;\n" +
            "    }\n" +
            "    public static void test() {\n" +
            "        System.out.println(B.FOO);\n" +
            "    }\n" +
            "    static {\n" +
            "        map = Collections.emptyMap();\n" +
            "        temp = B.BAZ;\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testEnumWithAnonymousClassValues() {
        verifyOutput(
            C.class,
            defaultSettings(),
            "private enum C {\n" +
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
