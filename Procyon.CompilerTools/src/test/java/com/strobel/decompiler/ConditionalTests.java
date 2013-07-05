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
}
