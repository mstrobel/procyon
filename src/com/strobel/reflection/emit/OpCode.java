package com.strobel.reflection.emit;

import static com.strobel.reflection.emit.OperandType.*;

/**
 * @author strobelm
 */
public enum OpCode {
    NOP(0x0),
    ACONST_NULL(0x1),
    ICONST_M1(0x2),
    ICONST_0(0x3),
    ICONST_1(0x4),
    ICONST_2(0x5),
    ICONST_3(0x6),
    ICONST_4(0x7),
    ICONST_5(0x8),
    LCONST_0(0x9),
    LCONST_1(0xa),
    FCONST_0(0xb),
    FCONST_1(0xc),
    FCONST_2(0xd),
    DCONST_0(0xe),
    DCONST_1(0xf),
    BIPUSH(0x10, Byte),
    SIPUSH(0x11, Short),
    LDC(0x12, CPRef),
    LDC_W(0x13, CPRefWide),
    LDC2_W(0x14, CPRefWide),
    ILOAD(0x15, Local),
    LLOAD(0x16, Local),
    FLOAD(0x17, Local),
    DLOAD(0x18, Local),
    ALOAD(0x19, Local),
    ILOAD_0(0x1a),
    ILOAD_1(0x1b),
    ILOAD_2(0x1c),
    ILOAD_3(0x1d),
    LLOAD_0(0x1e),
    LLOAD_1(0x1f),
    LLOAD_2(0x20),
    LLOAD_3(0x21),
    FLOAD_0(0x22),
    FLOAD_1(0x23),
    FLOAD_2(0x24),
    FLOAD_3(0x25),
    DLOAD_0(0x26),
    DLOAD_1(0x27),
    DLOAD_2(0x28),
    DLOAD_3(0x29),
    ALOAD_0(0x2a),
    ALOAD_1(0x2b),
    ALOAD_2(0x2c),
    ALOAD_3(0x2d),
    IALOAD(0x2e),
    LALOAD(0x2f),
    FALOAD(0x30),
    DALOAD(0x31),
    AALOAD(0x32),
    BALOAD(0x33),
    CALOAD(0x34),
    SALOAD(0x35),
    ISTORE(0x36, Local),
    LSTORE(0x37, Local),
    FSTORE(0x38, Local),
    DSTORE(0x39, Local),
    ASTORE(0x3a, Local),
    ISTORE_0(0x3b),
    ISTORE_1(0x3c),
    ISTORE_2(0x3d),
    ISTORE_3(0x3e),
    LSTORE_0(0x3f),
    LSTORE_1(0x40),
    LSTORE_2(0x41),
    LSTORE_3(0x42),
    FSTORE_0(0x43),
    FSTORE_1(0x44),
    FSTORE_2(0x45),
    FSTORE_3(0x46),
    DSTORE_0(0x47),
    DSTORE_1(0x48),
    DSTORE_2(0x49),
    DSTORE_3(0x4a),
    ASTORE_0(0x4b),
    ASTORE_1(0x4c),
    ASTORE_2(0x4d),
    ASTORE_3(0x4e),
    IASTORE(0x4f),
    LASTORE(0x50),
    FASTORE(0x51),
    DASTORE(0x52),
    AASTORE(0x53),
    BASTORE(0x54),
    CASTORE(0x55),
    SASTORE(0x56),
    POP(0x57),
    POP2(0x58),
    DUP(0x59),
    DUP_X1(0x5a),
    DUP_X2(0x5b),
    DUP2(0x5c),
    DUP2_X1(0x5d),
    DUP2_X2(0x5e),
    SWAP(0x5f),
    IADD(0x60),
    LADD(0x61),
    FADD(0x62),
    DADD(0x63),
    ISUB(0x64),
    LSUB(0x65),
    FSUB(0x66),
    DSUB(0x67),
    IMUL(0x68),
    LMUL(0x69),
    FMUL(0x6a),
    DMUL(0x6b),
    IDIV(0x6c),
    LDIV(0x6d),
    FDIV(0x6e),
    DDIV(0x6f),
    IREM(0x70),
    LREM(0x71),
    FREM(0x72),
    DREM(0x73),
    INEG(0x74),
    LNEG(0x75),
    FNEG(0x76),
    DNEG(0x77),
    ISHL(0x78),
    LSHL(0x79),
    ISHR(0x7a),
    LSHR(0x7b),
    IUSHR(0x7c),
    LUSHR(0x7d),
    IAND(0x7e),
    LAND(0x7f),
    IOR(0x80),
    LOR(0x81),
    IXOR(0x82),
    LXOR(0x83),
    IINC(0x84, LocalByte),
    I2L(0x85),
    I2F(0x86),
    I2D(0x87),
    L2I(0x88),
    L2F(0x89),
    L2D(0x8a),
    F2I(0x8b),
    F2L(0x8c),
    F2D(0x8d),
    D2I(0x8e),
    D2L(0x8f),
    D2F(0x90),
    I2B(0x91),
    I2C(0x92),
    I2S(0x93),
    LCMP(0x94),
    FCMPL(0x95),
    FCMPG(0x96),
    DCMPL(0x97),
    DCMPG(0x98),
    IFEQ(0x99, Branch),
    IFNE(0x9a, Branch),
    IFLT(0x9b, Branch),
    IFGE(0x9c, Branch),
    IFGT(0x9d, Branch),
    IFLE(0x9e, Branch),
    IF_ICMPEQ(0x9f, Branch),
    IF_ICMPNE(0xa0, Branch),
    IF_ICMPLT(0xa1, Branch),
    IF_ICMPGE(0xa2, Branch),
    IF_ICMPGT(0xa3, Branch),
    IF_ICMPLE(0xa4, Branch),
    IF_ACMPEQ(0xa5, Branch),
    IF_ACMPNE(0xa6, Branch),
    GOTO(0xa7, Branch),
    JSR(0xa8, Branch),
    RET(0xa9, Local),
    TABLESWITCH(0xaa, Dynamic),
    LOOKUPSWITCH(0xab, Dynamic),
    IRETURN(0xac),
    LRETURN(0xad),
    FRETURN(0xae),
    DRETURN(0xaf),
    ARETURN(0xb0),
    RETURN(0xb1),
    GETSTATIC(0xb2, CPRefWide),
    PUTSTATIC(0xb3, CPRefWide),
    GETFIELD(0xb4, CPRefWide),
    PUTFIELD(0xb5, CPRefWide),
    INVOKEVIRTUAL(0xb6, CPRefWide),
    INVOKESPECIAL(0xb7, CPRefWide),
    INVOKESTATIC(0xb8, CPRefWide),
    INVOKEINTERFACE(0xb9, CPRefWideUByteZero),
    INVOKEDYNAMIC(0xba, CPRefWideUByteZero),
    NEW(0xbb, CPRefWide),
    NEWARRAY(0xbc, Type),
    ANEWARRAY(0xbd, CPRefWide),
    ARRAYLENGTH(0xbe),
    ATHROW(0xbf),
    CHECKCAST(0xc0, CPRefWide),
    INSTANCEOF(0xc1, CPRefWide),
    MONITORENTER(0xc2),
    MONITOREXIT(0xc3),

