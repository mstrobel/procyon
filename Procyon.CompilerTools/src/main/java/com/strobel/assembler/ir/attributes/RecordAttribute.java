package com.strobel.assembler.ir.attributes;

import java.util.List;

public final class RecordAttribute extends SourceAttribute {
    private final List<RecordComponentInfo> _components;

    public RecordAttribute(final int length, final List<RecordComponentInfo> components) {
        super(AttributeNames.Record, length);
        _components = components;
    }

    public final List<RecordComponentInfo> getComponents() {
        return _components;
    }
}
