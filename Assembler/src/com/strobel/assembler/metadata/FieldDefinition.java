package com.strobel.assembler.metadata;

public abstract class FieldDefinition extends FieldReference implements IConstantValueProvider {
    // <editor-fold defaultstate="collapsed" desc="Field Attributes">

    public final boolean isEnumConstant() {
        return (getModifiers() & MODIFIER_ENUM) != 0;
    }

    @Override
    public abstract boolean hasConstantValue();

    @Override
    public abstract Object getConstantValue();

    // </editor-fold>
}