    // wide 0xc4
    MULTIANEWARRAY(0xc5, CPRefWideUByte),
    IFNULL(0xc6, Branch),
    IFNONNULL(0xc7, Branch),
    GOTO_W(0xc8, BranchW),
    JSR_W(0xc9, BranchW),

    BREAKPOINT(0xc9, NoOperands),

    // wide opcodes
    ILOAD_W(0xc415, WideCPRefWide),
    LLOAD_W(0xc416, WideCPRefWide),
    FLOAD_W(0xc417, WideCPRefWide),
    DLOAD_W(0xc418, WideCPRefWide),
    ALOAD_W(0xc419, WideCPRefWide),
    ISTORE_W(0xc436, WideCPRefWide),
    LSTORE_W(0xc437, WideCPRefWide),
    FSTORE_W(0xc438, WideCPRefWide),
    DSTORE_W(0xc439, WideCPRefWide),
    ASTORE_W(0xc43a, WideCPRefWide),
    IINC_W(0xc484, WideCPRefWideShort),
    RET_W(0xc4a9, WideCPRefWide);

    private OpCode(final int code) {
        this(code, NoOperands);
    }

    private OpCode(final int code, final OperandType operandType) {
        this._code = code;
        this._operandType = operandType;
    }

    public int getCode() {
        return _code;
    }

    public OperandType getOperandType() {
        return _operandType;
    }

    public int getSize() {
        return ((_code >> 16) == 0xc4) ? 2 : 1;
    }
    
    public int getSizeWithOperands() {
        return _operandType.length;
    }

    public OpCode negate() {
        if (this == IFNULL) {
            return IFNONNULL;
        }
        else if (this == IFNONNULL) {
            return IFNULL;
        }
        else return get(((_code + 1) ^ 1) - 1);
    }

    private final int _code;
    private final OperandType _operandType;

    /** Get the OpCode for a simple standard 1-byte opcode. */
    public static OpCode get(final int code) {
        return getOpcodeBlock(code >> 8)[code & 0xff];
    }

    private static OpCode[] getOpcodeBlock(final int prefix) {
        switch (prefix) {
            case STANDARD:
                return _standardOpCodes;
            case WIDE:
                return _wideOpCodes;
            default:
                return null;
        }

    }
    
    /** The byte prefix for the wide instructions. */
    public static final int STANDARD = 0x00;
    public static final int WIDE = 0xc4;
    
    private static OpCode[] _standardOpCodes = new OpCode[256];
    private static OpCode[] _wideOpCodes = new OpCode[256];
    
    static {
        for (final OpCode o: values())
            getOpcodeBlock(o._code >> 8)[o._code & 0xff] = o;
    }
}
