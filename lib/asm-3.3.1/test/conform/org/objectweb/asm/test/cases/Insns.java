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
import org.objectweb.asm.Type;

/**
 * Generates a class that contain all bytecode instruction types (except JSR and
 * RET). Also covers access flags, signatures, and unicode characters.
 * 
 * @author Eric Bruneton
 */
public class Insns extends Generator {

    public void generate(final String dir) throws IOException {
        generate(dir, "pkg/Insns.class", dump());
    }

    public byte[] dump() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(V1_5,
                ACC_PUBLIC + ACC_SUPER,
                "pkg/Insns",
                "<E:Ljava/lang/Object;F:Ljava/lang/Exception;>Ljava/util/ArrayList<Ljava/lang/String;>;LInterface<TE;>;",
                "java/util/ArrayList",
                new String[] { "Interface" });

        fv = cw.visitField(ACC_PRIVATE + ACC_FINAL,
                "z",
                "Z",
                null,
                new Integer(1));
        fv.visitEnd();

        fv = cw.visitField(ACC_PROTECTED, "b", "B", null, null);
        fv.visitEnd();

        fv = cw.visitField(ACC_PUBLIC, "c", "C", null, null);
        fv.visitEnd();

        fv = cw.visitField(ACC_STATIC, "s", "S", null, null);
        fv.visitEnd();

        fv = cw.visitField(ACC_PRIVATE + ACC_TRANSIENT, "i", "I", null, null);
        fv.visitEnd();

        fv = cw.visitField(ACC_PRIVATE + ACC_VOLATILE, "l", "J", null, null);
        fv.visitEnd();

        fv = cw.visitField(0, "f", "F", null, null);
        fv.visitEnd();

        fv = cw.visitField(0, "d", "D", null, null);
        fv.visitEnd();

        fv = cw.visitField(0, "str", "Ljava/lang/String;", null, "");
        fv.visitEnd();

