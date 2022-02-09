/*
 * CommonTypeReferences.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler.metadata;

import com.strobel.util.ContractUtils;

public final class CommonTypeReferences {
    public final static TypeReference Object;
    public final static TypeReference String;
    public final static TypeReference Serializable;
    public final static TypeReference Class;
    public final static TypeReference Annotation;
    public final static TypeReference Throwable;
    public final static TypeReference AutoCloseable;

    public final static TypeReference StringBuilder;
    public final static TypeReference StringBuffer;

    public final static TypeReference Number;

    public final static TypeReference Void;
    public final static TypeReference Boolean;
    public final static TypeReference Character;
    public final static TypeReference Byte;
    public final static TypeReference Short;
    public final static TypeReference Integer;
    public final static TypeReference Long;
    public final static TypeReference Float;
    public final static TypeReference Double;

    public final static TypeReference IllegalStateException;
    public final static TypeReference ClassNotFoundException;
    public final static TypeReference NoClassDefFoundError;

    public final static TypeReference LambdaMetafactory;
    public final static TypeReference StringConcatFactory;
    public final static TypeReference ObjectMethods;

    static {
        final MetadataParser parser = new MetadataParser(MetadataSystem.instance());

        Object = parser.parseTypeDescriptor("java/lang/Object");
        String = parser.parseTypeDescriptor("java/lang/String");
        Serializable = parser.parseTypeDescriptor("java/lang/Serializable");
        Class = parser.parseTypeDescriptor("java/lang/Class");
        Annotation = parser.parseTypeDescriptor("java/lang/annotation/Annotation");
        Throwable = parser.parseTypeDescriptor("java/lang/Throwable");
        AutoCloseable = parser.parseTypeDescriptor("java/lang/AutoCloseable");

        StringBuilder = parser.parseTypeDescriptor("java/lang/StringBuilder");
        StringBuffer = parser.parseTypeDescriptor("java/lang/StringBuffer");

        Number = parser.parseTypeDescriptor("java/lang/Number");

        Void = parser.parseTypeDescriptor("java/lang/Void");
        Boolean = parser.parseTypeDescriptor("java/lang/Boolean");
        Character = parser.parseTypeDescriptor("java/lang/Character");
        Byte = parser.parseTypeDescriptor("java/lang/Byte");
        Short = parser.parseTypeDescriptor("java/lang/Short");
        Integer = parser.parseTypeDescriptor("java/lang/Integer");
        Long = parser.parseTypeDescriptor("java/lang/Long");
        Float = parser.parseTypeDescriptor("java/lang/Float");
        Double = parser.parseTypeDescriptor("java/lang/Double");

        IllegalStateException = parser.parseTypeDescriptor("java/lang/IllegalStateException");
        ClassNotFoundException = parser.parseTypeDescriptor("java/lang/ClassNotFoundException");
        NoClassDefFoundError = parser.parseTypeDescriptor("java/lang/NoClassDefFoundError");

        LambdaMetafactory = parser.parseTypeDescriptor("java/lang/invoke/LambdaMetafactory");
        StringConcatFactory = parser.parseTypeDescriptor("java/lang/invoke/StringConcatFactory");
        ObjectMethods = parser.parseTypeDescriptor("java/lang/runtime/ObjectMethods");
    }

    private CommonTypeReferences() {
        throw ContractUtils.unreachable();
    }
}
