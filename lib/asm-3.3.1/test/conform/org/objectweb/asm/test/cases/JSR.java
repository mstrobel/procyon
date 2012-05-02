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
package org.objectweb.asm.test.cases;

import java.io.IOException;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * Generates classes with JSR/RET instructions. Covers forward and backward JSR
 * and JSR_W instructions. Also covers the wide form of IFNULL instruction, and
 * the V1_1 class version (these jump instructions are not directly generated in
 * their 'wide' form, but are transformed to this form by the method resizing
 * test).
 * 
 * @author Eric Bruneton
 * 
 */
public class JSR extends Generator {

    public void generate(final String dir) throws IOException {
        generate(dir, "pkg/JSR1.class", dumpForwardJSR());
        generate(dir, "pkg/JSR2.class", dumpBackwardJSR());
    }

    public byte[] dumpForwardJSR() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        MethodVisitor mv;

        cw.visit(V1_1, ACC_PUBLIC, "pkg/JSR1", null, "java/lang/Object", null);

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "forwardJSR", "([I)V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        Label l3 = new Label();
        Label l4 = new Label();
        Label l5 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, null);
        mv.visitTryCatchBlock(l2, l3, l2, null);
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "pkg/JSR1", "forwardJSR", "([I)V");
        mv.visitJumpInsn(JSR, l4); // forward JSR, will give forward JSR_W

        // many NOPs will be introduced here by the method resizing test

        mv.visitLabel(l1);
        mv.visitJumpInsn(GOTO, l5);
        mv.visitLabel(l2);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitJumpInsn(JSR, l4);
        mv.visitLabel(l3);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l4);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitInsn(DCONST_0);
        mv.visitVarInsn(DSTORE, 4);
        mv.visitVarInsn(RET, 3);
        mv.visitLabel(l5);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }

    public byte[] dumpBackwardJSR() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        MethodVisitor mv;

        cw.visit(V1_1, ACC_PUBLIC, "pkg/JSR2", null, "java/lang/Object", null);

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "backwardJSR", "([I)V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        Label l3 = new Label();
        Label l4 = new Label();
        Label l5 = new Label();
        Label l6 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, null);
        mv.visitTryCatchBlock(l2, l3, l2, null);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 4);
        mv.visitJumpInsn(GOTO, l0);
        mv.visitLabel(l4);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitIincInsn(4, 1);
        mv.visitVarInsn(RET, 3);

        /* extra instructions only used to trigger method resizing */
        mv.visitLabel(l0);
        mv.visitInsn(ACONST_NULL);
        mv.visitJumpInsn(IFNULL, l6); // will give wide IFNULL
        // many NOPs will be introduced here by the method resizing test
        mv.visitJumpInsn(GOTO, l6);
        mv.visitLabel(l6);

        mv.visitJumpInsn(JSR, l4); // backward JSR, will give JSR_W
        mv.visitLabel(l1);
        mv.visitJumpInsn(GOTO, l5);
        mv.visitLabel(l2);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitJumpInsn(JSR, l4);
        mv.visitLabel(l3);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l5);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }
}
