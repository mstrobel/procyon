package com.strobel.assembler.metadata;

/**
 * @author Mike Strobel
 */
public abstract class TypeSpecification extends TypeReference {
    public abstract TypeReference getElementType();
}
