package com.strobel.decompiler;

import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings({ "UnnecessaryReturnStatement", "ThrowFromFinallyBlock", "EmptyCatchBlock" })
public class HandlerTests extends DecompilerTest {
    private static class A {
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

    @SuppressWarnings("UnusedAssignment")
    private static class B {
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

    @Test
    @Ignore
    public void testSimpleNestedHandlerInFinally() throws Throwable {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static class A {\n" +
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
            B.class,
            defaultSettings(),
            "private static class B {\n" +
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
}
