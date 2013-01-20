package com.strobel.assembler.ir.attributes;

import com.strobel.core.ArrayUtilities;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class LocalVariableTableAttribute extends SourceAttribute {
    private final List<LocalVariableTableEntry> _entries;

    public LocalVariableTableAttribute(final String name, final LocalVariableTableEntry[] entries) {
        super(name, 2 + (entries.length * 10));
        _entries = ArrayUtilities.asUnmodifiableList(entries.clone());
    }

    public List<LocalVariableTableEntry> getEntries() {
        return _entries;
    }
}
