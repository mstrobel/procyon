package com.strobel.assembler.metadata;

/**
 * @author Mike Strobel
 */
public interface FieldMetadataVisitor<P, R> {
    R visitField(final FieldReference f, final P p);
}
