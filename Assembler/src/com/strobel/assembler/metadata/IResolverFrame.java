package com.strobel.assembler.metadata;

/**
 * @author Mike Strobel
 */
public interface IResolverFrame extends IGenericContext {
    public TypeReference findType(final String descriptor);
}
