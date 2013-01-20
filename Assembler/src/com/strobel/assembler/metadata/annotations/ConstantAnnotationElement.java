package com.strobel.assembler.metadata.annotations;

import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class ConstantAnnotationElement extends AnnotationElement {
    private final Object _constantValue;

    public ConstantAnnotationElement(final Object constantValue) {
        super(AnnotationElementType.Constant);
        _constantValue = VerifyArgument.notNull(constantValue, "constantValue");
    }

    public Object getConstantValue() {
        return _constantValue;
    }
}
