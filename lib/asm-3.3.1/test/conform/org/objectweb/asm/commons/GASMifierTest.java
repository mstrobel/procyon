/***
 * ASM tests
 * Copyright (c) 2002-2005 France Telecom
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
package org.objectweb.asm.commons;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.TestSuite;

import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.DebuggingInformation;
import org.codehaus.janino.IClassLoader;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.UnitCompiler;

import org.objectweb.asm.AbstractTest;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.attrs.CodeComment;
import org.objectweb.asm.attrs.Comment;

/**
 * GASMifier tests.
 * 
 * @author Eugene Kuleshov
 * @author Eric Bruneton
 */
public class GASMifierTest extends AbstractTest {

    public static final Compiler COMPILER = new Compiler();

    private final static TestClassLoader LOADER = new TestClassLoader();

    public static TestSuite suite() throws Exception {
        return new GASMifierTest().getSuite();
    }

    public void test() throws Exception {
        ClassReader cr = new ClassReader(is);

        if (cr.b.length > 20000) {
            return;
        }

        StringWriter sw = new StringWriter();
        GASMifierClassVisitor cv = new GASMifierClassVisitor(new PrintWriter(sw));
        cr.accept(cv,
                new Attribute[] { new Comment(), new CodeComment() },
                ClassReader.EXPAND_FRAMES);

        String generated = sw.toString();

        byte[] generatorClassData;
        try {
            generatorClassData = COMPILER.compile(n, generated);
        } catch (Exception ex) {
            trace(generated);
            throw ex;
        }

        ClassWriter cw = new ClassWriter(0);
        cr.accept(new ClassAdapter(cw) {
            public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String desc,
                final String signature,
                final String[] exceptions)
            {
                return new LocalVariablesSorter(access,
                        desc,
                        super.visitMethod(access,
                                name,
                                desc,
                                signature,
                                exceptions));
            }
        },
                new Attribute[] { new Comment(), new CodeComment() },
                ClassReader.EXPAND_FRAMES);
        cr = new ClassReader(cw.toByteArray());

        String nd = n + "Dump";
        if (n.indexOf('.') != -1) {
            nd = "asm." + nd;
        }

        Class c = LOADER.defineClass(nd, generatorClassData);
        Method m = c.getMethod("dump", new Class[0]);
        byte[] b;
        try {
            b = (byte[]) m.invoke(null, new Object[0]);
        } catch (InvocationTargetException ex) {
            trace(generated);
            throw (Exception) ex.getTargetException();
        }

        try {
            assertEquals(cr, new ClassReader(b), new Filter(), new Filter());
        } catch (Throwable e) {
            trace(generated);
            assertEquals(cr, new ClassReader(b), new Filter(), new Filter());
        }
    }

    private void trace(final String generated) {
        if (System.getProperty("asm.test.class") != null) {
            System.err.println(generated);
        }
    }

    static class TestClassLoader extends ClassLoader {

        public Class defineClass(final String name, final byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    static class Compiler {

        final static IClassLoader CL = new ClassLoaderIClassLoader(new URLClassLoader(new URL[0]));

        public byte[] compile(final String name, final String source)
                throws Exception
        {
            Parser p = new Parser(new Scanner(name, new StringReader(source)));
            UnitCompiler uc = new UnitCompiler(p.parseCompilationUnit(), CL);
            return uc.compileUnit(DebuggingInformation.ALL)[0].toByteArray();
        }
    }

    static class Filter extends ClassAdapter {

        public Filter() {
            super(null);
        }

        public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions)
        {
            return new MethodAdapter(super.visitMethod(access,
                    name,
                    desc,
                    signature,
                    exceptions))
            {
                public void visitMaxs(final int maxStack, final int maxLocals) {
                    super.visitMaxs(0, 0);
                }
            };
        }
    }
}
