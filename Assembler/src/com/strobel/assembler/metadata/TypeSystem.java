package com.strobel.assembler.metadata;

import com.strobel.reflection.SimpleType;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 5:07 PM
 */
public abstract class TypeSystem {
    public abstract void registerType(final TypeDefinition type);
    public abstract TypeReference lookupType(final String descriptor);
    public abstract TypeReference lookupType(final String packageName, final String typeName);
    public abstract TypeReference lookupSystemType(final String name, final SimpleType simpleType);
}
