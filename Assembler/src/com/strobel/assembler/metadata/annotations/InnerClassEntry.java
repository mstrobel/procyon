package com.strobel.assembler.metadata.annotations;

/**
 * @author Mike Strobel
 */
public final class InnerClassEntry {
    private String _innerClassName;
    private String _outerClassName;
    private String _shortName;
    private int _accessFlags;

    public InnerClassEntry(final String innerClassName, final String outerClassName, final String shortName, final int accessFlags) {
        _innerClassName = innerClassName;
        _outerClassName = outerClassName;
        _shortName = shortName;
        _accessFlags = accessFlags;
    }

    public String getInnerClassName() {
        return _innerClassName;
    }

    public String getOuterClassName() {
        return _outerClassName;
    }

    public String getShortName() {
        return _shortName;
    }

    public int getAccessFlags() {
        return _accessFlags;
    }
}
