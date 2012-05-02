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
package org.objectweb.asm.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import junit.framework.TestCase;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.attrs.Comment;
import org.objectweb.asm.commons.EmptyVisitor;

public class CheckClassAdapterUnitTest extends TestCase implements Opcodes {

    public void testCheckClassVisitor() throws Exception {
        String s = getClass().getName();
        CheckClassAdapter.main(new String[0]);
        CheckClassAdapter.main(new String[] { s });
        CheckClassAdapter.main(new String[] { "output/test/cases/Interface.class" });
    }

    public void testVerifyValidClass() throws Exception {
        ClassReader cr = new ClassReader(getClass().getName());
        CheckClassAdapter.verify(cr, true, new PrintWriter(System.err));
    }

    public void testVerifyInvalidClass() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "m", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ISTORE, 30);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 31);
        mv.visitEnd();
        cw.visitEnd();
        ClassReader cr = new ClassReader(cw.toByteArray());
        CheckClassAdapter.verify(cr, true, new PrintWriter(System.err));
    }

    public void testIllegalClassAccessFlag() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        try {
            cv.visit(V1_1, 1 << 20, "C", null, "java/lang/Object", null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalSuperClass() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        try {
            cv.visit(V1_1,
                    ACC_PUBLIC,
                    "java/lang/Object",
                    null,
                    "java/lang/Object",
                    null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalInterfaceSuperClass() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        try {
            cv.visit(V1_1, ACC_INTERFACE, "I", null, "C", null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalClassSignature() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        try {
            cv.visit(V1_1, ACC_PUBLIC, "C", "LC;I", "java/lang/Object", null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalClassAccessFlagSet() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        try {
            cv.visit(V1_1,
                    ACC_FINAL + ACC_ABSTRACT,
                    "C",
                    null,
                    "java/lang/Object",
                    null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalClassMemberVisitBeforeStart() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        try {
            cv.visitSource(null, null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalClassAttribute() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
        try {
            cv.visitAttribute(null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMultipleVisitCalls() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
        try {
            cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMultipleVisitSourceCalls() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
        cv.visitSource(null, null);
        try {
            cv.visitSource(null, null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalOuterClassName() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
        try {
            cv.visitOuterClass(null, null, null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMultipleVisitOuterClassCalls() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
        cv.visitOuterClass("name", null, null);
        try {
            cv.visitOuterClass(null, null, null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldAccessFlagSet() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
        try {
            cv.visitField(ACC_PUBLIC + ACC_PRIVATE, "i", "I", null, null);
            fail();
        } catch (Exception e) {
        }
    }
    
    public void testIllegalFieldSignature() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
        try {
            cv.visitField(ACC_PUBLIC, "i", "I", "L;", null);
            fail();
        } catch (Exception e) {
        }
        try {
            cv.visitField(ACC_PUBLIC, "i", "I", "LC+", null);
            fail();
        } catch (Exception e) {
        }
        try {
            cv.visitField(ACC_PUBLIC, "i", "I", "LC;I", null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalClassMemberVisitAfterEnd() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
        cv.visitEnd();
        try {
            cv.visitSource(null, null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldMemberVisitAfterEnd() {
        FieldVisitor fv = new CheckFieldAdapter(new EmptyVisitor());
        fv.visitEnd();
        try {
            fv.visitAttribute(new Comment());
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldAttribute() {
        FieldVisitor fv = new CheckFieldAdapter(new EmptyVisitor());
        try {
            fv.visitAttribute(null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalAnnotationDesc() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        try {
            mv.visitParameterAnnotation(0, "'", true);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalAnnotationName() {
        AnnotationVisitor av = new CheckAnnotationAdapter(new EmptyVisitor());
        try {
            av.visit(null, new Integer(0));
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalAnnotationValue() {
        AnnotationVisitor av = new CheckAnnotationAdapter(new EmptyVisitor());
        try {
            av.visit("name", new Object());
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalAnnotationEnumValue() {
        AnnotationVisitor av = new CheckAnnotationAdapter(new EmptyVisitor());
        try {
            av.visitEnum("name", "Lpkg/Enum;", null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalAnnotationValueAfterEnd() {
        AnnotationVisitor av = new CheckAnnotationAdapter(new EmptyVisitor());
        av.visitEnd();
        try {
            av.visit("name", new Integer(0));
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMethodMemberVisitAfterEnd() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitEnd();
        try {
            mv.visitAttribute(new Comment());
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMethodAttribute() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        try {
            mv.visitAttribute(null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMethodSignature() {
        ClassVisitor cv = new CheckClassAdapter(new EmptyVisitor());
        cv.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
        try {
            cv.visitMethod(ACC_PUBLIC, "m", "()V", "<T::LI.J<*+LA;>;>()V^LA;X", null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMethodInsnVisitBeforeStart() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        try {
            mv.visitInsn(NOP);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFrameType() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFrame(123, 0, null, 0, null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFrameLocalCount() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFrame(F_SAME, 1, new Object[] { INTEGER }, 0, null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFrameStackCount() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFrame(F_SAME, 0, null, 1, new Object[] { INTEGER });
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFrameLocalArray() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFrame(F_APPEND, 1, new Object[0], 0, null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFrameStackArray() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFrame(F_SAME1, 0, null, 1, new Object[0]);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFrameValue() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFrame(F_FULL, 1, new Object[] { "LC;" }, 0, null);
            fail();
        } catch (Exception e) {
        }
        try {
            mv.visitFrame(F_FULL, 1, new Object[] { new Integer(0) }, 0, null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMethodInsn() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitInsn(-1);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalByteInsnOperand() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitIntInsn(BIPUSH, Integer.MAX_VALUE);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalShortInsnOperand() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitIntInsn(SIPUSH, Integer.MAX_VALUE);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalVarInsnOperand() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitVarInsn(ALOAD, -1);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalIntInsnOperand() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitIntInsn(NEWARRAY, 0);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalTypeInsnOperand() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitTypeInsn(NEW, "[I");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalLabelInsnOperand() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        Label l = new Label();
        mv.visitLabel(l);
        try {
            mv.visitLabel(l);
            fail();
        } catch (Exception e) {
        }
    }
    
    public void testIllegalDebugLabelUse() throws IOException {
        ClassReader cr = new ClassReader("java.lang.Object");
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassAdapter(cw) {
            public MethodVisitor visitMethod(
                int access,
                String name,
                String desc,
                String signature,
                String[] exceptions)
            {
                final MethodVisitor next = cv.visitMethod(access,
                        name,
                        desc,
                        signature,
                        exceptions);
                if (next == null) {
                    return next;
                }
                return new MethodAdapter(new CheckMethodAdapter(next)) {
                    private Label entryLabel = null;

                    public void visitLabel(Label label) {
                        if (entryLabel == null) {
                            entryLabel = label;
                        }
                        mv.visitLabel(label);
                    }

                    public void visitMaxs(int maxStack, int maxLocals) {
                        Label unwindhandler = new Label();
                        mv.visitLabel(unwindhandler);
                        mv.visitInsn(Opcodes.ATHROW); // rethrow
                        mv.visitTryCatchBlock(entryLabel,
                                unwindhandler,
                                unwindhandler,
                                null);
                        mv.visitMaxs(maxStack, maxLocals);
                    }
                };
            }
        };
        try {
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalTableSwitchParameters1() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitTableSwitchInsn(1, 0, new Label(), new Label[0]);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalTableSwitchParameters2() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitTableSwitchInsn(0, 1, null, new Label[0]);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalTableSwitchParameters3() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitTableSwitchInsn(0, 1, new Label(), null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalTableSwitchParameters4() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitTableSwitchInsn(0, 1, new Label(), new Label[0]);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalLookupSwitchParameters1() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitLookupSwitchInsn(new Label(), null, new Label[0]);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalLookupSwitchParameters2() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitLookupSwitchInsn(new Label(), new int[0], null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalLookupSwitchParameters3() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitLookupSwitchInsn(new Label(), new int[0], new Label[1]);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldInsnNullOwner() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFieldInsn(GETFIELD, null, "i", "I");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldInsnOwner() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFieldInsn(GETFIELD, "-", "i", "I");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldInsnNullName() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFieldInsn(GETFIELD, "C", null, "I");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldInsnName() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFieldInsn(GETFIELD, "C", "-", "I");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldInsnName2() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();

        try {
            mv.visitFieldInsn(GETFIELD, "C", "a-", "I");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldInsnNullDesc() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFieldInsn(GETFIELD, "C", "i", null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldInsnVoidDesc() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFieldInsn(GETFIELD, "C", "i", "V");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldInsnPrimitiveDesc() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFieldInsn(GETFIELD, "C", "i", "II");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldInsnArrayDesc() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFieldInsn(GETFIELD, "C", "i", "[");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldInsnReferenceDesc() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFieldInsn(GETFIELD, "C", "i", "L");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalFieldInsnReferenceDesc2() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitFieldInsn(GETFIELD, "C", "i", "L-;");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMethodInsnNullName() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitMethodInsn(INVOKEVIRTUAL, "C", null, "()V");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMethodInsnName() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitMethodInsn(INVOKEVIRTUAL, "C", "-", "()V");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMethodInsnName2() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitMethodInsn(INVOKEVIRTUAL, "C", "a-", "()V");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMethodInsnNullDesc() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitMethodInsn(INVOKEVIRTUAL, "C", "m", null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMethodInsnDesc() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitMethodInsn(INVOKEVIRTUAL, "C", "m", "I");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMethodInsnParameterDesc() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitMethodInsn(INVOKEVIRTUAL, "C", "m", "(V)V");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMethodInsnReturnDesc() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitMethodInsn(INVOKEVIRTUAL, "C", "m", "()VV");
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalLdcInsnOperand() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitLdcInsn(new Object());
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMultiANewArrayDesc() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitMultiANewArrayInsn("I", 1);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMultiANewArrayDims() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitMultiANewArrayInsn("[[I", 0);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalMultiANewArrayDims2() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitMultiANewArrayInsn("[[I", 3);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalTryCatchBlock() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        Label m = new Label();
        Label n = new Label();
        mv.visitLabel(m);
        try {
            mv.visitTryCatchBlock(m, n, n, null);
            fail();
        } catch (Exception e) {
        }        
        try {
            mv.visitTryCatchBlock(n, m, n, null);
            fail();
        } catch (Exception e) {
        }        
        try {
            mv.visitTryCatchBlock(n, n, m, null);
            fail();
        } catch (Exception e) {
        }        
    }
    
    public void testIllegalDataflow() {
        MethodVisitor mv = new CheckMethodAdapter(ACC_PUBLIC,
                "m",
                "(I)V",
                new EmptyVisitor(),
                new HashMap());
        mv.visitCode();
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 2);
        try {
            mv.visitEnd();
            fail();
        } catch (Exception e) {
        }
    }
    
    public void testIllegalDataflow2() {
        MethodVisitor mv = new CheckMethodAdapter(ACC_PUBLIC,
                "m",
                "(I)I",
                new EmptyVisitor(),
                new HashMap());
        mv.visitCode();
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 2);
        try {
            mv.visitEnd();
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalLocalVariableLabels() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        Label m = new Label();
        Label n = new Label();
        mv.visitLabel(n);
        mv.visitInsn(NOP);
        mv.visitLabel(m);
        try {
            mv.visitLocalVariable("i", "I", null, m, n, 0);
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalLineNumerLabel() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        try {
            mv.visitLineNumber(0, new Label());
            fail();
        } catch (Exception e) {
        }
    }

    public void testIllegalInsnVisitAfterEnd() {
        MethodVisitor mv = new CheckMethodAdapter(new EmptyVisitor());
        mv.visitCode();
        mv.visitMaxs(0, 0);
        try {
            mv.visitInsn(NOP);
            fail();
        } catch (Exception e) {
        }
    }
}
