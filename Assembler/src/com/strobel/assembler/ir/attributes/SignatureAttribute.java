package com.strobel.assembler.ir.attributes;

/**
 * @author Mike Strobel
 */
public final class SignatureAttribute extends SourceAttribute {
    private final String _signature;

    public SignatureAttribute(final String signature) {
        super(AttributeNames.Signature, 4);
        _signature = signature;
    }

    public String getSignature() {
        return _signature;
    }
}
