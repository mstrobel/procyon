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

import junit.framework.TestCase;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceMethodVisitor;

/**
 * JsrInlinerTest
 * 
 * @author Eugene Kuleshov, Niko Matsakis, Eric Bruneton
 */
public class JSRInlinerAdapterUnitTest extends TestCase {

    private JSRInlinerAdapter jsr;
    private MethodNode exp;
    private MethodVisitor current;

    protected void setUp() throws Exception {
        super.setUp();
        jsr = new JSRInlinerAdapter(null, 0, "m", "()V", null, null) {
            public void visitEnd() {
                System.err.println("started w/ method:" + name);
                TraceMethodVisitor mv = new TraceMethodVisitor();
                for (int i = 0; i < instructions.size(); ++i) {
                    instructions.get(i).accept(mv);
                    System.err.print(Integer.toString(i + 100000).substring(1));
                    System.err.print(" : " + mv.text.get(i));
                }
                super.visitEnd();
                System.err.println("finished w/ method:" + name);
            }
        };
        exp = new MethodNode(0, "m", "()V", null, null);
    }

    private void setCurrent(final MethodVisitor cv) {
        this.current = cv;
    }

    private void ICONST_0() {
        this.current.visitInsn(Opcodes.ICONST_0);
    }

    private void ISTORE(final int var) {
        this.current.visitVarInsn(Opcodes.ISTORE, var);
    }

    private void ALOAD(final int var) {
        this.current.visitVarInsn(Opcodes.ALOAD, var);
    }

    private void ILOAD(final int var) {
        this.current.visitVarInsn(Opcodes.ILOAD, var);
    }

    private void ASTORE(final int var) {
        this.current.visitVarInsn(Opcodes.ASTORE, var);
    }

    private void RET(final int var) {
        this.current.visitVarInsn(Opcodes.RET, var);
    }

    private void ATHROW() {
        this.current.visitInsn(Opcodes.ATHROW);
    }

    private void ACONST_NULL() {
        this.current.visitInsn(Opcodes.ACONST_NULL);
    }

    private void RETURN() {
        this.current.visitInsn(Opcodes.RETURN);
    }

    private void LABEL(final Label l) {
        this.current.visitLabel(l);
    }

    private void IINC(final int var, final int amnt) {
        this.current.visitIincInsn(var, amnt);
    }

    private void GOTO(final Label l) {
        this.current.visitJumpInsn(Opcodes.GOTO, l);
    }

    private void JSR(final Label l) {
        this.current.visitJumpInsn(Opcodes.JSR, l);
    }

    private void IFNONNULL(final Label l) {
        this.current.visitJumpInsn(Opcodes.IFNONNULL, l);
    }

    private void IFNE(final Label l) {
        this.current.visitJumpInsn(Opcodes.IFNE, l);
    }

    private void TRYCATCH(
        final Label start,
        final Label end,
        final Label handler)
    {
        this.current.visitTryCatchBlock(start, end, handler, null);
    }

    private void LINE(final int line, final Label start) {
        this.current.visitLineNumber(line, start);
    }

    private void LOCALVAR(
        final String name,
        final String desc,
        final int index,
        final Label start,
        final Label end)
    {
        this.current.visitLocalVariable(name, desc, null, start, end, index);
    }

    private void END(final int maxStack, final int maxLocals) {
        this.current.visitMaxs(maxStack, maxLocals);
        this.current.visitEnd();
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_1,
                Opcodes.ACC_PUBLIC,
                "C",
                null,
                "java/lang/Object",
                null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        ((MethodNode) this.current).accept(cw);
        cw.visitEnd();
        byte[] b = cw.toByteArray();
        try {
            TestClassLoader loader = new TestClassLoader();
            Class c = loader.defineClass("C", b);
            c.newInstance();
        } catch (Throwable t) {
            fail(t.getMessage());
        }
        this.current = null;
    }

    static class TestClassLoader extends ClassLoader {

