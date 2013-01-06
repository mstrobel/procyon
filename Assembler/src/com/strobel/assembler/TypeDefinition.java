package com.strobel.assembler;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 4:51 PM
 */
public class TypeDefinition extends TypeReference {
    @Override
    public TypeDefinition resolve() {
        return this;
    }
}
