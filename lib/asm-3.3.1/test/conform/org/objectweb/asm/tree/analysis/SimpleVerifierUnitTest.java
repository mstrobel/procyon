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
package org.objectweb.asm.tree.analysis;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import junit.framework.TestCase;

/**
 * SimpleVerifier unit tests.
 * 
 * @author Eric Bruneton
 */
public class SimpleVerifierUnitTest extends TestCase implements Opcodes {

    private Analyzer a;

    private MethodNode mn;

    protected void setUp() {
        Type c = Type.getType("LC;");
        Type d = Type.getType("Ljava/lang/Number;");
        a = new Analyzer(new SimpleVerifier(c, d, false));
        mn = new MethodNode(ACC_PUBLIC, "m", "()V", null, null);
    }

    private void assertValid() throws AnalyzerException {
        mn.visitInsn(RETURN);
        mn.visitMaxs(10, 10);
        a.analyze("C", mn);
        Frame[] frames = a.getFrames();
        for (int i = 0; i < frames.length; ++i) {
            if (frames[i] != null) {
                frames[i].toString();
            }
        }
        a.getHandlers(0);
    }

    private void assertInvalid() {
        mn.visitInsn(RETURN);
        mn.visitMaxs(10, 10);
        try {
            a.analyze("C", mn);
            fail();
        } catch (AnalyzerException e) {
        } catch (RuntimeException e) {
        }
    }

    public void testInvalidOpcode() {
        mn.visitInsn(-1);
        assertInvalid();
    }

    public void testInvalidPop() {
        mn.visitInsn(LCONST_0);
        mn.visitInsn(POP);
        assertInvalid();
    }

