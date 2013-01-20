package com.strobel.assembler.metadata.annotations;

import com.strobel.assembler.metadata.TypeReference;

/**
 * @author Mike Strobel
 */
public final class ClassAnnotationElement extends AnnotationElement {
    private final TypeReference _classType;

    public ClassAnnotationElement(final TypeReference classType) {
        super(AnnotationElementType.Class);
        _classType = classType;
    }

    public TypeReference getClassType() {
        return _classType;
    }
}
