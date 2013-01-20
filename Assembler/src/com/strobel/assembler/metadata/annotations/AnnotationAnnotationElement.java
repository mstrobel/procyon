package com.strobel.assembler.metadata.annotations;

import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class AnnotationAnnotationElement extends AnnotationElement {
    private final CustomAnnotation _annotation;

    public AnnotationAnnotationElement(final CustomAnnotation annotation) {
        super(AnnotationElementType.Annotation);
        _annotation = VerifyArgument.notNull(annotation, "annotation");
    }

    public CustomAnnotation getAnnotation() {
        return _annotation;
    }
}
