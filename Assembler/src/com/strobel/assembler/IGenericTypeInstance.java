package com.strobel.assembler;

import java.util.List;

public interface IGenericTypeInstance {
    boolean hasTypeArguments();
    List<TypeReference> getTypeArguments();
}
