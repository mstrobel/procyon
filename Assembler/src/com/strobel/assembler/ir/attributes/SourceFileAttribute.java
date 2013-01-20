package com.strobel.assembler.ir.attributes;

/**
 * @author Mike Strobel
 */
public final class SourceFileAttribute extends SourceAttribute {
    private final String _sourceFile;

    public SourceFileAttribute(final String sourceFile) {
        super(AttributeNames.SourceFile, 2);
        _sourceFile = sourceFile;
    }

    public String getSourceFile() {
        return _sourceFile;
    }
}
