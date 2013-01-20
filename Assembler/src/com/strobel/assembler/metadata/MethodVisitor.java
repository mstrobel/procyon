package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.InstructionVisitor;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 4:03 PM
 */
public interface MethodVisitor<P> extends InstructionVisitor<P> {
    boolean canVisitBody(final P parameter);

    void visitBody(final P parameter);
    void visitEnd(final P parameter);
    void visitFrame(final P parameter, final Frame frame);

    void visitLineNumber(final P parameter, final Instruction instruction, final int lineNumber);

    void visitAttribute(final P parameter, final SourceAttribute attribute);
    void visitAnnotation(final P parameter, final CustomAnnotation annotation, final boolean visible);
}
