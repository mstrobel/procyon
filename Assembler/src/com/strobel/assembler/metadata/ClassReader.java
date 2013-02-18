package com.strobel.assembler.metadata;

/**
 * @author Mike Strobel
 */
public interface ClassReader {
    void accept(final TypeVisitor visitor);
}
