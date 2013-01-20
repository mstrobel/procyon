package com.strobel.assembler.metadata.annotations;

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class CustomAnnotation {
    private final TypeReference _annotationType;
    private final List<AnnotationParameter> _parameters;

    public CustomAnnotation(final TypeReference annotationType, final List<AnnotationParameter> parameters) {
        _annotationType = VerifyArgument.notNull(annotationType, "annotationType");
        _parameters = VerifyArgument.notNull(parameters, "parameters");
    }

    public TypeReference getAnnotationType() {
        return _annotationType;
    }

    public boolean hasParameters() {
        return !_parameters.isEmpty();
    }

    public List<AnnotationParameter> getParameters() {
        return _parameters;
    }
}
