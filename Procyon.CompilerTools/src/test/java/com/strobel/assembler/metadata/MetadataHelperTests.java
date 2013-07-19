package com.strobel.assembler.metadata;

import org.junit.Test;

import static com.strobel.assembler.metadata.MetadataHelper.*;
import static com.strobel.core.CollectionUtilities.*;
import static org.junit.Assert.*;

@SuppressWarnings("UnusedDeclaration")
public class MetadataHelperTests {
    private static TypeReference string() {
        return MetadataSystem.instance().resolveType("java/lang/String");
    }

    private static TypeReference charSequence() {
        return MetadataSystem.instance().resolveType("java/lang/CharSequence");
    }

    private static TypeReference integer() {
        return MetadataSystem.instance().resolveType("java/lang/Integer");
    }

    private static TypeReference list() {
        return MetadataSystem.instance().resolveType("java/util/List");
    }

    private static TypeReference arrayList() {
        return MetadataSystem.instance().resolveType("java/util/ArrayList");
    }

    private static TypeReference iterable() {
        return MetadataSystem.instance().resolveType("java/lang/Iterable");
    }

    @Test
    public void testIsSameTypeWithSimpleGenerics() throws Throwable {
/*
        final TypeReference arrayList = arrayList();
        final TypeReference rawArrayList = new RawType(arrayList());
        final TypeReference genericArrayList = arrayList().makeGenericType(string());

        assertTrue(isSameType(rawArrayList, genericArrayList, false));
        assertTrue(isSameType(genericArrayList, rawArrayList, false));

        assertFalse(isSameType(rawArrayList, genericArrayList, true));
        assertFalse(isSameType(genericArrayList, rawArrayList, true));

        assertTrue(isSameType(arrayList, arrayList, false));
        assertTrue(isSameType(rawArrayList, rawArrayList, false));
        assertTrue(isSameType(genericArrayList, genericArrayList, false));
        assertTrue(isSameType(arrayList, arrayList, true));
        assertTrue(isSameType(rawArrayList, rawArrayList, true));
        assertTrue(isSameType(genericArrayList, genericArrayList, true));

        assertFalse(isSameType(arrayList, rawArrayList, false));
        assertFalse(isSameType(arrayList, genericArrayList, false));
        assertFalse(isSameType(rawArrayList, arrayList, false));
        assertFalse(isSameType(genericArrayList, arrayList, false));

        assertFalse(isSameType(arrayList, rawArrayList, true));
        assertFalse(isSameType(arrayList, genericArrayList, true));
        assertFalse(isSameType(rawArrayList, arrayList, true));
        assertFalse(isSameType(genericArrayList, arrayList, true));
*/
    }

    @Test
    public void testAsSuperWithSimpleGenerics() throws Throwable {
        final TypeReference arrayList = arrayList();
        final TypeReference rawArrayList = new RawType(arrayList());
        final TypeReference genericArrayList = arrayList().makeGenericType(string());

        final TypeReference iterable = iterable();
        final TypeReference rawIterable = new RawType(iterable());
        final TypeReference genericIterable = iterable().makeGenericType(string());

        final TypeReference t1 = MetadataHelper.asSuper(arrayList, genericIterable);
        final TypeReference t2 = MetadataHelper.asSuper(genericArrayList, genericIterable);
        final TypeReference t3 = MetadataHelper.asSuper(rawArrayList, genericIterable);
        final TypeReference t4 = MetadataHelper.asSuper(arrayList, iterable);
        final TypeReference t5 = MetadataHelper.asSuper(genericArrayList, iterable);
        final TypeReference t6 = MetadataHelper.asSuper(rawArrayList, iterable);
        final TypeReference t7 = MetadataHelper.asSuper(arrayList, rawIterable);
        final TypeReference t8 = MetadataHelper.asSuper(genericArrayList, rawIterable);
        final TypeReference t9 = MetadataHelper.asSuper(rawArrayList, rawIterable);

        assertTrue(isSameType(t1, iterable.makeGenericType(single(arrayList.getGenericParameters())), true));
        assertTrue(isSameType(t2, genericIterable, true));
        assertTrue(isSameType(t3, rawIterable, true));
        assertTrue(isSameType(t4, iterable.makeGenericType(single(arrayList.getGenericParameters())), true));
        assertTrue(isSameType(t5, genericIterable, true));
        assertTrue(isSameType(t6, rawIterable, true));
        assertTrue(isSameType(t7, iterable.makeGenericType(single(arrayList.getGenericParameters())), true));
        assertTrue(isSameType(t8, genericIterable, true));
        assertTrue(isSameType(t9, rawIterable, true));
    }
}
