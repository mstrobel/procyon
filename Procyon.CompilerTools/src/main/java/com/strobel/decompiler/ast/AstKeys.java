package com.strobel.decompiler.ast;

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.componentmodel.Key;
import com.strobel.util.ContractUtils;

import java.util.List;

public final class AstKeys {
    public final static Key<List<TypeReference>> TYPE_ARGUMENTS = Key.create("TypeArguments");

    private AstKeys() {
        throw ContractUtils.unreachable();
    }
}
