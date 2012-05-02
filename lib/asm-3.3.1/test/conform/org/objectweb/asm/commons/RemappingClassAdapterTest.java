/***
 * ASM: a very small and fast Java bytecode manipulation framework
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

package org.objectweb.asm.commons;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class RemappingClassAdapterTest extends TestCase implements Opcodes {

    public void testRemappingClassAdapter() throws Exception {
        Map map = new HashMap();
        map.put("Boo", "B1");
        map.put("Coo", "C1");
        map.put("Doo", "D1");
        Remapper remapper = new SimpleRemapper(map);
        
        ClassNode cn = new ClassNode();
        dump(new RemappingClassAdapter(cn, remapper));
        
        assertEquals("D1", cn.name);
        assertEquals("B1", cn.superName);
        assertEquals(Arrays.asList(new String[] {"I", "I", "C1", "J", "B1"}), cn.interfaces);
        
        assertEquals("LB1;", field(cn, 0).desc);
        assertEquals("[LB1;", field(cn, 1).desc);
        
        assertEquals("D1", innerClass(cn, 0).name);
        assertEquals("B1", innerClass(cn, 0).outerName);
        // assertEquals("Doo", innerClass(cn, 0).innerName);
        
        assertEquals("B1", cn.outerClass);
        assertEquals("([[LB1;LC1;LD1;)LC1;", cn.outerMethodDesc);
        
        MethodNode mn0 = (MethodNode) cn.methods.get(0);
        Iterator it = mn0.instructions.iterator();
        
        FieldInsnNode n0 = (FieldInsnNode) it.next();
        assertEquals("D1", n0.owner);
        assertEquals("LB1;", n0.desc);
        
        assertEquals(Type.getType("LB1;"), ((LdcInsnNode) it.next()).cst);
        assertEquals(Type.getType("[LD1;"), ((LdcInsnNode) it.next()).cst);
        assertEquals(Type.getType("[I"), ((LdcInsnNode) it.next()).cst);
        assertEquals(Type.getType("J"), ((LdcInsnNode) it.next()).cst);
        
        assertEquals("B1", ((TypeInsnNode) it.next()).desc);
        assertEquals("[LD1;", ((TypeInsnNode) it.next()).desc);
        assertEquals("[I", ((TypeInsnNode) it.next()).desc);
        assertEquals("J", ((TypeInsnNode) it.next()).desc);
        
        MultiANewArrayInsnNode n3 = (MultiANewArrayInsnNode) it.next();
        assertEquals("[[LB1;", n3.desc);
        
        MethodInsnNode n4 = (MethodInsnNode) it.next();
        assertEquals("D1", n4.owner);
        assertEquals("([[LB1;LC1;LD1;)LC1;", n4.desc);
        
        FrameNode fn0 = (FrameNode) it.next();
        assertEquals(Collections.EMPTY_LIST, fn0.local);
        assertEquals(Collections.EMPTY_LIST, fn0.stack);
        
        assertEquals(Arrays.asList(new Object[] {"B1", "C1", "D1"}), ((FrameNode) it.next()).local);
        assertEquals(Arrays.asList(new Object[] {Opcodes.INTEGER, "C1", Opcodes.INTEGER, "D1"}), ((FrameNode) it.next()).local);
        assertEquals(Arrays.asList(new Object[] {Opcodes.INTEGER, Opcodes.INTEGER}), ((FrameNode) it.next()).local);
        // assertEquals(Collections.EMPTY_LIST, fn0.stack);
        
        TryCatchBlockNode tryCatchBlockNode = (TryCatchBlockNode) mn0.tryCatchBlocks.get(0);
        assertEquals("C1", tryCatchBlockNode.type);
        
        MethodNode mn1 = (MethodNode) cn.methods.get(1);
        assertEquals("([[LB1;LC1;LD1;)V", mn1.desc);
        assertEquals(Arrays.asList(new String[] {"I", "J"}), mn1.exceptions);
    }
    
    private FieldNode field(ClassNode cn, int n) {
        return (FieldNode) cn.fields.get(n);
    }

    private InnerClassNode innerClass(ClassNode cn, int n) {
        return (InnerClassNode) cn.innerClasses.get(n);
    }
    
    
    public static void dump(ClassVisitor cv) throws Exception {
        cv.visit(V1_5, 0, "Doo", null, "Boo", new String[] { "I", "I", "Coo", "J", "Boo"});

        cv.visitInnerClass("Doo", "Boo", "Doo", 0);
        
        cv.visitOuterClass("Boo", "foo", "([[LBoo;LCoo;LDoo;)LCoo;");
        
        cv.visitField(0, "boo", "LBoo;", null, null).visitEnd();
        cv.visitField(0, "boo1", "[LBoo;", null, null).visitEnd();
        cv.visitField(0, "s", "Ljava/lang/String;", null, null).visitEnd();
        cv.visitField(0, "i", "I", null, null).visitEnd();
        
        MethodVisitor mv;
        
        mv = cv.visitMethod(0, "foo", "()V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETFIELD, "Doo", "boo", "LBoo;");

        mv.visitLdcInsn(Type.getType("LBoo;"));
        mv.visitLdcInsn(Type.getType("[LDoo;"));
        mv.visitLdcInsn(Type.getType("[I"));
        mv.visitLdcInsn(Type.getType("J"));
        
        mv.visitTypeInsn(ANEWARRAY, "Boo");
        mv.visitTypeInsn(ANEWARRAY, "[LDoo;");
        mv.visitTypeInsn(ANEWARRAY, "[I");
        mv.visitTypeInsn(ANEWARRAY, "J");
        
        mv.visitMultiANewArrayInsn("[[LBoo;", 2);
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "Doo",
                "goo",
                "([[LBoo;LCoo;LDoo;)LCoo;");

        mv.visitFrame(Opcodes.F_NEW, 0, new Object[5], 0, new Object[10]);
        mv.visitFrame(Opcodes.F_NEW, 
                3, new Object[] { "Boo", "Coo", "Doo" },
                0, new Object[0]);
        mv.visitFrame(Opcodes.F_NEW, 
                4, new Object[] {Opcodes.INTEGER, "Coo", Opcodes.INTEGER, "Doo" }, 
                0, new Object[0]);
        mv.visitFrame(Opcodes.F_NEW, 
                2, new Object[] {Opcodes.INTEGER, Opcodes.INTEGER }, 
                0, new Object[0]);
        
        
        Label l = new Label();

        mv.visitLocalVariable("boo", "LBoo;", null, l, l, 1);
        mv.visitLocalVariable("boo1", "[LBoo;", null, l, l, 3);
        mv.visitLocalVariable("boo2", "[[LBoo;", null, l, l, 4);
        mv.visitMaxs(0, 0);
        
        mv.visitTryCatchBlock(l, l, l, "Coo");
        
        mv.visitEnd();
        
        mv = cv.visitMethod(0,
                "goo",
                "([[LBoo;LCoo;LDoo;)V",
                null,
                new String[] { "I", "J" });
        mv.visitEnd();

        cv.visitEnd();
    }
    
//    /*
    public static class Boo {
    }
    
    public static interface Coo {
    }
    
    public static class Doo extends Boo implements Coo {
        Boo boo = new Boo();
        Boo[] boo1 = new Boo[2];
        String s = "";
        int i = 5;
        
        static final Class c1 = Boo.class;
        static final Class c2 = Boo[].class;
        
        public Doo() {
        }
        public Doo(int i, Coo coo, Boo boo) {
        }
        
        void foo() {
            class Eoo {
                String s;
            }
            
            Eoo e = new Eoo();
            e.s = "aaa";
            
            // visitFieldInsn(int, String, String, String)
            // visitLocalVariable(String, String, String, Label, Label, int)
            Boo boo = this.boo;
            
            // visitLdcInsn(Object)
            Class cc = Boo.class;

            // visitTypeInsn(int, String)
            Boo[] boo1 = new Boo[2];

            // visitMultiANewArrayInsn(String, int)
            Boo[][] boo2 = new Boo[2][2];
            
            // visitMethodInsn(int, String, String, String)
            goo(boo2, this, this);
        }
        
        Coo goo(Boo[][] boo2, Coo coo, Doo doo) {
            return null;
        }
    }
//    */
}
