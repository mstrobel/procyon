package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;

/**
 * @author Mike Strobel
 */
public interface FieldVisitor<P> {
    void visitAttribute(final P parameter, final SourceAttribute attribute);
    void visitAnnotation(final P parameter, final CustomAnnotation annotation, final boolean visible);
    void visitEnd(final P parameter);
}
