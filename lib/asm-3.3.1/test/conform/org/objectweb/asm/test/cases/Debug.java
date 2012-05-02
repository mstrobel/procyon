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
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * Generates a class with debug information. Covers line number tables, local
 * variable tables, source file, source debug, etc. Also covers the
 * serialVersionUID field (to cover a branch in SerialVersionUIDAdder).
 * 
 * @author Eric Bruneton
 */
public class Debug extends Generator {

    public void generate(final String dir) throws IOException {
        generate(dir, "pkg/Debug.class", dump());
    }

    public byte[] dump() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        cw.visit(V1_5,
                ACC_PUBLIC + ACC_SUPER,
                "pkg/Debug",
                null,
                "java/lang/Object",
                new String[] { "java/io/Serializable" });

        cw.visitSource("Debug.java", "source-debug");

        FieldVisitor fv = cw.visitField(ACC_FINAL + ACC_STATIC,
                "serialVersionUID",
                "J",
                null,
                new Long(1L));
        fv.visitEnd();

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(3, l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(IFEQ, l1);
        mv.visitJumpInsn(GOTO, l1);
        mv.visitLabel(l1);
        mv.visitLineNumber(3, l1);
        mv.visitInsn(RETURN);
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "Lpkg/Debug;", "Lpkg/Debug;", l0, l2, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }
}
