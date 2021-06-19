/*
 * DecompilerContext.java
 *
 * Copyright (c) 2013 Mike Strobel
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

import com.strobel.annotations.NotNull;
import com.strobel.assembler.Collection;
import com.strobel.assembler.metadata.CompilerTarget;
import com.strobel.assembler.metadata.LanguageFeature;
import com.strobel.assembler.metadata.IMemberDefinition;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.componentmodel.UserDataStoreBase;
import com.strobel.core.BooleanBox;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DecompilerContext extends UserDataStoreBase {
    private final List<String> _reservedVariableNames = new Collection<>();
    private final Set<IMemberDefinition> _forcedVisibleMembers = new LinkedHashSet<>();
    private DecompilerSettings _settings = new DecompilerSettings();
    private BooleanBox _isCanceled;
    private TypeDefinition _currentType;
    private MethodDefinition _currentMethod;

    public DecompilerContext() {
    }

    public DecompilerContext(final DecompilerSettings settings) {
        _settings = settings;
    }

    public DecompilerSettings getSettings() {
        return _settings;
    }

    public void setSettings(final DecompilerSettings settings) {
        _settings = settings;
    }

    public BooleanBox getCanceled() {
        return _isCanceled;
    }

    public void setCanceled(final BooleanBox canceled) {
        _isCanceled = canceled;
    }

    public TypeDefinition getCurrentType() {
        return _currentType;
    }

    public void setCurrentType(final TypeDefinition currentType) {
        _currentType = currentType;
        isSupported(LanguageFeature.TEXT_BLOCKS);
    }

    public MethodDefinition getCurrentMethod() {
        return _currentMethod;
    }

    public void setCurrentMethod(final MethodDefinition currentMethod) {
        _currentMethod = currentMethod;
    }

    public List<String> getReservedVariableNames() {
        return _reservedVariableNames;
    }

    public Set<IMemberDefinition> getForcedVisibleMembers() {
        return _forcedVisibleMembers;
    }

    public boolean isSupported(final @NotNull LanguageFeature feature) {
        return isSupported(_currentType, feature);
    }

    public boolean isSupported(final TypeDefinition versionSource, final @NotNull LanguageFeature feature) {
        final boolean allowPreview = _settings.arePreviewFeaturesEnabled();

        CompilerTarget target = _settings.getForcedCompilerTarget();

        if (target == null && versionSource != null) {
            target = versionSource.getCompilerTarget();
        }

        if (target == null) {
            target = CompilerTarget.DEFAULT;
        }

        return feature.isAvailable(target, allowPreview);
    }

    public CompilerTarget target() {
        return target(_currentType);
    }

    public CompilerTarget target(final TypeDefinition versionSource) {
        CompilerTarget target = _settings.getForcedCompilerTarget();

        if (target == null && versionSource != null) {
            target = versionSource.getCompilerTarget();
        }

        if (target == null) {
            target = CompilerTarget.DEFAULT;
        }

        return target;
    }
}
