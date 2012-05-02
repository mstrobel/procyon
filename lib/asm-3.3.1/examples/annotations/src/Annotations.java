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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class Annotations {

    public static void foo(final @NotNull
    String arg)
    {
        System.out.println(arg);
    }

    public static void main(final String[] args) throws Exception {
        System.out.println("Calling foo(null) results in a NullPointerException:");
        try {
            foo(null);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        final String n = Annotations.class.getName();
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassReader cr = new ClassReader(n);
        cr.accept(new ClassAdapter(cw) {

            public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String desc,
                final String signature,
                final String[] exceptions)
            {
                final Type[] args = Type.getArgumentTypes(desc);
                MethodVisitor v = cv.visitMethod(access,
                        name,
                        desc,
                        signature,
                        exceptions);
                return new MethodAdapter(v) {

                    private final List params = new ArrayList();

                    public AnnotationVisitor visitParameterAnnotation(
                        final int parameter,
                        final String desc,
                        final boolean visible)
                    {
                        AnnotationVisitor av;
                        av = mv.visitParameterAnnotation(parameter,
                                desc,
                                visible);
                        if (desc.equals("LNotNull;")) {
                            params.add(new Integer(parameter));
                        }
                        return av;
                    }

                    public void visitCode() {
                        int var = (access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
                        for (int p = 0; p < params.size(); ++p) {
                            int param = ((Integer) params.get(p)).intValue();
                            for (int i = 0; i < param; ++i) {
                                var += args[i].getSize();
                            }
                            String c = "java/lang/IllegalArgumentException";
                            String d = "(Ljava/lang/String;)V";
                            Label end = new Label();
                            mv.visitVarInsn(Opcodes.ALOAD, var);
                            mv.visitJumpInsn(Opcodes.IFNONNULL, end);
                            mv.visitTypeInsn(Opcodes.NEW, c);
                            mv.visitInsn(Opcodes.DUP);
                            mv.visitLdcInsn("Argument " + param
                                    + " must not be null");
                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                                    c,
                                    "<init>",
                                    d);
                            mv.visitInsn(Opcodes.ATHROW);
                            mv.visitLabel(end);
                        }
                    }
                };
            }
        }, 0);

        Class c = new ClassLoader() {
            public Class loadClass(final String name)
                    throws ClassNotFoundException
            {
                if (name.equals(n)) {
                    byte[] b = cw.toByteArray();
                    return defineClass(name, b, 0, b.length);
                }
                return super.loadClass(name);
            }
        }.loadClass(n);

        System.out.println();
        System.out.println("Calling foo(null) on the transformed class results in an IllegalArgumentException:");
        Method m = c.getMethod("foo", new Class[] { String.class });
        try {
            m.invoke(null, new Object[] { null });
        } catch (InvocationTargetException e) {
            e.getCause().printStackTrace(System.out);
        }
    }
}
