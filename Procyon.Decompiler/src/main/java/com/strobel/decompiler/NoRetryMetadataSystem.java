package com.strobel.decompiler;

import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;

import java.util.HashSet;
import java.util.Set;

public final class NoRetryMetadataSystem extends MetadataSystem {
    private final Set<String> _failedTypes = new HashSet<>();

    NoRetryMetadataSystem() {
    }

    NoRetryMetadataSystem(final ITypeLoader typeLoader) {
        super(typeLoader);
    }

    @Override
    protected TypeDefinition resolveType(final String descriptor, final boolean mightBePrimitive) {
        if (_failedTypes.contains(descriptor)) {
            return null;
        }

        final TypeDefinition result = super.resolveType(descriptor, mightBePrimitive);

        if (result == null) {
            _failedTypes.add(descriptor);
        }

        return result;
    }
}