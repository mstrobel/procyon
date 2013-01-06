package com.strobel.assembler;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 4:03 PM
 */
public interface MethodVisitor<P> extends InstructionVisitor<P> {
    void visitBody(final P parameter);
    void visitEnd(final P parameter);
    void visitFrame(final P parameter, final Frame frame);
    void visitLineNumber(final P parameter, final Instruction instruction, final int lineNumber);
}
