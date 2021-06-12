package com.strobel.assembler.metadata;

import com.strobel.annotations.NotNull;
import com.strobel.annotations.Nullable;
import com.strobel.core.VerifyArgument;

public final class ModuleReference {
    private final String _name;
    private final String _version;

    public ModuleReference(final @NotNull String name, final @Nullable String version) {
        _name = VerifyArgument.notNull(name, "name");
        _version = version;
    }

    @NotNull
    public final String getName() {
        return _name;
    }

    @Nullable
    public final String getVersion() {
        return _version;
    }
}
