package com.strobel.assembler.ir.attributes;

/**
 * @author Mike Strobel
 */
public final class ConstantValueAttribute extends SourceAttribute {
    private final Object _value;

    public ConstantValueAttribute(final Object value) {
        super(AttributeNames.ConstantValue, 2);
        _value = value;
    }

    public Object getValue() {
        return _value;
    }
}
