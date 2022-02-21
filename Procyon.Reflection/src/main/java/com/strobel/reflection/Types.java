/*
 * Types.java
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

import com.strobel.core.BooleanBox;
import com.strobel.core.ByteBox;
import com.strobel.core.CharacterBox;
import com.strobel.core.Comparer;
import com.strobel.core.DoubleBox;
import com.strobel.core.FloatBox;
import com.strobel.core.IntegerBox;
import com.strobel.core.LongBox;
import com.strobel.core.ShortBox;
import com.strobel.core.StrongBox;

import java.io.Serializable;
import java.lang.Error;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Callable;

import static com.strobel.reflection.Type.of;
/**
 * @author Mike Strobel
 */
public final class Types {

    private Types() {}

    public static final Type<Object> Object;
    public static final Type<java.util.Objects> Objects;

    public static final Type<Type> Type;
    public static final Type<Enum> Enum;
    public static final Type<Number> Number;
    public static final Type<Boolean> Boolean;
    public static final Type<Byte> Byte;
    public static final Type<Character> Character;
    public static final Type<Short> Short;
    public static final Type<Integer> Integer;
    public static final Type<Long> Long;
    public static final Type<Float> Float;
    public static final Type<Double> Double;
    public static final Type<String> String;
    public static final Type<Date> Date;
    public static final Type<UUID> UUID;

    public static final Type<Comparer> Comparer;
    
    public static final Type<Runnable> Runnable;
    public static final Type<Callable> Callable;

    public static final Type<Error> Error;
    public static final Type<Throwable> Throwable;
    public static final Type<Exception> Exception;
    public static final Type<RuntimeException> RuntimeException;
    public static final Type<IllegalStateException> IllegalStateException;
    public static final Type<IllegalArgumentException> IllegalArgumentException;

    public static final Type<StringBuilder> StringBuilder;
    public static final Type<StringBuffer> StringBuffer;

    public static final Type<BigInteger> BigInteger;
    public static final Type<BigDecimal> BigDecimal;

    public static final Type<System> System;

    public static final Type<java.lang.annotation.Annotation> Annotation;
    public static final Type<Class> Class;
    public static final Type<ClassLoader> ClassLoader;

    public static final Type<Serializable> Serializable;
    public static final Type<Cloneable> Cloneable;
    public static final Type<Comparable> Comparable;

    public static final Type<Iterable> Iterable;
    public static final Type<Iterator> Iterator;
    public static final Type<Collection> Collection;
    public static final Type<List> List;
    public static final Type<Set> Set;
    public static final Type<Map> Map;
    public static final Type<ArrayList> ArrayList;
    public static final Type<HashMap> HashMap;
    public static final Type<HashSet> HashSet;

    public static final Type<MethodHandle> MethodHandle;

    public static final Type<StrongBox> StrongBox;
    public static final Type<BooleanBox> BooleanBox;
    public static final Type<CharacterBox> CharacterBox;
    public static final Type<ByteBox> ByteBox;
    public static final Type<ShortBox> ShortBox;
    public static final Type<IntegerBox> IntegerBox;
    public static final Type<LongBox> LongBox;
    public static final Type<FloatBox> FloatBox;
    public static final Type<DoubleBox> DoubleBox;

    static {
        Type = of(Type.class);
        Objects = of(Objects.class);
        Object = of(Object.class);
        Enum = of(Enum.class);
        Number = of(Number.class);
        Boolean = of(Boolean.class);
        Byte = of(Byte.class);
        Character = of(Character.class);
        Short = of(Short.class);
        Integer = of(Integer.class);
        Long = of(Long.class);
        Float = of(Float.class);
        Double = of(Double.class);
        String = of(String.class);
        Date = of(Date.class);
        UUID = of(UUID.class);

        Comparer = of(Comparer.class);

        Runnable = of(Runnable.class);
        Callable = of(Callable.class);

        Error = of(java.lang.Error.class);
        Throwable = of(Throwable.class);
        Exception = of(Exception.class);
        RuntimeException = of(RuntimeException.class);
        IllegalStateException = of(IllegalStateException.class);
        IllegalArgumentException = of(IllegalArgumentException.class);

        StringBuffer = of(StringBuffer.class);
        StringBuilder = of(StringBuilder.class);

        BigInteger = of(BigInteger.class);
        BigDecimal = of(BigDecimal.class);

        System = of(System.class);

        Annotation = of(Annotation.class);
        Class = of(Class.class);
        ClassLoader = of(ClassLoader.class);

        Serializable = of(Serializable.class);
        Cloneable = of(Cloneable.class);
        Comparable = of(Comparable.class);

        Iterable = of(Iterable.class);
        Iterator = of(Iterator.class);
        Collection = of(Collection.class);
        List = of(List.class);
        Set = of(Set.class);
        Map = of(Map.class);
        ArrayList = of(ArrayList.class);
        HashMap = of(HashMap.class);
        HashSet = of(HashSet.class);

        MethodHandle = of(MethodHandle.class);

        StrongBox = of(StrongBox.class);
        BooleanBox = of(BooleanBox.class);
        CharacterBox = of(CharacterBox.class);
        ByteBox = of(ByteBox.class);
        ShortBox = of(ShortBox.class);
        IntegerBox = of(IntegerBox.class);
        LongBox = of(LongBox.class);
        FloatBox = of(FloatBox.class);
        DoubleBox = of(DoubleBox.class);
    }

    static void ensureRegistered() {
        if (Types.Object != Type.CACHE.find(java.lang.Object.class)) {
            throw new IllegalStateException("Standard Java types were not successfully registered!");
        }
    }
}