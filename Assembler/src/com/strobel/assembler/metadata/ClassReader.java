package com.strobel.assembler.metadata;

/**
 * @author Mike Strobel
 */
public interface ClassReader {
    void accept(final IMetadataResolver metadataResolver, final ClassVisitor<IMetadataScope> visitor);
}
