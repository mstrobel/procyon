/***
 * ASM examples: examples showing how ASM can be used
 * Copyright (c) 2000-2007 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.objectweb.asm.jbfc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.objectweb.asm.ClassWriter;

/**
 * A naive implementation of compiler for Brain**** language.
 * http://www.muppetlabs.com/~breadbox/bf/ *
 * 
 * @author Eugene Kuleshov
 */
public class BFCompilerTest extends TestCase {
    private BFCompiler bc;

    private ClassWriter cw;

    protected void setUp() throws Exception {
        super.setUp();
        bc = new BFCompiler();
        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    }

    public void testCompileHelloWorld() throws Throwable {
        assertEquals("Hello World!\n",
                execute("Hello",
                        ">+++++++++[<++++++++>-]<.>+++++++[<++++>-]<+.+++++++..+++.[-]>++++++++[<++++>-]"
                                + "<.#>+++++++++++[<+++++>-]<.>++++++++[<+++>-]<.+++.------.--------.[-]>++++++++["
                                + "<++++>-]<+.[-]++++++++++.",
                        ""));
    }

    public void testCompileEcho() throws Throwable {
        assertEquals("AAA", execute("Echo", ",+[-.,+]", "AAA"));
    }

    public void testCompileYaPi() throws Throwable {
        assertEquals("3.1415926\n", execute("YaPi",
                ">+++++[<+++++++++>-]>>>>>>\r\n\r\n+++++ +++ (7 "
                        + "digits)\r\n\r\n[<<+>++++++++++>-]<<+>>+++<[->>+"
                        + "<-[>>>]>[[<+>-]>+>>]<<<<<]>[-]>[-]>[<+>-]<[>+<["
                        + "-\r\n>>>>>>>+<<<<<<<]>[->+>>>>>>+<<<<<<<]>>>>++"
                        + ">>-]>[-]<<<[<<<<<<<]<[->>>>>[>>>>>>>]<\r\n<<<<<"
                        + "<[>>>>[-]>>>>>>>[-<<<<<<<+>>>>>>>]<<<<<<<<[<<++"
                        + "++++++++>>-]>[<<<<[>+>>+<<<-\r\n]>>>[<<<+>>>-]>"
                        + "-]<<<<[>>++>+<<<-]>>->[<<<+>>>-]>[-]<<<[->>+<-["
                        + ">>>]>[[<+>-]>+>>]<\r\n<<<<]>[-]<<<<<<<<<]>+>>>>"
                        + ">>->>>>[<<<<<<<<+>>>>>>>>-]<<<<<<<[-]++++++++++"
                        + "<[->>+<-\r\n[>>>]>[[<+>-]>+>>]<<<<<]>[-]>[>>>>>"
                        + "+<<<<<-]>[<+>>+<-]>[<+>-]<<<+<+>>[-[-[-[-[-[-\r"
                        + "\n[-[-[-<->[-<+<->>[<<+>>[-]]]]]]]]]]]]<[+++++["
                        + "<<<<++++++++>>>>>++++++++<-]>+<<<<-\r\n>>[>+>-<"
                        + "<<<<+++++++++>>>-]<<<<[>>>>>>+<<<<<<-]<[>>>>>>>"
                        + ".<<<<<<<<[+.[-]]>>]>[<]<+\r\n>>>[<.>-]<[-]>>>>>"
                        + "[-]<[>>[<<<<<<<+>>>>>>>-]<<-]]>>[-]>+<<<<[-]<]+"
                        + "+++++++++.",
                ""));
    }

    public void testCompileTest1() throws Throwable {
        assertEquals("H\n", execute("Test1",
                "[]++++++++++[>++++++++++++++++++>+++++++>+<<<-]A;?@![#>>"
                        + "+<<]>[>++<[-]]>.>.",
                ""));
    }

    private String execute(
        final String name,
        final String code,
        final String input) throws Throwable
    {
        bc.compile(new StringReader(code), name, name, cw);

        // ClassReader cr = new ClassReader(cw.toByteArray());
        // cr.accept(new TraceClassVisitor(null, new PrintWriter(System.err)),
        // true);

        // File tmp = File.createTempFile(name, ".class");
        // System.err.println(tmp.getAbsolutePath());
        // FileOutputStream fos = new FileOutputStream(tmp);
        // fos.write(cw.toByteArray());
        // fos.flush();
        // fos.close();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = System.in;
        PrintStream os = System.out;
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        System.setOut(new PrintStream(bos));

        try {
            TestClassLoader cl = new TestClassLoader(getClass().getClassLoader(),
                    name,
                    cw.toByteArray());
            Class c = cl.loadClass(name);
            Method m = c.getDeclaredMethod("main",
                    new Class[] { String[].class });
            m.invoke(null, new Object[] { new String[0] });

        } catch (InvocationTargetException ex) {
            throw ex.getCause();

        } finally {
            System.setIn(is);
            System.setOut(os);

        }
        return new String(bos.toByteArray(), "ASCII");
    }

    private static final class TestClassLoader extends ClassLoader {
        private final String className;

        private final ClassLoader cl;

        private final byte[] bytecode;

        public TestClassLoader(
            final ClassLoader cl,
            final String className,
            final byte[] bytecode)
        {
            super();
            this.cl = cl;
            this.className = className;
            this.bytecode = bytecode;
        }

        public Class loadClass(final String name) throws ClassNotFoundException
        {
            if (className.equals(name)) {
                return super.defineClass(className,
                        bytecode,
                        0,
                        bytecode.length);
            }
            return cl.loadClass(name);
        }

    }

}
