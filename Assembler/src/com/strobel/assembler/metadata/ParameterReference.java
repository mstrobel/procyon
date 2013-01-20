package com.strobel.assembler.metadata;

import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 5:41 PM
 */
public abstract class ParameterReference {
    private String _name;
    private int _position = -1;
    private TypeReference _parameterType;

    protected ParameterReference(final String name, final TypeReference parameterType) {
        _name = name != null ? name : StringUtilities.EMPTY;
        _parameterType = VerifyArgument.notNull(parameterType, "parameterType");
    }

    public String getName() {
        return _name;
    }

    public void setName(final String name) {
        _name = name;
    }

    public int getPosition() {
        return _position;
    }

    protected void setPosition(final int position) {
        _position = position;
    }

    public TypeReference getParameterType() {
        return _parameterType;
    }

    public void setParameterType(final TypeReference parameterType) {
        _parameterType = parameterType;
    }

    @Override
    public String toString() {
        return getName();
    }

    public abstract ParameterDefinition resolve();
}
