package com.strobel.assembler.ir.attributes;

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class ExceptionsAttribute extends SourceAttribute {
    private final List<TypeReference> _exceptionTypes;

    public ExceptionsAttribute(final TypeReference... exceptionTypes) {
        super(
            AttributeNames.Exceptions,
            VerifyArgument.noNullElements(exceptionTypes, "exceptionTypes").length * 2
        );
        _exceptionTypes = ArrayUtilities.asUnmodifiableList(exceptionTypes);
    }

    public List<TypeReference> getExceptionTypes() {
        return _exceptionTypes;
    }
}
