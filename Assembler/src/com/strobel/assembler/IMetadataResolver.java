package com.strobel.assembler;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 5:07 PM
 */
public interface IMetadataResolver {
    TypeDefinition resolve(final TypeReference type);
    FieldDefinition resolve(final FieldReference field);
    MethodDefinition resolve(final MethodReference method);
}
