package com.strobel.assembler.ir.attributes;

import com.strobel.assembler.metadata.MethodReference;
import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class EnclosingMethodAttribute extends SourceAttribute {
    private final MethodReference _enclosingMethod;

    public EnclosingMethodAttribute(final MethodReference enclosingMethod) {
        super(AttributeNames.EnclosingMethod, 4);
        _enclosingMethod = VerifyArgument.notNull(enclosingMethod, "enclosingMethod");
    }

    public MethodReference getEnclosingMethod() {
        return _enclosingMethod;
    }
}
