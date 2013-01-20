package com.strobel.assembler.metadata;

import java.util.List;

public interface IGenericInstance {
    boolean hasTypeArguments();
    List<TypeReference> getTypeArguments();
    IGenericParameterProvider getGenericDefinition();
}

