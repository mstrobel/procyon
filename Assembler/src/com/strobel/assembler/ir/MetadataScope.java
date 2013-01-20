/*
package com.strobel.assembler.ir;

import com.strobel.assembler.metadata.*;
import com.strobel.core.VerifyArgument;

*/
/**
 * @author Mike Strobel
 *//*

public abstract class MetadataScope implements IMetadataScope {
    private final MetadataHelper _helper;

    protected MetadataScope(final IMetadataResolver resolver) {
        _helper = new MetadataHelper(VerifyArgument.notNull(resolver, "resolver"));
    }

    public void pushGenericContext(final IGenericParameterProvider provider) {
        _helper.pushGenericContext(provider);
    }

    public void pushGenericContext(final IGenericContext context) {
        _helper.pushGenericContext(context);
    }

    public void popGenericContext() {
        _helper.popGenericContext();
    }

    @Override
    public abstract TypeReference lookupType(final int token);

    @Override
    public abstract FieldReference lookupField(final int token);

    @Override
    public abstract MethodReference lookupMethod(final int token);

    @Override
    public abstract FieldReference lookupField(final int typeToken, final int nameAndTypeToken);

    @Override
    public abstract MethodReference lookupMethod(final int typeToken, final int nameAndTypeToken);

    @Override
    public abstract <T> T lookupConstant(final int token);

    @Override
    public TypeReference lookupTypeFromDescriptor(final String descriptor) {
        return _helper.lookupTypeFromDescriptor(descriptor);
    }

    @Override
    public TypeReference lookupTypeFromSignature(final String signature) {
        return _helper.lookupTypeFromSignature(signature);
    }

    @Override
    public TypeReference lookupType(final String packageName, final String typeName) {
        return _helper.lookupType(packageName, typeName);
    }

    @Override
    public FieldReference lookupField(final TypeReference declaringType, final String name, final String descriptor) {
        return _helper.lookupField(declaringType, name, descriptor);
    }

    @Override
    public MethodReference lookupMethod(final TypeReference declaringType, final String name, final String descriptor) {
        return _helper.lookupMethod(declaringType, name, descriptor);
    }
}
*/
