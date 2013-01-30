package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.Frame;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.InstructionVisitor;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 4:03 PM
 */
public interface MethodVisitor<P> {
    boolean canVisitBody(final P parameter);

    InstructionVisitor<P> visitBody(final P parameter, final int maxStack, final int maxLocals);

    void visitEnd(final P parameter);
    void visitFrame(final P parameter, final Frame frame);

    void visitLineNumber(final P parameter, final Instruction instruction, final int lineNumber);

    void visitAttribute(final P parameter, final SourceAttribute attribute);
    void visitAnnotation(final P parameter, final CustomAnnotation annotation, final boolean visible);
}
