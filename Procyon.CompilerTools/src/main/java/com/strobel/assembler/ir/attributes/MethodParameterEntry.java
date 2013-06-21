package com.strobel.assembler.ir.attributes;

import com.strobel.assembler.metadata.Flags;

public final class MethodParameterEntry {
    private final String _name;
    private final int _flags;

    public MethodParameterEntry(final String name, final int flags) {
        _name = name;
        _flags = flags;
    }

    public String getName() {
        return _name;
    }

    public int getFlags() {
        return _flags;
    }

    @Override
    public String toString() {
        return "MethodParameterEntry{" +
               "name='" + _name + "'" +
               ", flags=" + Flags.toString(_flags) +
               '}';
    }
}
