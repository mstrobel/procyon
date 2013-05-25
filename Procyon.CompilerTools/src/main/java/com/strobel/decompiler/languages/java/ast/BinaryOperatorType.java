/*
 * BinaryOperatorType.java
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

package com.strobel.decompiler.languages.java.ast;

public enum BinaryOperatorType
{
    ANY,
    BITWISE_AND,
    BITWISE_OR,
    LOGICAL_AND,
    LOGICAL_OR,
    EXCLUSIVE_OR,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    EQUALITY,
    INEQUALITY,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    MODULUS,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    UNSIGNED_SHIFT_RIGHT
}
