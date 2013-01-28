package com.strobel.assembler.metadata;

import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.StringUtilities;

import java.util.Collections;
import java.util.List;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 5:42 PM
 */
public final class ParameterDefinition extends ParameterReference implements IAnnotationsProvider {
    private IMethodSignature _method;
    private List<CustomAnnotation> _annotations;

    public ParameterDefinition(final TypeReference parameterType) {
        super(StringUtilities.EMPTY, parameterType);
    }

    public ParameterDefinition(final String name, final TypeReference parameterType) {
        super(name, parameterType);
    }

    public IMethodSignature getMethod() {
        return _method;
    }

    void setMethod(final IMethodSignature method) {
        _method = method;
    }

    @Override
    public boolean hasAnnotations() {
        return !getAnnotations().isEmpty();
    }

    @Override
    public List<CustomAnnotation> getAnnotations() {
        if (_annotations == null) {
            synchronized (this) {
                if (_annotations == null) {
                    _annotations = populateCustomAnnotations();
                }
            }
        }
        return _annotations;
    }

    @Override
    public ParameterDefinition resolve() {
        final TypeReference resolvedParameterType = super.getParameterType().resolve();

        if (resolvedParameterType != null) {
            setParameterType(resolvedParameterType);
        }

        return this;
    }

    // <editor-fold defaultstate="collapsed" desc="Metadata Loading">

    private List<CustomAnnotation> populateCustomAnnotations() {
        return Collections.emptyList();
    }

    // </editor-fold>
}
