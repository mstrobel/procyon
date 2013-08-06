package com.strobel.assembler.metadata;

import com.strobel.compilerservices.RuntimeHelpers;
import org.junit.Test;

import static com.strobel.assembler.metadata.MetadataHelper.isSameType;
import static com.strobel.core.CollectionUtilities.single;
import static java.lang.String.format;
import static org.junit.Assert.*;

@SuppressWarnings("UnusedDeclaration")
public class MetadataHelperTests {
    static {
        RuntimeHelpers.ensureClassInitialized(MetadataSystem.class);
    }

    private static final boolean[][] IS_ASSIGNABLE_BIT_SET = {
        { true, true, false, true, true, true, true }, // byte
        { false, true, false, true, true, true, true }, // short
        { false, false, true, true, true, true, true }, // char
        { false, false, false, true, true, true, true }, // int
        { false, false, false, false, true, true, true }, // long
        { false, false, false, false, false, true, true }, // float
        { false, false, false, false, false, false, true }, // double
    };

    private static TypeReference string() {
        return MetadataSystem.instance().lookupTypeCore("java/lang/String");
    }

    private static TypeReference charSequence() {
        return MetadataSystem.instance().lookupTypeCore("java/lang/CharSequence");
    }

    private static TypeReference integer() {
        return MetadataSystem.instance().lookupTypeCore("java/lang/Integer");
    }

    private static TypeReference list() {
        return MetadataSystem.instance().lookupTypeCore("java/util/List");
    }

    private static TypeReference arrayList() {
        return MetadataSystem.instance().lookupTypeCore("java/util/ArrayList");
    }

    private static TypeReference iterable() {
        return MetadataSystem.instance().lookupTypeCore("java/lang/Iterable");
    }

    @Test
    public void testIsAssignableBetweenPrimitives() throws Throwable {
        final JvmType[] jvmTypes = JvmType.values();

        final TypeReference[] primitiveTypes = {
            BuiltinTypes.Byte,
            BuiltinTypes.Short,
            BuiltinTypes.Character,
            BuiltinTypes.Integer,
            BuiltinTypes.Long,
            BuiltinTypes.Float,
            BuiltinTypes.Double,
        };

        for (int i = 0, n = IS_ASSIGNABLE_BIT_SET.length; i < n; i++) {
            for (int j = 0; j < n; j++) {
                assertEquals(
                    format(
                        "%s (assignable from) %s",
                        primitiveTypes[i],
                        primitiveTypes[j],
                        IS_ASSIGNABLE_BIT_SET[j][i]
                    ),
                    MetadataHelper.isAssignableFrom(
                        primitiveTypes[i],
                        primitiveTypes[j]
                    ),
                    IS_ASSIGNABLE_BIT_SET[j][i]
                );
            }
        }
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

        final TypeReference t1 = MetadataHelper.asSuper(genericIterable, arrayList);
        final TypeReference t2 = MetadataHelper.asSuper(genericIterable, genericArrayList);
        final TypeReference t3 = MetadataHelper.asSuper(genericIterable, rawArrayList);
        final TypeReference t4 = MetadataHelper.asSuper(iterable, arrayList);
        final TypeReference t5 = MetadataHelper.asSuper(iterable, genericArrayList);
        final TypeReference t6 = MetadataHelper.asSuper(iterable, rawArrayList);
        final TypeReference t7 = MetadataHelper.asSuper(rawIterable, arrayList);
        final TypeReference t8 = MetadataHelper.asSuper(rawIterable, genericArrayList);
        final TypeReference t9 = MetadataHelper.asSuper(rawIterable, rawArrayList);

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

    @Test
    public void testAsSubTypeWithSimpleGenerics() throws Throwable {
        final TypeReference arrayList = arrayList();
        final TypeReference rawArrayList = new RawType(arrayList());
        final TypeReference genericArrayList = arrayList().makeGenericType(string());

        final TypeReference iterable = iterable();
        final TypeReference rawIterable = new RawType(iterable());
        final TypeReference genericIterable = iterable().makeGenericType(string());

        final TypeReference t1 = MetadataHelper.asSubType(arrayList, genericIterable);
        final TypeReference t2 = MetadataHelper.asSubType(genericArrayList, genericIterable);
        final TypeReference t3 = MetadataHelper.asSubType(rawArrayList, genericIterable);
        final TypeReference t4 = MetadataHelper.asSubType(arrayList, iterable);
        final TypeReference t5 = MetadataHelper.asSubType(genericArrayList, iterable);
        final TypeReference t6 = MetadataHelper.asSubType(rawArrayList, iterable);
        final TypeReference t7 = MetadataHelper.asSubType(arrayList, rawIterable);
        final TypeReference t8 = MetadataHelper.asSubType(genericArrayList, rawIterable);
        final TypeReference t9 = MetadataHelper.asSubType(rawArrayList, rawIterable);

        assertTrue(isSameType(t1, genericArrayList, true));
        assertTrue(isSameType(t2, genericArrayList, true));
        assertTrue(isSameType(t3, genericArrayList, true));
        assertTrue(isSameType(t4, arrayList.makeGenericType(single(iterable.getGenericParameters())), true));
        assertTrue(isSameType(t5, genericArrayList, true));
        assertTrue(isSameType(t6, arrayList.makeGenericType(single(iterable.getGenericParameters())), true));
        assertTrue(isSameType(t7, rawArrayList, true));
        assertTrue(isSameType(t8, genericArrayList, true));
        assertTrue(isSameType(t9, rawArrayList, true));
    }
}
