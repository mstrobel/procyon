/*
 * MethodReader.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler.ir;

import com.strobel.assembler.ir.attributes.*;
import com.strobel.assembler.metadata.*;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;

import java.util.Arrays;
import java.util.List;

/**
 * @author Mike Strobel
 */
public class MethodReader implements CodeReader {
    private final static String[] LOCAL_VARIABLE_TABLES = {AttributeNames.LocalVariableTypeTable, AttributeNames.LocalVariableTable};

    private final CodeAttribute _code;
    private final IMetadataScope _scope;
    private final MethodBody _methodBody;

    public MethodReader(final CodeAttribute code, final IMetadataScope scope) {
        _code = VerifyArgument.notNull(code, "code");
        _scope = VerifyArgument.notNull(scope, "scope");
        _methodBody = new MethodBody();
        _methodBody.setCodeSize(_code.getCode().size());
        _methodBody.setMaxStackSize(_code.getMaxStack());
        _methodBody.setMaxLocals(_code.getMaxLocals());
    }

    @Override
    public MethodBody accept(final InstructionVisitor visitor) {
        return accept(null, visitor);
    }

    @SuppressWarnings("ConstantConditions")
    public MethodBody accept(final MethodVisitor methodVisitor, final InstructionVisitor visitor) {
        final Buffer b = _code.getCode();
        final InstructionCollection body = _methodBody.getInstructions();
        final VariableDefinitionCollection variables = _methodBody.getVariables();
        final VariableDefinition[] locals = new VariableDefinition[_code.getMaxLocals()];

        for (final String tableName : LOCAL_VARIABLE_TABLES) {
            final LocalVariableTableAttribute variableTable = SourceAttribute.find(tableName, _code.getAttributes());

            if (variableTable != null) {
                final List<LocalVariableTableEntry> entries = variableTable.getEntries();

                for (int i = 0; i < entries.size(); i++) {
                    final LocalVariableTableEntry entry = entries.get(i);
                    final int localIndex = entry.getIndex();

                    if (locals[localIndex] == null) {
                        locals[localIndex] = new VariableDefinition(entry.getName(), entry.getType());
                    }
                    else if (!locals[localIndex].hasName() &&
                             !StringUtilities.isNullOrEmpty(entry.getName())) {

                        locals[localIndex] = new VariableDefinition(
                            entry.getName(),
                            locals[localIndex].getVariableType()
                        );
                    }
                }
            }
        }

        for (int i = 0; i < locals.length; i++) {
            if (locals[i] == null) {
                locals[i] = new VariableDefinition(BuiltinTypes.Object);
            }

            variables.add(locals[i]);
        }

        @SuppressWarnings("unchecked")
        final Fixup[] fixups = new Fixup[b.size()];

        while (b.position() < b.size()) {
            final int offset = b.position();

            int code = b.readUnsignedByte();

            if (code == OpCode.WIDE) {
                code = code << 8 | b.readUnsignedByte();
            }

            final OpCode op = OpCode.get(code);
            final Instruction inst;

            switch (op.getOperandType()) {
                case None: {
                    inst = Instruction.create(op);
                    break;
                }

                case PrimitiveTypeCode: {
                    inst = Instruction.create(op, BuiltinTypes.fromPrimitiveTypeCode(b.readUnsignedByte()));
                    break;
                }

                case TypeReference: {
                    final int typeToken = b.readUnsignedShort();
                    inst = Instruction.create(op, _scope.lookupType(typeToken));
                    break;
                }

                case TypeReferenceU1: {
                    inst = Instruction.create(op, _scope.lookupType(b.readUnsignedShort()), b.readUnsignedByte());
                    break;
                }

                case MethodReference: {
                    inst = Instruction.create(op, _scope.lookupMethod(b.readUnsignedShort()));
                    break;
                }

                case FieldReference: {
                    inst = Instruction.create(op, _scope.lookupField(b.readUnsignedShort()));
                    break;
                }

                case BranchTarget: {
                    final int targetOffset;

                    inst = new Instruction(op);

                    if (op.isWide()) {
                        targetOffset = offset + _scope.<Integer>lookupConstant(b.readUnsignedShort());
                    }
                    else {
                        targetOffset = offset + (int) b.readShort();
                    }

                    if (targetOffset < offset) {
                        final Instruction target = body.atOffset(targetOffset);

                        if (!target.hasLabel()) {
                            target.setLabel(new Label(targetOffset));
                        }

                        inst.setOperand(target);
                    }
                    else if (targetOffset == offset) {
                        inst.setOperand(inst);
                        inst.setLabel(new Label(offset));
                    }
                    else if (targetOffset > b.size()) {
                        //
                        // Target is a label after the last instruction.  Insert a dummy NOP.
                        //
                        inst.setOperand(new Instruction(targetOffset, OpCode.NOP));
                    }
                    else {
                        final Fixup oldFixup = fixups[targetOffset];
                        final Fixup newFixup = new Fixup() {
                            @Override
                            public void fix(final Instruction target) {
                                inst.setOperand(target);
                            }
                        };

                        fixups[targetOffset] = oldFixup != null ? Fixup.combine(oldFixup, newFixup)
                                                                : newFixup;
                    }

                    break;
                }

                case I1: {
                    inst = Instruction.create(op, b.readByte());
                    break;
                }

                case I2: {
                    inst = Instruction.create(op, b.readShort());
                    break;
                }

                case I8: {
                    inst = Instruction.create(op, b.readLong());
                    break;
                }

                case Constant: {
                    inst = new Instruction(op, _scope.lookupConstant(b.readUnsignedByte()));
                    break;
                }

                case WideConstant: {
                    final int constantToken = b.readUnsignedShort();
                    inst = new Instruction(op, _scope.lookupConstant(constantToken));
                    break;
                }

                case Switch: {
                    while (b.position() % 4 != 0) {
                        b.readByte();
                    }

                    final SwitchInfo switchInfo = new SwitchInfo();
                    final int defaultOffset = offset + b.readInt();

                    inst = Instruction.create(op, switchInfo);

                    if (defaultOffset < offset) {
                        switchInfo.setDefaultTarget(body.atOffset(defaultOffset));
                    }
                    else if (defaultOffset == offset) {
                        switchInfo.setDefaultTarget(inst);
                    }
                    else {
                        switchInfo.setDefaultTarget(new Instruction(defaultOffset, OpCode.NOP));

                        final Fixup oldFixup = fixups[defaultOffset];
                        final Fixup newFixup = new Fixup() {
                            @Override
                            public void fix(final Instruction target) {
                                switchInfo.setDefaultTarget(target);
                            }
                        };

                        fixups[defaultOffset] = oldFixup != null ? Fixup.combine(oldFixup, newFixup)
                                                                 : newFixup;
                    }

                    if (op == OpCode.TABLESWITCH) {
                        final int low = b.readInt();
                        final int high = b.readInt();
                        final Instruction[] targets = new Instruction[high - low + 1];

                        switchInfo.setLowValue(low);
                        switchInfo.setHighValue(high);

                        for (int i = 0; i < targets.length; i++) {
                            final int targetIndex = i;
                            final int targetOffset = offset + b.readInt();

                            if (targetOffset < offset) {
                                targets[targetIndex] = body.atOffset(targetOffset);
                            }
                            else if (targetOffset == offset) {
                                targets[targetIndex] = inst;
                            }
                            else {
                                targets[targetIndex] = new Instruction(targetOffset, OpCode.NOP);

                                final Fixup oldFixup = fixups[targetOffset];
                                final Fixup newFixup = new Fixup() {
                                    @Override
                                    public void fix(final Instruction target) {
                                        targets[targetIndex] = target;
                                    }
                                };

                                fixups[targetOffset] = oldFixup != null ? Fixup.combine(oldFixup, newFixup)
                                                                        : newFixup;
                            }
                        }

                        switchInfo.setTargets(targets);
                    }
                    else {
                        final int pairCount = b.readInt();
                        final int[] keys = new int[pairCount];
                        final Instruction[] targets = new Instruction[pairCount];

                        for (int i = 0; i < pairCount; i++) {
                            final int targetIndex = i;

                            keys[targetIndex] = b.readInt();

                            final int targetOffset = offset + b.readInt();

                            if (targetOffset < offset) {
                                targets[targetIndex] = body.atOffset(targetOffset);
                            }
                            else if (targetOffset == offset) {
                                targets[targetIndex] = inst;
                            }
                            else {
                                targets[targetIndex] = new Instruction(targetOffset, OpCode.NOP);

                                final Fixup oldFixup = fixups[targetOffset];
                                final Fixup newFixup = new Fixup() {
                                    @Override
                                    public void fix(final Instruction target) {
                                        targets[targetIndex] = target;
                                    }
                                };

                                fixups[targetOffset] = oldFixup != null ? Fixup.combine(oldFixup, newFixup)
                                                                        : newFixup;
                            }
                        }

                        switchInfo.setKeys(keys);
                        switchInfo.setTargets(targets);
                    }

                    break;
                }

                case Local: {
                    final int variableIndex;

                    if (op.isWide()) {
                        variableIndex = b.readUnsignedShort();
                    }
                    else {
                        variableIndex = b.readUnsignedByte();
                    }

                    if (variableIndex < 0 || variableIndex >= variables.size()) {
                        inst = new Instruction(op, new ErrorOperand("!!! BAD LOCAL: " + variableIndex + " !!!"));
                    }
                    else {
                        inst = Instruction.create(op, variables.get(variableIndex));
                    }

                    break;
                }

                case LocalI1: {
                    final int variableIndex;
                    final int operand;

                    if (op.isWide()) {
                        variableIndex = b.readUnsignedShort();
                    }
                    else {
                        variableIndex = b.readUnsignedByte();
                    }

                    operand = b.readByte();

                    if (variableIndex < 0 || variableIndex >= variables.size()) {
                        inst = new Instruction(
                            op,
                            new ErrorOperand("!!! BAD LOCAL: " + variableIndex + " !!!"),
                            operand
                        );
                    }
                    else {
                        inst = Instruction.create(op, variables.get(variableIndex), operand);
                    }

                    break;
                }

                case LocalI2: {
                    final int variableIndex;
                    final int operand;

                    if (op.isWide()) {
                        variableIndex = b.readUnsignedShort();
                    }
                    else {
                        variableIndex = b.readUnsignedByte();
                    }

                    operand = b.readShort();

                    if (variableIndex < 0 || variableIndex >= variables.size()) {
                        inst = new Instruction(
                            op,
                            new ErrorOperand("!!! BAD LOCAL: " + variableIndex + " !!!"),
                            operand
                        );
                    }
                    else {
                        inst = Instruction.create(op, variables.get(variableIndex), operand);
                    }

                    break;
                }

                default: {
                    throw new IllegalStateException("Unrecognized opcode: " + code);
                }
            }

            inst.setOffset(offset);
            body.add(inst);

            final Fixup fixup = fixups[offset];

            if (fixup != null) {
                if (!inst.hasLabel()) {
                    inst.setLabel(new Label(offset));
                }
                fixup.fix(inst);
            }
        }

        int labelCount = 0;

        for (int i = 0; i < body.size(); i++) {
            final Instruction inst = body.get(i);

            if (inst.hasLabel()) {
                inst.getLabel().setIndex(labelCount++);
            }
        }

        if (methodVisitor != null) {
            final LineNumberTableAttribute lineNumbersAttribute = SourceAttribute.find(
                AttributeNames.LineNumberTable,
                _code.getAttributes()
            );

            final int[] lineNumbers;

            if (lineNumbersAttribute != null) {
                final List<LineNumberTableEntry> entries = lineNumbersAttribute.getEntries();

                lineNumbers = new int[body.size()];

                Arrays.fill(lineNumbers, -1);

                for (int i = 0; i < entries.size(); i++) {
                    final LineNumberTableEntry entry = entries.get(i);
                    final int offset = entry.getOffset();

                    if (offset >= 0 && offset < lineNumbers.length) {
                        lineNumbers[i] = entry.getLineNumber();
                    }
                }
            }
            else {
                lineNumbers = null;
            }

            for (int i = 0; i < body.size(); i++) {
                final Instruction inst = body.get(i);
                final int lineNumber = lineNumbers != null ? lineNumbers[i] : -1;

                if (lineNumber >= 0) {
                    methodVisitor.visitLineNumber(inst, lineNumber);
                }
            }
        }

        if (visitor != null) {
            for (int i = 0; i < body.size(); i++) {
                visitor.visit(body.get(i));
            }
        }

        return _methodBody;
    }

