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
    static final int MODIFIER_BRIDGE     = 0x00000040;
    static final int MODIFIER_VARARGS    = 0x00000080;
    static final int MODIFIER_SYNTHETIC  = 0x00001000;
    static final int MODIFIER_ANNOTATION = 0x00002000;
    static final int MODIFIER_ENUM       = 0x00004000;

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

    // <editor-fold defaultstate="collapsed" desc="Member Attributes">

    public abstract long getFlags();

    public final int getModifiers() {
        return Flags.toModifiers(getFlags());
    }

    public final boolean isFinal() {
        return Modifier.isFinal(getModifiers());
    }

    public final boolean isNonPublic() {
        return !Modifier.isPublic(getModifiers());
    }

    public final boolean isPrivate() {
        return Modifier.isPrivate(getModifiers());
    }

    public final boolean isProtected() {
        return Modifier.isProtected(getModifiers());
    }

    public final boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    public final boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    public final boolean isPackagePrivate() {
        return (getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)) == 0;
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

    /**
     * Human-readable brief description of a type or member, which does not
     * include information super types, thrown exceptions, or modifiers other
     * than 'static'.
     */
    public String getBriefDescription() {
        return appendBriefDescription(new StringBuilder()).toString();
    }

    /**
     * Human-readable full description of a type or member, which includes
     * specification of super types (in brief format), thrown exceptions,
     * and modifiers.
     */
    public String getDescription() {
        return appendDescription(new StringBuilder()).toString();
    }

    /**
     * Human-readable erased description of a type or member.
     */
    public String getErasedDescription() {
        return appendErasedDescription(new StringBuilder()).toString();
    }

    /**
     * Human-readable simple description of a type or member, which does not
     * include information super type or fully-qualified type names.
     */
    public String getSimpleDescription() {
        return appendSimpleDescription(new StringBuilder()).toString();
    }

    protected abstract StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName);
    protected abstract StringBuilder appendDescription(StringBuilder sb);
    protected abstract StringBuilder appendBriefDescription(StringBuilder sb);
    protected abstract StringBuilder appendErasedDescription(StringBuilder sb);
    protected abstract StringBuilder appendSignature(StringBuilder sb);
    protected abstract StringBuilder appendErasedSignature(StringBuilder sb);
    protected abstract StringBuilder appendSimpleDescription(final StringBuilder sb);

    @Override
    public String toString() {
        return getSimpleDescription();
    }

    // </editor-fold>
}

