package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public class ParameterInfo {
    private final String _name;
    private final int _position;
    private final Type _parameterType;

    public ParameterInfo(final String name, final int position, final Type parameterType) {
        _name = name;
        _position = position;
        _parameterType = parameterType;
    }

    public String getName() {
        return _name;
    }

    public Type getParameterType() {
        return _parameterType;
    }

    public int getPosition() {
        return _position;
    }
}
