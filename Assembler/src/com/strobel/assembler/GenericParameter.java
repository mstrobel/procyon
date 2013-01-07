package com.strobel.assembler;

import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;

import java.util.Collections;
import java.util.List;

public final class GenericParameter extends TypeReference implements IAnnotationsProvider {
    private String _name;
    private int _position;
    private GenericParameterType _type = GenericParameterType.Type;
    private IGenericParameterProvider _owner;
    private List<TypeReference> _upperBounds;
    private List<ICustomAnnotation> _customAnnotations;

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

    public boolean hasUpperBounds() {
        return !getUpperBounds().isEmpty();
    }

    public List<TypeReference> getUpperBounds() {
        if (_upperBounds == null) {
            synchronized (this) {
                if (_upperBounds == null) {
                    _upperBounds = populateUpperBounds();
                }
            }
        }
        return _upperBounds;
    }

    @Override
    public boolean hasAnnotations() {
        return !getAnnotations().isEmpty();
    }

    @Override
    public List<ICustomAnnotation> getAnnotations() {
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
    public int getModifiers() {
        return 0;
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

    private List<ICustomAnnotation> populateCustomAnnotations() {
        return Collections.emptyList();
    }

    private List<TypeReference> populateUpperBounds() {
        return Collections.emptyList();
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
    protected void afterSet(final int index, final GenericParameter p) {
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