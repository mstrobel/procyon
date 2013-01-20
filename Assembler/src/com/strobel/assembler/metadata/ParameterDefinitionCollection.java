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

        p.setMethod(signature);
        p.setPosition(index);

        current.setMethod(null);
        current.setPosition(-1);
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
