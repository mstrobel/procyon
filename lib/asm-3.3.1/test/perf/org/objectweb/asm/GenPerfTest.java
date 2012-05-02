/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2005 INRIA, France Telecom
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

package org.objectweb.asm;

import gnu.bytecode.Access;
import gnu.bytecode.ClassType;
import gnu.bytecode.CodeAttr;
import gnu.bytecode.Field;
import gnu.bytecode.Method;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import jbet.Descriptor;
import jbet.Instruction;
import jbet.Snippit;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUSH;

import org.cojen.classfile.MethodInfo;
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.TypeDesc;

import org.gjt.jclasslib.bytecode.ImmediateByteInstruction;
import org.gjt.jclasslib.bytecode.ImmediateShortInstruction;
import org.gjt.jclasslib.bytecode.SimpleInstruction;
import org.gjt.jclasslib.io.ByteCodeWriter;
import org.gjt.jclasslib.structures.AccessFlags;
import org.gjt.jclasslib.structures.AttributeInfo;
import org.gjt.jclasslib.structures.CPInfo;
import org.gjt.jclasslib.structures.ConstantPoolUtil;
import org.gjt.jclasslib.structures.InvalidByteCodeException;
import org.gjt.jclasslib.structures.attributes.CodeAttribute;
import org.gjt.jclasslib.structures.attributes.SourceFileAttribute;
import org.gjt.jclasslib.structures.constants.ConstantStringInfo;

import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;

import alt.jiapi.reflect.InstructionFactory;
import alt.jiapi.reflect.InstructionList;
import alt.jiapi.reflect.JiapiClass;
import alt.jiapi.reflect.JiapiMethod;
import alt.jiapi.reflect.MethodExistsException;
import alt.jiapi.reflect.Signature;

import com.claritysys.jvm.builder.CodeBuilder;
import com.claritysys.jvm.classfile.CfMethod;
import com.claritysys.jvm.classfile.ClassFile;
import com.claritysys.jvm.classfile.ConstantPool;
import com.claritysys.jvm.classfile.JVM;

/**
 * Performance tests for frameworks that can only do bytecode generation.
 * 
 * @author Eric Bruneton
 */
public class GenPerfTest {

