/*
 * DecompilerContext.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler;

import com.strobel.assembler.Collection;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.core.BooleanBox;

import java.util.List;

public final class DecompilerContext {
    private final List<String> _reservedVariableNames = new Collection<>();
    private DecompilerSettings _settings = new DecompilerSettings();
    private BooleanBox _isCanceled;
    private TypeDefinition _currentType;
    private MethodDefinition _currentMethod;

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
}
