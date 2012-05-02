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

import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A naive implementation of compiler for Brain**** language.
 * http://www.muppetlabs.com/~breadbox/bf/ *
 * 
 * @author Eugene Kuleshov
 */
public class BFCompiler implements Opcodes {

    private static final int V_IS = 0;

    private static final int V_OS = 1;

    private static final int V_P = 2;

    private static final int V_D = 3;

    public void compile(
        final Reader r,
        final String className,
        final String sourceName,
        final ClassVisitor cv) throws IOException
    {
        cv.visit(Opcodes.V1_3,
                ACC_PUBLIC,
                className.replace('.', '/'),
                null,
                "java/lang/Object",
                null);
        cv.visitSource(sourceName, null);

        MethodVisitor mv;
        {
            mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL,
                    "java/lang/Object",
                    "<init>",
                    "()V");
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        {
            // Init local vars for BF environment:
            // 0 InputStream
            // 1 OutputStream
            // 2 Data Pointer
            // 3 Data Array (int[ 30000])

            mv = cv.visitMethod(ACC_PUBLIC + ACC_STATIC,
                    "main",
                    "([Ljava/lang/String;)V",
                    null,
                    null);
            mv.visitCode();

            mv.visitFieldInsn(GETSTATIC,
                    "java/lang/System",
                    "in",
                    "Ljava/io/InputStream;");
            mv.visitVarInsn(ASTORE, V_IS);

            mv.visitFieldInsn(GETSTATIC,
                    "java/lang/System",
                    "out",
                    "Ljava/io/PrintStream;");
            mv.visitVarInsn(ASTORE, V_OS);

            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, V_P);

            mv.visitIntInsn(SIPUSH, 30000);
            mv.visitIntInsn(NEWARRAY, T_INT);
            mv.visitVarInsn(ASTORE, V_D);

            Stack labels = new Stack();

            int d = 0;
            int p = 0;

            int c;
            while ((c = r.read()) != -1) {
                switch (c) {
                    case '>':
                        d = storeD(mv, d);
                        p++;
                        break;

                    case '<':
                        d = storeD(mv, d);
                        p--;
                        break;

                    case '+':
                        p = storeP(mv, p);
                        d++;
                        break;

                    case '-':
                        p = storeP(mv, p);
                        d--;
                        break;

                    case '.':
                        p = storeP(mv, p);
                        d = storeD(mv, d);

                        mv.visitVarInsn(ALOAD, V_OS);
                        mv.visitVarInsn(ALOAD, V_D);
                        mv.visitVarInsn(ILOAD, V_P);
                        mv.visitInsn(IALOAD);
                        mv.visitMethodInsn(INVOKEVIRTUAL,
                                "java/io/OutputStream",
                                "write",
                                "(I)V");
                        break;

                    case ',':
                        p = storeP(mv, p);
                        d = storeD(mv, d);

                        mv.visitVarInsn(ALOAD, V_D);
                        mv.visitVarInsn(ILOAD, V_P);
                        mv.visitVarInsn(ALOAD, V_IS);
                        mv.visitMethodInsn(INVOKEVIRTUAL,
                                "java/io/InputStream",
                                "read",
                                "()I");
                        mv.visitInsn(IASTORE);
                        break;

                    case '[':
                        p = storeP(mv, p);
                        d = storeD(mv, d);

                        Label ls = new Label();
                        Label le = new Label();
                        labels.push(ls);
                        labels.push(le);
                        mv.visitJumpInsn(GOTO, le);
                        mv.visitLabel(ls);
                        break;

                    case ']':
                        p = storeP(mv, p);
                        d = storeD(mv, d);

                        mv.visitLabel((Label) labels.pop());
                        mv.visitVarInsn(ALOAD, V_D);
                        mv.visitVarInsn(ILOAD, V_P);
                        mv.visitInsn(IALOAD);
                        mv.visitJumpInsn(IFNE, (Label) labels.pop());
                        break;
                }
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

    }

    private int storeD(final MethodVisitor mv, final int d) {
        if (d != 0) {
            mv.visitVarInsn(ALOAD, V_D);
            mv.visitVarInsn(ILOAD, V_P);
            mv.visitInsn(DUP2);
            mv.visitInsn(IALOAD);
            mv.visitIntInsn(SIPUSH, d);
            mv.visitInsn(IADD);
            mv.visitInsn(IASTORE);
        }
        return 0;
    }

    private int storeP(final MethodVisitor mv, final int p) {
        if (p != 0) {
            mv.visitIincInsn(V_P, p);
        }
        return 0;
    }

}
