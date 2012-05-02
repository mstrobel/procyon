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

import java.util.StringTokenizer;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;

/**
 * TraceSignatureVisitor unit tests.
 * 
 * @author Eugene Kuleshov
 */
public class TraceSignatureVisitorUnitTest extends TestCase {

    public final static String[] DATA = {
        "C|E|<E extends java.lang.Enum<E>> implements java.lang.Comparable<E>, java.io.Serializable"
                + "|<E:Ljava/lang/Enum<TE;>;>Ljava/lang/Object;Ljava/lang/Comparable<TE;>;Ljava/io/Serializable;",

        "C|I|<D extends java.lang.reflect.GenericDeclaration> extends java.lang.reflect.Type"
                + "|<D::Ljava/lang/reflect/GenericDeclaration;>Ljava/lang/Object;Ljava/lang/reflect/Type;",

        "C|C|<K, V> extends java.util.AbstractMap<K, V> implements java.util.concurrent.ConcurrentMap<K, V>, java.io.Serializable"
                + "|<K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/util/AbstractMap<TK;TV;>;Ljava/util/concurrent/ConcurrentMap<TK;TV;>;Ljava/io/Serializable;",

        "C|C|<K extends java.lang.Enum<K>, V> extends java.util.AbstractMap<K, V> implements java.io.Serializable, java.lang.Cloneable"
                + "|<K:Ljava/lang/Enum<TK;>;V:Ljava/lang/Object;>Ljava/util/AbstractMap<TK;TV;>;Ljava/io/Serializable;Ljava/lang/Cloneable;",

        "F|C|java.lang.Class<?>|Ljava/lang/Class<*>;",

        "F|C|java.lang.reflect.Constructor<T>|Ljava/lang/reflect/Constructor<TT;>;",

        "F|C|T[]|[TT;",

        "F|C|java.util.Hashtable<?, ?>|Ljava/util/Hashtable<**>;",

        "F|C|java.util.concurrent.atomic.AtomicReferenceFieldUpdater<java.io.BufferedInputStream, byte[]>"
                + "|Ljava/util/concurrent/atomic/AtomicReferenceFieldUpdater<Ljava/io/BufferedInputStream;[B>;",

        "F|C|AA<byte[][]>|LAA<[[B>;",

        "F|C|AA<java.util.Map<java.lang.String, java.lang.String>[][]>"
                + "|LAA<[[Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;>;",

        "F|C|java.util.Hashtable<java.lang.Object, java.lang.String>"
                + "|Ljava/util/Hashtable<Ljava/lang/Object;Ljava/lang/String;>;",

        "M|C|void(boolean, byte, char, short, int, float, long, double)"
                + "|(ZBCSIFJD)V",

        "M|C|void()E, F|()V^TE;^TF;",

        "M|C|java.lang.Class<? extends E><E extends java.lang.Class>()"
                + "|<E:Ljava/lang/Class;>()Ljava/lang/Class<+TE;>;",

        "M|C|java.lang.Class<? super E><E extends java.lang.Class>()"
                + "|<E:Ljava/lang/Class;>()Ljava/lang/Class<-TE;>;",

        "M|C|void(A<E>.B)|(LA<TE;>.B;)V",

        "M|C|void(A<E>.B<F>)|(LA<TE;>.B<TF;>;)V",

        "M|C|void(java.lang.String, java.lang.Class<?>, java.lang.reflect.Method[], java.lang.reflect.Method, java.lang.reflect.Method)"
                + "|(Ljava/lang/String;Ljava/lang/Class<*>;[Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)V",

        "M|C|java.util.Map<java.lang.Object, java.lang.String>(java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>)"
                + "|(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;",

        "M|C|java.util.Map<java.lang.Object, java.lang.String><T>(java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>, T)"
                + "|<T:Ljava/lang/Object;>(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;TT;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;",

        "M|C|java.util.Map<java.lang.Object, java.lang.String><E, T extends java.lang.Comparable<E>>(java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>, T)"
                + "|<E:Ljava/lang/Object;T::Ljava/lang/Comparable<TE;>;>(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;TT;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;", };

    public static TestSuite suite() {
        TestSuite suite = new TestSuite(TraceSignatureVisitorUnitTest.class.getName());
        for (int i = 0; i < DATA.length; i++) {
            suite.addTest(new TraceSignatureVisitorUnitTest(new TestData(DATA[i])));
        }
        return suite;
    }

    private TestData data;

    private TraceSignatureVisitorUnitTest(final TestData data) {
        super("testSignature");
        this.data = data;
    }

    public void testSignature() {
        TraceSignatureVisitor d = new TraceSignatureVisitor(data.access);
        SignatureReader r = new SignatureReader(data.signature);

        switch (data.type) {
            case 'C':
                r.accept(d);
                assertEquals(data.declaration, d.getDeclaration());
                break;
            case 'F':
                r.acceptType(d);
                assertEquals(data.declaration, d.getDeclaration());
                break;
            case 'M':
                r.accept(d);
                String fullMethodDeclaration = d.getReturnType()
                        + d.getDeclaration()
                        + (d.getExceptions() != null ? d.getExceptions() : "");
                assertEquals(data.declaration, fullMethodDeclaration);
                break;
        }
    }

    public String getName() {
        return super.getName() + " " + data.signature;
    }

    public static class TestData {

        public final char type;

        public final int access;

        public final String declaration;

        public final String signature;

        public TestData(final String data) {
            StringTokenizer st = new StringTokenizer(data, "|");
            this.type = st.nextToken().charAt(0);

            String acc = st.nextToken();
            switch (acc.charAt(0)) {
                case 'E':
                    this.access = Opcodes.ACC_ENUM;
                    break;
                case 'I':
                    this.access = Opcodes.ACC_INTERFACE;
                    break;
                case 'A':
                    this.access = Opcodes.ACC_ANNOTATION;
                    break;
                default:
                    this.access = 0;
            }

            this.declaration = st.nextToken();
            this.signature = st.nextToken();
        }
    }
}
