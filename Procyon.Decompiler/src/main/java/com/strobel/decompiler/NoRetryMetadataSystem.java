/*
 * NoRetryMetadataSystem.java
 *
 * Copyright (c) 2013-2022 Mike Strobel and other contributors
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

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