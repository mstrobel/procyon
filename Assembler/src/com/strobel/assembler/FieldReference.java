package com.strobel.assembler;

import com.strobel.util.ContractUtils;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:30 PM
 */
public abstract class FieldReference extends MemberReference {
    private TypeReference _fieldType;

    public TypeReference getFieldType() {
        return _fieldType;
    }

    public void setFieldType(final TypeReference fieldType) {
        _fieldType = fieldType;
    }

    @Override
    public boolean containsGenericParameters() {
        return _fieldType.containsGenericParameters() ||
               super.containsGenericParameters();
    }

    public FieldDefinition resolve() {
        final TypeReference declaringType = getDeclaringType();

        if (declaringType == null)
            throw ContractUtils.unsupported();

        return declaringType.resolve(this);
    }
}

