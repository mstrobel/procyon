package com.strobel.assembler.ir.attributes;

import com.strobel.annotations.NotNull;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;

import java.util.List;

public final class RecordComponentInfo {
    private final String _name;
    private final String _descriptor;
    private final TypeReference _type;
    private final List<SourceAttribute> _attributes;

    public RecordComponentInfo(final String name, final String descriptor, final TypeReference type, final List<SourceAttribute> attributes) {
        _name = VerifyArgument.notNull(name, "name");
        _descriptor = VerifyArgument.notNull(descriptor, "descriptor");
        _type = VerifyArgument.notNull(type, "type");
        _attributes = VerifyArgument.notNull(attributes, "attributes");
    }

    @NotNull
    public String getName() {
        return _name;
    }

    @NotNull
    public String getDescriptor() {
        return _descriptor;
    }

    @NotNull
    public TypeReference getType() {
        return _type;
    }

    @NotNull
    public List<SourceAttribute> getAttributes() {
        return _attributes;
    }
}
