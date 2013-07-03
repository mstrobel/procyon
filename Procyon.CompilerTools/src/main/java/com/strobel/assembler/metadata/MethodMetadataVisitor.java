package com.strobel.assembler.metadata;

/**
 * @author Mike Strobel
 */
public interface MethodMetadataVisitor<P, R> {
    R visitParameterizedMethod(final MethodReference m, final P p);
    R visitMethod(final MethodReference m, final P p);
}
