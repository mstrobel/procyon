/*
 * AstBuilder.java
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

package com.strobel.decompiler.ast;

import com.strobel.assembler.ir.ExceptionHandler;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.InstructionCollection;
import com.strobel.assembler.ir.MethodBody;
import com.strobel.assembler.ir.OpCode;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.VariableDefinitionCollection;
import com.strobel.assembler.metadata.VariableReference;
import com.strobel.core.StrongBox;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.InstructionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static java.lang.String.format;

public final class AstBuilder {
    private final static AstCode[] CODES = AstCode.values();
    private final static StackSlot[] EMPTY_STACK = new StackSlot[0];
    private final static VariableSlot[] EMPTY_VARIABLES = new VariableSlot[0];

    private final Map<ExceptionHandler, ByteCode> _loadExceptions = new LinkedHashMap<>();
    private MethodBody _body;
    private MethodDefinition _methodDefinition;
    private boolean _optimize;
    private DecompilerContext _context;

    public static List<Node> build(final MethodBody body, final boolean optimize, final DecompilerContext context) {
        final AstBuilder builder = new AstBuilder();

        builder._body = VerifyArgument.notNull(body, "body");
        builder._methodDefinition = body.getMethod().resolve();
        builder._optimize = optimize;
        builder._context = VerifyArgument.notNull(context, "context");

        if (body.getInstructions().isEmpty()) {
            return Collections.emptyList();
        }

        final List<ByteCode> byteCode = builder.performStackAnalysis();
        final List<Node> ast = builder.convertToAst();

        return ast;
    }

    private List<ByteCode> performStackAnalysis() {
        final Map<Instruction, ByteCode> byteCodeMap = new IdentityHashMap<>();
        final InstructionCollection instructions = _body.getInstructions();
        final List<ByteCode> body = new ArrayList<>(instructions.size());

        final StrongBox<AstCode> codeBox = new StrongBox<>();
        final StrongBox<Object> operandBox = new StrongBox<>();

        for (final Instruction instruction : instructions) {
            final OpCode opCode = instruction.getOpCode();

            AstCode code = CODES[opCode.ordinal()];
            Object operand = instruction.hasOperand() ? instruction.getOperand(0) : null;

            codeBox.set(code);
            operandBox.set(operand);

            if (AstCode.expandMacro(codeBox, operandBox, _body)) {
                code = codeBox.get();
                operand = operandBox.get();
            }

            ByteCode byteCode = new ByteCode();

            byteCode.offset = instruction.getOffset();
            byteCode.endOffset = instruction.getEndOffset();
            byteCode.code = code;
            byteCode.operand = operand;
            byteCode.popCount = InstructionHelper.getPopDelta(instruction, _body);
            byteCode.pushCount = InstructionHelper.getPushDelta(instruction, _body);

            byteCodeMap.put(instruction, byteCode);
            body.add(byteCode);
        }
        final Stack<ByteCode> agenda = new Stack<>();
        final VariableDefinitionCollection variables = _body.getVariables();
        final List<ExceptionHandler> exceptionHandlers = _body.getExceptionHandlers();

        final int variableCount = variables.size();
        final Set<ByteCode> exceptionHandlerStarts = new HashSet<>(exceptionHandlers.size());
        final VariableSlot[] unknownVariables = VariableSlot.makeUnknownState(variableCount);

        for (final ExceptionHandler handler : exceptionHandlers) {
            final ByteCode handlerStart = byteCodeMap.get(handler.getHandlerBlock().getFirstInstruction());

            handlerStart.stackBefore = EMPTY_STACK;
            handlerStart.variablesBefore = unknownVariables;

            if (handler.isCatch()) {
                final ByteCode loadException = new ByteCode();

                loadException.code = AstCode.LoadException;
                loadException.operand = handler.getCatchType();
                loadException.popCount = 0;
                loadException.pushCount = 1;

                _loadExceptions.put(handler, loadException);

                handlerStart.stackBefore = new StackSlot[] { new StackSlot(new ByteCode[] { loadException }) };
            }

            agenda.push(handlerStart);
        }

        body.get(0).stackBefore = EMPTY_STACK;
        body.get(0).variablesBefore = unknownVariables;

        agenda.push(body.get(0));

        //
        // Process agenda.
        //
        while (!agenda.isEmpty()) {
            final ByteCode byteCode = agenda.pop();

            //
            // Calculate new stack.
            //
            final StackSlot[] newStack = StackSlot.matchStack(
                byteCode.stackBefore,
                byteCode.popCount < 0 ? byteCode.stackBefore.length : byteCode.popCount,
                byteCode.pushCount,
                byteCode
            );

            //
            // Calculate new variable state.
            //
            final VariableSlot[] newVariableState = VariableSlot.cloneVariableState(byteCode.variablesBefore);

            if (byteCode.isVariableDefinition()) {
                newVariableState[((VariableReference) byteCode.operand).getIndex()] = new VariableSlot(
                    new ByteCode[] { byteCode },
                    false
                );
            }

            //
            // Find all successors.
            //
        }

        return body;
    }

    private List<Node> convertToAst() {
        return Collections.emptyList();
    }

    // <editor-fold defaultstate="collapsed" desc="StackSlot Class">

    private final static class StackSlot {
        public final ByteCode[] definitions;
        public final Variable loadFrom;

        public StackSlot(final ByteCode[] definitions) {
            this.definitions = VerifyArgument.notNull(definitions, "definitions");
            this.loadFrom = null;
        }

        public StackSlot(final ByteCode[] definitions, final Variable loadFrom) {
            this.definitions = VerifyArgument.notNull(definitions, "definitions");
            this.loadFrom = loadFrom;
        }

        public static StackSlot[] matchStack(
            final StackSlot[] stack,
            final int popCount,
            final int pushCount,
            final ByteCode pushDefinition) {

            VerifyArgument.notNull(stack, "stack");
            VerifyArgument.isNonNegative(popCount, "popCount");
            VerifyArgument.isNonNegative(pushCount, "pushCount");

            final StackSlot[] newStack = new StackSlot[stack.length - popCount + pushCount];

            System.arraycopy(stack, 0, newStack, 0, stack.length - popCount);

            for (int i = stack.length - popCount; i < newStack.length; i++) {
                newStack[i] = new StackSlot(new ByteCode[] { pushDefinition });
            }

            return newStack;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="VariableSlot Class">

    private final static class VariableSlot {
        public final static VariableSlot UNKNOWN_INSTANCE = new VariableSlot(new ByteCode[0], true);
        public final ByteCode[] definitions;
        public final boolean unknownDefinition;

        public VariableSlot(final ByteCode[] definitions) {
            this.definitions = VerifyArgument.notNull(definitions, "definitions");
            this.unknownDefinition = false;
        }

        public VariableSlot(final ByteCode[] definitions, final boolean unknownDefinition) {
            this.definitions = VerifyArgument.notNull(definitions, "definitions");
            this.unknownDefinition = unknownDefinition;
        }

        public static VariableSlot[] cloneVariableState(final VariableSlot[] state) {
            return VerifyArgument.notNull(state, "state").clone();
        }

        public static VariableSlot[] makeUnknownState(final int variableCount) {
            final VariableSlot[] unknownVariableState = new VariableSlot[variableCount];

            for (int i = 0; i < variableCount; i++) {
                unknownVariableState[i] = UNKNOWN_INSTANCE;
            }

            return unknownVariableState;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="ByteCode Class">

    private final static class ByteCode {
        public Label label;
        public int offset;
        public int endOffset;
        public AstCode code;
        public Object operand;
        public int popCount = -1;
        public int pushCount;
        public ByteCode next;
        public StackSlot[] stackBefore;
        public VariableSlot[] variablesBefore;
        public List<Variable> storeTo;

        public final String name() {
            return format("#%1$04d", offset);
        }

        public final boolean isVariableDefinition() {
            return code == AstCode.Store;
        }

        @Override
        public final String toString() {
            final StringBuilder sb = new StringBuilder();

            //
            // Label
            //
            sb.append(name()).append(':');

            if (label != null) {
                sb.append('*');
            }

            //
            // Name
            //
            sb.append(' ');
            sb.append(code.getName());

            if (operand != null) {
                sb.append(' ');

                if (operand instanceof Instruction) {
                    sb.append(format("#%1$04d", ((Instruction) operand).getOffset()));
                }
                else if (operand instanceof Instruction[]) {
                    for (final Instruction instruction : (Instruction[]) operand) {
                        sb.append(format("#%1$04d", instruction.getOffset()));
                        sb.append(' ');
                    }
                }
                else if (operand instanceof Label) {
                    sb.append(((Label) operand).getName());
                }
                else if (operand instanceof Label[]) {
                    for (final Label l : (Label[]) operand) {
                        sb.append(l.getName());
                        sb.append(' ');
                    }
                }
                else {
                    sb.append(operand);
                }
            }

            if (stackBefore != null) {
                sb.append(" StackBefore={");

                for (int i = 0; i < stackBefore.length; i++) {
                    if (i != 0) {
                        sb.append(',');
                    }

                    final StackSlot slot = stackBefore[i];
                    final ByteCode[] definitions = slot.definitions;

                    for (int j = 0; j < definitions.length; j++) {
                        if (j != 0) {
                            sb.append('|');
                        }
                        sb.append(format("#%1$04d", definitions[j].offset));
                    }
                }

                sb.append('}');
            }

            if (storeTo != null && !storeTo.isEmpty()) {
                sb.append(" StoreTo={");

                for (int i = 0; i < storeTo.size(); i++) {
                    if (i != 0) {
                        sb.append(',');
                    }
                    sb.append(storeTo.get(i).getName());
                }

                sb.append('}');
            }

            if (variablesBefore != null) {
                sb.append(" VariablesBefore={");

                for (int i = 0; i < variablesBefore.length; i++) {
                    if (i != 0) {
                        sb.append(',');
                    }

                    final VariableSlot slot = variablesBefore[i];

                    if (slot.unknownDefinition) {
                        sb.append('?');
                    }
                    else {
                        final ByteCode[] definitions = slot.definitions;
                        for (int j = 0; j < definitions.length; j++) {
                            if (j != 0) {
                                sb.append('|');
                            }
                            sb.append(format("#%1$04d", definitions[j].offset));
                        }
                    }
                }

                sb.append('}');
            }

            return sb.toString();
        }
    }

    // </editor-fold>
}
