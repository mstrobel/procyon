package com.strobel.assembler;

import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:30 PM
 */
public class FieldReference extends MemberReference {
    private TypeReference _fieldType;

    public TypeReference getFieldType() {
        return _fieldType;
    }

    public void setFieldType(final TypeReference fieldType) {
        _fieldType = fieldType;
    }

    @Override
    public String getFullName() {
        return _fieldType.getFullName() + " " + getQualifiedMemberName();
    }

    @Override
    protected boolean containsGenericParameters() {
        return _fieldType.containsGenericParameters() ||
               super.containsGenericParameters();
    }

    public FieldReference (final String name, final TypeReference fieldType) {
        super(name);

        _fieldType = VerifyArgument.notNull(fieldType, "fieldType");
    }

    public FieldReference (final String name, final TypeReference fieldType, final TypeReference declaringType) {
        this(name, fieldType);
        setDeclaringType(declaringType);
    }

    public FieldDefinition resolve() {
        final TypeReference declaringType = getDeclaringType();

        if (declaringType == null)
            throw ContractUtils.unsupported();

        return declaringType.resolve(this);
    }
}

