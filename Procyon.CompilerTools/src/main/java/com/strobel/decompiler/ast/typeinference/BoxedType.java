package com.strobel.decompiler.ast.typeinference;

import com.strobel.assembler.metadata.TypeMetadataVisitor;
import com.strobel.assembler.metadata.TypeReference;

public class BoxedType extends TypeReference {
    private final TypeReference type;

    public BoxedType(TypeReference type) {
        this.type = type;
    }

    public TypeReference getType() {
        return type;
    }

    @Override
    public String getSimpleName() {
        return type.getSimpleName();
    }

    @Override
    public String getName() {
        return getSimpleName();
    }

    @Override
    public <R, P> R accept(TypeMetadataVisitor<P, R> visitor, P parameter) {
        return visitor.visitType(type, parameter);
    }
}
