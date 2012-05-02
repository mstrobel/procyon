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
import org.objectweb.asm.Type;

/**
 * Generates an enum class.
 * 
 * @author Eric Bruneton
 */
public class Enum extends Generator {

    public void generate(final String dir) throws IOException {
        generate(dir, "pkg/Enum.class", dump());
    }

    public byte[] dump() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(V1_5,
                ACC_PUBLIC + ACC_FINAL + ACC_SUPER + ACC_ENUM,
                "pkg/Enum",
                "Ljava/lang/Enum<Lpkg/Enum;>;",
                "java/lang/Enum",
                null);

        fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC + ACC_ENUM,
                "V0",
                "Lpkg/Enum;",
                null,
                null);
        fv.visitEnd();

        fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC + ACC_ENUM,
                "V1",
                "Lpkg/Enum;",
                null,
                null);
        fv.visitEnd();

        fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC + ACC_ENUM,
                "V2",
                "Lpkg/Enum;",
                null,
                null);
        fv.visitEnd();

        fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC + ACC_SYNTHETIC,
                "$VALUES",
                "[Lpkg/Enum;",
                null,
                null);
        fv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
                "values",
                "()[Lpkg/Enum;",
                null,
                null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, "pkg/Enum", "$VALUES", "[Lpkg/Enum;");
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "[Lpkg/Enum;",
                "clone",
                "()Ljava/lang/Object;");
        mv.visitTypeInsn(CHECKCAST, "[Lpkg/Enum;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC,
                "valueOf",
                "(Ljava/lang/String;)Lpkg/Enum;",
                null,
                null);
        mv.visitCode();
        mv.visitLdcInsn(Type.getType("Lpkg/Enum;"));
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC,
                "java/lang/Enum",
                "valueOf",
                "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;");
        mv.visitTypeInsn(CHECKCAST, "pkg/Enum");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PRIVATE,
                "<init>",
                "(Ljava/lang/String;I)V",
                "()V",
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitMethodInsn(INVOKESPECIAL,
                "java/lang/Enum",
                "<init>",
                "(Ljava/lang/String;I)V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, "pkg/Enum");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("V0");
        mv.visitInsn(ICONST_0);
        mv.visitMethodInsn(INVOKESPECIAL,
                "pkg/Enum",
                "<init>",
                "(Ljava/lang/String;I)V");
        mv.visitFieldInsn(PUTSTATIC, "pkg/Enum", "V0", "Lpkg/Enum;");
        mv.visitTypeInsn(NEW, "pkg/Enum");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("V1");
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKESPECIAL,
                "pkg/Enum",
                "<init>",
                "(Ljava/lang/String;I)V");
        mv.visitFieldInsn(PUTSTATIC, "pkg/Enum", "V1", "Lpkg/Enum;");
        mv.visitTypeInsn(NEW, "pkg/Enum");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("V2");
        mv.visitInsn(ICONST_2);
        mv.visitMethodInsn(INVOKESPECIAL,
                "pkg/Enum",
                "<init>",
                "(Ljava/lang/String;I)V");
        mv.visitFieldInsn(PUTSTATIC, "pkg/Enum", "V2", "Lpkg/Enum;");
        mv.visitInsn(ICONST_3);
        mv.visitTypeInsn(ANEWARRAY, "pkg/Enum");
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitFieldInsn(GETSTATIC, "pkg/Enum", "V0", "Lpkg/Enum;");
        mv.visitInsn(AASTORE);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_1);
        mv.visitFieldInsn(GETSTATIC, "pkg/Enum", "V1", "Lpkg/Enum;");
        mv.visitInsn(AASTORE);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_2);
        mv.visitFieldInsn(GETSTATIC, "pkg/Enum", "V2", "Lpkg/Enum;");
        mv.visitInsn(AASTORE);
        mv.visitFieldInsn(PUTSTATIC, "pkg/Enum", "$VALUES", "[Lpkg/Enum;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }
}
