package com.strobel.assembler;

import java.util.List;

public interface IGenericInstance {
    boolean hasTypeArguments();
    List<TypeReference> getTypeArguments();
}
