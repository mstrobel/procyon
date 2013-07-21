package com.strobel.decompiler;

import org.junit.Ignore;
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
    @Ignore
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
            "            catch (Exception e) {\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    @Ignore
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
            "            catch (Exception e) {\n" +
            "            }\n" +
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
            "            if (lockAcquired) {\n" +
            "                exit(this);\n" +
            "            }\n" +
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
}
