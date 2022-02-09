package com.strobel.assembler.ir.attributes;

import com.strobel.annotations.NotNull;
import com.strobel.annotations.Nullable;
import com.strobel.assembler.metadata.Flags;
import com.strobel.core.VerifyArgument;

import java.util.EnumSet;

public final class ModuleDependency {
    public final static ModuleDependency[] EMPTY = new ModuleDependency[0];

    private final String _name;
    private final String _version;
    private final EnumSet<Flags.Flag> _flags;

    public ModuleDependency(final @NotNull String name, final @Nullable String version, final int flags) {
        _name = VerifyArgument.notNull(name, "name");
        _version = version;
        _flags = Flags.asFlagSet(flags, Flags.Kind.Requires);
    }

    @NotNull
    public final String getName() {
        return _name;
    }

    @Nullable
    public final String getVersion() {
        return _version;
    }

    @NotNull
    public EnumSet<Flags.Flag> getFlags() {
        return _flags;
    }
}
