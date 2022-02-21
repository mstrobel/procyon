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
    "UnusedDeclaration",
    "ConstantConditions",
    "ReturnInsideFinallyBlock",
    "UnnecessaryBreak",
    "ContinueOrBreakFromFinallyBlock",
    "InfiniteLoopStatement",
    "finally",
    "divzero"
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
                catch (final RuntimeException e) {
                    rethrow(e);
                    return;
                }
            }
            catch (final UnsupportedOperationException e) {
                System.out.println("unchecked");
                return;
            }
            catch (final Throwable e) {
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
            catch (final Exception ex) {
            }
            finally {
                try {
                    throw new Exception();
                }
                catch (final Exception ex) {
                }
            }
        }
    }

    private static class D {
        void test() {
            try {
                throw new Exception();
            }
            catch (final Exception ex) {
            }
            finally {
                try {
                    int k = 0;
                    k = 1 / k;
                }
                catch (final Exception ex) {
                }
            }
        }
    }

    private static class E {
        void test(final String[] path) {
            try {
                final File file = new File(path[0]);
                final FileInputStream fileInputStream = new FileInputStream(file);
            }
            catch (final FileNotFoundException e) {
                System.out.println("File Not found");
                for (final String s : path) {
                    System.out.println(s);
                }
            }
        }
    }

    private static class F {
        private static boolean tryEnter(final Object o) {
            return true;
        }

        private static void exit(final Object o) {
        }

        private static void doSomething() throws FileNotFoundException {
        }

        boolean test() {
            final boolean lockAcquired = tryEnter(this);
            boolean result;

            try {
                doSomething();
                result = true;
            }
            catch (final FileNotFoundException t) {
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
                catch (final RuntimeException e) {
                    System.out.println("inner catch");
                }
                finally {
                    System.out.println("inner finally");
                }
                System.out.println("end of outer try");
            }
            catch (final RuntimeException e) {
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
            catch (final RuntimeException e) {
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
            catch (final RuntimeException e) {
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

    @SuppressWarnings("ConstantConditions")
    private static class K {
        private static String zero() {
            return "zero";
        }

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
                        else if (x == 0) {
                            return zero();
                        }
                    }
                    catch (final Throwable t) {
                        System.out.println("inner catch");
                        return "inner error";
                    }
                    finally {
                        System.out.println("inner finally");
                    }
                    return "unreachable";
                }
            }
            catch (final RuntimeException e) {
                System.out.println("catch");
                return "error";
            }
            finally {
                System.out.println("finally");
                throw new RuntimeException("whoop whoop");
            }
        }
    }

    private static final class L {
        public void test() {
            try {
                try {
                    System.out.print(3);
                    throw new NoSuchFieldException();
                }
                catch (final NoSuchFieldException ex) {
                }
            }
            finally {
                System.out.print("finally");
            }
            System.out.print(5);
        }
    }

    private static final class M {
        public int test() {
        exit:
            {
                try {
                    return 1;
                }
                finally {
                    break exit;
                }
            }
            System.out.println("TEST");
            return 1;
        }
    }

    private static final class N {
        int callWhichThrows() {
            throw new RuntimeException();
        }

        public int test() {
        bob:
            {
                try {
                    return callWhichThrows();
                }
                catch (final Throwable t) {
                }
                finally {
                    break bob;
                }
            }
            System.out.println("TEST!");
            return 1;
        }
    }

    private static final class O {
        private static void f() {
        }

        public void test() {
            do {
                try {
                    try {
                        System.out.print(1);
                        try {
                            System.out.print(2);
                            f();
                            System.out.print(3);
                        }
                        catch (final IllegalStateException e) {
                            System.out.print(4);
                        }
                        System.out.print(5);
                    }
                    catch (final RuntimeException e) {
                        System.out.print(6);
                    }
                    System.out.print(7);
                }
                finally {
                    System.out.print(8);
                }
            }
            while (true);
        }
    }

    private static final class P {
        private static void f() {
        }

        public void test() {
            try {
                System.out.println("A");
                f();
            }
            finally {
                try {
                    System.out.println("B");
                    f();
                }
                finally {
                    System.out.println("C");
                }
            }
        }
    }

    private static final class Q {
        private static void f() {
        }

        public void test() {
            try {
                System.out.println("A");
                f();
            }
            finally {
                try {
                    try {
                        System.out.println("B");
                        f();
                    }
                    finally {
                        try {
                            System.out.println("C");
                            f();
                        }
                        finally {
                            System.out.println("D");
                        }
                    }
                }
                finally {
                    System.out.println("E");
                }
                System.out.println("F");
            }
        }
    }

    private static class R {
        public void test(final String className, final String fieldName) {
            try {
                System.out.println(Class.forName(className).getField(fieldName));
                throw new IOException();
            }
            catch (final NoSuchFieldException | ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testThrowsSignatures() {
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
    public void testMultipleCatchHandlers() {
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
            "            catch (final RuntimeException e) {\n" +
            "                rethrow(e);\n" +
            "            }\n" +
            "        }\n" +
            "        catch (final UnsupportedOperationException e2) {\n" +
            "            System.out.println(\"unchecked\");\n" +
            "        }\n" +
            "        catch (final Throwable e3) {\n" +
            "            System.out.println(\"checked\");\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testSimpleNestedHandlerInFinally() {
        verifyOutput(
            C.class,
            defaultSettings(),
            "private static class C {\n" +
            "    void test() {\n" +
            "        try {\n" +
            "            throw new Exception();\n" +
            "        }\n" +
            "        catch (final Exception ex) {}\n" +
            "        finally {\n" +
            "            try {\n" +
            "                throw new Exception();\n" +
            "            }\n" +
            "            catch (final Exception ex2) {}\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testEmptyCatchWithinFinally() {
        verifyOutput(
            D.class,
            defaultSettings(),
            "private static class D {\n" +
            "    void test() {\n" +
            "        try {\n" +
            "            throw new Exception();\n" +
            "        }\n" +
            "        catch (final Exception ex) {}\n" +
            "        finally {\n" +
            "            try {\n" +
            "                int k = 0;\n" +
            "                k = 1 / k;\n" +
            "            }\n" +
            "            catch (final Exception ex2) {}\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testLoopInCatchClause() {
        verifyOutput(
            E.class,
            defaultSettings(),
            "private static class E {\n" +
            "    void test(final String[] path) {\n" +
            "        try {\n" +
            "            final File file = new File(path[0]);\n" +
            "            final FileInputStream fileInputStream = new FileInputStream(file);\n" +
            "        }\n" +
            "        catch (final FileNotFoundException e) {\n" +
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
    public void testSimpleTryCatchFinallyControlFlow() {
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
            "    boolean test() {\n" +
            "        final boolean lockAcquired = tryEnter(this);\n" +
            "        boolean result;\n" +
            "        try {\n" +
            "            doSomething();\n" +
            "            result = true;\n" +
            "        }\n" +
            "        catch (final FileNotFoundException t) {\n" +
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
    public void testNestedTryCatchFinally() {
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
            "            catch (final RuntimeException e) {\n" +
            "                System.out.println(\"inner catch\");\n" +
            "            }\n" +
            "            finally {\n" +
            "                System.out.println(\"inner finally\");\n" +
            "            }\n" +
            "            System.out.println(\"end of outer try\");\n" +
            "        }\n" +
            "        catch (final RuntimeException e) {\n" +
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
    public void testTryCatchFinallyWithNestedConditions() {
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
            "        catch (final RuntimeException e) {\n" +
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
    public void testTryCatchFinallyWithNestedConditionsAndThrowingFinally() {
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
            "        catch (final RuntimeException e) {\n" +
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
    public void testTryFinallyWhereFinallyOverridesReturn() {
        verifyOutput(
            J.class,
            defaultSettings(),
            "private static class J {\n" +
            "    public int test(final int x) {\n" +
            "        try {\n" +
            "            return x;\n" +
            "        }\n" +
            "        finally {\n" +
            "            return x + 1;\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testComplexNestedTryCatchFinallyWithThrowingOuterFinally() {
        verifyOutput(
            K.class,
            defaultSettings(),
            "private static class K {\n" +
            "    private static String zero() {\n" +
            "        return \"zero\";\n" +
            "    }\n" +
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
            "                if (x == 0) {\n" +
            "                    zero();\n" +
            "                }\n" +
            "            }\n" +
            "            catch (final Throwable t) {\n" +
            "                System.out.println(\"inner catch\");\n" +
            "            }\n" +
            "            finally {\n" +
            "                System.out.println(\"inner finally\");\n" +
            "            }\n" +
            "            return \"unreachable\";\n" +
            "        }\n" +
            "        catch (final RuntimeException e) {\n" +
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
    public void testFinallyEatingIntoCatch() {
        verifyOutput(
            L.class,
            defaultSettings(),
            "private static final class L {\n" +
            "    public void test() {\n" +
            "        try {\n" +
            "            System.out.print(3);\n" +
            "            throw new NoSuchFieldException();\n" +
            "        }\n" +
            "        catch (final NoSuchFieldException ex) {}\n" +
            "        finally {\n" +
            "            System.out.print(\"finally\");\n" +
            "        }\n" +
            "        System.out.print(5);\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testBreakOutOfFinally() {
        verifyOutput(
            M.class,
            defaultSettings(),
            "private static final class M {\n" +
            "    public int test() {\n" +
            "        System.out.println(\"TEST\");\n" +
            "        return 1;\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testBreakOutOfFinallyWithPossibleThrowInTry() {
        verifyOutput(
            N.class,
            defaultSettings(),
            "private static final class N {\n" +
            "    int callWhichThrows() {\n" +
            "        throw new RuntimeException();\n" +
            "    }\n" +
            "    public int test() {\n" +
            "        try {\n" +
            "            this.callWhichThrows();\n" +
            "        }\n" +
            "        catch (final Throwable t) {}\n" +
            "        System.out.println(\"TEST!\");\n" +
            "        return 1;\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testNestedTryCatchFinallyInLoop() {
        verifyOutput(
            O.class,
            defaultSettings(),
            "private static final class O {\n" +
            "    private static void f() {\n" +
            "    }\n" +
            "    public void test() {\n" +
            "        while (true) {\n" +
            "            try {\n" +
            "                try {\n" +
            "                    System.out.print(1);\n" +
            "                    try {\n" +
            "                        System.out.print(2);\n" +
            "                        f();\n" +
            "                        System.out.print(3);\n" +
            "                    }\n" +
            "                    catch (final IllegalStateException e) {\n" +
            "                        System.out.print(4);\n" +
            "                    }\n" +
            "                    System.out.print(5);\n" +
            "                }\n" +
            "                catch (final RuntimeException e2) {\n" +
            "                    System.out.print(6);\n" +
            "                }\n" +
            "                System.out.print(7);\n" +
            "            }\n" +
            "            finally {\n" +
            "                System.out.print(8);\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testFinallyWithinFinally() {
        verifyOutput(
            P.class,
            defaultSettings(),
            "private static final class P {\n" +
            "    private static void f() {\n" +
            "    }\n" +
            "    public void test() {\n" +
            "        try {\n" +
            "            System.out.println(\"A\");\n" +
            "            f();\n" +
            "        }\n" +
            "        finally {\n" +
            "            try {\n" +
            "                System.out.println(\"B\");\n" +
            "                f();\n" +
            "            }\n" +
            "            finally {\n" +
            "                System.out.println(\"C\");\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testFinallyWithinFinallyFourLevels() {
        verifyOutput(
            Q.class,
            defaultSettings(),
            "private static final class Q {\n" +
            "    private static void f() {\n" +
            "    }\n" +
            "    public void test() {\n" +
            "        try {\n" +
            "            System.out.println(\"A\");\n" +
            "            f();\n" +
            "        }\n" +
            "        finally {\n" +
            "            try {\n" +
            "                try {\n" +
            "                    System.out.println(\"B\");\n" +
            "                    f();\n" +
            "                }\n" +
            "                finally {\n" +
            "                    try {\n" +
            "                        System.out.println(\"C\");\n" +
            "                        f();\n" +
            "                    } finally {\n" +
            "                        System.out.println(\"D\");\n" +
            "                    }\n" +
            "                }\n" +
            "            } finally {\n" +
            "                System.out.println(\"E\");\n" +
            "            }\n" +
            "            System.out.println(\"F\");\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testCatchUnionType() {
        verifyOutput(
            R.class,
            defaultSettings(),
            "private static class R {\n" +
            "    public void test(final String className, final String fieldName) {\n" +
            "        try {\n" +
            "            System.out.println(Class.forName(className).getField(fieldName));\n" +
            "            throw new IOException();\n" +
            "        }\n" +
            "        catch (final NoSuchFieldException | ClassNotFoundException | IOException e) {\n" +
            "            e.printStackTrace();\n" +
            "        }\n" +
            "    }\n" +
            "}");
    }
}