    final static int N = 100000;

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 5; ++i) {
            asmTest();
        }
        for (int i = 0; i < 5; ++i) {
            gnuByteCodeTest();
        }
        for (int i = 0; i < 5; ++i) {
            csgBytecodeTest();
        }
        for (int i = 0; i < 5; ++i) {
            cojenTest();
        }
        for (int i = 0; i < 5; ++i) {
            jbetTest();
        }
        for (int i = 0; i < 5; ++i) {
            jClassLibTest();
        }
        for (int i = 0; i < 5; ++i) {
            jiapiTest();
        }
        for (int i = 0; i < 5; ++i) {
            mozillaClassFileTest();
        }
        for (int i = 0; i < 5; ++i) {
            bcelTest();
        }
        for (int i = 0; i < 5; ++i) {
            aspectjBcelTest();
        }
    }

    static void asmTest() {
        long t = System.currentTimeMillis();
        for (int i = 0; i < N; ++i) {
            asmHelloWorld();
        }
        t = System.currentTimeMillis() - t;
        System.out.println("ASM generation time: " + ((float) t) / N
                + " ms/class");
    }

    static void gnuByteCodeTest() {
        long t = System.currentTimeMillis();
        for (int i = 0; i < N; ++i) {
            gnuByteCodeHelloWorld();
        }
        t = System.currentTimeMillis() - t;
        System.out.println("gnu.bytecode generation time: " + ((float) t) / N
                + " ms/class");
    }

    static void csgBytecodeTest() {
        long t = System.currentTimeMillis();
        for (int i = 0; i < N; ++i) {
            csgBytecodeHelloWorld();
        }
        t = System.currentTimeMillis() - t;
        System.out.println("CSG bytecode generation time: " + ((float) t) / N
                + " ms/class");
    }

    static void cojenTest() throws IOException {
        long t = System.currentTimeMillis();
        for (int i = 0; i < N; ++i) {
            cojenHelloWorld();
        }
        t = System.currentTimeMillis() - t;
        System.out.println("Cojen generation time: " + ((float) t) / N
                + " ms/class");
    }

    static void jbetTest() throws IOException {
        long t = System.currentTimeMillis();
        for (int i = 0; i < N; ++i) {
            jbetHelloWorld();
        }
        t = System.currentTimeMillis() - t;
        System.out.println("JBET generation time: " + ((float) t) / N
                + " ms/class");
    }

    static void jClassLibTest() throws IOException, InvalidByteCodeException {
        long t = System.currentTimeMillis();
        for (int i = 0; i < N; ++i) {
            jClassLibHelloWorld();
        }
        t = System.currentTimeMillis() - t;
        System.out.println("JClassLib generation time: " + ((float) t) / N
                + " ms/class");
    }

    static void jiapiTest() throws MethodExistsException {
        int N = 1000;
        long t = System.currentTimeMillis();
        for (int i = 0; i < N; ++i) {
            jiapiHelloWorld();
        }
        t = System.currentTimeMillis() - t;
        System.out.println("Jiapi generation time: " + ((float) t) / N
                + " ms/class");
    }

    static void mozillaClassFileTest() {
        long t = System.currentTimeMillis();
        for (int i = 0; i < N; ++i) {
            mozillaClassFileHelloWorld();
        }
        t = System.currentTimeMillis() - t;
        System.out.println("Mozilla Class File generation time: " + ((float) t)
                / N + " ms/class");
    }

    static void bcelTest() {
        long t = System.currentTimeMillis();
        for (int i = 0; i < N; ++i) {
            bcelHelloWorld();
        }
        t = System.currentTimeMillis() - t;
        System.out.println("BCEL generation time: " + ((float) t) / N
                + " ms/class");
    }

    static void aspectjBcelTest() {
        long t = System.currentTimeMillis();
        for (int i = 0; i < N; ++i) {
            aspectjBcelHelloWorld();
        }
        t = System.currentTimeMillis() - t;
        System.out.println("AspectJ BCEL generation time: " + ((float) t) / N
                + " ms/class");
    }

    static byte[] asmHelloWorld() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        cw.visit(Opcodes.V1_1,
                Opcodes.ACC_PUBLIC,
                "HelloWorld",
                null,
                "java/lang/Object",
                null);
        cw.visitSource("HelloWorld.java", null);

        MethodVisitor mw = cw.visitMethod(Opcodes.ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null);
        mw.visitVarInsn(Opcodes.ALOAD, 0);
        mw.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V");
        mw.visitInsn(Opcodes.RETURN);
        mw.visitMaxs(0, 0);
        mw.visitEnd();

        mw = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V",
                null,
                null);
        mw.visitFieldInsn(Opcodes.GETSTATIC,
                "java/lang/System",
                "out",
                "Ljava/io/PrintStream;");
        mw.visitLdcInsn("Hello world!");
        mw.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V");
        mw.visitInsn(Opcodes.RETURN);
        mw.visitMaxs(0, 0);
        mw.visitEnd();

        return cw.toByteArray();
    }

    static Method objectCtor = gnu.bytecode.Type.pointer_type.getDeclaredMethod("<init>",
            0);

    static Field outField = ClassType.make("java.lang.System").getField("out");

    static Method printlnMethod = ClassType.make("java.io.PrintStream")
            .getDeclaredMethod("println",
                    new gnu.bytecode.Type[] { gnu.bytecode.Type.string_type });

    static byte[] gnuByteCodeHelloWorld() {
        ClassType c = new ClassType("HelloWorld");
        c.setSuper("java.lang.Object");
        c.setModifiers(Access.PUBLIC);
        c.setSourceFile("HelloWorld.java");

        Method m = c.addMethod("<init>", "()V", Access.PUBLIC);
        CodeAttr code = m.startCode();
        code.pushScope();
        code.emitPushThis();
        code.emitInvokeSpecial(objectCtor);
        code.emitReturn();
        code.popScope();

        m = c.addMethod("main", "([Ljava/lang/String;)V", Access.PUBLIC
                | Access.STATIC);
        code = m.startCode();
        code.pushScope();
        code.emitGetStatic(outField);
        code.emitPushString("Hello world!");
        code.emitInvokeVirtual(printlnMethod);
        code.emitReturn();
        code.popScope();

        return c.writeToArray();
    }

    static byte[] csgBytecodeHelloWorld() {
        ClassFile cf = new ClassFile("HelloWorld",
                "java/lang/Object",
                "HelloWorld.java");
        ConstantPool cp = cf.getConstantPool();

        CfMethod method = cf.addMethod(JVM.ACC_PUBLIC, "<init>", "()V");
        CodeBuilder code = new CodeBuilder(method);
        code.add(JVM.ALOAD_0);
        code.add(JVM.INVOKESPECIAL, cp.addMethodRef(false,
                "java/lang/Object",
                "<init>",
                "()V"));
        code.add(JVM.RETURN);
        code.flush();

        method = cf.addMethod(JVM.ACC_PUBLIC + JVM.ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V");
        code = new CodeBuilder(method);
        code.add(JVM.GETSTATIC, cp.addFieldRef("java/lang/System",
                "out",
                "Ljava/io/PrintStream;"));
        code.add(JVM.LDC, "Hello world!");
        code.add(JVM.INVOKEVIRTUAL, cp.addMethodRef(false,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V"));
        code.add(JVM.RETURN);
        code.flush();

        return cf.writeToArray();
    }

    static TypeDesc printStream = TypeDesc.forClass("java.io.PrintStream");

    static byte[] cojenHelloWorld() throws IOException {
        org.cojen.classfile.ClassFile cf = new org.cojen.classfile.ClassFile("HelloWorld");

        cf.setSourceFile("HelloWorld.java");

        cf.addDefaultConstructor();

        TypeDesc[] params = new TypeDesc[] { TypeDesc.STRING.toArrayType() };
        MethodInfo mi = cf.addMethod(Modifiers.PUBLIC_STATIC,
                "main",
                null,
                params);
        org.cojen.classfile.CodeBuilder b = new org.cojen.classfile.CodeBuilder(mi);
        b.loadStaticField("java.lang.System", "out", printStream);
        b.loadConstant("Hello world!");
        b.invokeVirtual(printStream,
                "println",
                null,
                new TypeDesc[] { TypeDesc.STRING });
        b.returnVoid();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        cf.writeTo(bos);

        return bos.toByteArray();
    }

    static Descriptor emptyDesc = new Descriptor("()V");

    static Descriptor mainDesc = new Descriptor("([Ljava/lang/String;)V");

    static Descriptor printlnDesc = new Descriptor("(Ljava/lang/String;)V");

    static jbet.Type printStreamType = new jbet.Type("Ljava/io/PrintStream;");

    static byte[] jbetHelloWorld() throws IOException {
        jbet.ClassInfo ci = new jbet.ClassInfo(null, "HelloWorld");

        ci.sourceFile = "HelloWorld.java";

        jbet.MethodInfo mi = new jbet.MethodInfo("<init>",
                emptyDesc,
                jbet.MethodInfo.ACC_PUBLIC);
        mi.code = new Snippit();
        mi.code.push(new Instruction().setAload(0));
        mi.code.push(new Instruction().setInvokeSpecial("java/lang/Object",
                "<init>",
                emptyDesc));
        mi.code.push(new Instruction().setReturn());
        mi.maxLocals = 1;
        mi.maxStack = 1;
        ci.addMethod(mi);

        mi = new jbet.MethodInfo("main", mainDesc, jbet.MethodInfo.ACC_PUBLIC
                | jbet.MethodInfo.ACC_STATIC);
        mi.code = new Snippit();
        mi.code.push(new Instruction().setGetstatic("java/lang/System",
                "out",
                printStreamType));
        mi.code.push(new Instruction().setSpush("Hello world!"));
        mi.code.push(new Instruction().setInvokeVirtual("java/io/PrintStream",
                "println",
                printlnDesc));
        mi.maxLocals = 1;
        mi.maxStack = 2;
        ci.addMethod(mi);

        ci.resolveConstants();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ci.writeFile(bos);

        return bos.toByteArray();
    }

    static byte[] jClassLibHelloWorld()
            throws InvalidByteCodeException, IOException
    {
        org.gjt.jclasslib.structures.ClassFile cf = new org.gjt.jclasslib.structures.ClassFile();
        cf.setConstantPool(new CPInfo[0]);
        ConstantPoolUtil.addConstantUTF8Info(cf, "", 0); // dummy constant
        cf.setMajorVersion(45);
        cf.setMinorVersion(3);
        cf.setAccessFlags(AccessFlags.ACC_PUBLIC);
        cf.setThisClass(ConstantPoolUtil.addConstantClassInfo(cf,
                "HelloWorld",
                0));
        cf.setSuperClass(ConstantPoolUtil.addConstantClassInfo(cf,
                "java/lang/Object",
                0));

        SourceFileAttribute sa = new SourceFileAttribute();
        sa.setAttributeNameIndex(ConstantPoolUtil.addConstantUTF8Info(cf,
                SourceFileAttribute.ATTRIBUTE_NAME,
                0));
        sa.setSourcefileIndex(ConstantPoolUtil.addConstantUTF8Info(cf,
                "HelloWorld.java",
                0));

        org.gjt.jclasslib.structures.MethodInfo mi1 = new org.gjt.jclasslib.structures.MethodInfo();
        mi1.setAccessFlags(AccessFlags.ACC_PUBLIC);
        mi1.setNameIndex(ConstantPoolUtil.addConstantUTF8Info(cf, "<init>", 0));
        mi1.setDescriptorIndex(ConstantPoolUtil.addConstantUTF8Info(cf,
                "()V",
                0));
        CodeAttribute ca1 = new CodeAttribute();
        ca1.setAttributeNameIndex(ConstantPoolUtil.addConstantUTF8Info(cf,
                CodeAttribute.ATTRIBUTE_NAME,
                0));
        ca1.setCode(ByteCodeWriter.writeByteCode(Arrays.asList(new org.gjt.jclasslib.bytecode.AbstractInstruction[] {
            new SimpleInstruction(org.gjt.jclasslib.bytecode.Opcodes.OPCODE_ALOAD_0),
            new ImmediateShortInstruction(org.gjt.jclasslib.bytecode.Opcodes.OPCODE_INVOKESPECIAL,
                    ConstantPoolUtil.addConstantMethodrefInfo(cf,
                            "java/lang/Object",
                            "<init>",
                            "()V",
                            0)),
            new SimpleInstruction(org.gjt.jclasslib.bytecode.Opcodes.OPCODE_RETURN) })));
        ca1.setMaxStack(1);
        ca1.setMaxLocals(1);
        mi1.setAttributes(new AttributeInfo[] { ca1 });

        ConstantStringInfo s = new ConstantStringInfo();
        s.setStringIndex(ConstantPoolUtil.addConstantUTF8Info(cf,
                "Hello world!",
                0));

        org.gjt.jclasslib.structures.MethodInfo mi2 = new org.gjt.jclasslib.structures.MethodInfo();
        mi2.setAccessFlags(AccessFlags.ACC_PUBLIC | AccessFlags.ACC_STATIC);
        mi2.setNameIndex(ConstantPoolUtil.addConstantUTF8Info(cf, "main", 0));
        mi2.setDescriptorIndex(ConstantPoolUtil.addConstantUTF8Info(cf,
                "([Ljava/lang/String;)V",
                0));
        CodeAttribute ca2 = new CodeAttribute();
        ca2.setAttributeNameIndex(ConstantPoolUtil.addConstantUTF8Info(cf,
                CodeAttribute.ATTRIBUTE_NAME,
                0));
        ca2.setCode(ByteCodeWriter.writeByteCode(Arrays.asList(new org.gjt.jclasslib.bytecode.AbstractInstruction[] {
            new ImmediateShortInstruction(org.gjt.jclasslib.bytecode.Opcodes.OPCODE_GETSTATIC,
                    ConstantPoolUtil.addConstantFieldrefInfo(cf,
                            "java/lang/System",
                            "out",
                            "Ljava/io/PrintStream;",
                            0)),
            new ImmediateByteInstruction(org.gjt.jclasslib.bytecode.Opcodes.OPCODE_LDC,
                    false,
                    ConstantPoolUtil.addConstantPoolEntry(cf, s, 0)),
            new ImmediateShortInstruction(org.gjt.jclasslib.bytecode.Opcodes.OPCODE_INVOKEVIRTUAL,
                    ConstantPoolUtil.addConstantMethodrefInfo(cf,
                            "java/io/PrintStream",
                            "println",
                            "(Ljava/lang/String;)V",
                            0)) })));
        ca2.setMaxStack(2);
        ca2.setMaxLocals(1);
        mi2.setAttributes(new AttributeInfo[] { ca2 });

        cf.setMethods(new org.gjt.jclasslib.structures.MethodInfo[] { mi1, mi2 });
        cf.setAttributes(new AttributeInfo[] { sa });

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        cf.write(dos);
        dos.close();

        return bos.toByteArray();
    }

    static Signature emptySig = new Signature("()V");

    static Signature mainSig = new Signature("([Ljava/lang/String;)V");

    static Signature printlnSig = new Signature("(Ljava/lang/String;)V");

    static byte[] jiapiHelloWorld() throws MethodExistsException {
        JiapiClass c = JiapiClass.createClass("HelloWorld");

        // No API to set SourceFile!

        JiapiMethod method = c.addMethod(Modifier.PUBLIC, "<init>", emptySig);
        InstructionList il = method.getInstructionList();
        InstructionFactory iFactory = il.getInstructionFactory();
        il.add(iFactory.aload(0));
        il.add(iFactory.invoke(0, "java/lang/Object", "<init>", emptySig));
        il.add(iFactory.returnMethod(method));

        method = c.addMethod(Modifier.PUBLIC | Modifier.STATIC, "main", mainSig);
        il = method.getInstructionList();
        iFactory = il.getInstructionFactory();
        il.add(iFactory.getField(Modifier.STATIC,
                "java/lang/System",
                "out",
                "Ljava/io/PrintStream;"));
        il.add(iFactory.pushConstant("Hello world!"));
        il.add(iFactory.invoke(0, "java/io/PrintStream", "println", printlnSig));
        il.add(iFactory.returnMethod(method));

        return c.getByteCode();
    }

    static byte[] mozillaClassFileHelloWorld() {
        ClassFileWriter c = new ClassFileWriter("HelloWorld",
                "java/lang/Object",
                "HelloWorld.java");

        c.startMethod("<init>", "()V", ClassFileWriter.ACC_PUBLIC);
        c.addLoadThis();
        c.addInvoke(ByteCode.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        c.add(ByteCode.RETURN);
        c.stopMethod((short) 1);

        c.startMethod("main",
                "()V",
                (short) (ClassFileWriter.ACC_PUBLIC | ClassFileWriter.ACC_STATIC));
        c.add(ByteCode.GETSTATIC,
                "java/lang/System",
                "out",
                "Ljava/io/PrintStream;");
        c.addPush("Hello world!");
        c.addInvoke(ByteCode.INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V");
        c.add(ByteCode.RETURN);
        c.stopMethod((short) 1);

        return c.toByteArray();
    }

    static org.apache.bcel.generic.Type printStreamT = org.apache.bcel.generic.Type.getType("Ljava/io/PrintStream;");

    static byte[] bcelHelloWorld() {
        ClassGen cg = new ClassGen("HelloWorld",
                "java/lang/Object",
                "HelloWorld.java",
                Constants.ACC_PUBLIC,
                null);

        cg.addEmptyConstructor(Constants.ACC_PUBLIC);

        ConstantPoolGen cp = cg.getConstantPool();
        org.apache.bcel.generic.InstructionList il = new org.apache.bcel.generic.InstructionList();
        org.apache.bcel.generic.InstructionFactory factory = new org.apache.bcel.generic.InstructionFactory(cg);

        MethodGen mg = new MethodGen(Constants.ACC_STATIC
                | Constants.ACC_PUBLIC,
                org.apache.bcel.generic.Type.VOID,
                new org.apache.bcel.generic.Type[] { new ArrayType(org.apache.bcel.generic.Type.STRING,
                        1) },
                null,
                "main",
                "HelloWorld",
                il,
                cp);
        il.append(factory.createGetStatic("java/lang/System",
                "out",
                printStreamT));
        il.append(new PUSH(cp, "Hello world!"));
        il.append(factory.createInvoke("java.io.PrintStream",
                "println",
                org.apache.bcel.generic.Type.VOID,
                new org.apache.bcel.generic.Type[] { org.apache.bcel.generic.Type.STRING },
                Constants.INVOKESPECIAL));

        mg.setMaxStack();
        cg.addMethod(mg.getMethod());

        return cg.getJavaClass().getBytes();
    }

    static org.aspectj.apache.bcel.generic.Type printStreamAT = org.aspectj.apache.bcel.generic.Type.getType("Ljava/io/PrintStream;");

    static byte[] aspectjBcelHelloWorld() {
        org.aspectj.apache.bcel.generic.ClassGen cg = new org.aspectj.apache.bcel.generic.ClassGen("HelloWorld",
                "java/lang/Object",
                "HelloWorld.java",
                Constants.ACC_PUBLIC,
                null);

        cg.addEmptyConstructor(Constants.ACC_PUBLIC);

        org.aspectj.apache.bcel.generic.ConstantPoolGen cp = cg.getConstantPool();
        org.aspectj.apache.bcel.generic.InstructionList il = new org.aspectj.apache.bcel.generic.InstructionList();
        org.aspectj.apache.bcel.generic.InstructionFactory factory = new org.aspectj.apache.bcel.generic.InstructionFactory(cg);

        org.aspectj.apache.bcel.generic.MethodGen mg = new org.aspectj.apache.bcel.generic.MethodGen(Constants.ACC_STATIC
                | Constants.ACC_PUBLIC,
                org.aspectj.apache.bcel.generic.Type.VOID,
                new org.aspectj.apache.bcel.generic.Type[] { new org.aspectj.apache.bcel.generic.ArrayType(org.aspectj.apache.bcel.generic.Type.STRING,
                        1) },
                null,
                "main",
                "HelloWorld",
                il,
                cp);
        il.append(factory.createGetStatic("java/lang/System",
                "out",
                printStreamAT));
        il.append(new org.aspectj.apache.bcel.generic.PUSH(cp, "Hello world!"));
        il.append(factory.createInvoke("java.io.PrintStream",
                "println",
                org.aspectj.apache.bcel.generic.Type.VOID,
                new org.aspectj.apache.bcel.generic.Type[] { org.aspectj.apache.bcel.generic.Type.STRING },
                Constants.INVOKESPECIAL));

        mg.setMaxStack();
        cg.addMethod(mg.getMethod());

        return cg.getJavaClass().getBytes();
    }
}
