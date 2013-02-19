package com.strobel.decompiler.ast;

import com.strobel.decompiler.ITextOutput;

public class Label extends Node {
    private String _name;

    public String getName() {
        return _name;
    }

    public void setName(final String name) {
        _name = name;
    }

    @Override
    public void writeTo(final ITextOutput output) {
        output.writeDefinition(getName() + ":", this);
    }
}
