package com.strobel.assembler;

public enum OperandType {
    /**
     * Opcode is not followed by any operands.
     */
    NoOperands(1),
    /**
     * Opcode is followed by a byte indicating a type.
     */
    Type(2),
    /**
     * Opcode is followed by a 2-byte branch offset.
     */
    BranchTarget(3),
    /**
     * Opcode is followed by a 4-byte branch offset.
     */
    BranchTargetWide(5),
    /**
     * Opcode is followed by a signed byte value.
     */
    I1(2),
    /**
     * Opcode is followed by a 1-byte index into the constant pool.
     */
    CPRef(2),
    /**
     * Opcode is followed by a 2-byte index into the constant pool.
     */
    CPRefWide(3),
    /**
     * Opcode is followed by a 2-byte index into the constant pool,
     * an unsigned byte value.
     */
    CPRefWideU1(4),
    /**
     * Opcode is followed by a 2-byte index into the constant pool.,
     * an unsigned byte value, and a zero byte.
     */
    CPRefWideU1Zero(5),
    /**
     * Opcode is followed by variable number of operands, depending
     * on the instruction.
     */
    Switch(-1),
    /**
     * Opcode is followed by a 1-byte reference to a local variable.
     */
    Local(2),
    /**
     * Opcode is followed by a 1-byte reference to a local variable,
     * and a signed byte value.
     */
    LocalI1(3),
    /**
     * Opcode is followed by a signed short value.
     */
    I2(3),
    /**
     * Wide opcode is not followed by any operands.
     */
    WideNoOperands(2),
    /**
     * Wide opcode is followed by a 2-byte index into the constant pool.
     */
    WideCPRefWide(4),
    /**
     * Wide opcode is followed by a 2-byte index into the constant pool,
     * and a signed short value.
     */
    WideCPRefWideI2(6),
    /**
     * Opcode was not recognized.
     */
    Unknown(1);

    @SuppressWarnings("PackageVisibleField")
    final int length;

    OperandType(final int length) {
        this.length = length;
    }

    public int size() {
        return length;
    }
}
