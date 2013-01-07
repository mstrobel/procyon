package com.strobel.assembler;

import java.util.List;

public interface IGenericParameterProvider {
    boolean hasGenericParameters();
    boolean isGenericDefinition();
    List<GenericParameter> getGenericParameters();
}


