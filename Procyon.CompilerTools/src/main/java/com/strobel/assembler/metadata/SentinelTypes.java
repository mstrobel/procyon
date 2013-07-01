package com.strobel.assembler.metadata;

final class BottomType extends TypeDefinition {
    final static BottomType INSTANCE = new BottomType();

    private BottomType() {
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
}

final class NullType extends TypeDefinition {
    final static NullType INSTANCE = new NullType();

    private NullType() {
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
}
