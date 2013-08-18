package com.strobel.assembler.metadata;

final class BottomType extends TypeDefinition {
    final static BottomType INSTANCE = new BottomType();

    private BottomType() {
        setName("__Bottom");
    }

    @Override
    public String getSimpleName() {
        return "__Bottom";
    }

    @Override
    public String getFullName() {
        return getSimpleName();
    }

    @Override
    public String getInternalName() {
        return getSimpleName();
    }

    @Override
    public final <R, P> R accept(final TypeMetadataVisitor<P, R> visitor, final P parameter) {
        return visitor.visitBottomType(this, parameter);
    }
}

final class NullType extends TypeDefinition {
    final static NullType INSTANCE = new NullType();

    private NullType() {
        setName("__Null");
    }

    @Override
    public String getSimpleName() {
        return "__Null";
    }

    @Override
    public String getFullName() {
        return getSimpleName();
    }

    @Override
    public String getInternalName() {
        return getSimpleName();
    }

    @Override
    public final <R, P> R accept(final TypeMetadataVisitor<P, R> visitor, final P parameter) {
        return visitor.visitNullType(this, parameter);
    }
}
