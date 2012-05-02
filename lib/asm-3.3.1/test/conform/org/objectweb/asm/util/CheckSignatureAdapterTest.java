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
package org.objectweb.asm.util;

import junit.framework.TestSuite;

import org.objectweb.asm.AbstractTest;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;

/**
 * CheckSignatureAdapter tests.
 * 
 * @author Eric Bruneton
 */
public class CheckSignatureAdapterTest extends AbstractTest {

    public static TestSuite suite() throws Exception {
        return new CheckSignatureAdapterTest().getSuite();
    }

    public void test() throws Exception {
        ClassReader cr = new ClassReader(is);
        cr.accept(new EmptyVisitor() {
            public void visit(
                int version,
                int access,
                String name,
                String signature,
                String superName,
                String[] interfaces)
            {
                if (signature != null) {
                    SignatureReader sr = new SignatureReader(signature);
                    SignatureWriter sw = new SignatureWriter();
                    sr.accept(new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE,
                            sw));
                    assertEquals(signature, sw.toString());
                }
            }

            public FieldVisitor visitField(
                int access,
                String name,
                String desc,
                String signature,
                Object value)
            {
                if (signature != null) {
                    SignatureReader sr = new SignatureReader(signature);
                    SignatureWriter sw = new SignatureWriter();
                    sr.acceptType(new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE,
                            sw));
                    assertEquals(signature, sw.toString());
                }
                return null;
            }

            public MethodVisitor visitMethod(
                int access,
                String name,
                String desc,
                String signature,
                String[] exceptions)
            {
                if (signature != null) {
                    SignatureReader sr = new SignatureReader(signature);
                    SignatureWriter sw = new SignatureWriter();
                    sr.accept(new CheckSignatureAdapter(CheckSignatureAdapter.METHOD_SIGNATURE,
                            sw));
                    assertEquals(signature, sw.toString());
                }
                return null;
            }

        }, 0);
    }
}
