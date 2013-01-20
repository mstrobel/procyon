package com.strobel.assembler.ir.attributes;

/**
 * @author Mike Strobel
 */
public final class LineNumberTableEntry {
    private final int _offset;
    private final int _lineNumber;

    public LineNumberTableEntry(final int offset, final int lineNumber) {
        _offset = offset;
        _lineNumber = lineNumber;
    }

    public int getOffset() {
        return _offset;
    }

    public int getLineNumber() {
        return _lineNumber;
    }
}