    // <editor-fold defaultstate="collapsed" desc="Fixup Class">

    private abstract static class Fixup {
        public abstract void fix(final Instruction target);

        public static Fixup combine(final Fixup first, final Fixup second) {
            final Fixup[] fixups;

            if (first instanceof MultiFixup) {
                final MultiFixup m1 = (MultiFixup) first;

                if (second instanceof MultiFixup) {
                    final MultiFixup m2 = (MultiFixup) second;

                    fixups = new Fixup[m1._fixups.length + m2._fixups.length];

                    System.arraycopy(
                        m2._fixups,
                        0,
                        fixups,
                        m1._fixups.length,
                        m2._fixups.length
                    );
                }
                else {
                    fixups = new Fixup[m1._fixups.length + 1];
                    fixups[m1._fixups.length] = second;
                }

                System.arraycopy(
                    m1._fixups,
                    0,
                    fixups,
                    0,
                    m1._fixups.length
                );
            }
            else {
                if (second instanceof MultiFixup) {
                    final MultiFixup m2 = (MultiFixup) second;

                    fixups = new Fixup[1 + m2._fixups.length];

                    System.arraycopy(
                        m2._fixups,
                        0,
                        fixups,
                        1,
                        m2._fixups.length
                    );
                }
                else {
                    fixups = new Fixup[]{first, second};
                }
            }

            return new MultiFixup(fixups);
        }

        private final static class MultiFixup extends Fixup {
            private final Fixup[] _fixups;

            private MultiFixup(final Fixup... fixups) {
                _fixups = VerifyArgument.noNullElements(fixups, "fixups");
            }

            @Override
            public void fix(final Instruction target) {
                for (final Fixup fixup : _fixups) {
                    fixup.fix(target);
                }
            }
        }
    }

    // </editor-fold>
}
