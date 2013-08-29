package com.strobel.decompiler;

import org.junit.Test;

import java.util.List;
import java.util.Set;

public class ConditionalTests extends DecompilerTest {
    private static class A {
        public boolean test(final List<Object> list, final Set<Object> set) {
            if (list == null) {
                if (set == null) {
                    System.out.println("a");
                }
                else {
                    System.out.println("b");
                }
            }
            else if (set == null) {
                System.out.println("c");
            }
            else if (list.isEmpty()) {
                if (set.isEmpty()) {
                    System.out.println("d");
                }
                else {
                    System.out.println("e");
                }
            }
            else if (set.size() < list.size()) {
                System.out.println("f");
            }
            else {
                System.out.println("g");
            }
            return true;
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static class B {
        public boolean test(final List<Object> list, final Set<Object> set) {
            if (list == null) {
                if (set == null) {
                    System.out.println("B");
                }
                else {
                }
            }
            else if (set == null) {
                if (list.isEmpty()) {
                    System.out.println("E");
                }
                else {
                }
            }

            return true;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static class C {
        public boolean test1(final boolean a, final boolean b, boolean c) {
            System.out.println((b && a == (c = b) && b) || !c);
            return c;
        }

        public boolean test2(final boolean a, final boolean b, boolean c) {
            System.out.println((b && a == (c = b)) || !c);
            return c;
        }

        public boolean test3(final boolean a, final boolean b, boolean c) {
            System.out.println(b && a || (c = b) || !c);
            return c;
        }

        public boolean test4(final boolean a, final boolean b, boolean c) {
            System.out.println(b && (c = a) || !c);
            return c;
        }

        public boolean test5(final boolean a, final boolean b, boolean c) {
            System.out.println(b || (c = a) || !c);
            return c;
        }

        public boolean test6(final boolean a, final boolean b, boolean c) {
            System.out.println(b && (c = a));
            return c;
        }

        public boolean test7(final boolean a, final boolean b, boolean c) {
            System.out.println(b || (c = a));
            return c;
        }

        public boolean test8(final boolean a, final boolean b, boolean c) {
            System.out.println(b && a == (c = b) && b && c);
            return c;
        }
    }

    private static class D {
        public boolean test(final boolean a, final boolean b, final boolean c, final boolean d) {
            return (a ? b : c) ? d : (c ? b : a);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static class E {
        public boolean test1(final boolean a, final boolean b, final boolean c) {
            System.out.println(a || (c ? a : b));
            return c;
        }

        public boolean test2(final boolean a, final boolean b, final boolean c) {
            System.out.println(a && (c ? a : b));
            return c;
        }

        public boolean test3(final boolean a, final boolean b, final boolean c) {
            System.out.println(!a || (c ? a : b));
            return c;
        }

        public boolean test4(final boolean a, final boolean b, final boolean c) {
            System.out.println(!a && (c ? a : b));
            return c;
        }
    }

    @Test
    public void testComplexIfElse() throws Throwable {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
            "    public boolean test(final List<Object> list, final Set<Object> set) {\n" +
            "        if (list == null) {\n" +
            "            if (set == null) {\n" +
            "                System.out.println(\"a\");\n" +
            "            }\n" +
            "            else {\n" +
            "                System.out.println(\"b\");\n" +
            "            }\n" +
            "        }\n" +
            "        else if (set == null) {\n" +
            "            System.out.println(\"c\");\n" +
            "        }\n" +
            "        else if (list.isEmpty()) {\n" +
            "            if (set.isEmpty()) {\n" +
            "                System.out.println(\"d\");\n" +
            "            }\n" +
            "            else {\n" +
            "                System.out.println(\"e\");\n" +
            "            }\n" +
            "        }\n" +
            "        else if (set.size() < list.size()) {\n" +
            "            System.out.println(\"f\");\n" +
            "        }\n" +
            "        else {\n" +
            "            System.out.println(\"g\");\n" +
            "        }\n" +
            "        return true;\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testEmptyElseBlocks() throws Throwable {
        verifyOutput(
            B.class,
            defaultSettings(),
            "private static class B {\n" +
            "    public boolean test(final List<Object> list, final Set<Object> set) {\n" +
            "        if (list == null) {\n" +
            "            if (set == null) {\n" +
            "                System.out.println(\"B\");\n" +
            "            }\n" +
            "        }\n" +
            "        else if (set == null && list.isEmpty()) {\n" +
            "            System.out.println(\"E\");\n" +
            "        }\n" +
            "        return true;\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testShortCircuitEmbeddedAssignments() throws Throwable {
        verifyOutput(
            C.class,
            defaultSettings(),
            "private static class C {\n" +
            "    public boolean test1(final boolean a, final boolean b, boolean c) {\n" +
            "        System.out.println((b && a == (c = b) && b) || !c);\n" +
            "        return c;\n" +
            "    }\n" +
            "    public boolean test2(final boolean a, final boolean b, boolean c) {\n" +
            "        System.out.println((b && a == (c = b)) || !c);\n" +
            "        return c;\n" +
            "    }\n" +
            "    public boolean test3(final boolean a, final boolean b, boolean c) {\n" +
            "        System.out.println((b && a) || (c = b) || !c);\n" +
            "        return c;\n" +
            "    }\n" +
            "    public boolean test4(final boolean a, final boolean b, boolean c) {\n" +
            "        System.out.println((b && (c = a)) || !c);\n" +
            "        return c;\n" +
            "    }\n" +
            "    public boolean test5(final boolean a, final boolean b, boolean c) {\n" +
            "        System.out.println(b || (c = a) || !c);\n" +
            "        return c;\n" +
            "    }\n" +
            "    public boolean test6(final boolean a, final boolean b, boolean c) {\n" +
            "        System.out.println(b && (c = a));\n" +
            "        return c;\n" +
            "    }\n" +
            "    public boolean test7(final boolean a, final boolean b, boolean c) {\n" +
            "        System.out.println(b || (c = a));\n" +
            "        return c;\n" +
            "    }\n" +
            "    public boolean test8(final boolean a, final boolean b, boolean c) {\n" +
            "        System.out.println(b && a == (c = b) && b && c);\n" +
            "        return c;\n" +
            "    }\n" +
            "}"
        );
    }
    @Test
    public void testTernaryWithTernaryCondition() throws Throwable {
        verifyOutput(
            D.class,
            defaultSettings(),
            "private static class D {\n" +
            "    public boolean test(final boolean a, final boolean b, final boolean c, final boolean d) {\n" +
            "        return (a ? b : c) ? d : (c ? b : a);\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testLogicalAndOrWithConditionals() throws Throwable {
        verifyOutput(
            E.class,
            defaultSettings(),
            "private static class E {\n" +
            "    public boolean test1(final boolean a, final boolean b, final boolean c) {\n" +
            "        System.out.println(a || (c ? a : b));\n" +
            "        return c;\n" +
            "    }\n" +
            "    public boolean test2(final boolean a, final boolean b, final boolean c) {\n" +
            "        System.out.println(a && (c ? a : b));\n" +
            "        return c;\n" +
            "    }\n" +
            "    public boolean test3(final boolean a, final boolean b, final boolean c) {\n" +
            "        System.out.println(!a || (c ? a : b));\n" +
            "        return c;\n" +
            "    }\n" +
            "    public boolean test4(final boolean a, final boolean b, final boolean c) {\n" +
            "        System.out.println(!a && (c ? a : b));\n" +
            "        return c;\n" +
            "    }\n" +
            "}"
        );
    }
}
