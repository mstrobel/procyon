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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Generates an annotation class with values of all types and a class using it.
 * 
 * @author Eric Bruneton
 */
public class Annotation extends Generator {

    final static int M = ACC_PUBLIC + ACC_ABSTRACT;

    final static String STRING = "Ljava/lang/String;";

    final static String CLASS = "Ljava/lang/Class;";

    final static String DOC = "Ljava/lang/annotation/Documented;";

    final static String DEPRECATED = "Ljava/lang/Deprecated;";

    public void generate(final String dir) throws IOException {
        generate(dir, "pkg/Annotation.class", dumpAnnotation());
        generate(dir, "pkg/Annotated.class", dumpAnnotated());
    }

    public byte[] dumpAnnotation() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        MethodVisitor mv;
        AnnotationVisitor av0, av1;

        cw.visit(V1_5,
                ACC_PUBLIC + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE,
                "pkg/Annotation",
                null,
                "java/lang/Object",
                new String[] { "java/lang/annotation/Annotation" });

        mv = cw.visitMethod(M, "byteValue", "()B", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new Byte((byte) 1));
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "charValue", "()C", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new Character((char) 1));
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "booleanValue", "()Z", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, Boolean.TRUE);
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "intValue", "()I", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new Integer(1));
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "shortValue", "()S", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new Short((short) 1));
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "longValue", "()J", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new Long(1L));
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "floatValue", "()F", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new Float("1.0"));
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "doubleValue", "()D", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new Double("1.0"));
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "stringValue", "()" + STRING, null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, "1");
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "classValue", "()" + CLASS, null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, Type.getType("Lpkg/Annotation;"));
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "enumValue", "()Lpkg/Enum;", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visitEnum(null, "Lpkg/Enum;", "V1");
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "annotationValue", "()" + DOC, null, null);
        av0 = mv.visitAnnotationDefault();
        av1 = av0.visitAnnotation(null, DOC);
        av1.visitEnd();
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "byteArrayValue", "()[B", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new byte[] { 0, 1 });
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "charArrayValue", "()[C", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new char[] { '0', '1' });
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "booleanArrayValue", "()[Z", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new boolean[] { false, true });
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "intArrayValue", "()[I", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new int[] { 0, 1 });
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "shortArrayValue", "()[S", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new short[] { (short) 0, (short) 1 });
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "longArrayValue", "()[J", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new long[] { 0L, 1L });
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "floatArrayValue", "()[F", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new float[] { 0.0f, 1.0f });
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "doubleArrayValue", "()[D", null, null);
        av0 = mv.visitAnnotationDefault();
        av0.visit(null, new double[] { 0.0d, 1.0d });
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "stringArrayValue", "()" + STRING, null, null);
        av0 = mv.visitAnnotationDefault();
        av1 = av0.visitArray(null);
        av1.visit(null, "0");
        av1.visit(null, "1");
        av1.visitEnd();
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "classArrayValue", "()[" + CLASS, null, null);
        av0 = mv.visitAnnotationDefault();
        av1 = av0.visitArray(null);
        av1.visit(null, Type.getType("Lpkg/Annotation;"));
        av1.visit(null, Type.getType("Lpkg/Annotation;"));
        av1.visitEnd();
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "enumArrayValue", "()[Lpkg/Enum;", null, null);
        av0 = mv.visitAnnotationDefault();
        av1 = av0.visitArray(null);
        av1.visitEnum(null, "Lpkg/Enum;", "V0");
        av1.visitEnum(null, "Lpkg/Enum;", "V1");
        av1.visitEnd();
        av0.visitEnd();
        mv.visitEnd();

        mv = cw.visitMethod(M, "annotationArrayValue", "()[" + DOC, null, null);
        av0 = mv.visitAnnotationDefault();
        av1 = av0.visitArray(null);
        av1.visitAnnotation(null, DOC).visitEnd();
        av1.visitAnnotation(null, DOC).visitEnd();
        av1.visitEnd();
        av0.visitEnd();
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }

    public byte[] dumpAnnotated() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0, av1;

        cw.visit(V1_5,
                ACC_PUBLIC + ACC_SUPER,
                "pkg/Annotated",
                null,
                "java/lang/Object",
                null);

        // visible class annotation
        cw.visitAnnotation(DEPRECATED, true).visitEnd();

        // invisible class annotation, with values of all types
        av0 = cw.visitAnnotation("Lpkg/Annotation;", false);
        av0.visit("byteValue", new Byte((byte) 0));
        av0.visit("charValue", new Character((char) 48));
        av0.visit("booleanValue", Boolean.FALSE);
        av0.visit("intValue", new Integer(0));
        av0.visit("shortValue", new Short((short) 0));
        av0.visit("longValue", new Long(0L));
        av0.visit("floatValue", new Float("0.0"));
        av0.visit("doubleValue", new Double("0.0"));
        av0.visit("stringValue", "0");
        av0.visitEnum("enumValue", "Lpkg/Enum;", "V0");
        av0.visitAnnotation("annotationValue", DOC).visitEnd();
        av0.visit("classValue", Type.getType("Lpkg/Annotation;"));
        av0.visit("byteArrayValue", new byte[] { 1, 0 });
        av0.visit("charArrayValue", new char[] { '1', '0' });
        av0.visit("booleanArrayValue", new boolean[] { true, false });
        av0.visit("intArrayValue", new int[] { 1, 0 });
        av0.visit("shortArrayValue", new short[] { (short) 1, (short) 0 });
        av0.visit("longArrayValue", new long[] { 1L, 0L });
        av0.visit("floatArrayValue", new float[] { 1.0f, 0.0f });
        av0.visit("doubleArrayValue", new double[] { 1.0d, 0.0d });
        av1 = av0.visitArray("stringArrayValue");
        av1.visit(null, "1");
        av1.visit(null, "0");
        av1.visitEnd();
        av0.visitArray("classArrayValue").visitEnd();
        av1 = av0.visitArray("enumArrayValue");
        av1.visitEnum(null, "Lpkg/Enum;", "V1");
        av1.visitEnum(null, "Lpkg/Enum;", "V2");
        av1.visitEnd();
        av0.visitArray("annotationArrayValue").visitEnd();
        av0.visitEnd();

        fv = cw.visitField(ACC_PUBLIC, "f", "I", null, null);
        // visible field annotation
        fv.visitAnnotation(DEPRECATED, true).visitEnd();
        // invisible field annotation
        av0 = fv.visitAnnotation("Lpkg/Annotation;", false);
        av0.visitEnum("enumValue", "Lpkg/Enum;", "V0");
        av0.visitEnd();
        fv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(IIIIIIIIII)V", null, null);
        // visible method annotation
        mv.visitAnnotation(DEPRECATED, true).visitEnd();
        // invisible method annotation
        av0 = mv.visitAnnotation("Lpkg/Annotation;", false);
        av0.visitAnnotation("annotationValue", DOC).visitEnd();
        av0.visitEnd();
        // synthetic parameter annotation
        mv.visitParameterAnnotation(0, "Ljava/lang/Synthetic;", false);
        // visible parameter annotation
        mv.visitParameterAnnotation(8, DEPRECATED, true).visitEnd();
        // invisible parameter annotation
        av0 = mv.visitParameterAnnotation(8, "Lpkg/Annotation;", false);
        av0.visitArray("stringArrayValue").visitEnd();
        av0.visitEnd();
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        return cw.toByteArray();
    }
}
