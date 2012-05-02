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
package org.objectweb.asm.commons;

import java.io.IOException;
import java.io.Serializable;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import junit.framework.TestCase;

/**
 * Test for the SerialVerionUid computation.
 * 
 * @author Alexandre Vasseur
 * @author Eric Bruneton
 */
public class SerialVersionUIDAdderUnitTest extends TestCase implements
        Serializable
{

    protected final static int aField = 32;

    static {
        System.gc();
    }

    public Object[] aMethod() {
        return null;
    }

    private long computeSerialVersionUID(final String className)
            throws IOException
    {
        final long[] svuid = new long[1];
        ClassVisitor cv = new SerialVersionUIDAdder(new EmptyVisitor()) {
            protected long computeSVUID() throws IOException {
                svuid[0] = super.computeSVUID();
                return svuid[0];
            }
        };
        new ClassReader(className).accept(cv, 0);
        return svuid[0];
    }

    public void test() throws Throwable {
        long UID = computeSerialVersionUID(getClass().getName());
        assertEquals(194753646298127968L, UID);
    }
}
