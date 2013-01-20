package com.strobel.assembler.metadata.annotations;

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class EnumAnnotationElement extends AnnotationElement {
    private final TypeReference _enumType;
    private final String _enumConstantName;

    public EnumAnnotationElement(final TypeReference enumType, final String enumConstantName) {
        super(AnnotationElementType.Enum);
        _enumType = VerifyArgument.notNull(enumType, "enumType");
        _enumConstantName = VerifyArgument.notNull(enumConstantName, "enumConstantName");
    }

    public TypeReference getEnumType() {
        return _enumType;
    }

    public String getEnumConstantName() {
        return _enumConstantName;
    }
}
