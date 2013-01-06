package com.strobel.assembler;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:11 PM
 */
public final class VariableDefinition extends VariableReference {
    public VariableDefinition(final TypeReference variableType) {
        super(variableType);
    }

    public VariableDefinition(final String name, final TypeReference variableType) {
        super(name, variableType);
    }

    @Override
    public VariableDefinition resolve() {
        return this;
    }
}
