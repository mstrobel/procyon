package com.strobel.assembler.ir.attributes;

import com.strobel.annotations.NotNull;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;

public final class ModuleMainClassAttribute extends SourceAttribute {
    private final TypeReference _mainClass;

    public ModuleMainClassAttribute(final TypeReference mainClass) {
        super(AttributeNames.ModuleMainClass, 8);
        _mainClass = VerifyArgument.notNull(mainClass, "mainClass");
    }

    @NotNull
    public final TypeReference getMainClass() {
        return _mainClass;
    }
}

