package com.strobel.assembler.metadata;

import com.strobel.assembler.Collection;
import com.strobel.core.VerifyArgument;

public final class GenericParameterCollection extends Collection<GenericParameter> {
    private final IGenericParameterProvider _owner;

    public GenericParameterCollection(final IGenericParameterProvider owner) {
        _owner = VerifyArgument.notNull(owner, "owner");
    }

    private void updateGenericParameter(final int index, final GenericParameter p) {
        p.setOwner(_owner);
        p.setPosition(index);
    }

    @Override
    protected void afterAdd(final int index, final GenericParameter p, final boolean appended) {
        updateGenericParameter(index, p);

        if (!appended) {
            for (int i = index + 1; i < size(); i++) {
                get(i).setPosition(i + 1);
            }
        }
    }

    @Override
    protected void beforeSet(final int index, final GenericParameter p) {
        final GenericParameter current = get(index);

        current.setOwner(null);
        current.setPosition(-1);

        updateGenericParameter(index, p);
    }

    @Override
    protected void afterRemove(final int index, final GenericParameter p) {
        p.setOwner(null);
        p.setPosition(-1);

        for (int i = index; i < size(); i++) {
            get(i).setPosition(i);
        }
    }

    @Override
    protected void beforeClear() {
        super.beforeClear();
    }
}
