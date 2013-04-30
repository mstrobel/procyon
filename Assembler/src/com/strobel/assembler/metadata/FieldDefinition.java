/*
 * FieldDefinition.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler.metadata;

import com.strobel.assembler.Collection;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;

import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.List;

public class FieldDefinition extends FieldReference implements IMemberDefinition, IConstantValueProvider {
    private final Collection<CustomAnnotation> _customAnnotations;
    private final Collection<SourceAttribute> _sourceAttributes;
    private final List<CustomAnnotation> _customAnnotationsView;
    private final List<SourceAttribute> _sourceAttributesView;
    private final IMetadataResolver _resolver;

    private String _name;
    private Object _fieldType;
    private TypeDefinition _declaringType;
    private Object _constantValue;
    private long _flags;

    protected FieldDefinition(final IMetadataResolver resolver) {
        _resolver = resolver;
        _customAnnotations = new Collection<>();
        _customAnnotationsView = Collections.unmodifiableList(_customAnnotations);
        _sourceAttributes = new Collection<>();
        _sourceAttributesView = Collections.unmodifiableList(_sourceAttributes);
    }

    @Override
    public final List<CustomAnnotation> getAnnotations() {
        return _customAnnotationsView;
    }

    protected final Collection<CustomAnnotation> getAnnotationsInternal() {
        return _customAnnotations;
    }

    public final List<SourceAttribute> getSourceAttributes() {
        return _sourceAttributesView;
    }

    protected final Collection<SourceAttribute> getSourceAttributesInternal() {
        return _sourceAttributes;
    }

    // <editor-fold defaultstate="collapsed" desc="Field Attributes">

    public final boolean isEnumConstant() {
        return Flags.testAny(getFlags(), Flags.ENUM);
    }

    @Override
    public final boolean hasConstantValue() {
        return _constantValue != null;
    }

    @Override
    public final Object getConstantValue() {
        return _constantValue;
    }

    public final TypeReference getFieldType() {
        if (_fieldType instanceof TypeReference) {
            return (TypeReference) _fieldType;
        }

        if (_fieldType instanceof String &&
            _resolver != null) {

            final TypeReference fieldType = _resolver.lookupType((String) _fieldType);

            if (fieldType != null) {
                _fieldType = fieldType;
                return fieldType;
            }
        }

        return null;
    }

    protected final void setFieldType(final TypeReference fieldType) {
        _fieldType = fieldType;
    }

    protected final void setConstantValue(final Object constantValue) {
        _constantValue = constantValue;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Member Attributes">

    public final String getName() {
        return _name;
    }

    protected final void setName(final String name) {
        _name = name;
    }

    @Override
    public final boolean isDefinition() {
        return true;
    }

    public final TypeDefinition getDeclaringType() {
        return _declaringType;
    }

    protected final void setDeclaringType(final TypeDefinition declaringType) {
        _declaringType = declaringType;
    }

    public final long getFlags() {
        return _flags;
    }

    protected final void setFlags(final long flags) {
        _flags = flags;
    }

    public final int getModifiers() {
        return Flags.toModifiers(getFlags());
    }

    public final boolean isFinal() {
        return Flags.testAny(getFlags(), Flags.FINAL);
    }

    public final boolean isNonPublic() {
        return !Flags.testAny(getFlags(), Flags.PUBLIC);
    }

    public final boolean isPrivate() {
        return Flags.testAny(getFlags(), Flags.PRIVATE);
    }

    public final boolean isProtected() {
        return Flags.testAny(getFlags(), Flags.PROTECTED);
    }

    public final boolean isPublic() {
        return Flags.testAny(getFlags(), Flags.PUBLIC);
    }

    public final boolean isStatic() {
        return Flags.testAny(getFlags(), Flags.STATIC);
    }

    public final boolean isSynthetic() {
        return Flags.testAny(getFlags(), Flags.SYNTHETIC);
    }

    public final boolean isDeprecated() {
        return Flags.testAny(getFlags(), Flags.DEPRECATED);
    }

    public final boolean isPackagePrivate() {
        return !Flags.testAny(getFlags(), Flags.PUBLIC | Flags.PROTECTED | Flags.PRIVATE);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Name and Signature Formatting">

    /**
     * Human-readable brief description of a type or member, which does not include information super types, thrown exceptions, or modifiers other than
     * 'static'.
     */
    public String getBriefDescription() {
        return appendBriefDescription(new StringBuilder()).toString();
    }

    /**
     * Human-readable full description of a type or member, which includes specification of super types (in brief format), thrown exceptions, and modifiers.
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
     * Human-readable simple description of a type or member, which does not include information super type or fully-qualified type names.
     */
    public String getSimpleDescription() {
        return appendSimpleDescription(new StringBuilder()).toString();
    }

    @Override
    protected StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        if (fullName) {
            final TypeDefinition declaringType = getDeclaringType();

            if (declaringType != null) {
                return declaringType.appendName(sb, true, false).append('.').append(getName());
            }
        }

        return sb.append(_name);
    }

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

    protected StringBuilder appendBriefDescription(final StringBuilder sb) {
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

    protected StringBuilder appendSimpleDescription(final StringBuilder sb) {
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
            s = fieldType.appendSimpleDescription(s);
        }

        s.append(' ');
        s.append(getName());

        return s;
    }

    @Override
    public String toString() {
        return getSimpleDescription();
    }

    // </editor-fold>}
}