    public void testInvalidPop2() {
        mn.visitInsn(LCONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(POP2);
        assertInvalid();
    }

    public void testInvalidDup() {
        mn.visitInsn(LCONST_0);
        mn.visitInsn(DUP);
        assertInvalid();
    }

    public void testInvalidDupx1() {
        mn.visitInsn(LCONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(DUP_X1);
        assertInvalid();
    }

    public void testInvalidDupx2() {
        mn.visitInsn(LCONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(DUP_X2);
        assertInvalid();
    }

    public void testInvalidDup2() {
        mn.visitInsn(LCONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(DUP2);
        assertInvalid();
    }

    public void testInvalidDup2x1() {
        mn.visitInsn(LCONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(DUP2_X1);
        assertInvalid();
    }

    public void testInvalidDup2x2() {
        mn.visitInsn(LCONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(DUP2_X2);
        assertInvalid();
    }

    public void testInvalidSwap() {
        mn.visitInsn(LCONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(SWAP);
        assertInvalid();
    }

    public void testInvalidGetLocal() {
        mn.visitVarInsn(ALOAD, 10);
        assertInvalid();
    }

    public void testInvalidSetLocal() {
        mn.visitInsn(ACONST_NULL);
        mn.visitVarInsn(ASTORE, 10);
        assertInvalid();
    }

    public void testInvalidEmptyStack() {
        mn.visitInsn(POP);
        assertInvalid();
    }

    public void testInvalidFullStack() {
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        assertInvalid();
    }

    public void testInconsistentStackHeights() {
        Label l0 = new Label();
        mn.visitInsn(ICONST_0);
        mn.visitJumpInsn(IFEQ, l0);
        mn.visitInsn(ICONST_0);
        mn.visitLabel(l0);
        assertInvalid();
    }

    public void testInvalidNewArray() {
        mn.visitInsn(ICONST_1);
        mn.visitIntInsn(NEWARRAY, -1);
        assertInvalid();
    }

    public void testInvalidAload() {
        mn.visitInsn(ICONST_0);
        mn.visitVarInsn(ISTORE, 1);
        mn.visitVarInsn(ALOAD, 1);
        assertInvalid();
    }

    public void testInvalidAstore() {
        mn.visitInsn(ICONST_0);
        mn.visitVarInsn(ASTORE, 1);
        assertInvalid();
    }

    public void testInvalidIstore() {
        mn.visitInsn(ACONST_NULL);
        mn.visitVarInsn(ISTORE, 1);
        assertInvalid();
    }

    public void testInvalidCheckcast() {
        mn.visitInsn(ICONST_0);
        mn.visitTypeInsn(CHECKCAST, "java/lang/String");
        assertInvalid();
    }

    public void testInvalidArraylength() {
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ARRAYLENGTH);
        assertInvalid();
    }

    public void testInvalidAthrow() {
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ATHROW);
        assertInvalid();
    }

    public void testInvalidIneg() {
        mn.visitInsn(FCONST_0);
        mn.visitInsn(INEG);
        assertInvalid();
    }

    public void testInvalidIadd() {
        mn.visitInsn(FCONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(IADD);
        assertInvalid();
    }

    public void testInvalidIsub() {
        mn.visitInsn(ICONST_0);
        mn.visitInsn(FCONST_0);
        mn.visitInsn(ISUB);
        assertInvalid();
    }

    public void testInvalidIastore() {
        mn.visitInsn(ICONST_1);
        mn.visitIntInsn(NEWARRAY, T_INT);
        mn.visitInsn(FCONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(IASTORE);
        assertInvalid();
    }

    public void testInvalidFastore() {
        mn.visitInsn(ICONST_1);
        mn.visitIntInsn(NEWARRAY, T_FLOAT);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(FASTORE);
        assertInvalid();
    }

    public void testInvalidLastore() {
        mn.visitInsn(ICONST_1);
        mn.visitInsn(ICONST_0);
        mn.visitInsn(LCONST_0);
        mn.visitInsn(LASTORE);
        assertInvalid();
    }

    public void testInvalidMultianewarray() {
        mn.visitInsn(FCONST_1);
        mn.visitInsn(ICONST_2);
        mn.visitMultiANewArrayInsn("[[I", 2);
        assertInvalid();
    }

    public void testInvalidInvokevirtual() {
        mn.visitInsn(ACONST_NULL);
        mn.visitTypeInsn(CHECKCAST, "java/lang/Object");
        mn.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "size", "()I");
        assertInvalid();
    }

    public void testInvalidInvokeinterface() {
        mn.visitInsn(ACONST_NULL);
        mn.visitTypeInsn(CHECKCAST, "java/util/List");
        mn.visitInsn(FCONST_0);
        mn.visitMethodInsn(INVOKEINTERFACE,
                "java/util/List",
                "get",
                "(I)Ljava/lang/Object;");
        assertInvalid();
    }

    public void testInvalidRet() {
        mn.visitVarInsn(RET, 1);
        assertInvalid();
    }

    public void testInvalidFalloff() {
        mn.visitMaxs(10, 10);
        try {
            a.analyze("C", mn);
            fail();
        } catch (AnalyzerException e) {
        } catch (RuntimeException e) {
        }
    }

    public void testInvalidSubroutineFalloff() {
        Label l0 = new Label();
        Label l1 = new Label();
        mn.visitJumpInsn(GOTO, l0);
        mn.visitLabel(l1);
        mn.visitVarInsn(ASTORE, 1);
        mn.visitVarInsn(RET, 1);
        mn.visitLabel(l0);
        mn.visitJumpInsn(JSR, l1);
        mn.visitMaxs(10, 10);
        try {
            a.analyze("C", mn);
            fail();
        } catch (AnalyzerException e) {
        } catch (RuntimeException e) {
        }
    }

    public void testNestedSubroutines() throws AnalyzerException {
        Label l0 = new Label();
        Label l1 = new Label();
        mn.visitJumpInsn(JSR, l0);
        mn.visitInsn(RETURN);
        mn.visitLabel(l0);
        mn.visitVarInsn(ASTORE, 1);
        mn.visitJumpInsn(JSR, l1);
        mn.visitJumpInsn(JSR, l1);
        mn.visitVarInsn(RET, 1);
        mn.visitLabel(l1);
        mn.visitVarInsn(ASTORE, 2);
        mn.visitVarInsn(RET, 2);
        assertValid();
    }

    public void testSubroutineLocalsAccess() throws AnalyzerException {
        MethodVisitor mv = mn;
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        Label l3 = new Label();
        mv.visitTryCatchBlock(l0, l0, l1, null);
        mv.visitTryCatchBlock(l0, l2, l2, "java/lang/RuntimeException");
        mv.visitLabel(l0);
        mv.visitJumpInsn(JSR, l3);
        mv.visitInsn(RETURN);
        mv.visitLabel(l1);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitJumpInsn(JSR, l3);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l3);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitVarInsn(RET, 2);
        mv.visitLabel(l2);
        mv.visitVarInsn(ASTORE, 4);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitInsn(ATHROW);
        assertValid();
    }

    public void _testOverlappingSubroutines() {
        // TODO currently Analyzer can not detect this situation. The problem
        // is that other overlapping subroutine situations are valid, such as
        // when a nested subroutine implicitely returns to its parent
        // subroutine, without a RET.
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mn.visitJumpInsn(JSR, l0);
        mn.visitJumpInsn(JSR, l1);
        mn.visitInsn(RETURN);
        mn.visitLabel(l0);
        mn.visitVarInsn(ASTORE, 1);
        mn.visitJumpInsn(GOTO, l2);
        mn.visitLabel(l1);
        mn.visitVarInsn(ASTORE, 1);
        mn.visitLabel(l2);
        mn.visitVarInsn(RET, 1);
        assertInvalid();
    }

    public void testMerge() throws AnalyzerException {
        Label l0 = new Label();
        mn.visitVarInsn(ALOAD, 0);
        mn.visitVarInsn(ASTORE, 1);
        mn.visitInsn(ACONST_NULL);
        mn.visitTypeInsn(CHECKCAST, "java/lang/Number");
        mn.visitVarInsn(ASTORE, 2);
        mn.visitVarInsn(ALOAD, 0);
        mn.visitVarInsn(ASTORE, 3);
        mn.visitLabel(l0);
        mn.visitInsn(ACONST_NULL);
        mn.visitTypeInsn(CHECKCAST, "java/lang/Number");
        mn.visitVarInsn(ASTORE, 1);
        mn.visitVarInsn(ALOAD, 0);
        mn.visitVarInsn(ASTORE, 2);
        mn.visitInsn(ACONST_NULL);
        mn.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mn.visitVarInsn(ASTORE, 3);
        mn.visitJumpInsn(GOTO, l0);
        assertValid();
    }

    public void testClassNotFound() {
        Label l0 = new Label();
        mn.visitVarInsn(ALOAD, 0);
        mn.visitVarInsn(ASTORE, 1);
        mn.visitLabel(l0);
        mn.visitInsn(ACONST_NULL);
        mn.visitTypeInsn(CHECKCAST, "D");
        mn.visitVarInsn(ASTORE, 1);
        mn.visitJumpInsn(GOTO, l0);
        mn.visitMaxs(10, 10);
        try {
            a.analyze("C", mn);
            fail();
        } catch (Exception e) {
        }
    }
}
