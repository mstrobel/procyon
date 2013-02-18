package com.strobel.assembler.metadata;

import com.strobel.assembler.metadata.annotations.CustomAnnotation;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 4:38 PM
 */
public abstract class MemberReference implements IAnnotationsProvider {
    protected MemberReference() {
    }

    public boolean isSpecialName() {
        return false;
    }

    public boolean isDefinition() {
        return false;
    }

    public boolean containsGenericParameters() {
        final TypeReference declaringType = getDeclaringType();

        return declaringType != null &&
               declaringType.containsGenericParameters();
    }

    public abstract TypeReference getDeclaringType();

    public boolean isEquivalentTo(final MemberReference member) {
        return member == this;
    }

    // <editor-fold defaultstate="collapsed" desc="Annotations">

    @Override
    public boolean hasAnnotations() {
        return !getAnnotations().isEmpty();
    }

    @Override
    public List<CustomAnnotation> getAnnotations() {
        return Collections.emptyList();
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Name and Signature Formatting">

    public abstract String getName();

    public String getFullName() {
        final StringBuilder name = new StringBuilder();
        appendName(name, true, false);
        return name.toString();
    }

    /**
     * Method that returns full generic signature of a type or member.
     */
    public String getSignature() {
        return appendSignature(new StringBuilder()).toString();
    }

    /**
     * Method that returns type erased signature of a type or member;
     * suitable as non-generic signature some packages need.
     */
    public String getErasedSignature() {
        return appendErasedSignature(new StringBuilder()).toString();
    }

    protected abstract StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName);
    protected abstract StringBuilder appendSignature(StringBuilder sb);
    protected abstract StringBuilder appendErasedSignature(StringBuilder sb);

    @Override
    public String toString() {
        return getSignature();
    }

    // </editor-fold>
}
