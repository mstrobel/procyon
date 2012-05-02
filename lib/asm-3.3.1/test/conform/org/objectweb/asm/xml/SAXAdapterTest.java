/***
 * ASM XML Adapter
 * Copyright (c) 2004, Eugene Kuleshov
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
package org.objectweb.asm.xml;

import java.io.ByteArrayOutputStream;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import junit.framework.TestSuite;

import org.objectweb.asm.AbstractTest;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * SAXAdapter tests
 * 
 * @author Eugene Kuleshov
 */
public class SAXAdapterTest extends AbstractTest {

    public static TestSuite suite() throws Exception {
        return new SAXAdapterTest().getSuite();
    }

    public void test() throws Exception {
        ClassReader cr = new ClassReader(is);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        SAXTransformerFactory saxtf = (SAXTransformerFactory) TransformerFactory.newInstance();
        TransformerHandler handler = saxtf.newTransformerHandler();
        handler.setResult(new SAXResult(new ASMContentHandler(bos, false)));
        handler.startDocument();
        cr.accept(new SAXClassAdapter(handler, false), 0);
        handler.endDocument();

        ClassWriter cw = new ClassWriter(0);
        cr.accept(cw, new Attribute[] { new Attribute("Comment") {
            protected Attribute read(
                final ClassReader cr,
                final int off,
                final int len,
                final char[] buf,
                final int codeOff,
                final Label[] labels)
            {
                return null; // skip these attributes
            }
        },
            new Attribute("CodeComment") {
                protected Attribute read(
                    final ClassReader cr,
                    final int off,
                    final int len,
                    final char[] buf,
                    final int codeOff,
                    final Label[] labels)
                {
                    return null; // skip these attributes
                }
            } }, 0);

        assertEquals(new ClassReader(cw.toByteArray()),
                new ClassReader(bos.toByteArray()));
    }
}
