package com.strobel.decompiler;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@SuppressWarnings({
                      "UnnecessaryReturnStatement",
                      "ThrowFromFinallyBlock",
                      "EmptyCatchBlock",
                      "UnusedParameters",
                      "UnusedAssignment",
                      "UnusedDeclaration"
                  })
public class HandlerTests extends DecompilerTest {
    private static class A {
        public static <X> List<X> f(final X x) throws IllegalStateException {
            throw new IllegalStateException();
        }

        public static <X> List<X> g(final X x) throws IOException {
            throw new IOException();
        }
    }

    private static class B {
        private static void rethrow(final Throwable t) throws Throwable {
            throw t;
        }

        static void test(final int a, final int b) {
            try {
                try {
                    throw new UnsupportedOperationException();
                }
                catch (RuntimeException e) {
                    rethrow(e);
                    return;
                }
            }
            catch (UnsupportedOperationException e) {
                System.out.println("unchecked");
                return;
            }
            catch (Throwable e) {
                System.out.println("checked");
                return;
            }
        }
    }

    private static class C {
        void test() {
            try {
                throw new Exception();
            }
            catch (Exception e) {
            }
            finally {
                try {
                    throw new Exception();
                }
                catch (Exception e) {
                }
            }
        }
    }

    private static class D {
        void test() {
            try {
                throw new Exception();
            }
            catch (Exception e) {
            }
            finally {
                try {
                    int k = 0;
                    k = 1 / k;
                }
                catch (Exception e) {
                }
            }
        }
    }

    @SuppressWarnings("LocalCanBeFinal")
    private static class E {
        void test(final String[] path) {
            try {
                final File file = new File(path[0]);
                final FileInputStream fileInputStream = new FileInputStream(file);
            }
            catch (FileNotFoundException e) {
                System.out.println("File Not found");
                for (final String s : path) {
                    System.out.println(s);
                }
            }
        }
    }

    @SuppressWarnings("LocalCanBeFinal")
    private static class F {
        private static boolean tryEnter(final Object o) {
            return true;
        }

        private static void exit(final Object o) {
        }

        private static void doSomething() throws FileNotFoundException {
        }

        boolean test(final String[] path) {
            final boolean lockAcquired = tryEnter(this);
            boolean result;

            try {
                doSomething();
                result = true;
            }
            catch (FileNotFoundException t) {
                result = false;
            }
            finally {
                if (lockAcquired) {
                    exit(this);
                }
            }
            return result;
        }
    }

    private static class G {
        void test() {
            try {
                System.out.println("try");
                try {
                    System.out.println("inner try");
                }
                catch (RuntimeException e) {
                    System.out.println("inner catch");
                }
                finally {
                    System.out.println("inner finally");
                }
                System.out.println("end of outer try");
            }
            catch (RuntimeException e) {
                System.out.println("catch");
                return;
            }
            finally {
                System.out.println("finally");
            }
            System.out.println("exit");
        }
    }

    private static class H {
        public String test(final int x) {
            try {
                if (x < 0) {
                    return "negative";
                }
                else if (x > 0) {
                    return "positive";
                }
                else if (x == 0) {
                    return "zero";
                }
                else {
                    return "unreachable";
                }
            }
            catch (RuntimeException e) {
                System.out.println("catch");
                return "error";
            }
            finally {
                System.out.println("finally");
            }
        }
    }

    private static class I {
        public String test(final int x) {
            try {
                if (x < 0) {
                    return "negative";
                }
                else if (x > 0) {
                    return "positive";
                }
                else if (x == 0) {
                    return "zero";
                }
                else {
                    return "unreachable";
                }
            }
            catch (RuntimeException e) {
                System.out.println("catch");
                return "error";
            }
            finally {
                System.out.println("finally");
                throw new RuntimeException("whoop whoop");
            }
        }
    }

    private static class J {
        public int test(final int x) {
            try {
                return x;
            }
            finally {
                return x + 1;
            }
        }
    }

    private static class K {
        private static String negative() {
            return "negative";
        }

        private static String positive() {
            return "positive";
        }