        fv = cw.visitField(0, "e", "Ljava/lang/Object;", "TE;", null);
        fv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL,
                "java/util/ArrayList",
                "<init>",
                "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC + ACC_SYNCHRONIZED,
                "m",
                "(ZBCSIFJDLjava/lang/Object;)Ljava/lang/Object;",
                "(ZBCSIFJDTE;)TE;",
                null);
        mv.visitCode();
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // instruction types
        constInsns(cw);
        varInsns(cw);
        arrayInsns(cw);
        stackInsns(cw);
        mathInsns(cw);
        castInsns(cw);
        jumpInsns(cw);
        returnInsns(cw);
        fieldInsns(cw);
        methodInsns(cw);
        monitorInsns(cw);

        // various method types not covered by other test cases
        varargMethod(cw);
        bridgeMethod(cw);
        nativeMethod(cw);
        clinitMethod(cw);

        cw.visitEnd();

        return cw.toByteArray();
    }

    private void constInsns(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                "constInsns",
                "()V",
                null,
                null);
        mv.visitInsn(NOP);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ICONST_M1);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ICONST_2);
        mv.visitInsn(ICONST_3);
        mv.visitInsn(ICONST_4);
        mv.visitInsn(ICONST_5);
        mv.visitInsn(LCONST_0);
        mv.visitInsn(LCONST_1);
        mv.visitInsn(FCONST_0);
        mv.visitInsn(FCONST_1);
        mv.visitInsn(FCONST_2);
        mv.visitInsn(DCONST_0);
        mv.visitInsn(DCONST_1);
        mv.visitIntInsn(BIPUSH, 16);
        mv.visitIntInsn(SIPUSH, 256);
        mv.visitLdcInsn(new Integer(65536));
        mv.visitLdcInsn(new Long(128L));
        mv.visitLdcInsn(new Float("128.0"));
        mv.visitLdcInsn(new Double("128.0"));
        mv.visitLdcInsn("\n\r\u0009\"\\");
        mv.visitLdcInsn("\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u1111\u0111\u0011\u0001");
        mv.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void varInsns(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                "varInsns",
                "(IJFDD)V",
                null,
                null);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(IINC, 1);
        mv.visitVarInsn(ISTORE, 1);
        mv.visitVarInsn(LLOAD, 2);
        mv.visitVarInsn(LSTORE, 2);
        mv.visitVarInsn(FLOAD, 4);
        mv.visitVarInsn(FSTORE, 4);
        mv.visitVarInsn(DLOAD, 5);
        mv.visitVarInsn(DSTORE, 5);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ASTORE, 0);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 3);
        mv.visitInsn(LCONST_0);
        mv.visitVarInsn(LSTORE, 6);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void arrayInsns(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                "arrayInsns",
                "()V",
                null,
                null);
        // boolean arrays
        mv.visitInsn(ICONST_1);
        mv.visitIntInsn(NEWARRAY, T_BOOLEAN);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(BASTORE);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(BALOAD);
        // byte arrays
        mv.visitInsn(ICONST_1);
        mv.visitIntInsn(NEWARRAY, T_BYTE);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(BASTORE);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(BALOAD);
        // char arrays
        mv.visitInsn(ICONST_1);
        mv.visitIntInsn(NEWARRAY, T_CHAR);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(CASTORE);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(CALOAD);
        // short arrays
        mv.visitInsn(ICONST_1);
        mv.visitIntInsn(NEWARRAY, T_SHORT);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(SASTORE);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(SALOAD);
        // int arrays
        mv.visitInsn(ICONST_1);
        mv.visitIntInsn(NEWARRAY, T_INT);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IASTORE);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IALOAD);
        // long arrays
        mv.visitInsn(ICONST_1);
        mv.visitIntInsn(NEWARRAY, T_LONG);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(LCONST_0);
        mv.visitInsn(LASTORE);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(LALOAD);
        // float arrays
        mv.visitInsn(ICONST_1);
        mv.visitIntInsn(NEWARRAY, T_FLOAT);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(FCONST_0);
        mv.visitInsn(FASTORE);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(FALOAD);
        // double arrays
        mv.visitInsn(ICONST_1);
        mv.visitIntInsn(NEWARRAY, T_DOUBLE);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(DCONST_0);
        mv.visitInsn(DASTORE);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(DALOAD);
        // object arrays
        mv.visitInsn(ICONST_1);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(AASTORE);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(AALOAD);
        // multi dimensional arrays
        mv.visitInsn(ICONST_1);
        mv.visitTypeInsn(ANEWARRAY, "[I");
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ICONST_2);
        mv.visitInsn(ICONST_3);
        mv.visitMultiANewArrayInsn("[[[I", 3);
        // array length
        mv.visitInsn(ARRAYLENGTH);
        // end method
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void stackInsns(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                "stackInsns",
                "()V",
                null,
                null);
        // pop
        mv.visitInsn(ICONST_0);
        mv.visitInsn(POP);
        // pop2 (two variants)
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(POP2);
        mv.visitInsn(LCONST_0);
        mv.visitInsn(POP2);
        // dup
        mv.visitInsn(ICONST_0);
        mv.visitInsn(DUP);
        // dup_x1
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(DUP_X1);
        // dup_x2 (two variants)
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(DUP_X2);
        mv.visitInsn(LCONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(DUP_X2);
        // dup2 (two variants)
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(DUP2);
        mv.visitInsn(LCONST_0);
        mv.visitInsn(DUP2);
        // dup2_x1 (two variants)
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(DUP2_X1);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(LCONST_0);
        mv.visitInsn(DUP2_X1);
        // dup2_x2 (four variants)
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(DUP2_X2);
        mv.visitInsn(LCONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(DUP2_X2);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(LCONST_0);
        mv.visitInsn(DUP2_X2);
        mv.visitInsn(LCONST_0);
        mv.visitInsn(LCONST_0);
        mv.visitInsn(DUP2_X2);
        // swap
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(SWAP);
        // end method
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void mathInsns(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                "mathInsns",
                "(IJFD)V",
                null,
                null);
        // int math insns
        for (int i = 0; i < 12; ++i) {
            mv.visitVarInsn(ILOAD, 1);
        }
        mv.visitInsn(IADD);
        mv.visitInsn(ISUB);
        mv.visitInsn(IMUL);
        mv.visitInsn(IDIV);
        mv.visitInsn(IREM);
        mv.visitInsn(INEG);
        mv.visitInsn(ISHL);
        mv.visitInsn(ISHR);
        mv.visitInsn(IUSHR);
        mv.visitInsn(IAND);
        mv.visitInsn(IOR);
        mv.visitInsn(IXOR);
        // long math insns
        for (int i = 0; i < 9; ++i) {
            mv.visitVarInsn(LLOAD, 2);
        }
        mv.visitInsn(LADD);
        mv.visitInsn(LSUB);
        mv.visitInsn(LMUL);
        mv.visitInsn(LDIV);
        mv.visitInsn(LREM);
        mv.visitInsn(LNEG);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(LSHL);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(LSHR);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(LUSHR);
        mv.visitInsn(LAND);
        mv.visitInsn(LOR);
        mv.visitInsn(LXOR);
        // float math insns
        for (int i = 0; i < 6; ++i) {
            mv.visitVarInsn(FLOAD, 4);
        }
        mv.visitInsn(FADD);
        mv.visitInsn(FSUB);
        mv.visitInsn(FMUL);
        mv.visitInsn(FDIV);
        mv.visitInsn(FREM);
        mv.visitInsn(FNEG);
        // double math insns
        for (int i = 0; i < 6; ++i) {
            mv.visitVarInsn(DLOAD, 5);
        }
        mv.visitInsn(DADD);
        mv.visitInsn(DSUB);
        mv.visitInsn(DMUL);
        mv.visitInsn(DDIV);
        mv.visitInsn(DREM);
        mv.visitInsn(DNEG);
        // end method
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void castInsns(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                "castInsns",
                "(IJFD)V",
                null,
                null);
        // I2x
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(I2L);
        mv.visitVarInsn(LSTORE, 2);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(I2F);
        mv.visitVarInsn(FSTORE, 4);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(I2D);
        mv.visitVarInsn(DSTORE, 5);
        // L2x
        mv.visitVarInsn(LLOAD, 2);
        mv.visitInsn(L2I);
        mv.visitVarInsn(ISTORE, 1);
        mv.visitVarInsn(LLOAD, 2);
        mv.visitInsn(L2F);
        mv.visitVarInsn(FSTORE, 4);
        mv.visitVarInsn(LLOAD, 2);
        mv.visitInsn(L2D);
        mv.visitVarInsn(DSTORE, 5);
        // F2x
        mv.visitVarInsn(FLOAD, 4);
        mv.visitInsn(F2I);
        mv.visitVarInsn(ISTORE, 1);
        mv.visitVarInsn(FLOAD, 4);
        mv.visitInsn(F2L);
        mv.visitVarInsn(LSTORE, 2);
        mv.visitVarInsn(FLOAD, 4);
        mv.visitInsn(F2D);
        mv.visitVarInsn(DSTORE, 5);
        // D2x
        mv.visitVarInsn(DLOAD, 5);
        mv.visitInsn(D2I);
        mv.visitVarInsn(ISTORE, 1);
        mv.visitVarInsn(DLOAD, 5);
        mv.visitInsn(D2L);
        mv.visitVarInsn(LSTORE, 2);
        mv.visitVarInsn(DLOAD, 5);
        mv.visitInsn(D2F);
        mv.visitVarInsn(FSTORE, 4);
        // I2B
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(I2B);
        mv.visitFieldInsn(PUTFIELD, "pkg/Insns", "b", "B");
        // I2C
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(I2C);
        mv.visitFieldInsn(PUTFIELD, "pkg/Insns", "c", "C");
        // I2S
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(I2S);
        mv.visitFieldInsn(PUTSTATIC, "pkg/Insns", "s", "S");
        // checkcast
        mv.visitInsn(ACONST_NULL);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitInsn(ACONST_NULL);
        mv.visitTypeInsn(CHECKCAST, "[[I");
        // instanceof
        mv.visitInsn(ACONST_NULL);
        mv.visitTypeInsn(INSTANCEOF, "java/lang/String");
        // end method
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void jumpInsns(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                "jumpInsns",
                "(IJFD)V",
                null,
                null);
        Label l0 = new Label();
        // ifxx
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IFNE, l0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IFEQ, l0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IFLE, l0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IFGE, l0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IFLT, l0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IFGT, l0);
        // ificmpxx
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IF_ICMPNE, l0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IF_ICMPEQ, l0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IF_ICMPLE, l0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IF_ICMPGE, l0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IF_ICMPLT, l0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IF_ICMPGT, l0);
        // lcmp
        mv.visitVarInsn(LLOAD, 2);
        mv.visitVarInsn(LLOAD, 2);
        mv.visitInsn(LCMP);
        mv.visitJumpInsn(IFNE, l0);
        // fcmpx
        mv.visitVarInsn(FLOAD, 4);
        mv.visitVarInsn(FLOAD, 4);
        mv.visitInsn(FCMPL);
        mv.visitJumpInsn(IFNE, l0);
        mv.visitVarInsn(FLOAD, 4);
        mv.visitVarInsn(FLOAD, 4);
        mv.visitInsn(FCMPG);
        mv.visitJumpInsn(IFNE, l0);
        // dcmpx
        mv.visitVarInsn(DLOAD, 5);
        mv.visitVarInsn(DLOAD, 5);
        mv.visitInsn(DCMPL);
        mv.visitJumpInsn(IFNE, l0);
        mv.visitVarInsn(DLOAD, 5);
        mv.visitVarInsn(DLOAD, 5);
        mv.visitInsn(DCMPG);
        mv.visitJumpInsn(IFNE, l0);
        // ifacmp
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitJumpInsn(IF_ACMPNE, l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitJumpInsn(IF_ACMPEQ, l0);
        // ifnull
        mv.visitVarInsn(ALOAD, 0);
        mv.visitJumpInsn(IFNULL, l0);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 7);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitJumpInsn(IFNONNULL, l0);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 7);
        mv.visitVarInsn(ALOAD, 0);
        // tableswitch
        Label l1 = new Label();
        Label l2 = new Label();
        Label l3 = new Label();
        mv.visitVarInsn(ILOAD, 1);
        mv.visitTableSwitchInsn(0, 2, l3, new Label[] { l1, l2, l3 });
        mv.visitLabel(l1);
        mv.visitInsn(ICONST_1);
        mv.visitVarInsn(ISTORE, 7);
        mv.visitJumpInsn(GOTO, l3);
        mv.visitLabel(l2);
        mv.visitInsn(ICONST_2);
        mv.visitVarInsn(ISTORE, 7);
        mv.visitJumpInsn(GOTO, l3);
        mv.visitLabel(l3);
        mv.visitVarInsn(ILOAD, 7);
        // lookupswitch
        Label l4 = new Label();
        Label l5 = new Label();
        Label l6 = new Label();
        mv.visitVarInsn(ILOAD, 1);
        mv.visitLookupSwitchInsn(l6, new int[] { 0, 1, 2 }, new Label[] {
            l4,
            l5,
            l6 });
        mv.visitLabel(l4);
        mv.visitInsn(ICONST_1);
        mv.visitVarInsn(ISTORE, 7);
        mv.visitJumpInsn(GOTO, l6);
        mv.visitLabel(l5);
        mv.visitInsn(ICONST_2);
        mv.visitVarInsn(ISTORE, 7);
        mv.visitJumpInsn(GOTO, l6);
        mv.visitLabel(l6);
        mv.visitVarInsn(ILOAD, 7);
        // throw
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ATHROW);
        // misc instructions to cover code in MethodWriter.resizeInsns
        mv.visitLabel(l0);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ICONST_2);
        mv.visitMultiANewArrayInsn("[[I", 2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()V");
        // end method
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void returnInsns(final ClassWriter cw) {
        MethodVisitor mv;
        mv = cw.visitMethod(ACC_STATIC, "ireturnInsn", "()I", null, null);
        mv.visitCode();
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(ACC_PRIVATE, "lreturnInsn", "()J", null, null);
        mv.visitCode();
        mv.visitInsn(LCONST_0);
        mv.visitInsn(LRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(0, "freturnInsn", "()F", null, null);
        mv.visitCode();
        mv.visitInsn(FCONST_0);
        mv.visitInsn(FRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(0, "dreturnInsn", "()D", null, null);
        mv.visitCode();
        mv.visitInsn(DCONST_0);
        mv.visitInsn(DRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void fieldInsns(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                "fieldInsns",
                "()V",
                null,
                null);
        mv.visitFieldInsn(GETSTATIC, "pkg/Insns", "s", "S");
        mv.visitFieldInsn(PUTSTATIC, "pkg/Insns", "s", "S");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "pkg/Insns", "i", "I");
        mv.visitFieldInsn(PUTFIELD, "pkg/Insns", "i", "I");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "pkg/Insns", "l", "J");
        mv.visitFieldInsn(PUTFIELD, "pkg/Insns", "l", "J");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void methodInsns(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                "methodInsns",
                "()V",
                null,
                null);
        // invokstatic
        mv.visitMethodInsn(INVOKESTATIC, "pkg/Insns", "ireturn", "()I");
        // invokespecial
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "pkg/Insns", "lreturn", "()J");
        // invokevirtual
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "pkg/Insns", "freturn", "()F");
        // invokeinterface
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void monitorInsns(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                "monitorInsns",
                "()V",
                null,
                null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(MONITORENTER);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(MONITOREXIT);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void varargMethod(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_VARARGS + ACC_STRICT,
                "varargMethod",
                "([Ljava/lang/Object;)V",
                "([Ljava/lang/Object;)V^TF;",
                new String[] { "java/lang/Exception" });
        mv.visitCode();
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void bridgeMethod(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                "get",
                "(I)Ljava/lang/String;",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL,
                "java/util/ArrayList",
                "get",
                "(I)Ljava/lang/Object;");
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC,
                "get",
                "(I)Ljava/lang/Object;",
                "(I)TE;",
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "pkg/Insns",
                "get",
                "(I)Ljava/lang/String;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void nativeMethod(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE + ACC_NATIVE,
                "nativeMethod",
                "()V",
                null,
                null);
        mv.visitEnd();
    }

    private void clinitMethod(final ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null);
        mv.visitCode();
        mv.visitInsn(ICONST_1);
        mv.visitFieldInsn(PUTSTATIC, "pkg/Insns", "s", "S");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
