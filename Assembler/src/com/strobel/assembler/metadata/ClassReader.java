package com.strobel.assembler.metadata;

/**
 * @author Mike Strobel
 */
public interface ClassReader<P> {
    void accept(final P parameter, final ClassVisitor<P> visitor);
}
