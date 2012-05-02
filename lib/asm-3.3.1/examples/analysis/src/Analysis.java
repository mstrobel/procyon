/***
 * ASM examples: examples showing how ASM can be used
 * Copyright (c) 2000-2007 INRIA, France Telecom
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.TraceMethodVisitor;

/**
 * @author Eric Bruneton
 */
public class Analysis implements Opcodes {

    public static void main(final String[] args) throws Exception {
        ClassReader cr = new ClassReader("Analysis");
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_DEBUG);

        List methods = cn.methods;
        for (int i = 0; i < methods.size(); ++i) {
            MethodNode method = (MethodNode) methods.get(i);
            if (method.instructions.size() > 0) {
                if (!analyze(cn, method)) {
                    Analyzer a = new Analyzer(new BasicVerifier());
                    try {
                        a.analyze(cn.name, method);
                    } catch (Exception ignored) {
                    }
                    final Frame[] frames = a.getFrames();

                    TraceMethodVisitor mv = new TraceMethodVisitor() {
                        public void visitMaxs(
                            final int maxStack,
                            final int maxLocals)
                        {
                            for (int i = 0; i < text.size(); ++i) {
                                String s = frames[i] == null
                                        ? "null"
                                        : frames[i].toString();
                                while (s.length() < Math.max(20, maxStack
                                        + maxLocals + 1))
                                {
                                    s += " ";
                                }
                                System.err.print(Integer.toString(i + 1000)
                                        .substring(1)
                                        + " " + s + " : " + text.get(i));
                            }
                            System.err.println();
                        }
                    };
                    for (int j = 0; j < method.instructions.size(); ++j) {
                        Object insn = method.instructions.get(j);
                        ((AbstractInsnNode) insn).accept(mv);
                    }
                    mv.visitMaxs(0, 0);
                }
            }
        }
    }

    /*
     * Detects unused xSTORE instructions, i.e. xSTORE instructions without at
     * least one xLOAD corresponding instruction in their successor instructions
     * (in the control flow graph).
     */
    public static boolean analyze(final ClassNode c, final MethodNode m)
            throws Exception
    {
        Analyzer a = new Analyzer(new SourceInterpreter());
        Frame[] frames = a.analyze(c.name, m);

        // for each xLOAD instruction, we find the xSTORE instructions that can
        // produce the value loaded by this instruction, and we put them in
        // 'stores'
        Set stores = new HashSet();
        for (int i = 0; i < m.instructions.size(); ++i) {
            Object insn = m.instructions.get(i);
            int opcode = ((AbstractInsnNode) insn).getOpcode();
            if ((opcode >= ILOAD && opcode <= ALOAD) || opcode == IINC) {
                int var = opcode == IINC
                        ? ((IincInsnNode) insn).var
                        : ((VarInsnNode) insn).var;
                Frame f = frames[i];
                if (f != null) {
                    Set s = ((SourceValue) f.getLocal(var)).insns;
                    Iterator j = s.iterator();
                    while (j.hasNext()) {
                        insn = j.next();
                        if (insn instanceof VarInsnNode) {
                            stores.add(insn);
                        }
                    }
                }
            }
        }

        // we then find all the xSTORE instructions that are not in 'stores'
        boolean ok = true;
        for (int i = 0; i < m.instructions.size(); ++i) {
            Object insn = m.instructions.get(i);
            if (insn instanceof AbstractInsnNode) {
                int opcode = ((AbstractInsnNode) insn).getOpcode();
                if (opcode >= ISTORE && opcode <= ASTORE) {
                    if (!stores.contains(insn)) {
                        ok = false;
                        System.err.println("method " + m.name
                                + ", instruction " + i
                                + ": useless store instruction");
                    }
                }
            }
        }
        return ok;
    }

    /*
     * Test for the above method, with three useless xSTORE instructions.
     */
    public int test(int i, int j) {
        i = i + 1; // ok, because i can be read after this point

        if (j == 0) {
            j = 1; // useless
        } else {
            try {
                j = j - 1; // ok, because j can be accessed in the catch
                int k = 0;
                if (i > 0) {
                    k = i - 1;
                }
                return k;
            } catch (Exception e) { // useless ASTORE (e is never used)
                j = j + 1; // useless
            }
        }

        return 0;
    }
}
