package com.strobel.assembler.ir;

/**
 * @author Mike Strobel
 */
public interface CodeReader {
    void accept(final InstructionVisitor visitor);
}
