package com.strobel.assembler.ir.attributes;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;

import java.util.Collections;
import java.util.List;

/**
 * @author Mike Strobel
 */
public final class CodeAttribute extends SourceAttribute {
    private final int _maxStack;
    private final int _maxLocals;
    private final Buffer _code;
    private final List<ExceptionTableEntry> _exceptionTableEntriesView;
    private final List<SourceAttribute> _attributesView;

    public CodeAttribute(
        final int size,
        final int maxStack,
        final int maxLocals,
        final int codeOffset,
        final int codeLength,
        final Buffer buffer,
        final ExceptionTableEntry[] exceptionTableEntries,
        final SourceAttribute[] attributes) {

        super(AttributeNames.Code, size);

        VerifyArgument.notNull(buffer, "buffer");
        VerifyArgument.notNull(exceptionTableEntries, "exceptionTableEntries");
        VerifyArgument.notNull(attributes, "attributes");

        _maxStack = maxStack;
        _maxLocals = maxLocals;

        final Buffer code = new Buffer(codeLength);

        System.arraycopy(
            buffer.array(),
            codeOffset,
            code.array(),
            0,
            codeLength
        );

        _code = code;
        _attributesView = ArrayUtilities.asUnmodifiableList(attributes.clone());
        _exceptionTableEntriesView = ArrayUtilities.asUnmodifiableList(exceptionTableEntries.clone());
    }

    public CodeAttribute(
        final int size,
        final int maxStack,
        final int maxLocals,
        final SourceAttribute[] attributes) {

        super(AttributeNames.Code, size);

        VerifyArgument.notNull(attributes, "attributes");

        _maxStack = maxStack;
        _maxLocals = maxLocals;
        _code = null;
        _attributesView = ArrayUtilities.asUnmodifiableList(attributes.clone());
        _exceptionTableEntriesView = Collections.emptyList();
    }

    public int getMaxStack() {
        return _maxStack;
    }

    public int getMaxLocals() {
        return _maxLocals;
    }

    public boolean hasCode() {
        return _code != null;
    }

    public Buffer getCode() {
        return _code;
    }

    public List<ExceptionTableEntry> getExceptionTableEntries() {
        return _exceptionTableEntriesView;
    }

    public List<SourceAttribute> getAttributes() {
        return _attributesView;
    }
}
