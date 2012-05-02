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
package org.objectweb.asm;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.util.TraceClassVisitor;

import junit.framework.TestSuite;

/**
 * ClassWriter tests.
 * 
 * @author Eric Bruneton
 */
public class ClassWriterComputeFramesTest extends AbstractTest {

    public static void premain(
        final String agentArgs,
        final Instrumentation inst)
    {
        inst.addTransformer(new ClassFileTransformer() {
            public byte[] transform(
                final ClassLoader loader,
                final String className,
                final Class classBeingRedefined,
                final ProtectionDomain domain,
                final byte[] classFileBuffer)
                    throws IllegalClassFormatException
            {
                String n = className.replace('/', '.');
                if (n.indexOf("junit") != -1) {
                    return null;
                }
                if (agentArgs.length() == 0 || n.indexOf(agentArgs) != -1) {
                    return transformClass(n, classFileBuffer);
                } else {
                    return null;
                }
            }
        });
    }

    static byte[] transformClass(final String n, final byte[] clazz) {
        ClassReader cr = new ClassReader(clazz);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            protected String getCommonSuperClass(
                final String type1,
                final String type2)
            {
                if (n.equals("pkg.Frames")) {
                    return super.getCommonSuperClass(type1, type2);
                }
                ClassInfo c, d;
                try {
                    c = new ClassInfo(type1, getClass().getClassLoader());
                    d = new ClassInfo(type2, getClass().getClassLoader());
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                if (c.isAssignableFrom(d)) {
                    return type1;
                }
                if (d.isAssignableFrom(c)) {
                    return type2;
                }
                if (c.isInterface() || d.isInterface()) {
                    return "java/lang/Object";
                } else {
                    do {
                        c = c.getSuperclass();
                    } while (!c.isAssignableFrom(d));
                    return c.getType().getInternalName();
                }
            }
        };
        cr.accept(new ClassAdapter(cw) {

            public void visit(
                final int version,
                final int access,
                final String name,
                final String signature,
                final String superName,
                final String[] interfaces)
            {
                super.visit(Opcodes.V1_6,
                        access,
                        name,
                        signature,
                        superName,
                        interfaces);
            }

        }, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    public static TestSuite suite() throws Exception {
        return new ClassWriterComputeFramesTest().getSuite();
    }

    public void test() throws Exception {
        try {
            Class.forName(n, true, getClass().getClassLoader());
        } catch (NoClassDefFoundError ncdfe) {
            // ignored
        } catch (UnsatisfiedLinkError ule) {
            // ignored
        } catch (ClassFormatError cfe) {
            fail(cfe.getMessage());
        } catch (VerifyError ve) {
            String s = n.replace('.', '/') + ".class";
            InputStream is = getClass().getClassLoader().getResourceAsStream(s);
            ClassReader cr = new ClassReader(is);
            byte[] b = transformClass("", cr.b);
            StringWriter sw1 = new StringWriter();
            StringWriter sw2 = new StringWriter();
            sw2.write(ve.toString() + "\n");
            ClassVisitor cv1 = new TraceClassVisitor(new PrintWriter(sw1));
            ClassVisitor cv2 = new TraceClassVisitor(new PrintWriter(sw2));
            cr.accept(cv1, 0);
            new ClassReader(b).accept(cv2, 0);
            String s1 = sw1.toString();
            String s2 = sw2.toString();
            assertEquals("different data", s1, s2);
        }
    }
}

/**
 * @author Eugene Kuleshov
 */
class ClassInfo {

    private Type type;

    private ClassLoader loader;

    int access;

    String superClass;

    String[] interfaces;

    public ClassInfo(final String type, final ClassLoader loader) {
        this.loader = loader;
        this.type = Type.getObjectType(type);
        String s = type.replace('.', '/') + ".class";
        InputStream is = null;
        ClassReader cr;
        try {
            is = loader.getResourceAsStream(s);
            cr = new ClassReader(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }

        // optimized version
        int h = cr.header;
        ClassInfo.this.access = cr.readUnsignedShort(h);
        char[] buf = new char[2048];
        // String name = cr.readClass( cr.header + 2, buf);

        int v = cr.getItem(cr.readUnsignedShort(h + 4));
        ClassInfo.this.superClass = v == 0 ? null : cr.readUTF8(v, buf);
        ClassInfo.this.interfaces = new String[cr.readUnsignedShort(h + 6)];
        h += 8;
        for (int i = 0; i < interfaces.length; ++i) {
            interfaces[i] = cr.readClass(h, buf);
            h += 2;
        }
    }

    String getName() {
        return type.getInternalName();
    }

    Type getType() {
        return type;
    }

    int getModifiers() {
        return access;
    }

    ClassInfo getSuperclass() {
        if (superClass == null) {
            return null;
        }
        return new ClassInfo(superClass, loader);
    }

    ClassInfo[] getInterfaces() {
        if (interfaces == null) {
            return new ClassInfo[0];
        }
        ClassInfo[] result = new ClassInfo[interfaces.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = new ClassInfo(interfaces[i], loader);
        }
        return result;
    }

    boolean isInterface() {
        return (getModifiers() & Opcodes.ACC_INTERFACE) > 0;
    }

    private boolean implementsInterface(final ClassInfo that) {
        for (ClassInfo c = this; c != null; c = c.getSuperclass()) {
            ClassInfo[] tis = c.getInterfaces();
            for (int i = 0; i < tis.length; ++i) {
                ClassInfo ti = tis[i];
                if (ti.type.equals(that.type) || ti.implementsInterface(that)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSubclassOf(final ClassInfo that) {
        for (ClassInfo c = this; c != null; c = c.getSuperclass()) {
            if (c.getSuperclass() != null
                    && c.getSuperclass().type.equals(that.type))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isAssignableFrom(final ClassInfo that) {
        if (this == that) {
            return true;
        }

        if (that.isSubclassOf(this)) {
            return true;
        }

        if (that.implementsInterface(this)) {
            return true;
        }

        if (that.isInterface()
                && getType().getDescriptor().equals("Ljava/lang/Object;"))
        {
            return true;
        }

        return false;
    }
}
