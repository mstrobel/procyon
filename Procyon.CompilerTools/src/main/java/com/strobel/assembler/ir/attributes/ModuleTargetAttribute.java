package com.strobel.assembler.ir.attributes;

import com.strobel.annotations.NotNull;
import com.strobel.core.VerifyArgument;

public final class ModuleTargetAttribute extends SourceAttribute {
    private final String _platform;

    public ModuleTargetAttribute(final String platform) {
        super(AttributeNames.ModuleTarget, 5);
        _platform = VerifyArgument.notNull(platform, "platform");
    }

    @NotNull
    public final String getPlatform() {
        return _platform;
    }
}
