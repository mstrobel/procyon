package com.strobel.assembler.ir.attributes;

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;

import java.util.List;

public final class PermittedSubclassesAttribute extends SourceAttribute {
    private final List<TypeReference> _permittedSubclasses;

    public PermittedSubclassesAttribute(final TypeReference[] permittedSubclasses) {
        super(AttributeNames.PermittedSubclasses, 2 + (VerifyArgument.notNull(permittedSubclasses, "permittedSubclasses").length * 2));

        _permittedSubclasses = ArrayUtilities.asUnmodifiableList(permittedSubclasses.clone());
    }

    public final List<TypeReference> getPermittedSubclasses() {
        return _permittedSubclasses;
    }
}
