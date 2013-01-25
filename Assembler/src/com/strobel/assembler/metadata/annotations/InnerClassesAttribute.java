package com.strobel.assembler.metadata.annotations;

import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.core.VerifyArgument;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class InnerClassesAttribute extends SourceAttribute {
    private final List<InnerClassEntry> _entries;

    public InnerClassesAttribute(final int length, final List<InnerClassEntry> entries) {
        super(AttributeNames.InnerClasses, length);
        _entries = VerifyArgument.notNull(entries, "entries");
    }

    public List<InnerClassEntry> getEntries() {
        return _entries;
    }
}
