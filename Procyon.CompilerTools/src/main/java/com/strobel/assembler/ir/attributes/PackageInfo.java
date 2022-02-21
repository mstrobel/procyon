package com.strobel.assembler.ir.attributes;

import com.strobel.annotations.NotNull;
import com.strobel.assembler.metadata.Flags;
import com.strobel.assembler.metadata.ModuleReference;
import com.strobel.assembler.metadata.PackageReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public final class PackageInfo {
    public final static PackageInfo[] EMPTY = new PackageInfo[0];

    private final EnumSet<Flags.Flag> _flags;
    private final PackageReference _package;
    private final List<ModuleReference> _modules;

    public PackageInfo(final String name, final int flags, final String[] modules) {
        _package = PackageReference.parse(VerifyArgument.notNull(name, "name"));
        _flags = Flags.asFlagSet(flags, Flags.Kind.ExportsOpens);

        if (ArrayUtilities.isNullOrEmpty(modules)) {
            _modules = Collections.emptyList();
        }
        else {
            VerifyArgument.noNullElements(modules, "modules");

            final ModuleReference[] moduleReferences = new ModuleReference[modules.length];

            for (int i = 0; i < moduleReferences.length; i++) {
                moduleReferences[i] = new ModuleReference(modules[i], null);
            }

            _modules = ArrayUtilities.asUnmodifiableList(moduleReferences);
        }
    }

    @NotNull
    public EnumSet<Flags.Flag> getFlags() {
        return _flags;
    }

    @NotNull
    public final PackageReference getPackage() {
        return _package;
    }

    @NotNull
    public final List<ModuleReference> getModules() {
        return _modules;
    }
}
