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
 * Generates a class which uses a lot of locals and constant pool values. Covers
 * the 'wide' form of instructions that have one (xLOAD and xSTORE, LDC, IINC,
 * GOTO and IF instructions - these jump instructions are not directly generated
 * in their 'wide' form, but are transformed to this form by the method resizing
 * test). Also covers the V1_2 class version.
 * 
 * @author Eric Bruneton
 */
public class Wide extends Generator {

    public void generate(final String dir) throws IOException {
        generate(dir, "pkg/Wide.class", dump());
    }

    public byte[] dump() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        MethodVisitor mv;

        cw.visit(V1_2, ACC_PUBLIC, "pkg/Wide", null, "java/lang/Object", null);

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        for (int i = 0; i < 256; ++i) {
            mv.visitLdcInsn(Integer.toString(i)); // wide form
            mv.visitInsn(POP);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "wideLocals", "(I)I", null, null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        mv.visitJumpInsn(GOTO, l1); // will give GOTO_W

        mv.visitLabel(l0);
        mv.visitIincInsn(300, 1); // covers 'update maxlocals' in MethodWriter
        mv.visitVarInsn(ILOAD, 300); // will give wide form
        mv.visitJumpInsn(IFEQ, l1); // will give long forward jump

        // many NOPs will be introduced here by the method resizing test

        mv.visitVarInsn(ILOAD, 300); // will give wide form
        mv.visitInsn(IRETURN);

        mv.visitLabel(l1);
        for (int i = 1; i < 300; ++i) {
            mv.visitVarInsn(ILOAD, i);
            if (i <= 5) {
                mv.visitInsn(ICONST_0 + i);
            } else if (i <= Byte.MAX_VALUE) {
                mv.visitIntInsn(BIPUSH, i);
            } else {
                mv.visitIntInsn(SIPUSH, i);
            }
            mv.visitInsn(IADD);
            mv.visitVarInsn(ISTORE, i + 1);
        }
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(IFEQ, l0); // will give long backward jump
        mv.visitJumpInsn(GOTO, l0); // will give long backward goto

        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }
}
