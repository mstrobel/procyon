package com.strobel.reflection;

import com.strobel.compilerservices.Closure;

import java.io.Serializable;
import java.lang.*;
import java.lang.Error;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.UUID;

/**
 * @author Mike Strobel
 */
public final class Types {
    private Types() {}

    public static final Type<Object> Object;

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

    public static final Type<Error> Error;
    public static final Type<Throwable> Throwable;
    public static final Type<Exception> Exception;
    public static final Type<RuntimeException> RuntimeException;

    public static final Type<StringBuilder> StringBuilder;
    public static final Type<StringBuffer> StringBuffer;

    public static final Type<BigInteger> BigInteger;
    public static final Type<BigDecimal> BigDecimal;

    public static final Type<System> System;

    public static final Type<Class> Class;
    public static final Type<ClassLoader> ClassLoader;

    public static final Type<Serializable> Serializable;
    public static final Type<Cloneable> Cloneable;

    public static final Type<Closure> Closure;

    static {
        Object = Type.of(Object.class);
        Number = Type.of(Number.class);
        Boolean = Type.of(Boolean.class);
        Byte = Type.of(Byte.class);
        Character = Type.of(Character.class);
        Short = Type.of(Short.class);
        Integer = Type.of(Integer.class);
        Long = Type.of(Long.class);
        Float = Type.of(Float.class);
        Double = Type.of(Double.class);
        String = Type.of(String.class);
        Date = Type.of(Date.class);
        UUID = Type.of(UUID.class);

        Error = Type.of(java.lang.Error.class);
        Throwable = Type.of(Throwable.class);
        Exception = Type.of(Exception.class);
        RuntimeException = Type.of(RuntimeException.class);

        StringBuffer = Type.of(StringBuffer.class);
        StringBuilder = Type.of(StringBuilder.class);

        BigInteger = Type.of(BigInteger.class);
        BigDecimal = Type.of(BigDecimal.class);

        System = Type.of(System.class);

        Class = Type.of(Class.class);
        ClassLoader = Type.of(ClassLoader.class);

        Serializable = Type.of(Serializable.class);
        Cloneable = Type.of(Cloneable.class);

        Closure = Type.of(Closure.class);
    }

    static void ensureRegistered() {
        if (Types.Object != Type.CACHE.find(java.lang.Object.class)) {
            throw new IllegalStateException("Standard Java types were not successfully registered!");
        }
    }
}