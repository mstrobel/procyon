package com.strobel.assembler.metadata;

import com.strobel.core.StringUtilities;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:07 PM
 */
public abstract class VariableReference {
    private String _name;
    private int _index = -1;
    private TypeReference _variableType;

    protected VariableReference(final TypeReference variableType) {
        _variableType = variableType;
    }

    protected VariableReference(final String name, final TypeReference variableType) {
        _name = name;
        _variableType = variableType;
    }

    public final String getName() {
        if (!StringUtilities.isNullOrEmpty(_name)) {
            return _name;
        }

        if (_index >= 0) {
            return "$" + _index;
        }

        return null;
    }

    protected final void setName(final String name) {
        _name = name;
    }

    public final int getIndex() {
        return _index;
    }

    protected final void setIndex(final int index) {
        _index = index;
    }

    public final TypeReference getVariableType() {
        return _variableType;
    }

    protected final void setVariableType(final TypeReference variableType) {
        _variableType = variableType;
    }

    public abstract VariableDefinition resolve();

    @Override
    public String toString() {
        return getName();
    }
}
