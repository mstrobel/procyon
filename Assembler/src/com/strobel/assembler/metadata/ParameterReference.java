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
        if (StringUtilities.isNullOrEmpty(_name)) {
            if (_position < 0) {
                return _name;
            }
            return "p" + _position;
        }
        return _name;
    }

    protected boolean hasName() {
        return !StringUtilities.isNullOrEmpty(_name);
    }

    protected void setName(final String name) {
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

    protected void setParameterType(final TypeReference parameterType) {
        _parameterType = parameterType;
    }

    @Override
    public String toString() {
        return getName();
    }

    public abstract ParameterDefinition resolve();
}
