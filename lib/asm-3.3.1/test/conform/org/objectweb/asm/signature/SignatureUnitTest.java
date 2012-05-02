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
package org.objectweb.asm.signature;

import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.util.TraceSignatureVisitorUnitTest;
import org.objectweb.asm.util.TraceSignatureVisitorUnitTest.TestData;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Signature tests.
 * 
 * @author Eugene Kuleshov
 * @author Eric Bruneton
 */
public class SignatureUnitTest extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite(SignatureUnitTest.class.getName());
        for (int i = 0; i < TraceSignatureVisitorUnitTest.DATA.length; i++) {
            suite.addTest(new SignatureUnitTest(new TestData(TraceSignatureVisitorUnitTest.DATA[i])));
        }
        return suite;
    }

    private TestData data;

    private SignatureUnitTest(final TestData data)
    {
        super("testSignature");
        this.data = data;
    }

    public void testSignature() {
        SignatureWriter wrt = new SignatureWriter();
        SignatureReader rdr = new SignatureReader(data.signature);
        switch (data.type) {
            case 'C':
            case 'M':
                rdr.accept(wrt);
                break;
            case 'F':
                rdr.acceptType(wrt);
                break;
            default:
                return;
        }
        assertEquals(data.signature, wrt.toString());
    }

    public String getName() {
        return super.getName() + " " + data.signature;
    }
}
