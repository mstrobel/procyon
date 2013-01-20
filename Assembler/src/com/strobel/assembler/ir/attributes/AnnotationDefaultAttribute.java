package com.strobel.assembler.ir.attributes;

import com.strobel.assembler.metadata.annotations.AnnotationElement;
import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class AnnotationDefaultAttribute extends SourceAttribute {
    private final AnnotationElement _defaultValue;

    public AnnotationDefaultAttribute(final int length, final AnnotationElement defaultValue) {
        super(AttributeNames.AnnotationDefault, length);
        _defaultValue = VerifyArgument.notNull(defaultValue, "defaultValue");
    }

    public AnnotationElement getDefaultValue() {
        return _defaultValue;
    }
}
