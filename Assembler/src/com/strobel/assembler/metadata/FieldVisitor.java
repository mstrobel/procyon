package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;

/**
 * @author Mike Strobel
 */
public interface FieldVisitor {
    void visitAttribute(final SourceAttribute attribute);
    void visitAnnotation(final CustomAnnotation annotation, final boolean visible);
    void visitEnd();
}
