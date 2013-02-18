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
public interface MethodVisitor {
    boolean canVisitBody();

    InstructionVisitor visitBody(final int maxStack, final int maxLocals);

    void visitEnd();
    void visitFrame(final Frame frame);

    void visitLineNumber(final Instruction instruction, final int lineNumber);

    void visitAttribute(final SourceAttribute attribute);
    void visitAnnotation(final CustomAnnotation annotation, final boolean visible);
}
