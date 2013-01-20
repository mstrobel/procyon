package com.strobel.assembler.metadata.annotations;

/**
 * @author Mike Strobel
 */
public abstract class AnnotationElement {
    private final AnnotationElementType _elementType;

    protected AnnotationElement(final AnnotationElementType elementType) {
        _elementType = elementType;
    }

    public AnnotationElementType getElementType() {
        return _elementType;
    }
}