        public Class defineClass(final String name, final byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    /**
     * Tests a method which has the most basic <code>try{}finally</code> form
     * imaginable:
     * 
     * <pre>
     *   public void a() {
     *     int a = 0;
     *     try {
     *       a++;
     *     } finally {
     *       a--;
     *     }
     *   }
     * </pre>
     */
    public void testBasic() {
        {
            Label L0 = new Label();
            Label L1 = new Label();
            Label L2 = new Label();
            Label L3 = new Label();
            Label L4 = new Label();

            setCurrent(jsr);
            ICONST_0();
            ISTORE(1);

            /* L0: body of try block */
            LABEL(L0);
            IINC(1, 1);
            GOTO(L1);

            /* L2: exception handler */
            LABEL(L2);
            ASTORE(3);
            JSR(L3);
            ALOAD(3);
            ATHROW();

            /* L3: subroutine */
            LABEL(L3);
            ASTORE(2);
            IINC(1, -1);
            RET(2);

            /* L1: non-exceptional exit from try block */
            LABEL(L1);
            JSR(L3);
            LABEL(L4); // L4
            RETURN();

            TRYCATCH(L0, L2, L2);
            TRYCATCH(L1, L4, L2);

            END(1, 4);
        }

        {
            Label L0 = new Label();
            Label L1 = new Label();
            Label L2 = new Label();
            Label L3_1a = new Label();
            Label L3_1b = new Label();
            Label L3_2a = new Label();
            Label L3_2b = new Label();
            Label L4 = new Label();

            setCurrent(exp);
            ICONST_0();
            ISTORE(1);
            // L0: try/catch block
            LABEL(L0);
            IINC(1, 1);
            GOTO(L1);

            // L2: Exception handler:
            LABEL(L2);
            ASTORE(3);
            ACONST_NULL();
            GOTO(L3_1a);
            LABEL(L3_1b); // L3_1b;
            ALOAD(3);
            ATHROW();

            // L1: On non-exceptional exit, try block leads here:
            LABEL(L1);
            ACONST_NULL();
            GOTO(L3_2a);
            LABEL(L3_2b); // L3_2b
            LABEL(L4); // L4
            RETURN();

            // L3_1a: First instantiation of subroutine:
            LABEL(L3_1a);
            ASTORE(2);
            IINC(1, -1);
            GOTO(L3_1b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            // L3_2a: Second instantiation of subroutine:
            LABEL(L3_2a);
            ASTORE(2);
            IINC(1, -1);
            GOTO(L3_2b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            TRYCATCH(L0, L2, L2);
            TRYCATCH(L1, L4, L2);

            END(1, 4);
        }

        assertEquals(exp, jsr);
    }

    /**
     * Tests a method which has an if/else-if w/in the finally clause:
     * 
     * <pre>
     *   public void a() {
     *     int a = 0;
     *     try {
     *       a++;
     *     } finally {
     *       if (a == 0)
     *         a+=2;
     *       else
     *         a+=3;
     *     }
     *   }
     * </pre>
     */
    public void testIfElseInFinally() {
        {
            Label L0 = new Label();
            Label L1 = new Label();
            Label L2 = new Label();
            Label L3 = new Label();
            Label L4 = new Label();
            Label L5 = new Label();
            Label L6 = new Label();

            setCurrent(jsr);
            ICONST_0();
            ISTORE(1);

            /* L0: body of try block */
            LABEL(L0);
            IINC(1, 1);
            GOTO(L1);

            /* L2: exception handler */
            LABEL(L2);
            ASTORE(3);
            JSR(L3);
            ALOAD(3);
            ATHROW();

            /* L3: subroutine */
            LABEL(L3);
            ASTORE(2);
            ILOAD(1);
            IFNE(L4);
            IINC(1, 2);
            GOTO(L5);
            LABEL(L4); // L4: a != 0
            IINC(1, 3);
            LABEL(L5); // L5: common exit
            RET(2);

            /* L1: non-exceptional exit from try block */
            LABEL(L1);
            JSR(L3);
            LABEL(L6); // L6 is used in the TRYCATCH below
            RETURN();

            TRYCATCH(L0, L2, L2);
            TRYCATCH(L1, L6, L2);

            END(1, 4);
        }

        {
            Label L0 = new Label();
            Label L1 = new Label();
            Label L2 = new Label();
            Label L3_1a = new Label();
            Label L3_1b = new Label();
            Label L3_2a = new Label();
            Label L3_2b = new Label();
            Label L4_1 = new Label();
            Label L4_2 = new Label();
            Label L5_1 = new Label();
            Label L5_2 = new Label();
            Label L6 = new Label();

            setCurrent(exp);
            ICONST_0();
            ISTORE(1);
            // L0: try/catch block
            LABEL(L0);
            IINC(1, 1);
            GOTO(L1);

            // L2: Exception handler:
            LABEL(L2);
            ASTORE(3);
            ACONST_NULL();
            GOTO(L3_1a);
            LABEL(L3_1b); // L3_1b;
            ALOAD(3);
            ATHROW();

            // L1: On non-exceptional exit, try block leads here:
            LABEL(L1);
            ACONST_NULL();
            GOTO(L3_2a);
            LABEL(L3_2b); // L3_2b
            LABEL(L6); // L6
            RETURN();

            // L3_1a: First instantiation of subroutine:
            LABEL(L3_1a);
            ASTORE(2);
            ILOAD(1);
            IFNE(L4_1);
            IINC(1, 2);
            GOTO(L5_1);
            LABEL(L4_1); // L4_1: a != 0
            IINC(1, 3);
            LABEL(L5_1); // L5_1: common exit
            GOTO(L3_1b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            // L3_2a: First instantiation of subroutine:
            LABEL(L3_2a);
            ASTORE(2);
            ILOAD(1);
            IFNE(L4_2);
            IINC(1, 2);
            GOTO(L5_2);
            LABEL(L4_2); // L4_2: a != 0
            IINC(1, 3);
            LABEL(L5_2); // L5_2: common exit
            GOTO(L3_2b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            TRYCATCH(L0, L2, L2);
            TRYCATCH(L1, L6, L2);

            END(1, 4);
        }

        assertEquals(exp, jsr);
    }

    /**
     * Tests a simple nested finally:
     * 
     * <pre>
     * public void a1() {
     *   int a = 0;
     *   try {
     *     a += 1;
     *   } finally {
     *     try {
     *       a += 2;
     *     } finally {
     *       a += 3;
     *     }
     *   }
     * }
     * </pre>
     */
    public void testSimpleNestedFinally() {
        {
            Label L0 = new Label();
            Label L1 = new Label();
            Label L2 = new Label();
            Label L3 = new Label();
            Label L4 = new Label();
            Label L5 = new Label();

            setCurrent(jsr);

            ICONST_0();
            ISTORE(1);

            // L0: Body of try block:
            LABEL(L0);
            IINC(1, 1);
            JSR(L3);
            GOTO(L1);

            // L2: First exception handler:
            LABEL(L2);
            JSR(L3);
            ATHROW();

            // L3: First subroutine:
            LABEL(L3);
            ASTORE(2);
            IINC(1, 2);
            JSR(L4);
            RET(2);

            // L5: Second exception handler:
            LABEL(L5);
            JSR(L4);
            ATHROW();

            // L4: Second subroutine:
            LABEL(L4);
            ASTORE(3);
            IINC(1, 3);
            RET(3);

            // L1: On normal exit, try block jumps here:
            LABEL(L1);
            RETURN();

            TRYCATCH(L0, L2, L2);
            TRYCATCH(L3, L5, L5);

            END(2, 6);
        }

        {
            Label L0 = new Label();
            Label L1 = new Label();
            Label L2 = new Label();
            Label L3_1a = new Label();
            Label L3_1b = new Label();
            Label L3_2a = new Label();
            Label L3_2b = new Label();
            Label L4_1a = new Label();
            Label L4_1b = new Label();
            Label L4_2a = new Label();
            Label L4_2b = new Label();
            Label L4_3a = new Label();
            Label L4_3b = new Label();
            Label L4_4a = new Label();
            Label L4_4b = new Label();
            Label L5_1 = new Label();
            Label L5_2 = new Label();

            setCurrent(exp);

            ICONST_0();
            ISTORE(1);

            // L0: Body of try block:
            LABEL(L0);
            IINC(1, 1);
            ACONST_NULL();
            GOTO(L3_1a);
            LABEL(L3_1b); // L3_1b
            GOTO(L1);

            // L2: First exception handler:
            LABEL(L2);
            ACONST_NULL();
            GOTO(L3_2a);
            LABEL(L3_2b); // L3_2b
            ATHROW();

            // L1: On normal exit, try block jumps here:
            LABEL(L1);
            RETURN();

            // L3_1a: First instantiation of first subroutine:
            LABEL(L3_1a);
            ASTORE(2);
            IINC(1, 2);
            ACONST_NULL();
            GOTO(L4_1a);
            LABEL(L4_1b); // L4_1b
            GOTO(L3_1b);
            LABEL(L5_1); // L5_1
            ACONST_NULL();
            GOTO(L4_2a);
            LABEL(L4_2b); // L4_2b
            ATHROW();
            LABEL(new Label()); // extra label emitted due to impl quirks

            // L3_2a: Second instantiation of first subroutine:
            LABEL(L3_2a);
            ASTORE(2);
            IINC(1, 2);
            ACONST_NULL();
            GOTO(L4_3a);
            LABEL(L4_3b); // L4_3b
            GOTO(L3_2b);
            LABEL(L5_2); // L5_2
            ACONST_NULL();
            GOTO(L4_4a);
            LABEL(L4_4b); // L4_4b
            ATHROW();
            LABEL(new Label()); // extra label emitted due to impl quirks

            // L4_1a: First instantiation of second subroutine:
            LABEL(L4_1a);
            ASTORE(3);
            IINC(1, 3);
            GOTO(L4_1b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            // L4_2a: Second instantiation of second subroutine:
            LABEL(L4_2a);
            ASTORE(3);
            IINC(1, 3);
            GOTO(L4_2b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            // L4_3a: Third instantiation of second subroutine:
            LABEL(L4_3a);
            ASTORE(3);
            IINC(1, 3);
            GOTO(L4_3b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            // L4_4a: Fourth instantiation of second subroutine:
            LABEL(L4_4a);
            ASTORE(3);
            IINC(1, 3);
            GOTO(L4_4b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            TRYCATCH(L0, L2, L2);
            TRYCATCH(L3_1a, L5_1, L5_1);
            TRYCATCH(L3_2a, L5_2, L5_2);

            END(2, 6);
        }

        assertEquals(exp, jsr);
    }

    /**
     * This tests a subroutine which has no ret statement, but ends in a
     * "return" instead.
     * 
     * We structure this as a try/finally with a break in the finally. Because
     * the while loop is infinite, it's clear from the byte code that the only
     * path which reaches the RETURN instruction is through the subroutine.
     * 
     * <pre>
     * public void a1() {
     *   int a = 0;
     *   while (true) {
     *     try {
     *       a += 1;
     *     } finally {
     *       a += 2;
     *       break;
     *     }
     *   }
     * }
     * </pre>
     */
    public void testSubroutineWithNoRet() {
        {
            Label L0 = new Label();
            Label L1 = new Label();
            Label L2 = new Label();
            Label L3 = new Label();
            Label L4 = new Label();

            setCurrent(jsr);
            ICONST_0();
            ISTORE(1);

            // L0: while loop header/try block
            LABEL(L0);
            IINC(1, 1);
            JSR(L1);
            GOTO(L2);

            // L3: implicit catch block
            LABEL(L3);
            ASTORE(2);
            JSR(L1);
            ALOAD(2);
            ATHROW();

            // L1: subroutine ...
            LABEL(L1);
            ASTORE(3);
            IINC(1, 2);
            GOTO(L4); // ...not that it does not return!

            // L2: end of the loop... goes back to the top!
            LABEL(L2);
            GOTO(L0);

            // L4:
            LABEL(L4);
            RETURN();

            TRYCATCH(L0, L3, L3);

            END(1, 4);
        }

        {
            Label L0 = new Label();
            Label L1_1a = new Label();
            Label L1_1b = new Label();
            Label L1_2a = new Label();
            Label L1_2b = new Label();
            Label L2 = new Label();
            Label L3 = new Label();
            Label L4_1 = new Label();
            Label L4_2 = new Label();

            setCurrent(exp);
            ICONST_0();
            ISTORE(1);

            // L0: while loop header/try block
            LABEL(L0);
            IINC(1, 1);
            ACONST_NULL();
            GOTO(L1_1a);
            LABEL(L1_1b); // L1_1b
            GOTO(L2);

            // L3: implicit catch block
            LABEL(L3);
            ASTORE(2);
            ACONST_NULL();
            GOTO(L1_2a);
            LABEL(L1_2b); // L1_2b
            ALOAD(2);
            ATHROW();

            // L2: end of the loop... goes back to the top!
            LABEL(L2);
            GOTO(L0);
            LABEL(new Label()); // extra label emitted due to impl quirks

            // L1_1a: first instantiation of subroutine ...
            LABEL(L1_1a);
            ASTORE(3);
            IINC(1, 2);
            GOTO(L4_1); // ...not that it does not return!
            LABEL(L4_1);
            RETURN();

            // L1_2a: second instantiation of subroutine ...
            LABEL(L1_2a);
            ASTORE(3);
            IINC(1, 2);
            GOTO(L4_2); // ...not that it does not return!
            LABEL(L4_2);
            RETURN();

            TRYCATCH(L0, L3, L3);

            END(1, 4);
        }

        assertEquals(exp, jsr);
    }

    /**
     * This tests a subroutine which has no ret statement, but ends in a
     * "return" instead.
     * 
     * <pre>
     *   JSR L0
     * L0:
     *   ASTORE 0 
     *   RETURN 
     * </pre>
     */
    public void testSubroutineWithNoRet2() {
        {
            Label L0 = new Label();

            setCurrent(jsr);
            JSR(L0);
            LABEL(L0);
            ASTORE(0);
            RETURN();
            END(1, 1);
        }

        {
            Label L0_1a = new Label();
            Label L0_1b = new Label();

            setCurrent(exp);

            ACONST_NULL();
            GOTO(L0_1a);
            LABEL(L0_1b);

            // L0_1a: First instantiation of subroutine:
            LABEL(L0_1a);
            ASTORE(0);
            RETURN();
            LABEL(new Label()); // extra label emitted due to impl quirks

            END(1, 1);
        }

        assertEquals(exp, jsr);
    }

    /**
     * This tests a subroutine which has no ret statement, but instead exits
     * implicitely by branching to code which is not part of the subroutine.
     * (Sadly, this is legal)
     * 
     * We structure this as a try/finally in a loop with a break in the finally.
     * The loop is not trivially infinite, so the RETURN statement is reachable
     * both from the JSR subroutine and from the main entry point.
     * 
     * <pre>
     * public void a1() {
     *   int a = 0;
     *   while (null == null) {
     *     try {
     *       a += 1;
     *     } finally {
     *       a += 2;
     *       break;
     *     }
     *   }
     * }
     * </pre>
     */
    public void testImplicitExit() {
        {
            Label L0 = new Label();
            Label L1 = new Label();
            Label L2 = new Label();
            Label L3 = new Label();
            Label L4 = new Label();
            Label L5 = new Label();

            setCurrent(jsr);
            ICONST_0();
            ISTORE(1);

            // L5: while loop header
            LABEL(L5);
            ACONST_NULL();
            IFNONNULL(L4);

            // L0: try block
            LABEL(L0);
            IINC(1, 1);
            JSR(L1);
            GOTO(L2);

            // L3: implicit catch block
            LABEL(L3);
            ASTORE(2);
            JSR(L1);
            ALOAD(2);
            ATHROW();

            // L1: subroutine ...
            LABEL(L1);
            ASTORE(3);
            IINC(1, 2);
            GOTO(L4); // ...not that it does not return!

            // L2: end of the loop... goes back to the top!
            LABEL(L2);
            GOTO(L0);

            // L4:
            LABEL(L4);
            RETURN();

            TRYCATCH(L0, L3, L3);

            END(1, 4);
        }

        {
            Label L0 = new Label();
            Label L1_1a = new Label();
            Label L1_1b = new Label();
            Label L1_2a = new Label();
            Label L1_2b = new Label();
            Label L2 = new Label();
            Label L3 = new Label();
            Label L4 = new Label();
            Label L5 = new Label();

            setCurrent(exp);
            ICONST_0();
            ISTORE(1);

            // L5: while loop header
            LABEL(L5);
            ACONST_NULL();
            IFNONNULL(L4);

            // L0: while loop header/try block
            LABEL(L0);
            IINC(1, 1);
            ACONST_NULL();
            GOTO(L1_1a);
            LABEL(L1_1b); // L1_1b
            GOTO(L2);

            // L3: implicit catch block
            LABEL(L3);
            ASTORE(2);
            ACONST_NULL();
            GOTO(L1_2a);
            LABEL(L1_2b); // L1_2b
            ALOAD(2);
            ATHROW();

            // L2: end of the loop... goes back to the top!
            LABEL(L2);
            GOTO(L0);

            // L4: exit, not part of subroutine
            // Note that the two subroutine instantiations branch here
            LABEL(L4);
            RETURN();

            // L1_1a: first instantiation of subroutine ...
            LABEL(L1_1a);
            ASTORE(3);
            IINC(1, 2);
            GOTO(L4); // ...note that it does not return!
            LABEL(new Label()); // extra label emitted due to impl quirks

            // L1_2a: second instantiation of subroutine ...
            LABEL(L1_2a);
            ASTORE(3);
            IINC(1, 2);
            GOTO(L4); // ...note that it does not return!
            LABEL(new Label()); // extra label emitted due to impl quirks

            TRYCATCH(L0, L3, L3);

            END(1, 4);
        }

        assertEquals(exp, jsr);
    }

    /**
     * Tests a nested try/finally with implicit exit from one subroutine to the
     * other subroutine. Equivalent to the following java code:
     * 
     * <pre>
     * void m(boolean b) {
     *   try {
     *     return;
     *   } finally {
     *     while (b) {
     *       try {
     *         return;
     *       } finally {
     *         // NOTE --- this break avoids the second return above (weird)
     *         if (b) break;
     *       }
     *     }
     *   }
     * }
     * </pre>
     * 
     * This example is from the paper, "Subroutine Inlining and Bytecode
     * Abstraction to Simplify Static and Dynamic Analysis" by Cyrille Artho and
     * Armin Biere.
     */
    public void testImplicitExitToAnotherSubroutine() {
        {
            Label T1 = new Label();
            Label C1 = new Label();
            Label S1 = new Label();
            Label L = new Label();
            Label C2 = new Label();
            Label S2 = new Label();
            Label W = new Label();
            Label X = new Label();

            // variable numbers:
            int b = 1;
            int e1 = 2;
            int e2 = 3;
            int r1 = 4;
            int r2 = 5;

            setCurrent(jsr);

            ICONST_0();
            ISTORE(1);

            // T1: first try:
            LABEL(T1);
            JSR(S1);
            RETURN();

            // C1: exception handler for first try
            LABEL(C1);
            ASTORE(e1);
            JSR(S1);
            ALOAD(e1);
            ATHROW();

            // S1: first finally handler
            LABEL(S1);
            ASTORE(r1);
            GOTO(W);

            // L: body of while loop, also second try
            LABEL(L);
            JSR(S2);
            RETURN();

            // C2: exception handler for second try
            LABEL(C2);
            ASTORE(e2);
            JSR(S2);
            ALOAD(e2);
            ATHROW();

            // S2: second finally handler
            LABEL(S2);
            ASTORE(r2);
            ILOAD(b);
            IFNE(X);
            RET(r2);

            // W: test for the while loop
            LABEL(W);
            ILOAD(b);
            IFNE(L); // falls through to X

            // X: exit from finally{} block
            LABEL(X);
            RET(r1);

            TRYCATCH(T1, C1, C1);
            TRYCATCH(L, C2, C2);

            END(1, 6);
        }

        {
            Label T1 = new Label();
            Label C1 = new Label();
            Label S1_1a = new Label();
            Label S1_1b = new Label();
            Label S1_2a = new Label();
            Label S1_2b = new Label();
            Label L_1 = new Label();
            Label L_2 = new Label();
            Label C2_1 = new Label();
            Label C2_2 = new Label();
            Label S2_1_1a = new Label();
            Label S2_1_1b = new Label();
            Label S2_1_2a = new Label();
            Label S2_1_2b = new Label();
            Label S2_2_1a = new Label();
            Label S2_2_1b = new Label();
            Label S2_2_2a = new Label();
            Label S2_2_2b = new Label();
            Label W_1 = new Label();
            Label W_2 = new Label();
            Label X_1 = new Label();
            Label X_2 = new Label();

            // variable numbers:
            int b = 1;
            int e1 = 2;
            int e2 = 3;
            int r1 = 4;
            int r2 = 5;

            setCurrent(exp);

            // --- Main Subroutine ---

            ICONST_0();
            ISTORE(1);

            // T1: first try:
            LABEL(T1);
            ACONST_NULL();
            GOTO(S1_1a);
            LABEL(S1_1b);
            RETURN();

            // C1: exception handler for first try
            LABEL(C1);
            ASTORE(e1);
            ACONST_NULL();
            GOTO(S1_2a);
            LABEL(S1_2b);
            ALOAD(e1);
            ATHROW();
            LABEL(new Label()); // extra label emitted due to impl quirks

            // --- First instantiation of first subroutine ---

            // S1: first finally handler
            LABEL(S1_1a);
            ASTORE(r1);
            GOTO(W_1);

            // L_1: body of while loop, also second try
            LABEL(L_1);
            ACONST_NULL();
            GOTO(S2_1_1a);
            LABEL(S2_1_1b);
            RETURN();

            // C2_1: exception handler for second try
            LABEL(C2_1);
            ASTORE(e2);
            ACONST_NULL();
            GOTO(S2_1_2a);
            LABEL(S2_1_2b);
            ALOAD(e2);
            ATHROW();

            // W_1: test for the while loop
            LABEL(W_1);
            ILOAD(b);
            IFNE(L_1); // falls through to X_1

            // X_1: exit from finally{} block
            LABEL(X_1);
            GOTO(S1_1b);

            // --- Second instantiation of first subroutine ---

            // S1: first finally handler
            LABEL(S1_2a);
            ASTORE(r1);
            GOTO(W_2);

            // L_2: body of while loop, also second try
            LABEL(L_2);
            ACONST_NULL();
            GOTO(S2_2_1a);
            LABEL(S2_2_1b);
            RETURN();

            // C2_2: exception handler for second try
            LABEL(C2_2);
            ASTORE(e2);
            ACONST_NULL();
            GOTO(S2_2_2a);
            LABEL(S2_2_2b);
            ALOAD(e2);
            ATHROW();

            // W_2: test for the while loop
            LABEL(W_2);
            ILOAD(b);
            IFNE(L_2); // falls through to X_2

            // X_2: exit from finally{} block
            LABEL(X_2);
            GOTO(S1_2b);

            // --- Second subroutine's 4 instantiations ---

            // S2_1_1a:
            LABEL(S2_1_1a);
            ASTORE(r2);
            ILOAD(b);
            IFNE(X_1);
            GOTO(S2_1_1b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            // S2_1_2a:
            LABEL(S2_1_2a);
            ASTORE(r2);
            ILOAD(b);
            IFNE(X_1);
            GOTO(S2_1_2b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            // S2_2_1a:
            LABEL(S2_2_1a);
            ASTORE(r2);
            ILOAD(b);
            IFNE(X_2);
            GOTO(S2_2_1b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            // S2_2_2a:
            LABEL(S2_2_2a);
            ASTORE(r2);
            ILOAD(b);
            IFNE(X_2);
            GOTO(S2_2_2b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            TRYCATCH(T1, C1, C1);
            TRYCATCH(L_1, C2_1, C2_1); // duplicated try/finally for each...
            TRYCATCH(L_2, C2_2, C2_2); // ...instantiation of first sub

            END(1, 6);
        }

        assertEquals(exp, jsr);
    }

    /**
     * This tests two subroutines, neither of which exit. Instead, they both
     * branch to a common set of code which returns from the method. This code
     * is not reachable except through these subroutines, and since they do not
     * invoke each other, it must be copied into both of them.
     * 
     * I don't believe this can be represented in Java.
     */
    public void testCommonCodeWhichMustBeDuplicated() {
        {
            Label L1 = new Label();
            Label L2 = new Label();
            Label L3 = new Label();

            setCurrent(jsr);
            ICONST_0();
            ISTORE(1);

            // Invoke the two subroutines, each twice:
            JSR(L1);
            JSR(L1);
            JSR(L2);
            JSR(L2);
            RETURN();

            // L1: subroutine 1
            LABEL(L1);
            IINC(1, 1);
            GOTO(L3); // ...note that it does not return!

            // L2: subroutine 2
            LABEL(L2);
            IINC(1, 2);
            GOTO(L3); // ...note that it does not return!

            // L3: common code to both subroutines: exit method
            LABEL(L3);
            RETURN();

            END(1, 2);
        }

        {
            Label L1_1a = new Label();
            Label L1_1b = new Label();
            Label L1_2a = new Label();
            Label L1_2b = new Label();
            Label L2_1a = new Label();
            Label L2_1b = new Label();
            Label L2_2a = new Label();
            Label L2_2b = new Label();
            Label L3_1 = new Label();
            Label L3_2 = new Label();
            Label L3_3 = new Label();
            Label L3_4 = new Label();

            setCurrent(exp);
            ICONST_0();
            ISTORE(1);

            // Invoke the two subroutines, each twice:
            ACONST_NULL();
            GOTO(L1_1a);
            LABEL(L1_1b);
            ACONST_NULL();
            GOTO(L1_2a);
            LABEL(L1_2b);
            ACONST_NULL();
            GOTO(L2_1a);
            LABEL(L2_1b);
            ACONST_NULL();
            GOTO(L2_2a);
            LABEL(L2_2b);
            RETURN();
            LABEL(new Label()); // extra label emitted due to impl quirks

            // L1_1a: instantiation 1 of subroutine 1
            LABEL(L1_1a);
            IINC(1, 1);
            GOTO(L3_1); // ...note that it does not return!
            LABEL(L3_1);
            RETURN();

            // L1_2a: instantiation 2 of subroutine 1
            LABEL(L1_2a);
            IINC(1, 1);
            GOTO(L3_2); // ...note that it does not return!
            LABEL(L3_2);
            RETURN();

            // L2_1a: instantiation 1 of subroutine 2
            LABEL(L2_1a);
            IINC(1, 2);
            GOTO(L3_3); // ...note that it does not return!
            LABEL(L3_3);
            RETURN();

            // L2_2a: instantiation 2 of subroutine 2
            LABEL(L2_2a);
            IINC(1, 2);
            GOTO(L3_4); // ...note that it does not return!
            LABEL(L3_4);
            RETURN();

            END(1, 2);
        }

        assertEquals(exp, jsr);
    }

    /**
     * This tests a simple subroutine where the control flow jumps back and
     * forth between the subroutine and the caller.
     * 
     * This would not normally be produced by a java compiler.
     */
    public void testInterleavedCode() {
        {
            Label L1 = new Label();
            Label L2 = new Label();
            Label L3 = new Label();
            Label L4 = new Label();

            setCurrent(jsr);
            ICONST_0();
            ISTORE(1);

            // Invoke the subroutine, each twice:
            JSR(L1);
            GOTO(L2);

            // L1: subroutine 1
            LABEL(L1);
            ASTORE(2);
            IINC(1, 1);
            GOTO(L3);

            // L2: second part of main subroutine
            LABEL(L2);
            IINC(1, 2);
            GOTO(L4);

            // L3: second part of subroutine 1
            LABEL(L3);
            IINC(1, 4);
            RET(2);

            // L4: third part of main subroutine
            LABEL(L4);
            JSR(L1);
            RETURN();

            END(1, 3);
        }

        {
            Label L1_1a = new Label();
            Label L1_1b = new Label();
            Label L1_2a = new Label();
            Label L1_2b = new Label();
            Label L2 = new Label();
            Label L3_1 = new Label();
            Label L3_2 = new Label();
            Label L4 = new Label();

            setCurrent(exp);

            // Main routine:
            ICONST_0();
            ISTORE(1);
            ACONST_NULL();
            GOTO(L1_1a);
            LABEL(L1_1b);
            GOTO(L2);
            LABEL(L2);
            IINC(1, 2);
            GOTO(L4);
            LABEL(L4);
            ACONST_NULL();
            GOTO(L1_2a);
            LABEL(L1_2b);
            RETURN();

            // L1_1: instantiation #1
            LABEL(L1_1a);
            ASTORE(2);
            IINC(1, 1);
            GOTO(L3_1);
            LABEL(L3_1);
            IINC(1, 4);
            GOTO(L1_1b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            // L1_2: instantiation #2
            LABEL(L1_2a);
            ASTORE(2);
            IINC(1, 1);
            GOTO(L3_2);
            LABEL(L3_2);
            IINC(1, 4);
            GOTO(L1_2b);
            LABEL(new Label()); // extra label emitted due to impl quirks

            END(1, 3);
        }

        assertEquals(exp, jsr);
    }

    /**
     * Tests a nested try/finally with implicit exit from one subroutine to the
     * other subroutine, and with a surrounding try/catch thrown in the mix.
     * Equivalent to the following java code:
     * 
     * <pre>
     * void m(int b) {
     *   try {
     *     try {
     *       return;
     *     } finally {
     *       while (b) {
     *         try {
     *           return;
     *         } finally {
     *           // NOTE --- this break avoids the second return above (weird)
     *           if (b) break;
     *         }
     *       }
     *     } 
     *   } catch (Exception e) {
     *     b += 3;
     *     return;
     *   }
     * }
     * </pre>
     */
    public void testImplicitExitInTryCatch() {
        {
            Label T1 = new Label();
            Label C1 = new Label();
            Label S1 = new Label();
            Label L = new Label();
            Label C2 = new Label();
            Label S2 = new Label();
            Label W = new Label();
            Label X = new Label();
            Label OT = new Label();
            Label OC = new Label();

            // variable numbers:
            int b = 1;
            int e1 = 2;
            int e2 = 3;
            int r1 = 4;
            int r2 = 5;

            setCurrent(jsr);

            ICONST_0();
            ISTORE(1);

            // OT: outermost try
            LABEL(OT);

            // T1: first try:
            LABEL(T1);
            JSR(S1);
            RETURN();

            // C1: exception handler for first try
            LABEL(C1);
            ASTORE(e1);
            JSR(S1);
            ALOAD(e1);
            ATHROW();

            // S1: first finally handler
            LABEL(S1);
            ASTORE(r1);
            GOTO(W);

            // L: body of while loop, also second try
            LABEL(L);
            JSR(S2);
            RETURN();

            // C2: exception handler for second try
            LABEL(C2);
            ASTORE(e2);
            JSR(S2);
            ALOAD(e2);
            ATHROW();

            // S2: second finally handler
            LABEL(S2);
            ASTORE(r2);
            ILOAD(b);
            IFNE(X);
            RET(r2);

            // W: test for the while loop
            LABEL(W);
            ILOAD(b);
            IFNE(L); // falls through to X

            // X: exit from finally{} block
            LABEL(X);
            RET(r1);

            // OC: outermost catch
            LABEL(OC);
            IINC(b, 3);
            RETURN();

            TRYCATCH(T1, C1, C1);
            TRYCATCH(L, C2, C2);
            TRYCATCH(OT, OC, OC);

            END(1, 6);
        }

        {
            Label T1 = new Label();
            Label C1 = new Label();
            Label S1_1a = new Label();
            Label S1_1b = new Label();
            Label S1_2a = new Label();
            Label S1_2b = new Label();
            Label L_1 = new Label();
            Label L_2 = new Label();
            Label C2_1 = new Label();
            Label C2_2 = new Label();
            Label S2_1_1a = new Label();
            Label S2_1_1b = new Label();
            Label S2_1_2a = new Label();
            Label S2_1_2b = new Label();
            Label S2_2_1a = new Label();
            Label S2_2_1b = new Label();
            Label S2_2_2a = new Label();
            Label S2_2_2b = new Label();
            Label W_1 = new Label();
            Label W_2 = new Label();
            Label X_1 = new Label();
            Label X_2 = new Label();
            Label OT_1 = S1_1a;
            Label OT_2 = S1_2a;
            Label OT_1_1 = S2_1_1a;
            Label OT_1_2 = S2_1_2a;
            Label OT_2_1 = S2_2_1a;
            Label OT_2_2 = S2_2_2a;
            Label OC = new Label();
            Label OC_1 = new Label();
            Label OC_2 = new Label();
            Label OC_1_1 = new Label();
            Label OC_1_2 = new Label();
            Label OC_2_1 = new Label();
            Label OC_2_2 = new Label();

            // variable numbers:
            int b = 1;
            int e1 = 2;
            int e2 = 3;
            int r1 = 4;
            int r2 = 5;

            setCurrent(exp);

            // --- Main Subroutine ---

            ICONST_0();
            ISTORE(1);

            // T1: outermost try / first try:
            LABEL(T1);
            ACONST_NULL();
            GOTO(S1_1a);
            LABEL(S1_1b);
            RETURN();

            // C1: exception handler for first try
            LABEL(C1);
            ASTORE(e1);
            ACONST_NULL();
            GOTO(S1_2a);
            LABEL(S1_2b);
            ALOAD(e1);
            ATHROW();

            // OC: Outermost catch
            LABEL(OC);
            IINC(b, 3);
            RETURN();

            // --- First instantiation of first subroutine ---

            // S1: first finally handler
            LABEL(S1_1a);
            ASTORE(r1);
            GOTO(W_1);

            // L_1: body of while loop, also second try
            LABEL(L_1);
            ACONST_NULL();
            GOTO(S2_1_1a);
            LABEL(S2_1_1b);
            RETURN();

            // C2_1: exception handler for second try
            LABEL(C2_1);
            ASTORE(e2);
            ACONST_NULL();
            GOTO(S2_1_2a);
            LABEL(S2_1_2b);
            ALOAD(e2);
            ATHROW();

            // W_1: test for the while loop
            LABEL(W_1);
            ILOAD(b);
            IFNE(L_1); // falls through to X_1

            // X_1: exit from finally{} block
            LABEL(X_1);
            GOTO(S1_1b);

            LABEL(OC_1);

            // --- Second instantiation of first subroutine ---

            // S1: first finally handler
            LABEL(S1_2a);
            ASTORE(r1);
            GOTO(W_2);

            // L_2: body of while loop, also second try
            LABEL(L_2);
            ACONST_NULL();
            GOTO(S2_2_1a);
            LABEL(S2_2_1b);
            RETURN();

            // C2_2: exception handler for second try
            LABEL(C2_2);
            ASTORE(e2);
            ACONST_NULL();
            GOTO(S2_2_2a);
            LABEL(S2_2_2b);
            ALOAD(e2);
            ATHROW();

            // W_2: test for the while loop
            LABEL(W_2);
            ILOAD(b);
            IFNE(L_2); // falls through to X_2

            // X_2: exit from finally{} block
            LABEL(X_2);
            GOTO(S1_2b);

            LABEL(OC_2);

            // --- Second subroutine's 4 instantiations ---

            // S2_1_1a:
            LABEL(S2_1_1a);
            ASTORE(r2);
            ILOAD(b);
            IFNE(X_1);
            GOTO(S2_1_1b);
            LABEL(OC_1_1);

            // S2_1_2a:
            LABEL(S2_1_2a);
            ASTORE(r2);
            ILOAD(b);
            IFNE(X_1);
            GOTO(S2_1_2b);
            LABEL(OC_1_2);

            // S2_2_1a:
            LABEL(S2_2_1a);
            ASTORE(r2);
            ILOAD(b);
            IFNE(X_2);
            GOTO(S2_2_1b);
            LABEL(OC_2_1);

            // S2_2_2a:
            LABEL(S2_2_2a);
            ASTORE(r2);
            ILOAD(b);
            IFNE(X_2);
            GOTO(S2_2_2b);
            LABEL(OC_2_2);

            // main subroutine handlers:
            TRYCATCH(T1, C1, C1);
            TRYCATCH(T1, OC, OC);

            // first instance of first sub try/catch handlers:
            TRYCATCH(L_1, C2_1, C2_1);
            TRYCATCH(OT_1, OC_1, OC); // note: reuses handler code from main
            // sub

            // second instance of first sub try/catch handlers:
            TRYCATCH(L_2, C2_2, C2_2);
            TRYCATCH(OT_2, OC_2, OC);

            // all 4 instances of second sub:
            TRYCATCH(OT_1_1, OC_1_1, OC);
            TRYCATCH(OT_1_2, OC_1_2, OC);
            TRYCATCH(OT_2_1, OC_2_1, OC);
            TRYCATCH(OT_2_2, OC_2_2, OC);

            END(1, 6);
        }

        assertEquals(exp, jsr);
    }

    /**
     * Tests a method which has line numbers and local variable declarations.
     * 
     * <pre>
     *   public void a() {
     * 1    int a = 0;
     * 2    try {
     * 3      a++;
     * 4    } finally {
     * 5      a--;
     * 6    }
     *   }
     *   LV "a" from 1 to 6
     * </pre>
     */
    public void testBasicLineNumberAndLocalVars() {
        {
            Label LM1 = new Label();
            Label L0 = new Label();
            Label L1 = new Label();
            Label L2 = new Label();
            Label L3 = new Label();
            Label L4 = new Label();

            setCurrent(jsr);
            LABEL(LM1);
            LINE(1, LM1);
            ICONST_0();
            ISTORE(1);

            /* L0: body of try block */
            LABEL(L0);
            LINE(3, L0);
            IINC(1, 1);
            GOTO(L1);

            /* L2: exception handler */
            LABEL(L2);
            ASTORE(3);
            JSR(L3);
            ALOAD(3);
            ATHROW();

            /* L3: subroutine */
            LABEL(L3);
            LINE(5, L3);
            ASTORE(2);
            IINC(1, -1);
            RET(2);

            /* L1: non-exceptional exit from try block */
            LABEL(L1);
            JSR(L3);
            LABEL(L4); // L4
            RETURN();

            TRYCATCH(L0, L2, L2);
            TRYCATCH(L1, L4, L2);
            LOCALVAR("a", "I", 1, LM1, L4);

            END(1, 4);
        }

        {
            Label LM1 = new Label();
            Label L0 = new Label();
            Label L1 = new Label();
            Label L2 = new Label();
            Label L3_1a = new Label();
            Label L3_1b = new Label();
            Label L3_1c = new Label();
            Label L3_2a = new Label();
            Label L3_2b = new Label();
            Label L3_2c = new Label();
            Label L4 = new Label();

            setCurrent(exp);
            LABEL(LM1);
            LINE(1, LM1);
            ICONST_0();
            ISTORE(1);
            // L0: try/catch block
            LABEL(L0);
            LINE(3, L0);
            IINC(1, 1);
            GOTO(L1);

            // L2: Exception handler:
            LABEL(L2);
            ASTORE(3);
            ACONST_NULL();
            GOTO(L3_1a);
            LABEL(L3_1b); // L3_1b;
            ALOAD(3);
            ATHROW();

            // L1: On non-exceptional exit, try block leads here:
            LABEL(L1);
            ACONST_NULL();
            GOTO(L3_2a);
            LABEL(L3_2b); // L3_2b
            LABEL(L4); // L4
            RETURN();

            // L3_1a: First instantiation of subroutine:
            LABEL(L3_1a);
            LINE(5, L3_1a);
            ASTORE(2);
            IINC(1, -1);
            GOTO(L3_1b);
            LABEL(L3_1c);

            // L3_2a: Second instantiation of subroutine:
            LABEL(L3_2a);
            LINE(5, L3_2a);
            ASTORE(2);
            IINC(1, -1);
            GOTO(L3_2b);
            LABEL(L3_2c);

            TRYCATCH(L0, L2, L2);
            TRYCATCH(L1, L4, L2);
            LOCALVAR("a", "I", 1, LM1, L4);
            LOCALVAR("a", "I", 1, L3_1a, L3_1c);
            LOCALVAR("a", "I", 1, L3_2a, L3_2c);

            END(1, 4);
        }

        assertEquals(exp, jsr);
    }

    public void assertEquals(final MethodNode exp, final MethodNode actual) {
        String textexp = getText(exp);
        String textact = getText(actual);
        System.err.println("Expected=" + textexp);
        System.err.println("Actual=" + textact);
        assertEquals(textexp, textact);
    }

    private String getText(final MethodNode mn) {
        TraceMethodVisitor tmv = new TraceMethodVisitor(null);
        mn.accept(tmv);

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < tmv.text.size(); i++) {
            sb.append(tmv.text.get(i));
        }
        return sb.toString();
    }
}
