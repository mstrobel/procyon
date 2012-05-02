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

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.objectweb.asm.AbstractTest;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * AdviceAdapter tests.
 * 
 * @author Eugene Kuleshov
 */
public class AdviceAdapterTest extends AbstractTest {

    public static void main(final String[] args) throws Exception {
        TestRunner.run(AdviceAdapterTest.suite());
    }

    public static TestSuite suite() throws Exception {
        return new AdviceAdapterTest().getSuite();
    }

    public void test() throws Exception {
        ClassReader cr = new ClassReader(is);
        ClassWriter cw1 = new ClassWriter(0);
        ClassWriter cw2 = new ClassWriter(0);
        cr.accept(new ReferenceClassAdapter(cw1), ClassReader.EXPAND_FRAMES);
        cr.accept(new AdviceClassAdapter(cw2), ClassReader.EXPAND_FRAMES);
        assertEquals(new ClassReader(cw1.toByteArray()),
                new ClassReader(cw2.toByteArray()));
    }

    static class ReferenceClassAdapter extends ClassAdapter {

        public ReferenceClassAdapter(final ClassVisitor cv) {
            super(cv);
        }

        public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions)
        {
            MethodVisitor mv = cv.visitMethod(access,
                    name,
                    desc,
                    signature,
                    exceptions);

            if (mv == null
                    || (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) > 0)
            {
                return mv;
            }

            return new LocalVariablesSorter(access, desc, mv);
        }
    }

    static class AdviceClassAdapter extends ClassAdapter {

        public AdviceClassAdapter(final ClassVisitor cv) {
            super(cv);
        }

        public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions)
        {
            MethodVisitor mv = cv.visitMethod(access,
                    name,
                    desc,
                    signature,
                    exceptions);

            if (mv == null
                    || (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) > 0)
            {
                return mv;
            }

            return new AdviceAdapter(mv, access, name, desc) {
                protected void onMethodEnter() {
                    // mv.visitInsn(NOP);
                    // mv.visitInsn(NOP);
                    // mv.visitInsn(NOP);
                }

                protected void onMethodExit(final int opcode) {
                    // mv.visitInsn(NOP);
                    // mv.visitInsn(NOP);
                    // mv.visitInsn(NOP);
                    // mv.visitInsn(NOP);
                }
            };
        }
    }
}
