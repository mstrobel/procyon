/*
 * ControlFlowGraphBuilder.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler.flowanalysis;

import com.strobel.assembler.Collection;
import com.strobel.assembler.ir.ExceptionBlock;
import com.strobel.assembler.ir.ExceptionHandler;
import com.strobel.assembler.ir.ExceptionHandlerType;
import com.strobel.assembler.ir.FlowControl;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.OpCode;
import com.strobel.assembler.ir.OperandType;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.assembler.metadata.MethodBody;
import com.strobel.assembler.metadata.SwitchInfo;
import com.strobel.core.Comparer;
import com.strobel.core.Predicate;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.strobel.core.CollectionUtilities.firstOrDefault;

@SuppressWarnings("ConstantConditions")
public final class ControlFlowGraphBuilder {
    public static ControlFlowGraph build(final MethodBody methodBody) {
        VerifyArgument.notNull(methodBody, "methodBody");

        final ControlFlowGraphBuilder builder = new ControlFlowGraphBuilder(
            methodBody.getInstructions(),
            methodBody.getExceptionHandlers()
        );

        return builder.build();
    }

    public static ControlFlowGraph build(final List<Instruction> instructions, final List<ExceptionHandler> exceptionHandlers) {
        final ControlFlowGraphBuilder builder = new ControlFlowGraphBuilder(
            VerifyArgument.notNull(instructions, "instructions"),
            VerifyArgument.notNull(exceptionHandlers, "exceptionHandlers")
        );

        return builder.build();
    }

    private final List<Instruction> _instructions;
    private final List<ExceptionHandler> _exceptionHandlers;
    private final List<ControlFlowNode> _nodes = new Collection<>();
    private final int[] _offsets;
    private final boolean[] _hasIncomingJumps;
    private final ControlFlowNode _entryPoint;
    private final ControlFlowNode _regularExit;
    private final ControlFlowNode _exceptionalExit;


    private int _nextBlockId;
    boolean copyFinallyBlocks = false;

    private ControlFlowGraphBuilder(final List<Instruction> instructions, final List<ExceptionHandler> exceptionHandlers) {
        _instructions = VerifyArgument.notNull(instructions, "instructions");
        _exceptionHandlers = coalesceExceptionHandlers(VerifyArgument.notNull(exceptionHandlers, "exceptionHandlers"));

        _offsets = new int[instructions.size()];
        _hasIncomingJumps = new boolean[_offsets.length];

        for (int i = 0; i < instructions.size(); i++) {
            _offsets[i] = instructions.get(i).getOffset();
        }

        _entryPoint = new ControlFlowNode(_nextBlockId++, 0, ControlFlowNodeType.EntryPoint);
        _regularExit = new ControlFlowNode(_nextBlockId++, -1, ControlFlowNodeType.RegularExit);
        _exceptionalExit = new ControlFlowNode(_nextBlockId++, -1, ControlFlowNodeType.ExceptionalExit);

        _nodes.add(_entryPoint);
        _nodes.add(_regularExit);
        _nodes.add(_exceptionalExit);
    }

    public final ControlFlowGraph build() {
        calculateIncomingJumps();
        createNodes();
        createRegularControlFlow();
        createExceptionalControlFlow();

        if (copyFinallyBlocks) {
            copyFinallyBlocksIntoLeaveEdges();
        }
        else {
            transformLeaveEdges();
        }

        return new ControlFlowGraph(_nodes.toArray(new ControlFlowNode[_nodes.size()]));
    }

    private void calculateIncomingJumps() {
        //
        // Step 1: Determine which instructions are jump targets.
        //

        for (final Instruction instruction : _instructions) {
            final OpCode opCode = instruction.getOpCode();

            if (opCode.getOperandType() == OperandType.BranchTarget) {
                _hasIncomingJumps[getInstructionIndex(instruction.<Instruction>getOperand(0))] = true;
            }
            else if (opCode.getOperandType() == OperandType.Switch) {
                final SwitchInfo switchInfo = instruction.getOperand(0);

                _hasIncomingJumps[getInstructionIndex(switchInfo.getDefaultTarget())] = true;

                for (final Instruction target : switchInfo.getTargets()) {
                    _hasIncomingJumps[getInstructionIndex(target)] = true;
                }
            }
        }

        for (final ExceptionHandler handler : _exceptionHandlers) {
            _hasIncomingJumps[getInstructionIndex(handler.getHandlerBlock().getFirstInstruction())] = true;
        }
    }

    private void createNodes() {
        //
        // Step 2a: Find basic blocks and create nodes for them.
        //

        final List<Instruction> instructions = _instructions;

        for (int i = 0, n = instructions.size(); i < n; i++) {
            final Instruction blockStart = instructions.get(i);
            final ExceptionHandler blockStartExceptionHandler = findInnermostExceptionHandler(blockStart.getOffset());

            //
            // See how big we can make that block...
            //
            for (; i + 1 < n; i++) {
                final Instruction instruction = instructions.get(i);
                final OpCode opCode = instruction.getOpCode();

                if (opCode.isUnconditionalBranch() /*|| opCode.canThrow()*/ || _hasIncomingJumps[i + 1]) {
                    break;
                }

                final Instruction next = instruction.getNext();

                if (next != null) {
                    //
                    // Ensure that blocks never contain instructions from different try blocks.
                    //
                    final ExceptionHandler innermostExceptionHandler = findInnermostExceptionHandler(next.getOffset());

                    if (innermostExceptionHandler != blockStartExceptionHandler) {
                        break;
                    }
                }
            }

            _nodes.add(new ControlFlowNode(_nodes.size(), blockStart, instructions.get(i)));
        }

        //
        // Step 2b: Create special nodes for exception handling constructs.
        //

        for (final ExceptionHandler handler : _exceptionHandlers) {
            ControlFlowNode endFinallyNode = null;
            final int index = _nodes.size();

            if (handler.getHandlerType() == ExceptionHandlerType.Finally) {
                endFinallyNode = new ControlFlowNode(
                    index,
                    handler.getHandlerBlock().getLastInstruction().getEndOffset(),
                    ControlFlowNodeType.EndFinally
                );
            }

            _nodes.add(new ControlFlowNode(index, handler, endFinallyNode));
        }
    }

    private void createRegularControlFlow() {
        //
        // Step 3: Create edges for the normal control flow (assuming no exceptions thrown).
        //

        final List<Instruction> instructions = _instructions;

        createEdge(_entryPoint, instructions.get(0), JumpType.Normal);

        for (final ControlFlowNode node : _nodes) {
            final Instruction end = node.getEnd();

            if (end == null || end.getOffset() >= _instructions.get(_instructions.size() - 1).getEndOffset()) {
                continue;
            }

            final OpCode endOpCode = end.getOpCode();

            //
            // Create normal edges from one instruction to the next.
            //
            if (!endOpCode.isUnconditionalBranch()) {
                createEdge(node, end.getNext(), JumpType.Normal);
            }

            //
            // Create edges for branch instructions.
            //
            for (Instruction instruction = node.getStart();
                 instruction != null && instruction.getOffset() <= end.getOffset();
                 instruction = instruction.getNext()) {

                final OpCode opCode = instruction.getOpCode();

                if (opCode.getOperandType() == OperandType.BranchTarget) {
                    final ControlFlowNode handlerBlock = findInnermostHandlerBlock(node.getEnd().getOffset());

                    if (handlerBlock.getNodeType() == ControlFlowNodeType.FinallyHandler) {
                        createEdge(node, instruction.<Instruction>getOperand(0), JumpType.LeaveTry);
                    }
                    else {
                        createEdge(node, instruction.<Instruction>getOperand(0), JumpType.Normal);
                    }
                }
                else if (opCode.getOperandType() == OperandType.Switch) {
                    final SwitchInfo switchInfo = instruction.getOperand(0);

                    createEdge(node, switchInfo.getDefaultTarget(), JumpType.Normal);

                    for (final Instruction target : switchInfo.getTargets()) {
                        createEdge(node, target, JumpType.Normal);
                    }
                }
            }

            //
            // Create edges for return instructions.
            //
            if (endOpCode.getFlowControl() == FlowControl.Return) {
                createEdge(node, _regularExit, JumpType.Normal);
            }
        }
    }

    private void createExceptionalControlFlow() {
        //
        // Step 4: Create edges for the exceptional control flow.
        //

        for (final ControlFlowNode node : _nodes) {
            final Instruction end = node.getEnd();

            if (end != null &&
                end.getOffset() < _instructions.get(_instructions.size() - 1).getEndOffset() &&
                end.getOpCode().canThrow()) {

                final ControlFlowNode innermostHandler = findInnermostExceptionHandlerNode(node.getEnd().getOffset());

                if (innermostHandler == _exceptionalExit) {
                    createEdge(node, innermostHandler, JumpType.JumpToExceptionHandler);
                }
                else {
                    for (final ExceptionHandler handler : _exceptionHandlers) {
                        if (Comparer.equals(handler.getTryBlock(), innermostHandler.getExceptionHandler().getTryBlock())) {
                            final ControlFlowNode handlerNode = firstOrDefault(
                                _nodes,
                                new Predicate<ControlFlowNode>() {
                                    @Override
                                    public boolean test(final ControlFlowNode node) {
                                        return node.getExceptionHandler() == handler;
                                    }
                                }
                            );

                            createEdge(node, handlerNode, JumpType.JumpToExceptionHandler);
                        }
                    }
                }
            }

            final ExceptionHandler exceptionHandler = node.getExceptionHandler();

            if (exceptionHandler != null) {
                final ControlFlowNode endFinallyNode = node.getEndFinallyNode();

                if (endFinallyNode != null) {
                    createEdge(
                        endFinallyNode,
                        findParentExceptionHandlerNode(node),
                        JumpType.JumpToExceptionHandler
                    );
                }
                else {
                    final ControlFlowNode parentHandler = findParentExceptionHandlerNode(node);

                    createEdge(node, parentHandler, JumpType.JumpToExceptionHandler);

                    if (parentHandler.getNodeType() != ControlFlowNodeType.ExceptionalExit) {
                        for (final ExceptionHandler handler : _exceptionHandlers) {
                            if (Comparer.equals(handler.getTryBlock(), parentHandler.getExceptionHandler().getTryBlock())) {
                                final ControlFlowNode handlerNode = firstOrDefault(
                                    _nodes,
                                    new Predicate<ControlFlowNode>() {
                                        @Override
                                        public boolean test(final ControlFlowNode node) {
                                            return node.getExceptionHandler() == handler;
                                        }
                                    }
                                );

                                if (handlerNode != node && handlerNode != parentHandler) {
                                    createEdge(node, handlerNode, JumpType.JumpToExceptionHandler);
                                }
                            }
                        }
                    }
                }

                createEdge(
                    node,
                    exceptionHandler.getHandlerBlock().getFirstInstruction(),
                    JumpType.Normal
                );
            }
        }
    }

    private void transformLeaveEdges() {
        //
        // Step 5: Replace LeaveTry edges with EndFinally edges.
        //

        for (int n = _nodes.size(), i = n - 1; i >= 0; i--) {
            final ControlFlowNode node = _nodes.get(i);
            final Instruction end = node.getEnd();

            if (end != null &&
                node.getOutgoing().size() == 1 &&
                node.getOutgoing().get(0).getType() == JumpType.LeaveTry) {

                assert end.getOpCode() == OpCode.GOTO ||
                       end.getOpCode() == OpCode.GOTO_W;

                final ControlFlowNode handler = findInnermostFinallyHandlerNode(end.getOffset());

                final ControlFlowEdge edge = node.getOutgoing().get(0);
                final ControlFlowNode target = edge.getTarget();

                target.getIncoming().remove(edge);
                node.getOutgoing().clear();

                if (handler.getNodeType() == ControlFlowNodeType.ExceptionalExit) {
                    createEdge(node, handler, JumpType.Normal);
                    continue;
                }

                assert handler.getNodeType() == ControlFlowNodeType.FinallyHandler;

                createEdge(node, handler, JumpType.Normal);
                createEdge(handler.getEndFinallyNode(), target, JumpType.EndFinally);
            }
        }
    }

    private void copyFinallyBlocksIntoLeaveEdges() {
        //
        // Step 5b: Copy finally blocks into the LeaveTry edges.
        //

        for (int n = _nodes.size(), i = n - 1; i >= 0; i--) {
            final ControlFlowNode node = _nodes.get(i);
            final Instruction end = node.getEnd();

            if (end != null &&
                node.getOutgoing().size() == 1 &&
                node.getOutgoing().get(0).getType() == JumpType.LeaveTry) {

                assert end.getOpCode() == OpCode.GOTO ||
                       end.getOpCode() == OpCode.GOTO_W;

                final ControlFlowEdge edge = node.getOutgoing().get(0);
                final ControlFlowNode target = edge.getTarget();

                target.getIncoming().remove(edge);
                node.getOutgoing().clear();

                final ControlFlowNode handler = findInnermostExceptionHandlerNode(end.getEndOffset());

                assert handler.getNodeType() == ControlFlowNodeType.FinallyHandler;

                final ControlFlowNode copy = copyFinallySubGraph(handler, handler.getEndFinallyNode(), target);

                createEdge(node, copy, JumpType.Normal);
            }
        }
    }

    private ControlFlowNode copyFinallySubGraph(final ControlFlowNode start, final ControlFlowNode end, final ControlFlowNode newEnd) {
        return new CopyFinallySubGraphLogic(start, end, newEnd).copyFinallySubGraph();
    }

    private ControlFlowNode findParentExceptionHandlerNode(final ControlFlowNode node) {
        assert node.getNodeType() == ControlFlowNodeType.CatchHandler ||
               node.getNodeType() == ControlFlowNodeType.FinallyHandler;

        final int offset = node.getExceptionHandler().getHandlerBlock().getFirstInstruction().getOffset();

        for (int i = node.getBlockIndex() + 1, n = _nodes.size(); i < n; i++) {
            final ControlFlowNode currentNode = _nodes.get(i);
            final ExceptionHandler handler = currentNode.getExceptionHandler();

            if (handler != null &&
                handler.getTryBlock().getFirstInstruction().getOffset() <= offset &&
                offset < handler.getTryBlock().getLastInstruction().getEndOffset()) {

                return currentNode;
            }
        }

        return _exceptionalExit;
    }

    private ControlFlowNode findInnermostExceptionHandlerNode(final int offset) {
        final ExceptionHandler handler = findInnermostExceptionHandler(offset);

        if (handler == null) {
            return _exceptionalExit;
        }

        for (final ControlFlowNode node : _nodes) {
            if (node.getExceptionHandler() == handler && node.getCopyFrom() == null) {
                return node;
            }
        }

        throw new IllegalStateException("Could not find node for exception handler!");
    }

    private ControlFlowNode findInnermostFinallyHandlerNode(final int offset) {
        final ExceptionHandler handler = findInnermostFinallyHandler(offset);

        if (handler == null) {
            return _exceptionalExit;
        }

        for (final ControlFlowNode node : _nodes) {
            if (node.getExceptionHandler() == handler && node.getCopyFrom() == null) {
                return node;
            }
        }

        throw new IllegalStateException("Could not find node for exception handler!");
    }

    private int getInstructionIndex(final Instruction instruction) {
        final int index = Arrays.binarySearch(_offsets, instruction.getOffset());
        assert index >= 0;
        return index;
    }

    private ExceptionHandler findInnermostExceptionHandler(final int offsetInTryBlock) {
        for (final ExceptionHandler handler : _exceptionHandlers) {
            final ExceptionBlock tryBlock = handler.getTryBlock();

            if (tryBlock.getFirstInstruction().getOffset() <= offsetInTryBlock &&
                offsetInTryBlock < tryBlock.getLastInstruction().getEndOffset()) {

                return handler;
            }
        }

        return null;
    }

    private ExceptionHandler findInnermostFinallyHandler(final int offsetInTryBlock) {
        for (final ExceptionHandler handler : _exceptionHandlers) {
            if (!handler.isFinally()) {
                continue;
            }

            final ExceptionBlock tryBlock = handler.getTryBlock();

            if (tryBlock.getFirstInstruction().getOffset() <= offsetInTryBlock &&
                offsetInTryBlock < tryBlock.getLastInstruction().getEndOffset()) {

                return handler;
            }
        }

        return null;
    }

    private ControlFlowNode findInnermostHandlerBlock(final int instructionOffset) {
        for (final ExceptionHandler handler : _exceptionHandlers) {
            final ExceptionBlock tryBlock = handler.getTryBlock();
            final ExceptionBlock handlerBlock = handler.getHandlerBlock();

            if ((tryBlock.getFirstInstruction().getOffset() <= instructionOffset &&
                 instructionOffset <= tryBlock.getLastInstruction().getEndOffset()) ||
                (handlerBlock.getFirstInstruction().getOffset() <= instructionOffset &&
                 instructionOffset <= handlerBlock.getLastInstruction().getEndOffset())) {

                for (final ControlFlowNode node : _nodes) {
                    if (node.getExceptionHandler() == handler && node.getCopyFrom() == null) {
                        return node;
                    }
                }

                throw new IllegalStateException("Could not find innermost handler block!");
            }
        }

        return _exceptionalExit;
    }

    private ControlFlowEdge createEdge(final ControlFlowNode fromNode, final Instruction toInstruction, final JumpType type) {
        ControlFlowNode target = null;

        for (final ControlFlowNode node : _nodes) {
            if (node.getStart() != null && node.getStart().getOffset() == toInstruction.getOffset()) {
                if (target != null) {
                    throw new IllegalStateException("Multiple edge targets detected!");
                }
                target = node;
            }
        }

        if (target != null) {
            return createEdge(fromNode, target, type);
        }

        throw new IllegalStateException("Could not find target node!");
    }

    private ControlFlowEdge createEdge(final ControlFlowNode fromNode, final ControlFlowNode toNode, final JumpType type) {
        final ControlFlowEdge edge = new ControlFlowEdge(fromNode, toNode, type);

        fromNode.getOutgoing().add(edge);
        toNode.getIncoming().add(edge);

        return edge;
    }

    private static List<ExceptionHandler> coalesceExceptionHandlers(final List<ExceptionHandler> handlers) {
        final ArrayList<ExceptionHandler> copy = new ArrayList<>(handlers);

        for (int i = 0; i < copy.size(); i++) {
            final ExceptionHandler handler = copy.get(i);

            if (!handler.isCatch()) {
                continue;
            }

            final ExceptionBlock tryBlock = handler.getTryBlock();
            final ExceptionBlock handlerBlock = handler.getHandlerBlock();

            for (int j = i + 1; j < copy.size(); j++) {
                final ExceptionHandler other = copy.get(j);

                if (!other.isCatch()) {
                    continue;
                }

                final ExceptionBlock otherTry = other.getTryBlock();
                final ExceptionBlock otherHandler = other.getHandlerBlock();

                if (otherTry.getFirstInstruction().getOffset() == tryBlock.getFirstInstruction().getOffset() &&
                    otherTry.getLastInstruction().getOffset() == tryBlock.getLastInstruction().getOffset() &&
                    otherHandler.getFirstInstruction().getOffset() == handlerBlock.getFirstInstruction().getOffset() &&
                    otherHandler.getLastInstruction().getOffset() == handlerBlock.getLastInstruction().getOffset()) {

                    copy.set(
                        i,
                        ExceptionHandler.createCatch(
                            tryBlock,
                            handlerBlock,
                            MetadataHelper.findCommonSuperType(handler.getCatchType(), other.getCatchType())
                        )
                    );

                    copy.remove(j--);
                }
            }
        }

        return copy;
    }

    private final class CopyFinallySubGraphLogic {
        final Map<ControlFlowNode, ControlFlowNode> oldToNew = new IdentityHashMap<>();
        final ControlFlowNode start;
        final ControlFlowNode end;
        final ControlFlowNode newEnd;

        CopyFinallySubGraphLogic(final ControlFlowNode start, final ControlFlowNode end, final ControlFlowNode newEnd) {
            this.start = start;
            this.end = end;
            this.newEnd = newEnd;
        }

        final ControlFlowNode copyFinallySubGraph() {
            for (final ControlFlowNode node : end.getPredecessors()) {
                collectNodes(node);
            }

            for (final ControlFlowNode old : oldToNew.keySet()) {
                reconstructEdges(old, oldToNew.get(old));
            }

            return getNew(start);
        }

        private void collectNodes(final ControlFlowNode node) {
            if (node == end || node == newEnd) {
                throw new IllegalStateException("Unexpected cycle involing finally constructs!");
            }

            if (oldToNew.containsKey(node)) {
                return;
            }

            final int newBlockIndex = _nodes.size();
            final ControlFlowNode copy;

            switch (node.getNodeType()) {
                case Normal:
                    copy = new ControlFlowNode(newBlockIndex, node.getStart(), node.getEnd());
                    break;

                case FinallyHandler:
                    copy = new ControlFlowNode(newBlockIndex, node.getExceptionHandler(), node.getEndFinallyNode());
                    break;

                default:
                    throw ContractUtils.unsupported();
            }

            copy.setCopyFrom(node);
            _nodes.add(copy);
            oldToNew.put(node, copy);

            if (node != start) {
                for (final ControlFlowNode predecessor : node.getPredecessors()) {
                    collectNodes(predecessor);
                }
            }
        }

        private void reconstructEdges(final ControlFlowNode oldNode, final ControlFlowNode newNode) {
            for (final ControlFlowEdge oldEdge : oldNode.getOutgoing()) {
                createEdge(newNode, getNew(oldEdge.getTarget()), oldEdge.getType());
            }
        }

        private ControlFlowNode getNew(final ControlFlowNode oldNode) {
            if (oldNode == end) {
                return newEnd;
            }

            final ControlFlowNode newNode = oldToNew.get(oldNode);

            return newNode != null ? newNode : oldNode;
        }
    }
}