        public String test(final int x) {
            try {
                if (x == 0) {
                    return "zero";
                }
                else {
                    try {
                        if (x < 0) {
                            return negative();
                        }
                        else if (x > 0) {
                            return positive();
                        }
                    }
                    catch (Throwable t) {
                        System.out.println("inner catch");
                        return "inner error";
                    }
                    finally {
                        System.out.println("inner finally");
                    }
                    return "unreachable";
                }
            }
            catch (RuntimeException e) {
                System.out.println("catch");
                return "error";
            }
            finally {
                System.out.println("finally");
                throw new RuntimeException("whoop whoop");
            }
        }
    }

    @Test
    public void testThrowsSignatures() throws Throwable {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
            "    public static <X> List<X> f(final X x) throws IllegalStateException {\n" +
            "        throw new IllegalStateException();\n" +
            "    }\n" +
            "    public static <X> List<X> g(final X x) throws IOException {\n" +
            "        throw new IOException();\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testMultipleCatchHandlers() throws Throwable {
        verifyOutput(
            B.class,
            defaultSettings(),
            "private static class B {\n" +
            "    private static void rethrow(final Throwable t) throws Throwable {\n" +
            "        throw t;\n" +
            "    }\n" +
            "    static void test(final int a, final int b) {\n" +
            "        try {\n" +
            "            try {\n" +
            "                throw new UnsupportedOperationException();\n" +
            "            }\n" +
            "            catch (RuntimeException e) {\n" +
            "                rethrow(e);\n" +
            "                return;\n" +
            "            }\n" +
            "        }\n" +
            "        catch (UnsupportedOperationException e) {\n" +
            "            System.out.println(\"unchecked\");\n" +
            "            return;\n" +
            "        }\n" +
            "        catch (Throwable e) {\n" +
            "            System.out.println(\"checked\");\n" +
            "            return;\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testSimpleNestedHandlerInFinally() throws Throwable {
        verifyOutput(
            C.class,
            defaultSettings(),
            "private static class C {\n" +
            "    void test() {\n" +
            "        try {\n" +
            "            throw new Exception();\n" +
            "        }\n" +
            "        catch (Exception e) {}\n" +
            "        finally {\n" +
            "            try {\n" +
            "                throw new Exception();\n" +
            "            }\n" +
            "            catch (Exception e2) {}\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testEmptyCatchWithinFinally() throws Throwable {
        verifyOutput(
            D.class,
            defaultSettings(),
            "private static class D {\n" +
            "    void test() {\n" +
            "        try {\n" +
            "            throw new Exception();\n" +
            "        }\n" +
            "        catch (Exception e) {}\n" +
            "        finally {\n" +
            "            try {\n" +
            "                int k = 0;\n" +
            "                k = 1 / k;\n" +
            "            }\n" +
            "            catch (Exception e2) {}\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testLoopInCatchClause() throws Throwable {
        verifyOutput(
            E.class,
            defaultSettings(),
            "private static class E {\n" +
            "    void test(final String[] path) {\n" +
            "        try {\n" +
            "            final File file = new File(path[0]);\n" +
            "            final FileInputStream fileInputStream = new FileInputStream(file);\n" +
            "        }\n" +
            "        catch (FileNotFoundException e) {\n" +
            "            System.out.println(\"File Not found\");\n" +
            "            for (final String s : path) {\n" +
            "                System.out.println(s);\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testSimpleTryCatchFinallyControlFlow() throws Throwable {
        verifyOutput(
            F.class,
            defaultSettings(),
            "private static class F {\n" +
            "    private static boolean tryEnter(final Object o) {\n" +
            "        return true;\n" +
            "    }\n" +
            "    private static void exit(final Object o) {\n" +
            "    }\n" +
            "    private static void doSomething() throws FileNotFoundException {\n" +
            "    }\n" +
            "    boolean test(final String[] path) {\n" +
            "        final boolean lockAcquired = tryEnter(this);\n" +
            "        boolean result;\n" +
            "        try {\n" +
            "            doSomething();\n" +
            "            result = true;\n" +
            "        }\n" +
            "        catch (FileNotFoundException t) {\n" +
            "            result = false;\n" +
            "        }\n" +
            "        finally {\n" +
            "            if (lockAcquired) {\n" +
            "                exit(this);\n" +
            "            }\n" +
            "        }\n" +
            "        return result;\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testNestedTryCatchFinally() throws Throwable {
        verifyOutput(
            G.class,
            defaultSettings(),
            "private static class G {\n" +
            "    void test() {\n" +
            "        try {\n" +
            "            System.out.println(\"try\");\n" +
            "            try {\n" +
            "                System.out.println(\"inner try\");\n" +
            "            }\n" +
            "            catch (RuntimeException e) {\n" +
            "                System.out.println(\"inner catch\");\n" +
            "            }\n" +
            "            finally {\n" +
            "                System.out.println(\"inner finally\");\n" +
            "            }\n" +
            "            System.out.println(\"end of outer try\");\n" +
            "        }\n" +
            "        catch (RuntimeException e) {\n" +
            "            System.out.println(\"catch\");\n" +
            "            return;\n" +
            "        }\n" +
            "        finally {\n" +
            "            System.out.println(\"finally\");\n" +
            "        }\n" +
            "        System.out.println(\"exit\");\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testTryCatchFinallyWithNestedConditions() throws Throwable {
        verifyOutput(
            H.class,
            defaultSettings(),
            "private static class H {\n" +
            "    public String test(final int x) {\n" +
            "        try {\n" +
            "            if (x < 0) {\n" +
            "                return \"negative\";\n" +
            "            }\n" +
            "            if (x > 0) {\n" +
            "                return \"positive\";\n" +
            "            }\n" +
            "            if (x == 0) {\n" +
            "                return \"zero\";\n" +
            "            }\n" +
            "            return \"unreachable\";\n" +
            "        }\n" +
            "        catch (RuntimeException e) {\n" +
            "            System.out.println(\"catch\");\n" +
            "            return \"error\";\n" +
            "        }\n" +
            "        finally {\n" +
            "            System.out.println(\"finally\");\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testTryCatchFinallyWithNestedConditionsAndThrowingFinally() throws Throwable {
        verifyOutput(
            I.class,
            defaultSettings(),
            "private static class I {\n" +
            "    public String test(final int x) {\n" +
            "        try {\n" +
            "            if (x < 0) {\n" +
            "                return \"negative\";\n" +
            "            }\n" +
            "            if (x > 0) {\n" +
            "                return \"positive\";\n" +
            "            }\n" +
            "            if (x == 0) {\n" +
            "                return \"zero\";\n" +
            "            }\n" +
            "            return \"unreachable\";\n" +
            "        }\n" +
            "        catch (RuntimeException e) {\n" +
            "            System.out.println(\"catch\");\n" +
            "            return \"error\";\n" +
            "        }\n" +
            "        finally {\n" +
            "            System.out.println(\"finally\");\n" +
            "            throw new RuntimeException(\"whoop whoop\");\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testTryFinallyWhereFinallyOverridesReturn() throws Throwable {
        verifyOutput(
            J.class,
            defaultSettings(),
            "private static class J {\n" +
            "    public int test(final int x) {\n" +
            "        try {\n" +
            "            return x + 1;\n" +
            "        }\n" +
            "        finally {\n" +
            "            return x + 1;\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testComplexNestedTryCatchFinallyWithThrowingOuterFinally() throws Throwable {
        verifyOutput(
            K.class,
            defaultSettings(),
            "private static class K {\n" +
            "    private static String negative() {\n" +
            "        return \"negative\";\n" +
            "    }\n" +
            "    private static String positive() {\n" +
            "        return \"positive\";\n" +
            "    }\n" +
            "    public String test(final int x) {\n" +
            "        try {\n" +
            "            if (x == 0) {\n" +
            "                return \"zero\";\n" +
            "            }\n" +
            "            try {\n" +
            "                if (x < 0) {\n" +
            "                    negative();\n" +
            "                }\n" +
            "                if (x > 0) {\n" +
            "                    positive();\n" +
            "                }\n" +
            "            }\n" +
            "            catch (Throwable t) {\n" +
            "                System.out.println(\"inner catch\");\n" +
            "            }\n" +
            "            finally {\n" +
            "                System.out.println(\"inner finally\");\n" +
            "            }\n" +
            "            return \"unreachable\";\n" +
            "        }\n" +
            "        catch (RuntimeException e) {\n" +
            "            System.out.println(\"catch\");\n" +
            "            return \"error\";\n" +
            "        }\n" +
            "        finally {\n" +
            "            System.out.println(\"finally\");\n" +
            "            throw new RuntimeException(\"whoop whoop\");\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }
}
