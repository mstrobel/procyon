/*
 * StackValue.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler.ir;

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.Comparer;
import com.strobel.core.VerifyArgument;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 4:14 PM
 */
public final class StackValue {
    public final static StackValue TOP = new StackValue(StackValueType.Top);
    public final static StackValue INTEGER = new StackValue(StackValueType.Integer);
    public final static StackValue FLOAT = new StackValue(StackValueType.Float);
    public final static StackValue LONG = new StackValue(StackValueType.Long);
    public final static StackValue DOUBLE = new StackValue(StackValueType.Double);
    public final static StackValue NULL = new StackValue(StackValueType.Null);
    public final static StackValue UNINITIALIZED_THIS = new StackValue(StackValueType.UninitializedThis);

    private final StackValueType _type;
    private final Object _parameter;

    private StackValue(final StackValueType type) {
        _type = type;
        _parameter = null;
    }

    private StackValue(final StackValueType type, final Object parameter) {
        _type = type;
        _parameter = parameter;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof StackValue) {
            final StackValue that = (StackValue) o;
            return that._type == _type &&
                   Comparer.equals(that._parameter, _parameter);
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = _type.hashCode();
        result = 31 * result + (_parameter != null ? _parameter.hashCode() : 0);
        return result;
    }

    // <editor-fold defaultstate="collapsed" desc="Factory Methods">

    public static StackValue makeReference(final TypeReference type) {
        return new StackValue(StackValueType.Reference, VerifyArgument.notNull(type, "type"));
    }

    public static StackValue makeUninitializedReference(final Instruction newInstruction) {
        VerifyArgument.notNull(newInstruction, "newInstruction");

        if (newInstruction.getOpCode() != OpCode.NEW) {
            throw new IllegalArgumentException("Parameter must be a NEW instruction.");
        }

        return new StackValue(StackValueType.Reference, newInstruction);
    }

    // </editor-fold>
}
