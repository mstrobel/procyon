package com.strobel.assembler.ir.attributes;

import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class EnclosingMethodAttribute extends SourceAttribute {
    private final TypeReference _enclosingType;
    private final MethodReference _enclosingMethod;

    public EnclosingMethodAttribute(final TypeReference enclosingType, final MethodReference enclosingMethod) {
        super(AttributeNames.EnclosingMethod, 4);
        _enclosingType = VerifyArgument.notNull(enclosingType, "enclosingType");
        _enclosingMethod = enclosingMethod;
    }

    public TypeReference getEnclosingType() {
        return _enclosingType;
    }

    public MethodReference getEnclosingMethod() {
        return _enclosingMethod;
    }
}
