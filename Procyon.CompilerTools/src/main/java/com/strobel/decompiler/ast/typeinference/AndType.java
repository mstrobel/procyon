package com.strobel.decompiler.ast.typeinference;

import com.strobel.assembler.metadata.TypeMetadataVisitor;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.CollectionUtilities;
import com.strobel.functions.Functions;

import java.util.LinkedHashSet;
import java.util.Set;

public class AndType extends TypeReference {
    private final Set<TypeReference> types;

    public AndType(final TypeReference... types) {
        final Set<TypeReference> actualTypes = new LinkedHashSet<>();

        for (final TypeReference type : types) {
            if (type instanceof AndType) {
                actualTypes.addAll(((AndType) type).getTypes());
            } else {
                actualTypes.add(type);
            }
        }

        this.types = actualTypes;
    }

    public Set<TypeReference> getTypes() {
        return types;
    }

    @Override
    public String getSimpleName() {
        return CollectionUtilities.collectionToString(types, Functions.<TypeReference>objectToString(), " & ");
    }


    @Override
    public String getName() {
        return getSimpleName();
    }

    @Override
    public <R, P> R accept(final TypeMetadataVisitor<P, R> visitor, final P parameter) {
        return visitor.visitAndType(this, parameter);
    }
}
