package com.strobel.assembler.ir.attributes;

import com.strobel.core.VerifyArgument;

import java.util.List;

public final class MethodParametersAttribute extends SourceAttribute {
    private final List<MethodParameterEntry> _entries;

    public MethodParametersAttribute(final List<MethodParameterEntry> entries) {
        super(AttributeNames.MethodParameters, 1 + entries.size() * 4);
        _entries = VerifyArgument.notNull(entries, "entries");
    }

    public List<MethodParameterEntry> getEntries() {
        return _entries;
    }

}
