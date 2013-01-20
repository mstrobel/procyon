package com.strobel.assembler.metadata.annotations;

import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class ArrayAnnotationElement extends AnnotationElement {
    private final AnnotationElement[] _elements;

    public ArrayAnnotationElement(final AnnotationElement[] elements) {
        super(AnnotationElementType.Array);
        _elements = VerifyArgument.notNull(elements, "elements");
    }

    public AnnotationElement[] getElements() {
        return _elements;
    }
}
