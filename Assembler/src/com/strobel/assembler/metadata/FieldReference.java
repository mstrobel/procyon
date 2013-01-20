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
        return getFieldType().containsGenericParameters() ||
               super.containsGenericParameters();
    }

    public FieldDefinition resolve() {
        final TypeReference declaringType = getDeclaringType();

        if (declaringType == null)
            throw ContractUtils.unsupported();

        return declaringType.resolve(this);
    }

    // <editor-fold defaultstate="collapsed" desc="Name and Signature Formatting">

    @Override
    protected abstract StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName);

    @Override
    protected StringBuilder appendDescription(final StringBuilder sb) {
        StringBuilder s = sb;

        for (final Modifier modifier : Flags.asModifierSet(getModifiers())) {
            s.append(modifier.toString());
            s.append(' ');
        }

        final TypeReference fieldType = getFieldType();

        if (fieldType.isGenericParameter()) {
            s.append(fieldType.getName());
        }
        else {
            s = fieldType.appendBriefDescription(s);
        }

        s.append(' ');
        s.append(getName());

        return s;
    }

    @Override
    protected StringBuilder appendBriefDescription(final StringBuilder sb) {
        StringBuilder s = sb;

        if (isStatic()) {
            s.append(Modifier.STATIC.toString());
            s.append(' ');
        }

        final TypeReference fieldType = getFieldType();

        if (fieldType.isGenericParameter()) {
            s.append(fieldType.getName());
        }
        else {
            s = fieldType.appendBriefDescription(s);
        }

        s.append(' ');
        s.append(getName());

        return s;
    }

    @Override
    protected StringBuilder appendErasedDescription(final StringBuilder sb) {
        StringBuilder s = sb;

        for (final Modifier modifier : Flags.asModifierSet(getModifiers())) {
            s.append(modifier.toString());
            s.append(' ');
        }

        s = getFieldType().getRawType().appendErasedDescription(s);
        s.append(' ');
        s.append(getName());

        return s;
    }

    @Override
    protected StringBuilder appendSignature(final StringBuilder sb) {
        return getFieldType().appendSignature(sb);
    }

    @Override
    protected StringBuilder appendErasedSignature(final StringBuilder sb) {
        return getFieldType().appendErasedSignature(sb);
    }

    @Override
    protected StringBuilder appendSimpleDescription(final StringBuilder sb) {
        StringBuilder s = sb;

        if (isStatic()) {
            s.append(Modifier.STATIC.toString());
            s.append(' ');
        }

        final TypeReference fieldType = getFieldType();

        if (fieldType.isGenericParameter()) {
            s.append(fieldType.getName());
        }
        else {
            s = fieldType.appendSimpleDescription(s);
        }

        s.append(' ');
        s.append(getName());

        return s;
    }

    // </editor-fold>
}

