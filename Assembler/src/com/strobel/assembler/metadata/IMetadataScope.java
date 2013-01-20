package com.strobel.assembler.metadata;

/**
 * @author Mike Strobel
 */
public interface IMetadataScope {
    public abstract TypeReference lookupType(final int token);
    public abstract FieldReference lookupField(final int token);
    public abstract MethodReference lookupMethod(final int token);
    public abstract FieldReference lookupField(final int typeToken, final int nameAndTypeToken);
    public abstract MethodReference lookupMethod(final int typeToken, final int nameAndTypeToken);
    public abstract <T> T lookupConstant(final int token);
}
