/*
 * ParameterDefinitionCollection.java
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

import com.strobel.assembler.Collection;

/**
 * @author Mike Strobel
 */
public final class ParameterDefinitionCollection extends Collection<ParameterDefinition> {
    final IMethodSignature signature;

    ParameterDefinitionCollection(final IMethodSignature signature) {
        this.signature = signature;
    }

    @Override
    protected void afterAdd(final int index, final ParameterDefinition p, final boolean appended) {
        p.setMethod(signature);
        p.setPosition(index);

        if (!appended) {
            for (int i = index + 1; i < size(); i++) {
                get(i).setPosition(i + 1);
            }
        }
    }

    @Override
    protected void beforeSet(final int index, final ParameterDefinition p) {
        final ParameterDefinition current = get(index);

        current.setMethod(null);
        current.setPosition(-1);

        p.setMethod(signature);
        p.setPosition(index);
    }

    @Override
    protected void afterRemove(final int index, final ParameterDefinition p) {
        p.setMethod(null);
        p.setPosition(-1);

        for (int i = index; i < size(); i++) {
            get(i).setPosition(i);
        }
    }

    @Override
    protected void beforeClear() {
        for (int i = 0; i < size(); i++) {
            get(i).setMethod(null);
            get(i).setPosition(-1);
        }
    }
}
