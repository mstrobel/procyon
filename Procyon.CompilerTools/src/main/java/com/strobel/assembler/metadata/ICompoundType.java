package com.strobel.assembler.metadata;

import com.strobel.annotations.NotNull;
import com.strobel.annotations.Nullable;

import java.util.List;

public interface ICompoundType {
    @Nullable
    TypeReference getBaseType();

    @NotNull
    List<TypeReference> getInterfaces();

    @Nullable
    IMetadataResolver getResolver();
}
