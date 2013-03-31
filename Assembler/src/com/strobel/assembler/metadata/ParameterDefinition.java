/*
 * ParameterDefinition.java
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

package com.strobel.assembler.metadata;

import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.StringUtilities;

import java.util.Collections;
import java.util.List;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 5:42 PM
 */
public final class ParameterDefinition extends ParameterReference implements IAnnotationsProvider {
    private final int _size;
    private int _slot;
    private IMethodSignature _method;
    private List<CustomAnnotation> _annotations;

    public ParameterDefinition(final int slot, final TypeReference parameterType) {
        super(StringUtilities.EMPTY, parameterType);
        _slot = slot;
        _size = parameterType.getSimpleType().isDoubleWord() ? 2 : 1;
    }

    public ParameterDefinition(final int slot, final String name, final TypeReference parameterType) {
        super(name, parameterType);
        _slot = slot;
        _size = parameterType.getSimpleType().isDoubleWord() ? 2 : 1;
    }

    public final int getSize() {
        return _size;
    }

    public final int getSlot() {
        return _slot;
    }

    final void setSlot(final int slot) {
        _slot = slot;
    }

    public final IMethodSignature getMethod() {
        return _method;
    }

    final void setMethod(final IMethodSignature method) {
        _method = method;
    }

    @Override
    public boolean hasAnnotations() {
        return !getAnnotations().isEmpty();
    }

    @Override
    public List<CustomAnnotation> getAnnotations() {
        if (_annotations == null) {
            synchronized (this) {
                if (_annotations == null) {
                    _annotations = populateCustomAnnotations();
                }
            }
        }
        return _annotations;
    }

    @Override
    public ParameterDefinition resolve() {
        final TypeReference resolvedParameterType = super.getParameterType().resolve();

        if (resolvedParameterType != null) {
            setParameterType(resolvedParameterType);
        }

        return this;
    }

    // <editor-fold defaultstate="collapsed" desc="Metadata Loading">

    private List<CustomAnnotation> populateCustomAnnotations() {
        return Collections.emptyList();
    }

    // </editor-fold>
}
