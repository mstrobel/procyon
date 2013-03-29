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

import com.strobel.assembler.flowanalysis.ControlFlowGraph;
import com.strobel.assembler.flowanalysis.ControlFlowGraphBuilder;
import com.strobel.assembler.flowanalysis.ControlFlowNode;
import com.strobel.assembler.flowanalysis.ControlFlowNodeType;
import com.strobel.assembler.ir.*;
import com.strobel.assembler.metadata.*;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.StringUtilities;
import com.strobel.core.StrongBox;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.InstructionHelper;

import java.util.*;

import static com.strobel.core.CollectionUtilities.*;
import static java.lang.String.format;

public final class AstBuilder {
    private final static AstCode[] CODES = AstCode.values();
    private final static StackSlot[] EMPTY_STACK = new StackSlot[0];
    private final static ByteCode[] EMPTY_DEFINITIONS = new ByteCode[0];

    private final Map<ExceptionHandler, ByteCode> _loadExceptions = new LinkedHashMap<>();
    private Map<Instruction, Instruction> _originalInstructionMap;
    private InstructionCollection _instructions;
    private List<ExceptionHandler> _exceptionHandlers;
    private MethodBody _body;
    private boolean _optimize;
    private DecompilerContext _context;

    public static List<Node> build(final MethodBody body, final boolean optimize, final DecompilerContext context) {
        final AstBuilder builder = new AstBuilder();

        builder._body = VerifyArgument.notNull(body, "body");
        builder._optimize = optimize;
        builder._context = VerifyArgument.notNull(context, "context");

        if (body.getInstructions().isEmpty()) {
            return Collections.emptyList();
        }

        builder.analyzeHandlers();

        final List<ByteCode> byteCode = builder.performStackAnalysis();

        @SuppressWarnings("UnnecessaryLocalVariable")
        final List<Node> ast = builder.convertToAst(byteCode, new LinkedHashSet<>(builder._exceptionHandlers));

        return ast;
    }

    private static List<ExceptionHandler> remapHandlers(final List<ExceptionHandler> handlers, final InstructionCollection instructions) {
        final List<ExceptionHandler> newHandlers = new ArrayList<>();

        for (final ExceptionHandler handler : handlers) {
            final ExceptionBlock oldTry = handler.getTryBlock();
            final ExceptionBlock oldHandler = handler.getHandlerBlock();

            final ExceptionBlock newTry = new ExceptionBlock(
                instructions.atOffset(oldTry.getFirstInstruction().getOffset()),
                instructions.atOffset(oldTry.getLastInstruction().getOffset())
            );

            final ExceptionBlock newHandler = new ExceptionBlock(
                instructions.atOffset(oldHandler.getFirstInstruction().getOffset()),
                instructions.atOffset(oldHandler.getLastInstruction().getOffset())
            );

            if (handler.isCatch()) {
                newHandlers.add(
                    ExceptionHandler.createCatch(
                        newTry,
                        newHandler,
                        handler.getCatchType()
                    )
                );
            }
            else {
                newHandlers.add(
                    ExceptionHandler.createFinally(
                        newTry,
                        newHandler
                    )
                );
            }
        }

        return newHandlers;
    }

    private final static class HandlerInfo {
        final ExceptionHandler handler;
        final List<ControlFlowNode> allNodes = new ArrayList<>();
        final List<ControlFlowNode> tryNodes = new ArrayList<>();
        final List<ControlFlowNode> handlerNodes = new ArrayList<>();

