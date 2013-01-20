package com.strobel.assembler.metadata;

/**
 * @author Mike Strobel
 */
public interface IMetadataScope {
    public abstract TypeReference lookupType(final int token);
    public abstract FieldReference lookupField(final int typeToken, final int fieldToken);
    public abstract MethodReference lookupMethod(final int typeToken, final int methodToken);
    public abstract <T> T lookupConstant(final int token);

    public abstract TypeReference lookupType(final String descriptor);
    public abstract TypeReference lookupType(final String packageName, final String typeName);
}
