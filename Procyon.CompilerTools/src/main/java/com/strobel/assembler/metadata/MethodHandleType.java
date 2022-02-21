/*
 * MethodHandleType.java
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

public enum MethodHandleType {
    GetField,
    GetStatic,
    PutField,
    PutStatic,
    InvokeVirtual,
    InvokeStatic,
    InvokeSpecial,
    NewInvokeSpecial,
    InvokeInterface;

    public boolean isStatic() {
        return this == InvokeStatic ||
               this == GetStatic ||
               this == PutStatic;
    }

    public boolean isField() {
        return this == GetField ||
               this == PutField ||
               this == GetStatic ||
               this == PutStatic;
    }

    public String lookupMethodName() {
        // @formatter:off
        switch (this) {
            case GetField:          return "findGetter";
            case GetStatic:         return "findStaticGetter";
            case PutField:          return "findSetter";
            case PutStatic:         return "findStaticSetter";
            case InvokeStatic:      return "findStatic";
            case InvokeSpecial:     return "findSpecial";
            case NewInvokeSpecial:  return "findConstructor";
            default:                return "findVirtual";
        }
        // @formatter:off
    }

    public String lookupDescriptor() {
        // @formatter:off
        switch (this) {
            case GetField:
            case GetStatic:
            case PutField:
            case PutStatic:         return "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;";
            case InvokeSpecial:     return "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;";
            case NewInvokeSpecial:  return "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;";
            default:                return "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;";
        }
        // @formatter:off
    }
}
