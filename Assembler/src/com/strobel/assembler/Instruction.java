package com.strobel.assembler;

import com.strobel.core.VerifyArgument;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 1:31 AM
 */
public final class Instruction {
    private int _offset;
    private OpCode _opCode;
    private Object _operand;

    private Instruction _previous;
    private Instruction _next;

    public Instruction(final int offset, final OpCode opCode) {
        _offset = offset;
        _opCode = opCode;
    }

    public Instruction(final OpCode opCode, final Object operand) {
        _opCode = opCode;
        _operand = operand;
    }

    public int getOffset() {
        return _offset;
    }

    public void setOffset(final int offset) {
        _offset = offset;
    }

    public OpCode getOpCode() {
        return _opCode;
    }

    public void setOpCode(final OpCode opCode) {
        _opCode = opCode;
    }

    public Object getOperand() {
        return _operand;
    }

    public void setOperand(final Object operand) {
        _operand = operand;
    }

    public Instruction getPrevious() {
        return _previous;
    }

    public void setPrevious(final Instruction previous) {
        _previous = previous;
    }

    public Instruction getNext() {
        return _next;
    }

    public void setNext(final Instruction next) {
        _next = next;
    }

    public int getSize() {
        switch (_opCode) {
            case TABLESWITCH:
                return (3 + ((Instruction[]) _operand).length) * 4;
            case LOOKUPSWITCH:
                return ((Instruction[]) _operand).length * 4;
        }
        return _opCode.getSize();
    }

    // <editor-fold defaultstate="collapsed" desc="Factory Methods">

    public static Instruction create(final OpCode opCode) {
        VerifyArgument.notNull(opCode, "opCode");

        if (opCode.getOperandType() != OperandType.NoOperands) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, null);
    }

    public static Instruction create(final OpCode opCode, final Instruction target) {
        VerifyArgument.notNull(opCode, "opCode");
        VerifyArgument.notNull(target, "target");

        if (opCode.getOperandType() != OperandType.BranchTarget) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, target);
    }

    public static Instruction create(final OpCode opCode, final Instruction[] target) {
        VerifyArgument.notNull(opCode, "opCode");
        VerifyArgument.notNull(target, "target");

        if (opCode.getOperandType() != OperandType.Switch) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, target);
    }

    // </editor-fold>
}
