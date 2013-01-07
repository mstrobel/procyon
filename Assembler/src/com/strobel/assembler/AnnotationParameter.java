package com.strobel.assembler;

/**
 * @author Mike Strobel
 */
public final class AnnotationParameter {
    private final MemberReference _attribute;
    private final Object _value;

    public AnnotationParameter(final MemberReference attribute, final Object value) {
        _attribute = attribute;
        _value = value;
    }

    public final MemberReference getAttribute() {
        return _attribute;
    }

    public final TypeReference getType() {
        return _attribute.getDeclaringType();
    }

    public final Object getValue() {
        return _value;
    }
}
