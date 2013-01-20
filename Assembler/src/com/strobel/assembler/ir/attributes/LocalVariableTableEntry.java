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
}

