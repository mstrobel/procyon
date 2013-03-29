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

package com.strobel.assembler.metadata;

import com.strobel.assembler.flowanalysis.ControlFlowGraph;
import com.strobel.assembler.flowanalysis.ControlFlowGraphBuilder;
import com.strobel.assembler.flowanalysis.ControlFlowNode;
import com.strobel.assembler.flowanalysis.ControlFlowNodeType;
import com.strobel.assembler.ir.ErrorOperand;
import com.strobel.assembler.ir.ExceptionBlock;
import com.strobel.assembler.ir.ExceptionHandler;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.InstructionCollection;
import com.strobel.assembler.ir.OpCode;
import com.strobel.assembler.ir.OpCodeHelpers;
import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.CodeAttribute;
import com.strobel.assembler.ir.attributes.ExceptionTableEntry;
import com.strobel.assembler.ir.attributes.LocalVariableTableAttribute;
import com.strobel.assembler.ir.attributes.LocalVariableTableEntry;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.ast.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MethodReader {
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

    @SuppressWarnings("ConstantConditions")
    public MethodBody accept(final MethodVisitor methodVisitor) {
        final Buffer b = _code.getCode();
        final InstructionCollection body = _methodBody.getInstructions();
        final VariableDefinitionCollection variables = _methodBody.getVariables();

        final LocalVariableTableAttribute localVariableTable = SourceAttribute.find(
            AttributeNames.LocalVariableTable,
            _code.getAttributes()
        );

        final LocalVariableTableAttribute localVariableTypeTable = SourceAttribute.find(
            AttributeNames.LocalVariableTable,
            _code.getAttributes()
        );

        if (localVariableTable != null) {
            final List<LocalVariableTableEntry> entries = localVariableTable.getEntries();

            for (final LocalVariableTableEntry entry : entries) {
                final int scopeStart = entry.getScopeOffset();
                final int scopeEnd = scopeStart + entry.getScopeLength();

                final VariableDefinition variable = new VariableDefinition(
                    entry.getIndex(),
                    entry.getName(),
                    entry.getType()
                );

                variable.setTypeKnown(true);
                variable.setScopeStart(scopeStart);
                variable.setScopeEnd(scopeEnd);

                variables.add(variable);
            }
        }

        if (localVariableTypeTable != null) {
            final List<LocalVariableTableEntry> entries = localVariableTable.getEntries();

            for (final LocalVariableTableEntry entry : entries) {
                final int slot = entry.getIndex();
                final int scopeStart = entry.getScopeOffset();
                final int scopeEnd = scopeStart + entry.getScopeLength();

                boolean found = false;

                for (final VariableDefinition variable : variables) {
                    if (variable.getSlot() == slot &&
                        variable.getScopeStart() == scopeStart &&
                        variable.getScopeEnd() == scopeEnd) {

                        if (!variable.hasName()) {
                            variable.setName(entry.getName());
                        }

                        variable.setVariableType(entry.getType());

                        found = true;
                        break;
                    }
                }

                if (!found) {
                    final VariableDefinition variable = new VariableDefinition(
                        slot,
                        entry.getName(),
                        entry.getType()
                    );

                    variable.setTypeKnown(true);
                    variable.setScopeStart(scopeStart);
                    variable.setScopeEnd(scopeEnd);

                    variables.add(variable);
                }
            }
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
                    if (op.isLoad() || op.isStore()) {
                        variables.ensure(OpCodeHelpers.getLoadStoreMacroArgumentIndex(op), op, offset);
                    }
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

                    if (op == OpCode.INVOKEINTERFACE) {
                        b.readUnsignedByte();
                        b.readUnsignedByte();
                    }

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
                    final int variableSlot;

                    if (op.isWide()) {
                        variableSlot = b.readUnsignedShort();
                    }
                    else {
                        variableSlot = b.readUnsignedByte();
                    }

                    final VariableDefinition variable = variables.ensure(variableSlot, op, offset);

                    if (variableSlot < 0) {
                        inst = new Instruction(op, new ErrorOperand("!!! BAD LOCAL: " + variableSlot + " !!!"));
                    }
                    else {
                        inst = Instruction.create(op, variable);
                    }

                    break;
                }

                case LocalI1: {
                    final int variableSlot;
                    final int operand;

                    if (op.isWide()) {
                        variableSlot = b.readUnsignedShort();
                    }
                    else {
                        variableSlot = b.readUnsignedByte();
                    }

                    final VariableDefinition variable = variables.ensure(variableSlot, op, offset);

                    operand = b.readByte();

                    if (variableSlot < 0) {
                        inst = new Instruction(
                            op,
                            new ErrorOperand("!!! BAD LOCAL: " + variableSlot + " !!!"),
                            operand
                        );
                    }
                    else {
                        inst = Instruction.create(op, variable, operand);
                    }

                    break;
                }

                case LocalI2: {
                    final int variableSlot;
                    final int operand;

                    if (op.isWide()) {
                        variableSlot = b.readUnsignedShort();
                    }
                    else {
                        variableSlot = b.readUnsignedByte();
                    }

                    final VariableDefinition variable = variables.ensure(variableSlot, op, offset);

                    operand = b.readShort();

                    if (variableSlot < 0) {
                        inst = new Instruction(
                            op,
                            new ErrorOperand("!!! BAD LOCAL: " + variableSlot + " !!!"),
                            operand
                        );
                    }
                    else {
                        inst = Instruction.create(op, variable, operand);
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

        variables.updateScopes(_code.getCodeSize());

        int labelCount = 0;

        for (int i = 0; i < body.size(); i++) {
            final Instruction inst = body.get(i);

            if (inst.hasLabel()) {
                inst.getLabel().setIndex(labelCount++);
            }
        }

        final List<ExceptionTableEntry> exceptionTable = _code.getExceptionTableEntries();

        if (!exceptionTable.isEmpty()) {
            populateExceptionHandlerInfo(body, exceptionTable);
        }

        return _methodBody;
    }

    // <editor-fold defaultstate="collapsed" desc="Exception Handler Info">

    @SuppressWarnings("ConstantConditions")
    private void populateExceptionHandlerInfo(final InstructionCollection body, final List<ExceptionTableEntry> exceptionTable) {
        if (body.isEmpty()) {
            return;
        }

        final Instruction bodyEndInstruction = body.get(body.size() - 1);

        final List<HandlerWithRange> entries = new ArrayList<>(exceptionTable.size());

        for (final ExceptionTableEntry entry : exceptionTable) {
            entries.add(
                new HandlerWithRange(
                    entry,
                    new Range(entry.getHandlerOffset(), Integer.MAX_VALUE)
                )
            );
        }

        Collections.sort(entries);

        final ControlFlowGraph cfg = ControlFlowGraphBuilder.build(body, Collections.<ExceptionHandler>emptyList());

        cfg.computeDominance();

        for (int i = 0; i < entries.size(); i++) {
            final HandlerWithRange entry = entries.get(i);
            int minOffset = Integer.MAX_VALUE;

            final List<ControlFlowNode> nodes = cfg.getNodes();
            for (int j = 0; j < nodes.size(); j++) {

                final ControlFlowNode node = nodes.get(j);

                if (node.getNodeType() != ControlFlowNodeType.Normal) {
                    continue;
                }

                if (node.getStart().getOffset() == entry.range.getStart()) {
                    final ControlFlowNode end = findHandlerEnd(node, new LinkedHashSet<ControlFlowNode>());

                    if (end != null && end.getNodeType() == ControlFlowNodeType.Normal) {
                        minOffset = end.getEnd().getEndOffset();
                    }
                    else {
                        minOffset = node.getEnd().getEndOffset();
                    }

                    break;
                }
            }

            if (minOffset != Integer.MAX_VALUE) {
                entry.range.setEnd(minOffset);
            }
        }

        Collections.sort(entries);

        final List<ExceptionHandler> exceptionHandlers = _methodBody.getExceptionHandlers();

        for (final HandlerWithRange entry : entries) {
            final int startOffset = entry.entry.getStartOffset();
            final int endOffset = entry.entry.getEndOffset();
            final int handlerStart = entry.range.getStart();
            final int handlerEnd = entry.range.getEnd();
            final TypeReference catchType = entry.entry.getCatchType();

            final Instruction firstInstruction = body.tryGetAtOffset(startOffset);
            final Instruction lastInstruction;
            final Instruction handlerFirstInstruction = body.tryGetAtOffset(handlerStart);
            final Instruction handlerLastInstruction;

            if (endOffset <= bodyEndInstruction.getOffset()) {
                lastInstruction = body.tryGetAtOffset(endOffset).getPrevious();
            }
            else if (endOffset == bodyEndInstruction.getEndOffset()) {
                lastInstruction = bodyEndInstruction;
            }
            else {
                lastInstruction = new Instruction(endOffset, OpCode.NOP);
            }

            if (handlerEnd <= bodyEndInstruction.getOffset()) {
                handlerLastInstruction = body.tryGetAtOffset(handlerEnd).getPrevious();
            }
            else if (handlerEnd == bodyEndInstruction.getEndOffset()) {
                handlerLastInstruction = bodyEndInstruction;
            }
            else {
                handlerLastInstruction = new Instruction(handlerEnd, OpCode.NOP);
            }

            final ExceptionHandler handler;

            if (catchType == null) {
                handler = ExceptionHandler.createFinally(
                    new ExceptionBlock(firstInstruction, lastInstruction),
                    new ExceptionBlock(handlerFirstInstruction, handlerLastInstruction)
                );
            }
            else {
                handler = ExceptionHandler.createCatch(
                    new ExceptionBlock(firstInstruction, lastInstruction),
                    new ExceptionBlock(handlerFirstInstruction, handlerLastInstruction),
                    catchType
                );
            }

            exceptionHandlers.add(handler);
        }
    }

    private static ControlFlowNode findHandlerEnd(final ControlFlowNode node, final Set<ControlFlowNode> visited) {
        if (!visited.add(node)) {
            return null;
        }

        for (final ControlFlowNode successor : node.getSuccessors()) {
            if (successor.getDominatorTreeChildren().isEmpty()) {
                final ControlFlowNode result = findHandlerEnd(successor, visited);

                if (result != null) {
                    return result;
                }

                return successor;
            }
        }

        return null;
    }

    private final static class HandlerWithRange implements Comparable<HandlerWithRange> {
        final ExceptionTableEntry entry;
        final Range range;

        HandlerWithRange(final ExceptionTableEntry entry, final Range range) {
            this.entry = entry;
            this.range = range;
        }

        @Override
        public final int compareTo(final HandlerWithRange o) {
            return range.compareTo(o.range);
        }

        @Override
        public String toString() {
            final TypeReference catchType = entry.getCatchType();

            return "Entry{" +
                   "Try=" + entry.getStartOffset() + ":" + entry.getEndOffset() +
                   ", Handler=" + range.getStart() + ":" + range.getEnd() +
                   ", Type=" + (catchType != null ? catchType.getName() : "any") +
                   '}';
        }
    }

    // </editor-fold>

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
                    fixups = new Fixup[] { first, second };
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
