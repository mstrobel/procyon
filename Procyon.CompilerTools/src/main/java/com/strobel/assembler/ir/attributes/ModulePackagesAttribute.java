package com.strobel.assembler.ir.attributes;

import com.strobel.annotations.NotNull;
import com.strobel.assembler.metadata.PackageReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;

import java.util.Collections;
import java.util.List;

public final class ModulePackagesAttribute extends SourceAttribute {
    private final List<PackageReference> _packages;

    public ModulePackagesAttribute(final String[] packages) {
        super(AttributeNames.ModulePackages, 8 + (packages == null ? 0 : packages.length * 2));

        if (ArrayUtilities.isNullOrEmpty(packages)) {
            _packages = Collections.emptyList();
        }
        else {
            VerifyArgument.noNullElements(packages, "packages");

            final PackageReference[] packageReferences = new PackageReference[packages.length];

            for (int i = 0; i < packageReferences.length; i++) {
                packageReferences[i] =  PackageReference.parse(packages[i]);
            }

            _packages = ArrayUtilities.asUnmodifiableList(packageReferences);
        }
    }

    @NotNull
    public final List<PackageReference> getPackages() {
        return _packages;
    }
}

