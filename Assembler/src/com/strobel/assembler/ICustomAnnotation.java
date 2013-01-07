package com.strobel.assembler;

import java.util.List;

public interface ICustomAnnotation {
    TypeReference getAnnotationType();
    boolean hasParameters();
    List<AnnotationParameter> getParameters();
}
