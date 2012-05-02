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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Type unit tests.
 * 
 * @author Eric Bruneton
 */
public class TypeUnitTest extends TestCase implements Opcodes {

    public void testConstants() {
        assertEquals(Type.INT_TYPE, Type.getType(Integer.TYPE));
        assertEquals(Type.VOID_TYPE, Type.getType(Void.TYPE));
        assertEquals(Type.BOOLEAN_TYPE, Type.getType(Boolean.TYPE));
        assertEquals(Type.BYTE_TYPE, Type.getType(Byte.TYPE));
        assertEquals(Type.CHAR_TYPE, Type.getType(Character.TYPE));
        assertEquals(Type.SHORT_TYPE, Type.getType(Short.TYPE));
        assertEquals(Type.DOUBLE_TYPE, Type.getType(Double.TYPE));
        assertEquals(Type.FLOAT_TYPE, Type.getType(Float.TYPE));
        assertEquals(Type.LONG_TYPE, Type.getType(Long.TYPE));
    }

    public void testInternalName() {
        String s1 = Type.getType(TypeUnitTest.class).getInternalName();
        String s2 = Type.getInternalName(TypeUnitTest.class);
        assertEquals(s1, s2);
    }

    public void testConstructorDescriptor() {
        for (int i = 0; i < String.class.getConstructors().length; ++i) {
            Constructor c = String.class.getConstructors()[i];
            Type.getConstructorDescriptor(c);
        }
    }

    public void testMethodDescriptor() {
        for (int i = 0; i < Arrays.class.getMethods().length; ++i) {
            Method m = Arrays.class.getMethods()[i];
            Type[] args = Type.getArgumentTypes(m);
            Type r = Type.getReturnType(m);
            String d1 = Type.getMethodDescriptor(r, args);
            String d2 = Type.getMethodDescriptor(m);
            assertEquals(d1, d2);
        }
    }

    public void testGetOpcode() {
        Type object = Type.getType("Ljava/lang/Object;");
        assertEquals(BALOAD, Type.BOOLEAN_TYPE.getOpcode(IALOAD));
        assertEquals(BALOAD, Type.BYTE_TYPE.getOpcode(IALOAD));
        assertEquals(CALOAD, Type.CHAR_TYPE.getOpcode(IALOAD));
        assertEquals(SALOAD, Type.SHORT_TYPE.getOpcode(IALOAD));
        assertEquals(IALOAD, Type.INT_TYPE.getOpcode(IALOAD));
        assertEquals(FALOAD, Type.FLOAT_TYPE.getOpcode(IALOAD));
        assertEquals(LALOAD, Type.LONG_TYPE.getOpcode(IALOAD));
        assertEquals(DALOAD, Type.DOUBLE_TYPE.getOpcode(IALOAD));
        assertEquals(AALOAD, object.getOpcode(IALOAD));
        assertEquals(IADD, Type.BOOLEAN_TYPE.getOpcode(IADD));
        assertEquals(IADD, Type.BYTE_TYPE.getOpcode(IADD));
        assertEquals(IADD, Type.CHAR_TYPE.getOpcode(IADD));
        assertEquals(IADD, Type.SHORT_TYPE.getOpcode(IADD));
        assertEquals(IADD, Type.INT_TYPE.getOpcode(IADD));
        assertEquals(FADD, Type.FLOAT_TYPE.getOpcode(IADD));
        assertEquals(LADD, Type.LONG_TYPE.getOpcode(IADD));
        assertEquals(DADD, Type.DOUBLE_TYPE.getOpcode(IADD));
    }

    public void testHashcode() {
        Type.getType("Ljava/lang/Object;").hashCode();
    }
    
    public void testObjectType() throws Exception {
        Type t1 = Type.getObjectType("java/lang/Object");
        Type t2 = Type.getType("Ljava/lang/Object;");
        assertEquals(t2.getSort(), t1.getSort());
        assertEquals(t2.getClassName(), t1.getClassName());
        assertEquals(t2.getDescriptor(), t1.getDescriptor());
    }
}
