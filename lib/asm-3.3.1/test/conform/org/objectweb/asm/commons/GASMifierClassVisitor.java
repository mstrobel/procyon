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

import java.io.FileInputStream;
import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.ASMifierClassVisitor;

/**
 * Sub class of the ASMifier class visitor used to test GeneratorAdapter.
 * 
 * @author Eric Bruneton
 */
public class GASMifierClassVisitor extends ASMifierClassVisitor {

    /**
     * Prints the ASM source code to generate the given class to the standard
     * output. <p> Usage: ASMifierClassVisitor [-debug] &lt;fully qualified
     * class name or class file name&gt;
     * 
     * @param args the command line arguments.
     * 
     * @throws Exception if the class cannot be found, or if an IO exception
     *         occurs.
     */
    public static void main(final String[] args) throws Exception {
        int i = 0;
        int flags = ClassReader.SKIP_DEBUG;

        boolean ok = true;
        if (args.length < 1 || args.length > 2) {
            ok = false;
        }
        if (ok && args[0].equals("-debug")) {
            i = 1;
            flags = 0;
            if (args.length != 2) {
                ok = false;
            }
        }
        if (!ok) {
            System.err.println("Prints the ASM code to generate the given class.");
            System.err.println("Usage: GASMifierClassVisitor [-debug] "
                    + "<fully qualified class name or class file name>");
            System.exit(-1);
        }
        ClassReader cr;
        if (args[i].endsWith(".class")) {
            cr = new ClassReader(new FileInputStream(args[i]));
        } else {
            cr = new ClassReader(args[i]);
        }
        cr.accept(new GASMifierClassVisitor(new PrintWriter(System.out)),
                getDefaultAttributes(),
                ClassReader.EXPAND_FRAMES | flags);
    }

    public GASMifierClassVisitor(final PrintWriter pw) {
        super(pw);
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
        int n;
        if (name.lastIndexOf('/') != -1) {
            n = 1;
        } else {
            n = 0;
        }
        text.set(n + 5,
                "ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);\n");
        text.set(n + 7, "GeneratorAdapter mg;\n");
        text.add(n + 1, "import org.objectweb.asm.commons.*;\n");
    }

    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
        buf.setLength(0);
        buf.append("{\n");
        buf.append("mg = new GeneratorAdapter(");
        buf.append(access);
        buf.append(", ");
        buf.append(GASMifierMethodVisitor.getMethod(name, desc));
        buf.append(", ");
        if (signature == null) {
            buf.append("null");
        } else {
            buf.append('"').append(signature).append('"');
        }
        buf.append(", ");
        if (exceptions != null && exceptions.length > 0) {
            buf.append("new Type[] {");
            for (int i = 0; i < exceptions.length; ++i) {
                buf.append(i == 0 ? " " : ", ");
                buf.append(GASMifierMethodVisitor.getType(exceptions[i]));
            }
            buf.append(" }");
        } else {
            buf.append("null");
        }
        buf.append(", cw);\n");
        text.add(buf.toString());
        GASMifierMethodVisitor acv = new GASMifierMethodVisitor(access, desc);
        text.add(acv.getText());
        text.add("}\n");
        return new LocalVariablesSorter(access, desc, acv);
    }
}
