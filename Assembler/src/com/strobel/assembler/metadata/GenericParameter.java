package com.strobel.assembler.metadata;

import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;

import java.util.Collections;
import java.util.List;

public final class GenericParameter extends TypeReference {
    private String _name;
    private int _position;
    private GenericParameterType _type = GenericParameterType.Type;
    private IGenericParameterProvider _owner;
    private TypeReference _extendsBound;
    private List<CustomAnnotation> _customAnnotations;

    public GenericParameter(final String name, final TypeReference extendsBound) {
        _name = name != null ? name : StringUtilities.EMPTY;
        _extendsBound = VerifyArgument.notNull(extendsBound, "extendsBound");
    }

    void setPosition(final int position) {
        _position = position;
    }

    void setOwner(final IGenericParameterProvider owner) {
        _owner = owner;

        _type = owner instanceof MethodReference ? GenericParameterType.Method
                                                 : GenericParameterType.Type;
    }

    @Override
    public String getName() {
        final String name = _name;

        if (!StringUtilities.isNullOrEmpty(name)) {
            return name;
        }

        return "T" + _position;
    }

    @Override
    public String getFullName() {
        return getName();
    }

    @Override
    public boolean isGenericParameter() {
        return true;
    }

    @Override
    public boolean containsGenericParameters() {
        return true;
    }

    @Override
    public TypeReference getDeclaringType() {
        final IGenericParameterProvider owner = _owner;

        if (owner instanceof TypeReference) {
            return (TypeReference) owner;
        }

        return null;
    }

    public int getPosition() {
        return _position;
    }

    public GenericParameterType getType() {
        return _type;
    }

    public IGenericParameterProvider getOwner() {
        return _owner;
    }

    @Override
    public boolean hasExtendsBound() {
        return true;
    }

    @Override
    public TypeReference getExtendsBound() {
        return _extendsBound;
    }

    @Override
    public boolean hasAnnotations() {
        return !getAnnotations().isEmpty();
    }

    @Override
    public List<CustomAnnotation> getAnnotations() {
        if (_customAnnotations == null) {
            synchronized (this) {
                if (_customAnnotations == null) {
                    _customAnnotations = populateCustomAnnotations();
                }
            }
        }
        return _customAnnotations;
    }

    @Override
    public long getFlags() {
        return Flags.PUBLIC;
    }

    @Override
    protected StringBuilder appendDescription(final StringBuilder sb) {
        return null;
    }

    @Override
    protected StringBuilder appendBriefDescription(final StringBuilder sb) {
        return null;
    }

    @Override
    protected StringBuilder appendErasedDescription(final StringBuilder sb) {
        return null;
    }

    @Override
    protected StringBuilder appendSignature(final StringBuilder sb) {
        return null;
    }

    @Override
    protected StringBuilder appendErasedSignature(final StringBuilder sb) {
        return null;
    }

    @Override
    protected StringBuilder appendSimpleDescription(final StringBuilder sb) {
        return null;
    }

    @Override
    public TypeDefinition resolve() {
        return null;
    }

    // <editor-fold defaultstate="collapsed" desc="Metadata Loading">

    private List<CustomAnnotation> populateCustomAnnotations() {
        return Collections.emptyList();
    }

    // </editor-fold>
}
