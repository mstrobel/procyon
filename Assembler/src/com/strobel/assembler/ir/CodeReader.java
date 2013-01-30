package com.strobel.assembler.ir;

/**
 * @author Mike Strobel
 */
public interface CodeReader<P> {
    void accept(final P parameter, final InstructionVisitor<P> visitor);
}
