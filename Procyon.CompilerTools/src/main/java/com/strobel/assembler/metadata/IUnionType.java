package com.strobel.assembler.metadata;

import com.strobel.annotations.NotNull;

import java.util.List;

public interface IUnionType {
    @NotNull
    List<TypeReference> getAlternatives();
}
