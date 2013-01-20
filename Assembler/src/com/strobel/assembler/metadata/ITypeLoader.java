package com.strobel.assembler.metadata;

/**
 * @author Mike Strobel
 */
public interface ITypeLoader {
    public boolean tryLoadType(final String internalName, final Buffer buffer);
}
