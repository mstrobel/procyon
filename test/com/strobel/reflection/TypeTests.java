/*
 * TypeTests.java
 *
 * Copyright (c) 2012 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.reflection;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Mike Strobel
 */
public final class TypeTests {
    private static class StringList extends ArrayList<String> {}
    private final static class ExtendsStringList extends StringList {}
    
    @Test
    public void testGenericAssignmentCompatibility() throws Throwable {
        final Type<Enum> e = Types.Enum;
        final Type<MemberType> m = Type.of(MemberType.class);
        final Type<List> l = Types.List;
        final Type<Iterable> i = Types.Iterable;
        final Type<ArrayList> a = Types.ArrayList;
        final Type<CharSequence> c = Type.of(CharSequence.class);
        final Type<String> s = Types.String;
        final Type<StringList> sl = Type.of(StringList.class);
        final Type<ExtendsStringList> esl = Type.of(ExtendsStringList.class);

        // Enum<E extends Enum<E>> e = MemberType.All;
        assertTrue(e.isAssignableFrom(m));

        // Enum e = MemberType.All;
        assertTrue(e.getErasedType().isAssignableFrom(m));

        // Enum<?> e = MemberType.All;
        assertTrue(e.makeGenericType(Type.makeWildcard()).isAssignableFrom(m));

        // Enum<? extends Enum> e = MemberType.All;
        assertTrue(e.makeGenericType(Type.makeExtendsWildcard(e.getErasedType())).isAssignableFrom(m));

        // Enum<? extends Enum<?>> e = MemberType.All;
        assertTrue(e.makeGenericType(Type.makeExtendsWildcard(e.makeGenericType(Type.makeWildcard()))).isAssignableFrom(m));

        // Enum<MemberType> e = MemberType.All;
        assertTrue(e.makeGenericType(m).isAssignableFrom(m));

        // Enum<? extends MemberType> e = MemberType.All;
        assertTrue(e.makeGenericType(Type.makeExtendsWildcard(m)).isAssignableFrom(m));

        // List l = new ArrayList<String>();
        assertTrue(l.getErasedType().isAssignableFrom(a.makeGenericType(s)));

        // List<CharSequence> l = new ArrayList<String>();
        assertFalse(l.makeGenericType(c).isAssignableFrom(a.makeGenericType(s)));

        // Iterable i = new ArrayList<String>();
        assertTrue(i.getErasedType().isAssignableFrom(a.makeGenericType(s)));

        // Iterable i = new ArrayList();
        assertTrue(i.getErasedType().isAssignableFrom(a.getErasedType()));

        // Iterable<CharSequence> i = new ArrayList<String>();
        assertFalse(i.makeGenericType(c).isAssignableFrom(a.makeGenericType(s)));

        // Iterable<CharSequence> i = new StringList();
        assertFalse(i.makeGenericType(c).isAssignableFrom(sl));

        // Iterable<CharSequence> i = new ExtendsStringList();
        assertFalse(i.makeGenericType(c).isAssignableFrom(esl));

        // Iterable<CharSequence> i = new ArrayList<String>();
        assertFalse(i.makeGenericType(c).isAssignableFrom(a.makeGenericType(s)));

        // Iterable<CharSequence> i = new StringList();
        assertFalse(i.makeGenericType(c).isAssignableFrom(sl));

        // Iterable<CharSequence> i = new ExtendsStringList();
        assertFalse(i.makeGenericType(c).isAssignableFrom(esl));

        // List<? extends CharSequence> l = new ArrayList<String>();
        assertTrue(l.makeGenericType(Type.makeExtendsWildcard(c)).isAssignableFrom(a.makeGenericType(s)));

        // Iterable<? extends CharSequence> i = new ArrayList<String>();
        assertTrue(i.makeGenericType(Type.makeExtendsWildcard(c)).isAssignableFrom(a.makeGenericType(s)));

        // Iterable<? extends CharSequence> i = new StringList();
        assertTrue(i.makeGenericType(Type.makeExtendsWildcard(c)).isAssignableFrom(sl));

        // Iterable<? extends CharSequence> i = new ExtendsStringList();
        assertTrue(i.makeGenericType(Type.makeExtendsWildcard(c)).isAssignableFrom(esl));

        // Iterable<? extends CharSequence> i = new ArrayList<String>();
        assertTrue(i.makeGenericType(Type.makeExtendsWildcard(c)).isAssignableFrom(a.makeGenericType(s)));

        // Iterable<? extends CharSequence> i = new StringList();
        assertTrue(i.makeGenericType(Type.makeExtendsWildcard(c)).isAssignableFrom(sl));

        // Iterable<? extends CharSequence> i = new ExtendsStringList();
        assertTrue(i.makeGenericType(Type.makeExtendsWildcard(c)).isAssignableFrom(esl));

        // List<?> l = new ArrayList<String>();
        assertTrue(l.makeGenericType(Type.makeWildcard()).isAssignableFrom(a.makeGenericType(s)));

        // Iterable<?> i = new ArrayList<String>();
        assertTrue(i.makeGenericType(Type.makeWildcard()).isAssignableFrom(a.makeGenericType(s)));

        // Iterable<?> i = new StringList();
        assertTrue(i.makeGenericType(Type.makeWildcard()).isAssignableFrom(sl));

        // Iterable<?> i = new ExtendsStringList();
        assertTrue(i.makeGenericType(Type.makeWildcard()).isAssignableFrom(esl));

        // Iterable<?> i = new ArrayList<String>();
        assertTrue(i.makeGenericType(Type.makeWildcard()).isAssignableFrom(a.makeGenericType(s)));

        // Iterable<?> i = new StringList();
        assertTrue(i.makeGenericType(Type.makeWildcard()).isAssignableFrom(sl));

        // Iterable<?> i = new ExtendsStringList();
        assertTrue(i.makeGenericType(Type.makeWildcard()).isAssignableFrom(esl));

        // List<String> l = new ArrayList<CharSequence>();
        assertFalse(l.makeGenericType(s).isAssignableFrom(a.makeGenericType(c)));

        // ArrayList<?> a; List<? extends CharSequence> l = a;
        assertFalse(l.makeGenericType(Type.makeExtendsWildcard(c)).isAssignableFrom(a.makeGenericType(Type.makeWildcard())));

        // ArrayList<CharSequence> a; List<? super String> l = a;
        assertTrue(l.makeGenericType(Type.makeSuperWildcard(s)).isAssignableFrom(a.makeGenericType(c)));

        // ArrayList<? extends CharSequence> a; List<? super String> l = a;
        assertFalse(l.makeGenericType(Type.makeSuperWildcard(s)).isAssignableFrom(a.makeGenericType(Type.makeExtendsWildcard(c))));

        // ArrayList<?> a; List<? super String> l = a;
        assertFalse(l.makeGenericType(Type.makeSuperWildcard(s)).isAssignableFrom(a.makeGenericType(Type.makeWildcard())));
        
        // ArrayList<T> a; List<String> l = a;
        assertFalse(l.makeGenericType(s).isAssignableFrom(a));
        
        // ArrayList<String> a; List<T> l = a;
        assertTrue(l.isAssignableFrom(a.makeGenericType(s)));

        // ArrayList<? extends String> a; List<T> l = a;
        assertTrue(l.isAssignableFrom(a.makeGenericType(Type.makeExtendsWildcard(s))));

        // ArrayList<? super String> a; List<T> l = a;
        assertTrue(l.isAssignableFrom(a.makeGenericType(Type.makeSuperWildcard(s))));

        // ArrayList<?> a; List<T> l = a;
        assertTrue(l.isAssignableFrom(a.makeGenericType(Type.makeWildcard())));

        // ArrayList a; List l = a;
        assertTrue(l.getErasedType().isAssignableFrom(a.getErasedType()));
    }
}
