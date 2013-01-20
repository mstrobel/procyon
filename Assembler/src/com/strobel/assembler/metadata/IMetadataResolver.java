package com.strobel.assembler.metadata;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 5:07 PM
 */
public interface IMetadataResolver {
    public TypeReference lookupType(final String descriptor);

    public TypeDefinition resolve(final TypeReference type);
    public FieldDefinition resolve(final FieldReference field);
    public MethodDefinition resolve(final MethodReference method);
}
