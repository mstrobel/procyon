package com.strobel.assembler.ir;

/**
 * @author Mike Strobel
 */
public interface CodeOutputStream {
    void put(final byte b);
    void put(final byte[] array, final int offset, final int length);

    void putByte(final int value);
    void putShort(final int value);
    void putInteger(final int value);
    void putLong(final long value);
    void putFloat(final float value);
    void putDouble(final double value);
    void putUtf8(final String value);
}
