package com.strobel.assembler;

public class FieldDefinition extends FieldReference {
    public FieldDefinition(final String name, final TypeReference fieldType) {
        super(name, fieldType);
    }

    public FieldDefinition(final String name, final TypeReference fieldType, final TypeDefinition declaringType) {
        super(name, fieldType, declaringType);
    }
}
