/*
 * LocalVariableTableEntry.java
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

package com.strobel.assembler.ir.attributes;

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class LocalVariableTableEntry {
    private final int _index;
    private final String _name;
    private final TypeReference _type;
    private final int _scopeOffset;
    private final int _scopeLength;

    public LocalVariableTableEntry(
        final int index,
        final String name,
        final TypeReference type,
        final int scopeOffset,
        final int scopeLength) {

        _index = VerifyArgument.isNonNegative(index, "index");
        _name = VerifyArgument.notNull(name, "name");
        _type = VerifyArgument.notNull(type, "type");
        _scopeOffset = VerifyArgument.isNonNegative(scopeOffset, "scopeOffset");
        _scopeLength = VerifyArgument.isNonNegative(scopeLength, "scopeLength");
    }

    public int getIndex() {
        return _index;
    }

    public String getName() {
        return _name;
    }

    public TypeReference getType() {
        return _type;
    }

    public int getScopeOffset() {
        return _scopeOffset;
    }

    public int getScopeLength() {
        return _scopeLength;
    }

    @Override
    public String toString() {
        return "LocalVariableTableEntry{" +
               "Index=" + _index +
               ", Name='" + _name + '\'' +
               ", Type=" + _type +
               ", ScopeOffset=" + _scopeOffset +
               ", ScopeLength=" + _scopeLength +
               '}';
    }
}