        HandlerInfo(final ExceptionHandler handler) {
            this.handler = handler;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private InstructionCollection analyzeHandlers() {
        final Map<ExceptionHandler, HandlerInfo> handlers = new LinkedHashMap<>();
        final InstructionCollection oldInstructions = _body.getInstructions();
        final InstructionCollection instructions = _instructions = copyInstructions(oldInstructions);
        final List<ExceptionHandler> exceptionHandlers = _exceptionHandlers = remapHandlers(_body.getExceptionHandlers(), instructions);

        final Map<Instruction, Instruction> originalInstructionMap = _originalInstructionMap = new IdentityHashMap<>();

        for (int i = 0, n = instructions.size(); i < n; i++) {
            originalInstructionMap.put(instructions.get(i), oldInstructions.get(i));
        }

        //
        // Java does this retarded thing where a try block gets split along exit branches,
        // but with the split parts sharing the same handler.  We can't represent this in
        // out AST, so just merge the parts back together.
        //

        for (int i = 0; i < exceptionHandlers.size() - 1; i++) {
            final ExceptionHandler current = exceptionHandlers.get(i);
            final ExceptionHandler next = exceptionHandlers.get(i + 1);

            if (current.getHandlerBlock().getFirstInstruction() == next.getHandlerBlock().getFirstInstruction() &&
                current.getHandlerBlock().getLastInstruction() == next.getHandlerBlock().getLastInstruction()) {

                final Instruction lastInCurrent = current.getTryBlock().getLastInstruction();
                final Instruction firstInNext = next.getTryBlock().getFirstInstruction();
                final Instruction branchInBetween = lastInCurrent.getNext();

                if (branchInBetween != null &&
                    branchInBetween.getOpCode().isBranch() &&
                    branchInBetween == firstInNext.getPrevious()) {

                    final ExceptionHandler newHandler;

                    if (current.isFinally()) {
                        newHandler = ExceptionHandler.createFinally(
                            new ExceptionBlock(
                                current.getTryBlock().getFirstInstruction(),
                                next.getTryBlock().getLastInstruction()
                            ),
                            new ExceptionBlock(
                                current.getHandlerBlock().getFirstInstruction(),
                                current.getHandlerBlock().getLastInstruction()
                            )
                        );
                    }
                    else {
                        newHandler = ExceptionHandler.createCatch(
                            new ExceptionBlock(
                                current.getTryBlock().getFirstInstruction(),
                                next.getTryBlock().getLastInstruction()
                            ),
                            new ExceptionBlock(
                                current.getHandlerBlock().getFirstInstruction(),
                                current.getHandlerBlock().getLastInstruction()
                            ),
                            current.getCatchType()
                        );
                    }

                    exceptionHandlers.set(i, newHandler);
                    exceptionHandlers.remove(i + 1);
                }
            }
        }

        final ControlFlowGraph cfg = ControlFlowGraphBuilder.build(instructions, exceptionHandlers);

        cfg.computeDominance();

        for (final ExceptionHandler handler : exceptionHandlers) {
            final HandlerInfo handlerInfo = new HandlerInfo(handler);

            for (final ControlFlowNode node : cfg.getNodes()) {
                if (node.getNodeType() == ControlFlowNodeType.Normal) {
                    if (node.getStart().getOffset() >= handler.getTryBlock().getFirstInstruction().getOffset() &&
                        node.getEnd().getOffset() <= handler.getTryBlock().getLastInstruction().getOffset()) {

                        handlerInfo.tryNodes.add(node);
                        handlerInfo.allNodes.add(node);
                    }
                    else if (node.getStart().getOffset() >= handler.getHandlerBlock().getFirstInstruction().getOffset() &&
                             node.getEnd().getOffset() <= handler.getHandlerBlock().getLastInstruction().getOffset()) {

                        handlerInfo.handlerNodes.add(node);
                        handlerInfo.allNodes.add(node);
                    }
                }
            }

            handlers.put(handler, handlerInfo);
        }

        analyzeHandlers(instructions, handlers);

        return instructions;
    }

    @SuppressWarnings("ConstantConditions")
    private void analyzeHandlers(final InstructionCollection instructions, final Map<ExceptionHandler, HandlerInfo> handlers) {
        final Set<Instruction> toRemove = new LinkedHashSet<>();
        final Map<Instruction, Instruction> remappedJumps = new LinkedHashMap<>();

        for (final ExceptionHandler handler : _exceptionHandlers) {
            if (!handler.isFinally()) {
                continue;
            }

            final ExceptionBlock handlerTry = handler.getTryBlock();

            for (final ExceptionHandler other : _exceptionHandlers) {
//                if (other == handler) {
//                    continue;
//                }

                final ExceptionBlock otherTry = other.getTryBlock();
                final HandlerInfo finallyInfo = handlers.get(handler);
                final ControlFlowNode firstInFinally = firstOrDefault(finallyInfo.handlerNodes);

                if (firstInFinally == null) {
                    continue;
                }

                Instruction firstFinallyInstruction = firstInFinally.getStart();

                switch (firstFinallyInstruction.getOpCode()) {
                    case ASTORE:
                    case ASTORE_1:
                    case ASTORE_2:
                    case ASTORE_3:
                    case ASTORE_W:
                        firstFinallyInstruction = firstFinallyInstruction.getNext();
                        break;
                }

                Instruction lastFinallyInstruction = lastOrDefault(finallyInfo.handlerNodes).getEnd();

                if (lastFinallyInstruction.getOpCode() == OpCode.ATHROW &&
                    lastFinallyInstruction.getPrevious() != null) {

                    final Instruction previousFinallyInstruction = lastFinallyInstruction.getPrevious();

                    switch (previousFinallyInstruction.getOpCode()) {
                        case ALOAD:
                        case ALOAD_1:
                        case ALOAD_2:
                        case ALOAD_3:
                        case ALOAD_W:
                            lastFinallyInstruction = previousFinallyInstruction.getPrevious();
                            break;
                    }
                }

                if (otherTry.getFirstInstruction().getOffset() == handlerTry.getFirstInstruction().getOffset() &&
                    otherTry.getLastInstruction().getOffset() == handlerTry.getLastInstruction().getOffset()) {

                    final HandlerInfo handlerInfo = handlers.get(other);
                    final ControlFlowNode lastInTry = lastOrDefault(handlerInfo.tryNodes);

                    if (lastInTry == null || lastInTry.precedes(firstInFinally)) {
                        continue;
                    }

                    ControlFlowNode firstAfterTry = null;

                    final Instruction lastInstructionInTry = lastInTry.getEnd();
                    final Instruction firstInstructionAfterTry = lastInstructionInTry.getNext();

                    if (firstInstructionAfterTry == null) {
                        continue;
                    }

                    for (final ControlFlowNode successor : lastInTry.getSuccessors()) {
                        if (successor.getStart() == firstInstructionAfterTry) {
                            firstAfterTry = successor;
                            break;
                        }
                    }

                    if (firstAfterTry == null || firstAfterTry == firstInFinally) {
                        continue;
                    }

                    assert firstAfterTry != null;

                    boolean first = true;

                    for (Instruction pTry = firstAfterTry.getStart(), pFinally = firstFinallyInstruction;
                         (pFinally != null &&
                          pTry != null &&
                          pFinally.getOffset() <= lastFinallyInstruction.getOffset() &&
                          pTry.getOpCode() == pFinally.getOpCode());
                         pTry = pTry.getNext(), pFinally = pFinally.getNext()) {

                        if (first) {
                            if ((lastInstructionInTry.getOpCode() == OpCode.GOTO ||
                                 lastInstructionInTry.getOpCode() == OpCode.GOTO_W) &&
                                lastInstructionInTry.getOperand(0) == pTry) {

                                //
                                // If the try block ends with an explicit break to the finally handler, we can remove it.
                                //
                                toRemove.add(pTry);
                            }

                            if (pTry.getLabel() != null) {
                                remappedJumps.put(pTry, firstFinallyInstruction);
                            }

                            first = false;
                        }

                        toRemove.add(pTry);
                    }
                }
            }
        }

        if (toRemove.isEmpty()) {
            return;
        }

        for (final Instruction instruction : toRemove) {
            instructions.remove(instruction);
        }

        for (final Instruction instruction : instructions) {
            if (instruction.getOperandCount() == 0) {
                continue;
            }

            final Object operand = instruction.getOperand(0);

            if (operand instanceof Instruction) {
                final Instruction oldTarget = (Instruction) operand;
                final Instruction newTarget = remappedJumps.get(oldTarget);

                if (newTarget != null) {
                    instruction.setOperand(newTarget);

                    if (!newTarget.hasLabel()) {
                        newTarget.setLabel(new com.strobel.assembler.metadata.Label(newTarget.getOffset()));
                    }
                }
            }
            else if (operand instanceof SwitchInfo) {
                final SwitchInfo oldOperand = (SwitchInfo) operand;

                final Instruction oldDefault = oldOperand.getDefaultTarget();
                final Instruction newDefault = remappedJumps.get(oldDefault);

                if (newDefault != null && !newDefault.hasLabel()) {
                    newDefault.setLabel(new com.strobel.assembler.metadata.Label(newDefault.getOffset()));
                }

                final Instruction[] oldTargets = oldOperand.getTargets();

                Instruction[] newTargets = null;

                for (int i = 0; i < oldTargets.length; i++) {
                    final Instruction newTarget = remappedJumps.get(oldTargets[i]);

                    if (newTarget != null) {
                        if (newTargets == null) {
                            newTargets = Arrays.copyOf(oldTargets, oldTargets.length);
                        }

                        newTargets[i] = newTarget;

                        if (!newTarget.hasLabel()) {
                            newTarget.setLabel(new com.strobel.assembler.metadata.Label(newTarget.getOffset()));
                        }
                    }
                }

                if (newDefault != null || newTargets != null) {
                    final SwitchInfo newOperand = new SwitchInfo(
                        oldOperand.getKeys(),
                        newDefault != null ? newDefault : oldDefault,
                        newTargets != null ? newTargets : oldTargets
                    );

                    instruction.setOperand(newOperand);
                }
            }
        }
    }

    private static InstructionCollection copyInstructions(final InstructionCollection instructions) {
        final InstructionCollection instructionsCopy = new InstructionCollection();

        for (final Instruction instruction : instructions) {
            final Instruction copy = new Instruction(instruction.getOffset(), instruction.getOpCode());

            if (instruction.getOperandCount() > 1) {
                final Object[] operands = new Object[instruction.getOperandCount()];

                for (int i = 0; i < operands.length; i++) {
                    operands[i] = instruction.getOperand(i);
                }

                copy.setOperand(operands);
            }
            else {
                copy.setOperand(instruction.getOperand(0));
            }

            copy.setLabel(instruction.getLabel());

            instructionsCopy.add(copy);
        }

        for (final Instruction instruction : instructionsCopy) {
            if (!instruction.hasOperand()) {
                continue;
            }

            final Object operand = instruction.getOperand(0);

            if (operand instanceof Instruction) {
                instruction.setOperand(instructionsCopy.atOffset(((Instruction) operand).getOffset()));
            }
            else if (operand instanceof SwitchInfo) {
                final SwitchInfo oldOperand = (SwitchInfo) operand;

                final Instruction oldDefault = oldOperand.getDefaultTarget();
                final Instruction newDefault = instructionsCopy.atOffset(oldDefault.getOffset());

                final Instruction[] oldTargets = oldOperand.getTargets();
                final Instruction[] newTargets = new Instruction[oldTargets.length];

                for (int i = 0; i < newTargets.length; i++) {
                    newTargets[i] = instructionsCopy.atOffset(oldTargets[i].getOffset());
                }

                final SwitchInfo newOperand = new SwitchInfo(oldOperand.getKeys(), newDefault, newTargets);

                instruction.setOperand(newOperand);
            }
        }

        return instructionsCopy;
    }

    @SuppressWarnings("ConstantConditions")
    private List<ByteCode> performStackAnalysis() {
        final Map<Instruction, ByteCode> byteCodeMap = new LinkedHashMap<>();
        final InstructionCollection instructions = _instructions;
        final List<ExceptionHandler> exceptionHandlers = _exceptionHandlers;

        final List<ByteCode> body = new ArrayList<>(instructions.size());
        final StackMappingVisitor stackMapper = new StackMappingVisitor();
        final InstructionVisitor instructionVisitor = stackMapper.visitBody(_body);

        final StrongBox<AstCode> codeBox = new StrongBox<>();
        final StrongBox<Object> operandBox = new StrongBox<>();

        for (final Instruction instruction : instructions) {
            final OpCode opCode = instruction.getOpCode();

            AstCode code = CODES[opCode.ordinal()];
            Object operand = instruction.hasOperand() ? instruction.getOperand(0) : null;

            final Object secondOperand = instruction.getOperandCount() > 1 ? instruction.getOperand(1) : null;

            codeBox.set(code);
            operandBox.set(operand);

            final int offset = _originalInstructionMap.get(instruction).getOffset();

            if (AstCode.expandMacro(codeBox, operandBox, _body, offset)) {
                code = codeBox.get();
                operand = operandBox.get();
            }

            final ByteCode byteCode = new ByteCode();

            byteCode.instruction = instruction;
            byteCode.offset = instruction.getOffset();
            byteCode.endOffset = instruction.getEndOffset();
            byteCode.code = code;
            byteCode.operand = operand;
            byteCode.secondOperand = secondOperand;
            byteCode.popCount = InstructionHelper.getPopDelta(instruction, _body);
            byteCode.pushCount = InstructionHelper.getPushDelta(instruction, _body);

            byteCodeMap.put(instruction, byteCode);
            body.add(byteCode);
        }

        for (int i = 0, n = body.size() - 1; i < n; i++) {
            final ByteCode next = body.get(i + 1);
            final ByteCode current = body.get(i);

            current.next = next;
            next.previous = current;
        }

        final Stack<ByteCode> agenda = new Stack<>();
        final VariableDefinitionCollection variables = _body.getVariables();

        final int variableCount = variables.slotCount();
        final Set<ByteCode> exceptionHandlerStarts = new LinkedHashSet<>(exceptionHandlers.size());
        final VariableSlot[] unknownVariables = VariableSlot.makeUnknownState(variableCount);
        final MethodReference method = _body.getMethod();
        final List<ParameterDefinition> parameters = method.getParameters();
        final boolean isConstructor = method.isConstructor();

        if (isConstructor) {
            unknownVariables[0] = new VariableSlot(FrameValue.UNINITIALIZED_THIS, EMPTY_DEFINITIONS);
        }

        for (int i = 0; i < parameters.size(); i++) {
            final ParameterDefinition parameter = parameters.get(i);
            final TypeReference parameterType = parameter.getParameterType();
            final int slot = parameter.getSlot();

            switch (parameterType.getSimpleType()) {
                case Boolean:
                case Byte:
                case Character:
                case Short:
                case Integer:
                    unknownVariables[isConstructor ? slot + 1 : slot] = new VariableSlot(FrameValue.INTEGER, EMPTY_DEFINITIONS);
                    break;
                case Long:
                    unknownVariables[isConstructor ? slot + 1 : slot] = new VariableSlot(FrameValue.LONG, EMPTY_DEFINITIONS);
                    break;
                case Float:
                    unknownVariables[isConstructor ? slot + 1 : slot] = new VariableSlot(FrameValue.FLOAT, EMPTY_DEFINITIONS);
                    break;
                case Double:
                    unknownVariables[isConstructor ? slot + 1 : slot] = new VariableSlot(FrameValue.DOUBLE, EMPTY_DEFINITIONS);
                    break;
                default:
                    unknownVariables[isConstructor ? slot + 1 : slot] = new VariableSlot(FrameValue.makeReference(parameterType), EMPTY_DEFINITIONS);
                    break;
            }
        }

        for (final ExceptionHandler handler : exceptionHandlers) {
            final ByteCode handlerStart = byteCodeMap.get(handler.getHandlerBlock().getFirstInstruction());

            handlerStart.stackBefore = EMPTY_STACK;
            handlerStart.variablesBefore = unknownVariables;

            final ByteCode loadException = new ByteCode();
            final TypeReference catchType;

            if (handler.isFinally()) {
                catchType = _context.getCurrentType().getResolver().lookupType("java/lang/Throwable");
            }
            else {
                catchType = handler.getCatchType();
            }

            loadException.code = AstCode.LoadException;
            loadException.operand = catchType;
            loadException.popCount = 0;
            loadException.pushCount = 1;

            _loadExceptions.put(handler, loadException);

            handlerStart.stackBefore = new StackSlot[] {
                new StackSlot(
                    FrameValue.makeReference(catchType),
                    new ByteCode[] { loadException }
                )
            };

            exceptionHandlerStarts.add(handlerStart);
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

            stackMapper.visitFrame(byteCode.getFrameBefore());
            instructionVisitor.visit(byteCode.instruction);

            final StackSlot[] newStack = createModifiedStack(byteCode, stackMapper);

            //
            // Calculate new variable state.
            //
            VariableSlot[] newVariableState = VariableSlot.cloneVariableState(byteCode.variablesBefore);

            if (byteCode.isVariableDefinition()) {
                final int slot = ((VariableReference) byteCode.operand).getSlot();

                newVariableState[slot] = new VariableSlot(
                    stackMapper.getLocalValue(slot),
                    new ByteCode[] { byteCode }
                );
            }

            if (byteCode.code == AstCode.Goto ||
                byteCode.code == AstCode.__GotoW) {

                //
                // After GOTO, finally block might have touched the variables.
                //
                newVariableState = VariableSlot.makeUnknownState(variableCount);
            }

            //
            // Find all successors.
            //
            final ArrayList<ByteCode> branchTargets = new ArrayList<>();

            if (!byteCode.code.isUnconditionalControlFlow()) {
                if (exceptionHandlerStarts.contains(byteCode.next)) {
                    //
                    // Do not fall through down to exception handler.  It's invalid bytecode,
                    // but a non-compliant compiler could produce it.
                    //
                }
                else {
                    branchTargets.add(byteCode.next);
                }
            }

            if (byteCode.operand instanceof Instruction[]) {
                for (final Instruction instruction : (Instruction[]) byteCode.operand) {
                    final ByteCode target = byteCodeMap.get(instruction);

                    branchTargets.add(target);

                    //
                    // Branch target must have a label.
                    //
                    if (target.label == null) {
                        target.label = new Label();
                        target.label.setName(target.name());
                    }
                }
            }
            else if (byteCode.operand instanceof Instruction) {
                final Instruction instruction = (Instruction) byteCode.operand;
                final ByteCode target = byteCodeMap.get(instruction);

                branchTargets.add(target);

                //
                // Branch target must have a label.
                //
                if (target.label == null) {
                    target.label = new Label();
                    target.label.setName(target.name());
                }
            }
            else if (byteCode.operand instanceof SwitchInfo) {
                final SwitchInfo switchInfo = (SwitchInfo) byteCode.operand;
                final ByteCode defaultTarget = byteCodeMap.get(switchInfo.getDefaultTarget());

                branchTargets.add(defaultTarget);

                //
                // Branch target must have a label.
                //
                if (defaultTarget.label == null) {
                    defaultTarget.label = new Label();
                    defaultTarget.label.setName(defaultTarget.name());
                }

                for (final Instruction instruction : switchInfo.getTargets()) {
                    final ByteCode target = byteCodeMap.get(instruction);

                    branchTargets.add(target);

                    //
                    // Branch target must have a label.
                    //
                    if (target.label == null) {
                        target.label = new Label();
                        target.label.setName(target.name());
                    }
                }
            }

            //
            // Apply the state to successors.
            //
            for (final ByteCode branchTarget : branchTargets) {
                if (branchTarget.stackBefore == null && branchTarget.variablesBefore == null) {
                    if (branchTargets.size() == 1) {
                        branchTarget.stackBefore = newStack;
                        branchTarget.variablesBefore = newVariableState;
                    }
                    else {
                        //
                        // Do not share data for several bytecodes.
                        //
                        branchTarget.stackBefore = StackSlot.modifyStack(newStack, 0, null);
                        branchTarget.variablesBefore = VariableSlot.cloneVariableState(newVariableState);
                    }

                    agenda.push(branchTarget);
                }
                else {
                    if (branchTarget.stackBefore.length != newStack.length) {
                        throw new IllegalStateException("Inconsistent stack size at " + byteCode.name() + ".");
                    }

                    //
                    // Be careful not to change our new data; it might be reused for several branch targets.
                    // In general, be careful that two bytecodes never share data structures.
                    //

                    boolean modified = false;

                    //
                    // Merge stacks; modify the target.
                    //
                    for (int i = 0; i < newStack.length; i++) {
                        final ByteCode[] oldDefinitions = branchTarget.stackBefore[i].definitions;
                        final ByteCode[] newDefinitions = ArrayUtilities.union(oldDefinitions, newStack[i].definitions);

                        if (newDefinitions.length > oldDefinitions.length) {
                            branchTarget.stackBefore[i] = new StackSlot(newStack[i].value, newDefinitions);
                            modified = true;
                        }
                    }

                    //
                    // Merge variables; modify the target;
                    //
                    for (int i = 0; i < newVariableState.length; i++) {
                        final VariableSlot oldSlot = branchTarget.variablesBefore[i];
                        final VariableSlot newSlot = newVariableState[i];

                        if (!oldSlot.isUninitialized()) {
                            if (newSlot.isUninitialized()) {
                                branchTarget.variablesBefore[i] = newSlot;
                                modified = true;
                            }
                            else {
                                final ByteCode[] oldDefinitions = oldSlot.definitions;
                                final ByteCode[] newDefinitions = ArrayUtilities.union(oldSlot.definitions, newSlot.definitions);

                                if (newDefinitions.length > oldDefinitions.length) {
                                    branchTarget.variablesBefore[i] = new VariableSlot(oldSlot.value, newDefinitions);
                                    modified = true;
                                }
                            }
                        }
                    }

                    if (modified) {
                        agenda.push(branchTarget);
                    }
                }
            }
        }

        //
        // Occasionally, compilers or obfuscators may generate unreachable code (which might be intentionally invalid).
        // It should be safe to simply remove it.
        //

        ArrayList<ByteCode> unreachable = null;

        for (final ByteCode byteCode : body) {
            if (byteCode.stackBefore == null) {
                if (unreachable == null) {
                    unreachable = new ArrayList<>();
                }

                unreachable.add(byteCode);
            }
        }

        if (unreachable != null) {
            body.removeAll(unreachable);
        }

        //
        // Generate temporary variables to replace stack values.
        //
        for (final ByteCode byteCode : body) {
            final int popCount = byteCode.popCount != -1 ? byteCode.popCount : byteCode.stackBefore.length;

            int argumentIndex = 0;

            for (int i = byteCode.stackBefore.length - popCount; i < byteCode.stackBefore.length; i++) {
                final Variable tempVariable = new Variable();

                tempVariable.setName(format("stack_%1$02X_%2$d", byteCode.offset, argumentIndex));
                tempVariable.setGenerated(true);

                final FrameValue value = byteCode.stackBefore[i].value;

                switch (value.getType()) {
                    case Integer:
                        tempVariable.setType(BuiltinTypes.Integer);
                        break;
                    case Float:
                        tempVariable.setType(BuiltinTypes.Float);
                        break;
                    case Long:
                        tempVariable.setType(BuiltinTypes.Long);
                        break;
                    case Double:
                        tempVariable.setType(BuiltinTypes.Double);
                        break;
                    case UninitializedThis:
                        tempVariable.setType(_context.getCurrentType());
                        break;
                    case Reference:
                        tempVariable.setType((TypeReference) value.getParameter());
                        break;
                }

                byteCode.stackBefore[i] = new StackSlot(value, byteCode.stackBefore[i].definitions, tempVariable);

                for (final ByteCode pushedBy : byteCode.stackBefore[i].definitions) {
                    if (pushedBy.storeTo == null) {
                        pushedBy.storeTo = new ArrayList<>();
                    }

                    pushedBy.storeTo.add(tempVariable);
                }

                argumentIndex++;
            }
        }

        //
        // Try to use a single temporary variable instead of several, if possible (especially useful for DUP).
        // This has to be done after all temporary variables are assigned so we know about all loads.
        //
        for (final ByteCode byteCode : body) {
            if (byteCode.storeTo != null && byteCode.storeTo.size() > 1) {
                final List<Variable> localVariables = byteCode.storeTo;

                //
                // For each of the variables, find the location where it is loaded; there should be exactly one.
                //
                List<StackSlot> loadedBy = null;

                for (final Variable local : localVariables) {
                inner:
                    for (final ByteCode bc : body) {
                        for (final StackSlot s : bc.stackBefore) {
                            if (s.loadFrom == local) {
                                if (loadedBy == null) {
                                    loadedBy = new ArrayList<>();
                                }

                                loadedBy.add(s);
                                break inner;
                            }
                        }
                    }
                }

                if (loadedBy == null) {
                    continue;
                }

                //
                // We know that all the temp variables have a single load; now make sure they have a single store. 
                //
                boolean singleStore = true;
                TypeReference type = null;

                for (final StackSlot slot : loadedBy) {
                    if (slot.definitions.length != 1) {
                        singleStore = false;
                        break;
                    }
                    else if (slot.definitions[0] != byteCode) {
                        singleStore = false;
                        break;
                    }
                    else if (type == null) {
                        switch (slot.value.getType()) {
                            case Integer:
                                type = BuiltinTypes.Integer;
                                break;
                            case Float:
                                type = BuiltinTypes.Float;
                                break;
                            case Long:
                                type = BuiltinTypes.Long;
                                break;
                            case Double:
                                type = BuiltinTypes.Double;
                                break;
                            case Reference:
                                type = (TypeReference) slot.value.getParameter();
                                break;
                        }
                    }
                }

                if (!singleStore) {
                    continue;
                }

                //
                // We can now reduce everything into a single variable.
                //
                final Variable tempVariable = new Variable();

                tempVariable.setName(format("expr_%1$02X", byteCode.offset));
                tempVariable.setGenerated(true);
                tempVariable.setType(type);

                byteCode.storeTo = Collections.singletonList(tempVariable);

                for (final ByteCode bc : body) {
                    for (int i = 0; i < bc.stackBefore.length; i++) {
                        //
                        // Is it one of the variables we merged?
                        //
                        if (localVariables.contains(bc.stackBefore[i].loadFrom)) {
                            //
                            // Replace with the new temp variable.
                            //
                            bc.stackBefore[i] = new StackSlot(bc.stackBefore[i].value, bc.stackBefore[i].definitions, tempVariable);
                        }
                    }
                }
            }
        }

        //
        // Split and convert the normal local variables.
        //
        convertLocalVariables(body);

        //
        // Convert branch targets to labels.
        //
        for (final ByteCode byteCode : body) {
            if (byteCode.operand instanceof Instruction[]) {
                final Instruction[] branchTargets = (Instruction[]) byteCode.operand;
                final Label[] newOperand = new Label[branchTargets.length];

                for (int i = 0; i < branchTargets.length; i++) {
                    newOperand[i] = byteCodeMap.get(branchTargets[i]).label;
                }

                byteCode.operand = newOperand;
            }
            else if (byteCode.operand instanceof Instruction) {
                //noinspection SuspiciousMethodCalls
                byteCode.operand = byteCodeMap.get(byteCode.operand).label;
            }
            else if (byteCode.operand instanceof SwitchInfo) {
                final SwitchInfo switchInfo = (SwitchInfo) byteCode.operand;
                final Instruction[] branchTargets = ArrayUtilities.prepend(switchInfo.getTargets(), switchInfo.getDefaultTarget());
                final Label[] newOperand = new Label[branchTargets.length];

                for (int i = 0; i < branchTargets.length; i++) {
                    newOperand[i] = byteCodeMap.get(branchTargets[i]).label;
                }

                byteCode.operand = newOperand;
            }
        }

        //
        // Convert parameters to Variables.
        //
//        convertParameters(body);

        return body; //removeRedundantFinallyBlocks(body, byteCodeMap, exceptionHandlers);
    }

    private static StackSlot[] createModifiedStack(final ByteCode byteCode, final StackMappingVisitor stackMapper) {
        final StackSlot[] oldStack = byteCode.stackBefore;

        if (byteCode.popCount == 0 && byteCode.pushCount == 0) {
            return oldStack;
        }

        switch (byteCode.code) {
            case Dup:
                return ArrayUtilities.append(
                    oldStack,
                    new StackSlot(stackMapper.getStackValue(0), new ByteCode[] { byteCode }/*, oldStack[oldStack.length - 1].loadFrom*/)
                );

            case DupX1:
                return ArrayUtilities.insert(
                    oldStack,
                    oldStack.length - 2,
                    new StackSlot(stackMapper.getStackValue(0), new ByteCode[] { byteCode }/*, oldStack[oldStack.length - 1].loadFrom*/)
                );

            case DupX2:
                return ArrayUtilities.insert(
                    oldStack,
                    oldStack.length - 3,
                    new StackSlot(stackMapper.getStackValue(0), new ByteCode[] { byteCode }/*, oldStack[oldStack.length - 1].loadFrom*/)
                );

            case Dup2:
                return ArrayUtilities.append(
                    oldStack,
                    new StackSlot(stackMapper.getStackValue(0), new ByteCode[] { byteCode }/*, oldStack[oldStack.length - 2].loadFrom*/),
                    new StackSlot(stackMapper.getStackValue(0), new ByteCode[] { byteCode }/*, oldStack[oldStack.length - 1].loadFrom*/)
                );

            case Dup2X1:
                return ArrayUtilities.insert(
                    oldStack,
                    oldStack.length - 3,
                    new StackSlot(stackMapper.getStackValue(1), new ByteCode[] { byteCode }/*, oldStack[oldStack.length - 2].loadFrom*/),
                    new StackSlot(stackMapper.getStackValue(0), new ByteCode[] { byteCode }/*, oldStack[oldStack.length - 1].loadFrom*/)
                );

            case Dup2X2:
                return ArrayUtilities.insert(
                    oldStack,
                    oldStack.length - 4,
                    new StackSlot(stackMapper.getStackValue(1), new ByteCode[] { byteCode }/*, oldStack[oldStack.length - 2].loadFrom*/),
                    new StackSlot(stackMapper.getStackValue(0), new ByteCode[] { byteCode }/*, oldStack[oldStack.length - 1].loadFrom*/)
                );

            case Swap:
                final StackSlot[] newStack = new StackSlot[oldStack.length];

                ArrayUtilities.copy(oldStack, newStack);

                final StackSlot temp = newStack[oldStack.length - 1];

                newStack[oldStack.length - 1] = newStack[oldStack.length - 2];
                newStack[oldStack.length - 2] = temp;

                return newStack;

            default:
                final FrameValue[] pushValues = new FrameValue[byteCode.pushCount];

                for (int i = 0; i < byteCode.pushCount; i++) {
                    pushValues[pushValues.length - i - 1] = stackMapper.getStackValue(i);
                }

                return StackSlot.modifyStack(
                    oldStack,
                    byteCode.popCount != -1 ? byteCode.popCount : oldStack.length,
                    byteCode,
                    pushValues
                );
        }
    }

    private final static class VariableInfo {
        final Variable variable;
        final List<ByteCode> definitions;
        final List<ByteCode> references;

        VariableInfo(final Variable variable, final List<ByteCode> definitions, final List<ByteCode> references) {
            this.variable = variable;
            this.definitions = definitions;
            this.references = references;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void convertLocalVariables(final List<ByteCode> body) {
        final MethodDefinition method = _context.getCurrentMethod();
        final List<ParameterDefinition> parameters = method.getParameters();
        final VariableDefinitionCollection variables = _body.getVariables();
        final ParameterDefinition[] parameterMap = new ParameterDefinition[variables.slotCount()];
        final boolean hasThis = _body.hasThis();

        if (hasThis) {
            parameterMap[0] = _body.getThisParameter();
        }

        for (final ParameterDefinition parameter : parameters) {
            parameterMap[parameter.getSlot() + (hasThis ? 1 : 0)] = parameter;
        }

        for (final VariableDefinition variableDefinition : variables) {
            //
            // Find all definitions of and references to this variable.
            //

            final List<ByteCode> definitions = new ArrayList<>();
            final List<ByteCode> references = new ArrayList<>();

            for (final ByteCode b : body) {
                if (b.operand == variableDefinition) {
                    if (b.isVariableDefinition()) {
                        definitions.add(b);
                    }
                    else {
                        references.add(b);
                    }
                }
            }

            final List<VariableInfo> newVariables;
            boolean fromUnknownDefinition = false;

            final int variableIndex = variableDefinition.getSlot();

            if (_optimize) {
                for (final ByteCode b : references) {
                    if (b.variablesBefore[variableIndex].isUninitialized()) {
                        fromUnknownDefinition = true;
                        break;
                    }
                }
            }

            final ParameterDefinition parameter = parameterMap[variableIndex];

            if (parameter != null) {
                final Variable variable = new Variable();

                variable.setName(
                    StringUtilities.isNullOrEmpty(parameter.getName()) ? "p" + parameter.getPosition()
                                                                       : parameter.getName()
                );

                variable.setType(parameter.getParameterType());
                variable.setOriginalParameter(parameter);

                final VariableInfo variableInfo = new VariableInfo(variable, definitions, references);

                newVariables = Collections.singletonList(variableInfo);
            }
            else if (!_optimize || fromUnknownDefinition) {
                final Variable variable = new Variable();

                variable.setName(
                    StringUtilities.isNullOrEmpty(variableDefinition.getName()) ? "var_" + variableIndex
                                                                                : variableDefinition.getName()
                );

                if (variableDefinition.isTypeKnown()) {
                    variable.setType(variableDefinition.getVariableType());
                }
                else {
                    for (final ByteCode b : definitions) {
                        final FrameValue stackValue = b.stackBefore[b.stackBefore.length - b.popCount].value;

                        if (stackValue != FrameValue.UNINITIALIZED &&
                            stackValue != FrameValue.UNINITIALIZED_THIS) {

                            final TypeReference variableType;

                            switch (stackValue.getType()) {
                                case Integer:
                                    variableType = BuiltinTypes.Integer;
                                    break;
                                case Float:
                                    variableType = BuiltinTypes.Float;
                                    break;
                                case Long:
                                    variableType = BuiltinTypes.Long;
                                    break;
                                case Double:
                                    variableType = BuiltinTypes.Double;
                                    break;
                                case UninitializedThis:
                                    variableType = _context.getCurrentType();
                                    break;
                                case Reference:
                                    variableType = (TypeReference) stackValue.getParameter();
                                    break;
                                default:
                                    variableType = variableDefinition.getVariableType();
                                    break;
                            }

                            variable.setType(variableType);
                            break;
                        }
                    }
                }

                variable.setOriginalVariable(variableDefinition);

                final VariableInfo variableInfo = new VariableInfo(variable, definitions, references);

                newVariables = Collections.singletonList(variableInfo);
            }
            else {
                newVariables = new ArrayList<>();

                for (final ByteCode b : definitions) {
                    final Variable variable = new Variable();

                    variable.setName(
                        format(
                            "%1$s_%2$02X",
                            StringUtilities.isNullOrEmpty(variableDefinition.getName()) ? "var_" + variableIndex
                                                                                        : variableDefinition.getName(),
                            b.offset
                        )
                    );

                    final TypeReference variableType;
                    final FrameValue stackValue = b.stackBefore[b.stackBefore.length - b.popCount].value;

                    switch (stackValue.getType()) {
                        case Integer:
                            variableType = BuiltinTypes.Integer;
                            break;
                        case Float:
                            variableType = BuiltinTypes.Float;
                            break;
                        case Long:
                            variableType = BuiltinTypes.Long;
                            break;
                        case Double:
                            variableType = BuiltinTypes.Double;
                            break;
                        case UninitializedThis:
                            variableType = _context.getCurrentType();
                            break;
                        case Reference:
                            variableType = (TypeReference) stackValue.getParameter();
                            break;
                        default:
                            variableType = variableDefinition.getVariableType();
                            break;
                    }

                    variable.setType(variableType);
                    variable.setOriginalVariable(variableDefinition);

                    final VariableInfo variableInfo = new VariableInfo(
                        variable,
                        new ArrayList<ByteCode>(),
                        new ArrayList<ByteCode>()
                    );

                    variableInfo.definitions.add(b);
                    newVariables.add(variableInfo);
                }

                //
                // Add loads to the data structure; merge variables if necessary.
                //
                for (final ByteCode ref : references) {
                    final ByteCode[] refDefinitions = ref.variablesBefore[variableIndex].definitions;

                    if (refDefinitions.length == 1) {
                        VariableInfo newVariable = null;

                        for (final VariableInfo v : newVariables) {
                            if (v.definitions.contains(refDefinitions[0])) {
                                newVariable = v;
                                break;
                            }
                        }

                        assert newVariable != null;

                        newVariable.references.add(ref);
                    }
                    else {
                        final ArrayList<VariableInfo> mergeVariables = new ArrayList<>();

                        for (final VariableInfo v : newVariables) {
                            boolean hasIntersection = false;

                        outer:
                            for (final ByteCode b1 : v.definitions) {
                                for (final ByteCode b2 : refDefinitions) {
                                    if (b1 == b2) {
                                        hasIntersection = true;
                                        break outer;
                                    }
                                }
                            }

                            if (hasIntersection) {
                                mergeVariables.add(v);
                            }
                        }

                        final ArrayList<ByteCode> mergedDefinitions = new ArrayList<>();
                        final ArrayList<ByteCode> mergedReferences = new ArrayList<>();

                        for (final VariableInfo v : mergeVariables) {
                            mergedDefinitions.addAll(v.definitions);
                            mergedReferences.addAll(v.references);
                        }

                        final VariableInfo mergedVariable = new VariableInfo(
                            mergeVariables.get(0).variable,
                            mergedDefinitions,
                            mergedReferences
                        );

                        mergedVariable.references.add(ref);
                        newVariables.removeAll(mergeVariables);
                        newVariables.add(mergedVariable);
                    }
                }
            }

            //
            // Set bytecode operands.
            //
            for (final VariableInfo newVariable : newVariables) {
                for (final ByteCode definition : newVariable.definitions) {
                    definition.operand = newVariable.variable;
                }
                for (final ByteCode reference : newVariable.references) {
                    reference.operand = newVariable.variable;
                }
            }
        }
    }

/*
    final List<Variable> parameters = new ArrayList<>();

    private void convertParameters(final List<ByteCode> body) {
        Variable thisParameter = null;

        if (_body.hasThis()) {
            final TypeReference type = _methodDefinition.getDeclaringType();

            thisParameter = new Variable();
            thisParameter.setName("this");
            thisParameter.setType(type);
            thisParameter.setOriginalParameter(_body.getThisParameter());
        }

        for (final ParameterDefinition parameter : _methodDefinition.getParameters()) {
            final Variable variable = new Variable();

            variable.setName(parameter.getName());
            variable.setType(parameter.getParameterType());
            variable.setOriginalParameter(parameter);

            this.parameters.add(variable);
        }

        for (final ByteCode byteCode : body) {
            ParameterDefinition p;
            
            switch (byteCode.code) {
                case __ILoad:
                case __LLoad:
                case __FLoad:
                case __DLoad:
            }
        }
    }
*/

    @SuppressWarnings("ConstantConditions")
    private List<Node> convertToAst(final List<ByteCode> body, final Set<ExceptionHandler> exceptionHandlers) {
        final ArrayList<Node> ast = new ArrayList<>();

        while (!exceptionHandlers.isEmpty()) {
            final TryCatchBlock tryCatchBlock = new TryCatchBlock();

            //
            // Find the first and widest scope;
            //

            int tryStart = Integer.MAX_VALUE;
            int tryEnd = -1;

            for (final ExceptionHandler handler : exceptionHandlers) {
                final int start = handler.getTryBlock().getFirstInstruction().getOffset();

                if (start < tryStart) {
                    tryStart = start;
                }
            }

            for (final ExceptionHandler handler : exceptionHandlers) {
                final int start = handler.getTryBlock().getFirstInstruction().getOffset();

                if (start == tryStart) {
                    final Instruction lastInstruction = handler.getTryBlock().getLastInstruction();
                    final int end = lastInstruction.getEndOffset();

                    if (end > tryEnd) {
                        tryEnd = end;
                    }
                }
            }

            final ArrayList<ExceptionHandler> handlers = new ArrayList<>();

            for (final ExceptionHandler handler : exceptionHandlers) {
                final int start = handler.getTryBlock().getFirstInstruction().getOffset();
                final int end = handler.getTryBlock().getLastInstruction().getEndOffset();

                if (start == tryStart && end == tryEnd) {
                    handlers.add(handler);
                }
            }

            Collections.sort(
                handlers,
                new Comparator<ExceptionHandler>() {
                    @Override
                    public int compare(final ExceptionHandler o1, final ExceptionHandler o2) {
                        return Integer.compare(
                            o1.getTryBlock().getFirstInstruction().getOffset(),
                            o2.getTryBlock().getFirstInstruction().getOffset()
                        );
                    }
                }
            );

            //
            // Remember that any part of the body might have been removed due to unreachability.
            //

            //
            // Cut all instructions up to the try block.
            //
            int tryStartIndex = 0;

            while (tryStartIndex < body.size() &&
                   body.get(tryStartIndex).offset < tryStart) {

                tryStartIndex++;
            }

            ast.addAll(convertToAst(body.subList(0, tryStartIndex)));

            for (int i = 0; i < tryStartIndex; i++) {
                body.remove(0);
            }

            //
            // Cut the try block.
            //
            {
                final Set<ExceptionHandler> nestedHandlers = new LinkedHashSet<>();

                for (final ExceptionHandler eh : exceptionHandlers) {
                    final int ts = eh.getTryBlock().getFirstInstruction().getOffset();
                    final int te = eh.getTryBlock().getLastInstruction().getEndOffset();

                    if (tryStart <= ts && te < tryEnd ||
                        tryStart < ts && te <= tryEnd) {

                        nestedHandlers.add(eh);
                    }
                }

                exceptionHandlers.removeAll(nestedHandlers);

                int tryEndIndex = 0;

                while (tryEndIndex < body.size() && body.get(tryEndIndex).offset < tryEnd) {
                    tryEndIndex++;
                }

                final Block tryBlock = new Block();
                final ArrayList<ByteCode> tryBody = new ArrayList<>(body.subList(0, tryEndIndex));

                for (int i = 0; i < tryEndIndex; i++) {
                    body.remove(0);
                }

                final List<Node> tryAst = convertToAst(tryBody, nestedHandlers);
                final Node lastInTry = lastOrDefault(tryAst);

                if (lastInTry != null && !lastInTry.isUnconditionalControlFlow()) {
                    tryAst.add(new Expression(AstCode.Leave, null));
                }

                tryBlock.getBody().addAll(tryAst);
                tryCatchBlock.setTryBlock(tryBlock);
            }

            //
            // Cut all handlers.
            //
        HandlerLoop:
            for (final ExceptionHandler eh : handlers) {
                final TypeReference catchType = eh.getCatchType();
                final ExceptionBlock handlerBlock = eh.getHandlerBlock();

                final int handlerStart = handlerBlock.getFirstInstruction().getOffset();

                final int handlerEnd = handlerBlock.getLastInstruction() != null
                                       ? handlerBlock.getLastInstruction().getEndOffset()
                                       : _body.getCodeSize();

                int startIndex = 0;

                while (startIndex < body.size() &&
                       body.get(startIndex).offset < handlerStart) {

                    startIndex++;
                }

                int endIndex = startIndex;

                while (endIndex < body.size() &&
                       body.get(endIndex).offset < handlerEnd) {

                    endIndex++;
                }

                //
                // See if we share a block with another handler; if so, add our catch type and move on.
                //
                for (final CatchBlock catchBlock : tryCatchBlock.getCatchBlocks()) {
                    final Expression firstExpression = firstOrDefault(catchBlock.getSelfAndChildrenRecursive(Expression.class));
                    final int otherHandlerStart = firstExpression.getRanges().get(0).getStart();

                    if (otherHandlerStart == handlerStart) {
                        catchBlock.getCaughtTypes().add(catchType);

                        catchBlock.setExceptionType(
                            MetadataHelper.findCommonSuperType(
                                catchBlock.getExceptionType(),
                                catchType
                            )
                        );

                        continue HandlerLoop;
                    }
                }

                final Set<ExceptionHandler> nestedHandlers = new LinkedHashSet<>();

                for (final ExceptionHandler e : exceptionHandlers) {
                    final int ts = eh.getTryBlock().getFirstInstruction().getOffset();
                    final int te = eh.getTryBlock().getLastInstruction().getEndOffset();
                    final int hs = eh.getHandlerBlock().getFirstInstruction().getOffset();
                    final int he = eh.getHandlerBlock().getLastInstruction().getEndOffset();

                    if (hs == handlerStart && he == handlerEnd) {
                        continue;
                    }

                    if (handlerStart <= ts && te < handlerEnd ||
                        handlerStart < ts && te <= handlerEnd) {

                        nestedHandlers.add(e);
                    }
                }

                exceptionHandlers.removeAll(nestedHandlers);

                final ArrayList<ByteCode> handlerCode = new ArrayList<>(body.subList(startIndex, endIndex));

                for (int i = startIndex; i < endIndex; i++) {
                    body.remove(startIndex);
                }

                final List<Node> handlerAst = convertToAst(handlerCode, nestedHandlers);
                final Node lastInHandler = lastOrDefault(handlerAst);

                if (lastInHandler != null && !lastInHandler.isUnconditionalControlFlow()) {
                    handlerAst.add(new Expression(AstCode.Leave, null));
                }

                if (eh.isCatch()) {
                    final CatchBlock catchBlock = new CatchBlock();

                    catchBlock.setExceptionType(catchType);
                    catchBlock.getCaughtTypes().add(catchType);
                    catchBlock.getBody().addAll(handlerAst);

                    final ByteCode loadException = _loadExceptions.get(eh);

                    if (loadException.storeTo == null || loadException.storeTo.isEmpty()) {
                        //
                        // Exception is not used.
                        //
                        catchBlock.setExceptionVariable(null);
                    }
                    else if (loadException.storeTo.size() == 1) {
                        if (catchBlock.getBody().get(0) instanceof Expression) {
                            final Expression first = (Expression) catchBlock.getBody().get(0);
                            final AstCode firstCode = first.getCode();
                            final Expression firstArgument = first.getArguments().get(0);

                            if (firstCode == AstCode.Pop &&
                                firstArgument.getCode() == AstCode.Load &&
                                firstArgument.getOperand() == loadException.storeTo.get(0)) {

                                //
                                // The exception is just popped; optimize it away.
                                //
                                if (_context.getSettings().getAlwaysGenerateExceptionVariableForCatchBlocks()) {
                                    final Variable exceptionVariable = new Variable();

                                    exceptionVariable.setName(format("ex_%1$02X", handlerStart));
                                    exceptionVariable.setGenerated(true);

                                    catchBlock.setExceptionVariable(exceptionVariable);
                                }
                                else {
                                    catchBlock.setExceptionVariable(null);
                                }
                            }
                            else {
                                catchBlock.setExceptionVariable(loadException.storeTo.get(0));
                            }
                        }
                    }
                    else {
                        final Variable exceptionTemp = new Variable();

                        exceptionTemp.setName(format("ex_%1$02X", handlerStart));
                        exceptionTemp.setGenerated(true);

                        catchBlock.setExceptionVariable(exceptionTemp);

                        for (final Variable storeTo : loadException.storeTo) {
                            catchBlock.getBody().add(
                                0,
                                new Expression(AstCode.Store, storeTo, new Expression(AstCode.Load, exceptionTemp))
                            );
                        }
                    }

                    tryCatchBlock.getCatchBlocks().add(catchBlock);
                }
                else if (eh.isFinally()) {
                    final Block finallyBlock = new Block();

                    finallyBlock.getBody().addAll(handlerAst);
                    tryCatchBlock.setFinallyBlock(finallyBlock);
                }
            }

            exceptionHandlers.removeAll(handlers);

            final Expression first;
            final Expression last;

            first = firstOrDefault(tryCatchBlock.getTryBlock().getSelfAndChildrenRecursive(Expression.class));

            if (!tryCatchBlock.getCatchBlocks().isEmpty()) {
                last = lastOrDefault(lastOrDefault(tryCatchBlock.getCatchBlocks()).getSelfAndChildrenRecursive(Expression.class));
            }
            else {
                last = lastOrDefault(tryCatchBlock.getFinallyBlock().getSelfAndChildrenRecursive(Expression.class));
            }

            if (first == null && last == null) {
                //
                // Ignore empty handlers.  These can crop up due to finally blocks which handle themselves.
                //
                continue;
            }

            ast.add(tryCatchBlock);
        }

        ast.addAll(convertToAst(body));

        return ast;
    }

    private List<Node> convertToAst(final List<ByteCode> body) {
        final ArrayList<Node> ast = new ArrayList<>();

        //
        // Convert stack-based bytecode to bytecode AST.
        //
        for (final ByteCode byteCode : body) {
            final Range codeRange = new Range(byteCode.offset, byteCode.endOffset);

            if (byteCode.stackBefore == null) {
                //
                // Unreachable code.
                //
                continue;
            }

            final Expression expression = new Expression(byteCode.code, byteCode.operand);

            if (byteCode.code == AstCode.Inc) {
                assert byteCode.secondOperand instanceof Integer;

                expression.setCode(AstCode.Inc);
                expression.getArguments().add(new Expression(AstCode.LdC, byteCode.secondOperand));
            }

            expression.getRanges().add(codeRange);

            //
            // Include the instruction's label, if it has one.
            //
            if (byteCode.label != null) {
                ast.add(byteCode.label);
            }

            //
            // Reference arguments using temporary variables.
            //

            final int popCount = byteCode.popCount != -1 ? byteCode.popCount
                                                         : byteCode.stackBefore.length;

            for (int i = byteCode.stackBefore.length - popCount; i < byteCode.stackBefore.length; i++) {
                final StackSlot slot = byteCode.stackBefore[i];

                if (slot.value.getType().isDoubleWord()) {
                    i++;
                }

                expression.getArguments().add(new Expression(AstCode.Load, slot.loadFrom));
            }

            //
            // Store the result to temporary variables, if needed.
            //
            if (byteCode.storeTo == null || byteCode.storeTo.isEmpty()) {
                ast.add(expression);
            }
            else if (byteCode.storeTo.size() == 1) {
                ast.add(new Expression(AstCode.Store, byteCode.storeTo.get(0), expression));
            }
            else {
                final Variable tempVariable = new Variable();

                tempVariable.setName(format("expr_%1$02X", byteCode.offset));
                tempVariable.setGenerated(true);

                ast.add(new Expression(AstCode.Store, tempVariable, expression));

                for (int i = byteCode.storeTo.size() - 1; i >= 0; i--) {
                    ast.add(
                        new Expression(
                            AstCode.Store,
                            byteCode.storeTo.get(i),
                            new Expression(AstCode.Load, tempVariable)
                        )
                    );
                }
            }
        }

        return ast;
    }

    // <editor-fold defaultstate="collapsed" desc="StackSlot Class">

    private final static class StackSlot {
        final FrameValue value;
        final ByteCode[] definitions;
        final Variable loadFrom;

        public StackSlot(final FrameValue value, final ByteCode[] definitions) {
            this.value = VerifyArgument.notNull(value, "value");
            this.definitions = VerifyArgument.notNull(definitions, "definitions");
            this.loadFrom = null;
        }

        public StackSlot(final FrameValue value, final ByteCode[] definitions, final Variable loadFrom) {
            this.value = VerifyArgument.notNull(value, "value");
            this.definitions = VerifyArgument.notNull(definitions, "definitions");
            this.loadFrom = loadFrom;
        }

        public static StackSlot[] modifyStack(
            final StackSlot[] stack,
            final int popCount,
            final ByteCode pushDefinition,
            final FrameValue... pushTypes) {

            VerifyArgument.notNull(stack, "stack");
            VerifyArgument.isNonNegative(popCount, "popCount");
            VerifyArgument.noNullElements(pushTypes, "pushTypes");

            final StackSlot[] newStack = new StackSlot[stack.length - popCount + pushTypes.length];

            System.arraycopy(stack, 0, newStack, 0, stack.length - popCount);

            for (int i = stack.length - popCount, j = 0; i < newStack.length; i++, j++) {
                newStack[i] = new StackSlot(pushTypes[j], new ByteCode[] { pushDefinition });
            }

            return newStack;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="VariableSlot Class">

    private final static class VariableSlot {
        final static VariableSlot UNKNOWN_INSTANCE = new VariableSlot(FrameValue.UNINITIALIZED, EMPTY_DEFINITIONS);

        final ByteCode[] definitions;
        final FrameValue value;

        public VariableSlot(final FrameValue value, final ByteCode[] definitions) {
            this.value = VerifyArgument.notNull(value, "value");
            this.definitions = VerifyArgument.notNull(definitions, "definitions");
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

        public final boolean isUninitialized() {
            return value == FrameValue.UNINITIALIZED || value == FrameValue.UNINITIALIZED_THIS;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="ByteCode Class">

    private final static class ByteCode {
        Label label;
        Instruction instruction;
        int offset;
        int endOffset;
        AstCode code;
        Object operand;
        Object secondOperand;
        int popCount = -1;
        int pushCount;
        ByteCode next;
        ByteCode previous;
        FrameValue type;
        StackSlot[] stackBefore;
        VariableSlot[] variablesBefore;
        List<Variable> storeTo;

        public final String name() {
            return format("#%1$04d", offset);
        }

        public final Frame getFrameBefore() {
            final FrameValue[] stackValues;
            final FrameValue[] variableValues;

            if (stackBefore.length == 0) {
                stackValues = FrameValue.EMPTY_VALUES;
            }
            else {
                stackValues = new FrameValue[stackBefore.length];

                for (int i = 0; i < stackBefore.length; i++) {
                    stackValues[i] = stackBefore[i].value;
                }
            }
            if (variablesBefore.length == 0) {
                variableValues = FrameValue.EMPTY_VALUES;
            }
            else {
                variableValues = new FrameValue[variablesBefore.length];

                for (int i = 0; i < variablesBefore.length; i++) {
                    variableValues[i] = variablesBefore[i].value;
                }
            }

            return new Frame(FrameType.New, variableValues, stackValues);
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

                    if (slot.isUninitialized()) {
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
