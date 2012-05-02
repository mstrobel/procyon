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
import org.objectweb.asm.MethodVisitor;

/**
 * Generates a class with two inner classes. Covers all features used by inner
 * classes (visitInner, visitOuter, synthetic members, etc). Also covers the
 * V1_4 class version and the DEPRECATED access flag.
 * 
 * @author Eric Bruneton
 */
public class Outer extends Generator {

    public void generate(final String dir) throws IOException {
        generate(dir, "pkg/Outer.class", dump());
        generate(dir, "pkg/Outer$1.class", dump1());
        generate(dir, "pkg/Outer$Inner.class", dumpInner());
    }

    public byte[] dump() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(V1_4,
                ACC_PUBLIC + ACC_SUPER + ACC_DEPRECATED,
                "pkg/Outer",
                null,
                "java/lang/Object",
                null);

        cw.visitInnerClass("pkg/Outer$Inner", "pkg/Outer", "C", 0);
        cw.visitInnerClass("pkg/Outer$1", null, null, 0);

        fv = cw.visitField(ACC_PRIVATE + ACC_DEPRECATED, "i", "I", null, null);
        fv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_DEPRECATED, "m", "()V", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, "pkg/Outer$1");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL,
                "pkg/Outer$1",
                "<init>",
                "(Lpkg/Outer;)V");
        mv.visitInsn(POP);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_STATIC + ACC_SYNTHETIC,
                "access$000",
                "(Lpkg/Outer;)I",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "pkg/Outer", "i", "I");
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }

    public static byte[] dump1() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(V1_4, ACC_SUPER, "pkg/Outer$1", null, "pkg/Outer", null);

        cw.visitOuterClass("pkg/Outer", "m", "()V");
        cw.visitInnerClass("pkg/Outer$1", null, null, 0);

        fv = cw.visitField(ACC_FINAL + ACC_SYNTHETIC,
                "this$0",
                "Lpkg/Outer;",
                null,
                null);
        fv.visitEnd();

        mv = cw.visitMethod(0, "<init>", "(Lpkg/Outer;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, "pkg/Outer$1", "this$0", "Lpkg/Outer;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "pkg/Outer", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC,
                "toString",
                "()Ljava/lang/String;",
                null,
                null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "()V");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "pkg/Outer$1", "this$0", "Lpkg/Outer;");
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        mv.visitLdcInsn(" ");
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "pkg/Outer$1", "this$0", "Lpkg/Outer;");
        mv.visitMethodInsn(INVOKESTATIC,
                "pkg/Outer",
                "access$000",
                "(Lpkg/Outer;)I");
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(I)Ljava/lang/StringBuilder;");
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }

    public static byte[] dumpInner() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(V1_4,
                ACC_SUPER,
                "pkg/Outer$Inner",
                null,
                "java/lang/Object",
                null);

        cw.visitInnerClass("pkg/Outer$Inner", "pkg/Outer", "C", 0);

        fv = cw.visitField(ACC_FINAL + ACC_SYNTHETIC,
                "this$0",
                "Lpkg/Outer;",
                null,
                null);
        fv.visitEnd();

        mv = cw.visitMethod(0, "<init>", "(Lpkg/Outer;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, "pkg/Outer$Inner", "this$0", "Lpkg/Outer;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }
}
