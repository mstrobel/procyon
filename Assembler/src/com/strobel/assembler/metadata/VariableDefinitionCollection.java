/*
 * VariableDefinitionCollection.java
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
public final class VariableDefinitionCollection extends Collection<VariableDefinition> {
    @Override
    protected void afterAdd(final int index, final VariableDefinition v, final boolean appended) {
        v.setIndex(index);

        if (!appended) {
            for (int i = index + 1; i < size(); i++) {
                get(i).setIndex(i + 1);
            }
        }
    }

    @Override
    protected void beforeSet(final int index, final VariableDefinition v) {
        final VariableDefinition current = get(index);

        current.setIndex(-1);

        v.setIndex(index);
    }

    @Override
    protected void afterRemove(final int index, final VariableDefinition v) {
        v.setIndex(-1);

        for (int i = index; i < size(); i++) {
            get(i).setIndex(i);
        }
    }

    @Override
    protected void beforeClear() {
        super.beforeClear();
    }
}
