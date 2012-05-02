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

import junit.framework.TestCase;

/**
 * ClassReader unit tests.
 * 
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public class ClassReaderUnitTest extends TestCase implements Opcodes {

    public void testIllegalConstructorArgument() {
        try {
            new ClassReader((InputStream) null);
            fail();
        } catch (IOException e) {
        }
    }

    public void testGetItem() throws IOException {
        ClassReader cr = new ClassReader(getClass().getName());
        int item = cr.getItem(1);
        assertTrue(item >= 10);
        assertTrue(item < cr.header);
    }

    public void testReadByte() throws IOException {
        ClassReader cr = new ClassReader(getClass().getName());
        assertEquals(cr.b[0] & 0xFF, cr.readByte(0));
    }
    
    public void testGetAccess() throws Exception {
        String name = getClass().getName();
        assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, new ClassReader(name).getAccess());
    }
    
    public void testGetClassName() throws Exception {
        String name = getClass().getName();
        assertEquals(name.replace('.', '/'), new ClassReader(name).getClassName());
    }
    
    public void testGetSuperName() throws Exception {
        assertEquals(TestCase.class.getName().replace('.', '/'), new ClassReader(getClass().getName()).getSuperName());
        assertEquals(null, new ClassReader(Object.class.getName()).getSuperName());
    }

    public void testGetInterfaces() throws Exception {
        String[] interfaces = new ClassReader(getClass().getName()).getInterfaces();
        assertNotNull(interfaces);
        assertEquals(1, interfaces.length);
        assertEquals(Opcodes.class.getName().replace('.', '/'), interfaces[0]);
        
        interfaces = new ClassReader(Opcodes.class.getName()).getInterfaces();
        assertNotNull(interfaces);
    }
}
