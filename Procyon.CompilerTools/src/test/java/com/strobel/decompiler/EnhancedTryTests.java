/*
 * EnhancedTryTests.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

public class EnhancedTryTests extends DecompilerTest {
    private final static class A {
        public void test() throws IOException {
            try (final StringWriter writer = new StringWriter()) {
                writer.write("This is only a test.");
            }
        }
    }

    private final static class B {
        public void test() throws IOException {
            try (final StringWriter writer1 = new StringWriter();
                 final StringWriter writer2 = new StringWriter()) {
                writer1.write("This is only a test.");
                writer2.write("This is also a test.");
            }
        }
    }

    @Test
    public void testEnhancedTryOneResource() throws Throwable {
        verifyOutput(
            A.class,
            defaultSettings(),
            "private static final class A {\n" +
            "    public void test() throws IOException {\n" +
            "        final StringWriter writer = new StringWriter();\n" +
            "        Throwable t = null;\n" +
            "        try {\n" +
            "            writer.write(\"This is only a test.\");\n" +
            "        }\n" +
            "        catch (Throwable t2) {\n" +
            "            t = t2;\n" +
            "            throw t2;\n" +
            "        }\n" +
            "        finally {\n" +
            "            Label_0084: {\n" +
            "                if (writer != null) {\n" +
            "                    if (t != null) {\n" +
            "                        try {\n" +
            "                            writer.close();\n" +
            "                            break Label_0084;\n" +
            "                        }\n" +
            "                        catch (Throwable x2) {\n" +
            "                            t.addSuppressed(x2);\n" +
            "                            break Label_0084;\n" +
            "                        }\n" +
            "                    }\n" +
            "                    writer.close();\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }

    @Test
    public void testEnhancedTryTwoResources() throws Throwable {
        verifyOutput(
            B.class,
            defaultSettings(),
            "private static final class B {\n" +
            "    public void test() throws IOException {\n" +
            "        final StringWriter writer1 = new StringWriter();\n" +
            "        Throwable t = null;\n" +
            "        try {\n" +
            "            final StringWriter writer2 = new StringWriter();\n" +
            "            Throwable t2 = null;\n" +
            "            try {\n" +
            "                writer1.write(\"This is only a test.\");\n" +
            "                writer2.write(\"This is also a test.\");\n" +
            "            }\n" +
            "            catch (Throwable t3) {\n" +
            "                t2 = t3;\n" +
            "                throw t3;\n" +
            "            }\n" +
            "            finally {\n" +
            "            Label_0111: {\n" +
            "                    if (writer2 != null) {\n" +
            "                        if (t2 != null) {\n" +
            "                            try {\n" +
            "                                writer2.close();\n" +
            "                                break Label_0111;\n" +
            "                            }\n" +
            "                            catch (Throwable x2) {\n" +
            "                                t2.addSuppressed(x2);\n" +
            "                                break Label_0111;\n" +
            "                            }\n" +
            "                        }\n" +
            "                        writer2.close();\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "        catch (Throwable t4) {\n" +
            "            t = t4;\n" +
            "            throw t4;\n" +
            "        }\n" +
            "        finally {\n" +
            "        Label_0182: {\n" +
            "                if (writer1 != null) {\n" +
            "                    if (t != null) {\n" +
            "                        try {\n" +
            "                            writer1.close();\n" +
            "                            break Label_0182;\n" +
            "                        }\n" +
            "                        catch (Throwable x2) {\n" +
            "                            t.addSuppressed(x2);\n" +
            "                            break Label_0182;\n" +
            "                        }\n" +
            "                    }\n" +
            "                    writer1.close();\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n"
        );
    }
}
