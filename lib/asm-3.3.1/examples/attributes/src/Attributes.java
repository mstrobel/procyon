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
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Label;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.FileOutputStream;
import java.io.PrintWriter;

/**
 * @author Eric Bruneton
 */
public class Attributes extends ClassLoader {

    public static void main(final String args[]) throws Exception {
        // loads the original class and adapts it
        ClassReader cr = new ClassReader("CommentAttribute");
        ClassWriter cw = new ClassWriter(0);
        ClassVisitor cv = new AddCommentClassAdapter(cw);
        cr.accept(cv, new Attribute[] { new CommentAttribute("") }, 0);
        byte[] b = cw.toByteArray();

        // stores the adapted class on disk
        FileOutputStream fos = new FileOutputStream("CommentAttribute.class.new");
        fos.write(b);
        fos.close();

        // "disassembles" the adapted class
        cr = new ClassReader(b);
        cv = new TraceClassVisitor(new PrintWriter(System.out));
        cr.accept(cv, new Attribute[] { new CommentAttribute("") }, 0);
    }
}

class AddCommentClassAdapter extends ClassAdapter implements Opcodes {

    public AddCommentClassAdapter(final ClassVisitor cv) {
        super(cv);
    }

    public void visit(
        final int version,
        final int access,
        final String name,
        final String signature,
        final String superName,
        final String[] interfaces)
    {
        super.visit(version, access, name, signature, superName, interfaces);
        visitAttribute(new CommentAttribute("this is a class comment"));
    }

    public FieldVisitor visitField(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final Object value)
    {
        FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        fv.visitAttribute(new CommentAttribute("this is a field comment"));
        return fv;
    }

    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
        MethodVisitor mv = cv.visitMethod(access,
                name,
                desc,
                signature,
                exceptions);
        if (mv != null) {
            mv.visitAttribute(new CommentAttribute("this is a method comment"));
        }
        return mv;
    }
}

class CommentAttribute extends Attribute {

    private String comment;

    public CommentAttribute(final String comment) {
        super("Comment");
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public boolean isUnknown() {
        return false;
    }

    protected Attribute read(
        final ClassReader cr,
        final int off,
        final int len,
        final char[] buf,
        final int codeOff,
        final Label[] labels)
    {
        return new CommentAttribute(cr.readUTF8(off, buf));
    }

    protected ByteVector write(
        final ClassWriter cw,
        final byte[] code,
        final int len,
        final int maxStack,
        final int maxLocals)
    {
        return new ByteVector().putShort(cw.newUTF8(comment));
    }
}
