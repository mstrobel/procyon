package com.strobel.assembler.ir.attributes;

import com.strobel.core.ArrayUtilities;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class CodeAttribute extends SourceAttribute {
    private final int _maxStack;
    private final int _maxLocals;
    private final int _codeOffset;
    private final int _codeLength;
    private final List<ExceptionTableEntry> _exceptionTableEntriesView;
    private final List<SourceAttribute> _attributesView;

    public CodeAttribute(
        final int size,
        final int maxStack,
        final int maxLocals,
        final int codeOffset,
        final int codeLength,
        final ExceptionTableEntry[] exceptionTableEntries,
        final SourceAttribute[] attributes) {

        super(AttributeNames.Code, size);

        _maxStack = maxStack;
        _maxLocals = maxLocals;
        _codeOffset = codeOffset;
        _codeLength = codeLength;
        _attributesView = ArrayUtilities.asUnmodifiableList(attributes.clone());
        _exceptionTableEntriesView = ArrayUtilities.asUnmodifiableList(exceptionTableEntries.clone());
    }

    public int getMaxStack() {
        return _maxStack;
    }

    public int getMaxLocals() {
        return _maxLocals;
    }

    public int getCodeOffset() {
        return _codeOffset;
    }

    public int getCodeLength() {
        return _codeLength;
    }

    public List<ExceptionTableEntry> getExceptionTableEntries() {
        return _exceptionTableEntriesView;
    }

    public List<SourceAttribute> getAttributes() {
        return _attributesView;
    }
}
