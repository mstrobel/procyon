package com.strobel.assembler.metadata;

import com.strobel.assembler.Collection;
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

    GenericParameter(final String name) {
        _name = name != null ? name : StringUtilities.EMPTY;
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
        if (_extendsBound == null) {
            synchronized (this) {
                if (_extendsBound == null) {
                    _extendsBound = populateExtendsBound();
                }
            }
        }
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

    private TypeReference populateExtendsBound() {
        return BuiltinTypes.Object;
    }

    // </editor-fold>
}

final class GenericParameterCollection extends Collection<GenericParameter> {
    private final IGenericParameterProvider _owner;

    GenericParameterCollection(final IGenericParameterProvider owner) {
        _owner = VerifyArgument.notNull(owner, "owner");
    }

    private void updateGenericParameter(final int index, final GenericParameter p) {
        p.setOwner(_owner);
        p.setPosition(index);
    }

    @Override
    protected void afterAdd(final int index, final GenericParameter p, final boolean appended) {
        updateGenericParameter(index, p);

        if (!appended) {
            for (int i = index + 1; i < size(); i++) {
                get(i).setPosition(i + 1);
            }
        }
    }

    @Override
    protected void beforeSet(final int index, final GenericParameter p) {
        final GenericParameter current = get(index);

        current.setOwner(null);
        current.setPosition(-1);

        updateGenericParameter(index, p);
    }

    @Override
    protected void afterRemove(final int index, final GenericParameter p) {
        p.setOwner(null);
        p.setPosition(-1);

        for (int i = index; i < size(); i++) {
            get(i).setPosition(i);
        }
    }

    @Override
    protected void beforeClear() {
        super.beforeClear();
    }
}