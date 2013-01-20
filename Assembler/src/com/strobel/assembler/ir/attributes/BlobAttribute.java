package com.strobel.assembler.ir.attributes;

import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class BlobAttribute extends SourceAttribute {
    private final byte[] _data;

    public BlobAttribute(final String name, final byte[] data) {
        super(name, data.length);
        _data = VerifyArgument.notNull(data, "data");
    }

    public byte[] getData() {
        return _data;
    }
}
