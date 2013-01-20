package com.strobel.assembler.ir.attributes;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class LineNumberTableAttribute extends SourceAttribute {
    private final List<LineNumberTableEntry> _entries;

    public LineNumberTableAttribute(final LineNumberTableEntry[] entries) {
        super(AttributeNames.LineNumberTable, 2 + (VerifyArgument.notNull(entries, "entries").length * 4));
        _entries = ArrayUtilities.asUnmodifiableList(entries.clone());
    }

    public List<LineNumberTableEntry> getEntries() {
        return _entries;
    }
}
