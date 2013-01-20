package com.strobel.assembler.ir.attributes;

import com.strobel.assembler.metadata.TypeReference;

/**
 * @author Mike Strobel
 */
public final class ExceptionTableEntry {
    private final int _startOffset;
    private final int _endOffset;
    private final int _handlerOffset;
    private final TypeReference _catchType;

    public ExceptionTableEntry(final int startOffset, final int endOffset, final int handlerOffset, final TypeReference catchType) {
        _startOffset = startOffset;
        _endOffset = endOffset;
        _handlerOffset = handlerOffset;
        _catchType = catchType;
    }

    public int getStartOffset() {
        return _startOffset;
    }

    public int getEndOffset() {
        return _endOffset;
    }

    public int getHandlerOffset() {
        return _handlerOffset;
    }

    public TypeReference getCatchType() {
        return _catchType;
    }
}
