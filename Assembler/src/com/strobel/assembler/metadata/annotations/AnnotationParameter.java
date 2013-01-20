package com.strobel.assembler.metadata.annotations;

import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class AnnotationParameter {
    private final AnnotationElement _value;
    private final String _member;

    public AnnotationParameter(final String member, final AnnotationElement value) {
        _member = VerifyArgument.notNull(member, "member");
        _value = VerifyArgument.notNull(value, "value");
    }

    public final String getMember() {
        return _member;
    }

    public final AnnotationElement getValue() {
        return _value;
    }
}
