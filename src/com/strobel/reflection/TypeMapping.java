package com.strobel.reflection;

import com.strobel.core.Mapping;

/**
 * @author Mike Strobel
 */
public abstract class TypeMapping extends Mapping<Type<?>> {
    protected TypeMapping() {}

    protected TypeMapping(final String name) {
        super(name);
    }
}
