package com.strobel.assembler.ir.attributes;

import com.strobel.annotations.NotNull;
import com.strobel.annotations.Nullable;
import com.strobel.assembler.metadata.Flags;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public final class ModuleAttribute extends SourceAttribute {
    private final String _name;
    private final String _version;
    private final EnumSet<Flags.Flag> _flags;
    private final List<ModuleDependency> _requires;
    private final List<PackageInfo> _exports;
    private final List<PackageInfo> _opens;
    private final List<TypeReference> _uses;
    private final List<ServiceInfo> _provides;

    public ModuleAttribute(
        final int length,
        final String moduleName,
        final String version,
        final int flags,
        final ModuleDependency[] requires,
        final PackageInfo[] exports,
        final PackageInfo[] opens,
        final TypeReference[] uses,
        final ServiceInfo[] provides) {

        super(AttributeNames.Module, length);

        _name = VerifyArgument.notNull(moduleName, "moduleName");
        _version = version;
        _flags = Flags.asFlagSet(flags, Flags.Kind.Module);
        _requires = ArrayUtilities.isNullOrEmpty(requires) ? Collections.<ModuleDependency>emptyList() : ArrayUtilities.asUnmodifiableList(requires);
        _exports = ArrayUtilities.isNullOrEmpty(exports) ? Collections.<PackageInfo>emptyList() : ArrayUtilities.asUnmodifiableList(exports);
        _opens = ArrayUtilities.isNullOrEmpty(opens) ? Collections.<PackageInfo>emptyList() : ArrayUtilities.asUnmodifiableList(opens);
        _uses = ArrayUtilities.isNullOrEmpty(uses) ? Collections.<TypeReference>emptyList() : ArrayUtilities.asUnmodifiableList(uses);
        _provides = ArrayUtilities.isNullOrEmpty(provides) ? Collections.<ServiceInfo>emptyList() : ArrayUtilities.asUnmodifiableList(provides);
    }

    @NotNull
    public final String getModuleName() {
        return _name;
    }

    @NotNull
    public final EnumSet<Flags.Flag> getFlags() {
        return _flags;
    }

    @Nullable
    public final String getVersion() {
        return _version;
    }

    @NotNull
    public final List<ModuleDependency> getRequires() {
        return _requires;
    }

    @NotNull
    public final List<PackageInfo> getExports() {
        return _exports;
    }

    @NotNull
    public final List<PackageInfo> getOpens() {
        return _opens;
    }

    @NotNull
    public final List<TypeReference> getUses() {
        return _uses;
    }

    @NotNull
    public List<ServiceInfo> getProvides() {
        return _provides;
    }
}
