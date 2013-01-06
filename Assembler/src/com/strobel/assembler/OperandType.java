package com.strobel.assembler;

public enum OperandType {
    /**
     * Opcode is not followed by any operands.
     */
    None(0),
    /**
     * Opcode is followed by a primitive type code.
     */
    PrimitiveTypeCode(1),
    /**
     * Opcode is followed by a type reference.
     */
    TypeReference(2),
    /**
     * Opcode is followed by a type reference and an unsigned byte.
     */
    TypeReferenceU1(3),
    /**
     * Opcode is followed by a method reference.
     */
    MethodReference(2),
    /**
     * Opcode is followed by a field reference.
     */
    FieldReference(2),
    /**
     * Opcode is followed by a 2-byte branch offset.
     */
    BranchTarget(2),
    /**
     * Opcode is followed by a signed byte.
     */
    I1(1),
    /**
     * Opcode is followed by a signed short integer.
     */
    I2(2),
    /**
     * Opcode is followed by a signed integer.
     */
    I4(4),
    /**
     * Opcode is followed by a signed long integer.
     */
    I8(8),
    /**
     * Opcode is followed by an unsigned byte.
     */
    U1(1),
    /**
     * Opcode is followed by an unsigned short integer.
     */
    U2(2),
    /**
     * Opcode is followed by a 4-byte floating point number.
     */
    R4(4),
    /**
     * Opcode is followed by an 8-byte floating point number.
     */
    R8(8),
    /**
     * Opcode is followed by a string.
     */
    String(2),
    /**
     * Opcode is followed by variable number of operands, depending
     * on the instruction.
     */
    Switch(-1),
    /**
     * Opcode is followed by a 1-byte reference to a local variable.
     */
    Local(1),
    /**
     * Opcode is followed by a 1-byte reference to a local variable
     * and a signed byte value.
     */
    LocalI1(2),
    /**
     * Opcode is followed by a 2-byte reference to a local variable
     * and a signed short integer.
     */
    LocalI2(4);

    private final int size;

    OperandType(final int size) {
        this.size = size;
    }

    public final int getBaseSize() {
        return this.size;
    }
}
