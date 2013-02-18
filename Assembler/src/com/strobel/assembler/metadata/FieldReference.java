package com.strobel.assembler.metadata;

import com.strobel.util.ContractUtils;

import javax.lang.model.element.Modifier;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:30 PM
 */
public abstract class FieldReference extends MemberReference {
    public abstract TypeReference getFieldType();

    @Override
    public boolean containsGenericParameters() {
        final TypeReference fieldType = getFieldType();

        return fieldType != null && fieldType.containsGenericParameters() ||
               super.containsGenericParameters();
    }

    public FieldDefinition resolve() {
        final TypeReference declaringType = getDeclaringType();

        if (declaringType == null) {
            throw ContractUtils.unsupported();
        }

        return declaringType.resolve(this);
    }

    // <editor-fold defaultstate="collapsed" desc="Name and Signature Formatting">

    @Override
    protected abstract StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName);


    @Override
    protected StringBuilder appendSignature(final StringBuilder sb) {
        return getFieldType().appendSignature(sb);
    }

    @Override
    protected StringBuilder appendErasedSignature(final StringBuilder sb) {
        return getFieldType().appendErasedSignature(sb);
    }

    // </editor-fold>
}

