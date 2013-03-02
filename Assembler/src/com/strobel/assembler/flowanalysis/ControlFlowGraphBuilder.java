/*
 * ControlFlowGraphBuilder.java
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

package com.strobel.assembler.flowanalysis;

import com.strobel.assembler.Collection;
import com.strobel.assembler.ir.ExceptionBlock;
import com.strobel.assembler.ir.ExceptionHandler;
import com.strobel.assembler.ir.ExceptionHandlerType;
import com.strobel.assembler.ir.FlowControl;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.MethodBody;
import com.strobel.assembler.ir.OpCode;
import com.strobel.assembler.ir.OperandType;
import com.strobel.assembler.metadata.SwitchInfo;
import com.strobel.core.VerifyArgument;

import java.util.Arrays;
import java.util.List;

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

    private ControlFlowGraphBuilder(final List<Instruction> instructions, final List<ExceptionHandler> exceptionHandlers) {
        _instructions = VerifyArgument.notNull(instructions, "instructions");
        _exceptionHandlers = VerifyArgument.notNull(exceptionHandlers, "exceptionHandlers");

        _offsets = new int[instructions.size()];
        _hasIncomingJumps = new boolean[_offsets.length];

        for (int i = 0; i < instructions.size(); i++) {
            _offsets[i] = instructions.get(i).getOffset();
        }

        _entryPoint = new ControlFlowNode(0, 0, ControlFlowNodeType.EntryPoint);
        _regularExit = new ControlFlowNode(1, -1, ControlFlowNodeType.RegularExit);
        _exceptionalExit = new ControlFlowNode(2, -1, ControlFlowNodeType.ExceptionalExit);

        _nodes.add(_entryPoint);
        _nodes.add(_regularExit);
        _nodes.add(_exceptionalExit);
    }

    public final ControlFlowGraph build() {
        calculateIncomingJumps();
        createNodes();
        createRegularControlFlow();
        createExceptionalControlFlow();
        transformLeaveEdges();

        return new ControlFlowGraph(_nodes.toArray(new ControlFlowNode[_nodes.size()]));
    }

    private void calculateIncomingJumps() {
        //
        // Step 1: Determine which instructions are jump targets.
        //

        for (final Instruction instruction : _instructions) {
            final OpCode opCode = instruction.getOpCode();

            if (opCode.getOperandType() == OperandType.BranchTarget) {
                _hasIncomingJumps[getInstructionIndex(instruction)] = true;
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

                if (opCode.isBranch() || opCode.canThrow() || _hasIncomingJumps[i + 1]) {
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

            if (handler.getHandlerType() == ExceptionHandlerType.Finally) {
                endFinallyNode = new ControlFlowNode(
                    _nodes.size(),
                    handler.getHandlerBlock().getLastInstruction().getEndOffset(),
                    ControlFlowNodeType.EndFinally
                );
            }

            _nodes.add(new ControlFlowNode(_nodes.size(), handler, endFinallyNode));
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

            if (end == null) {
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
            if (endOpCode.getOperandType() == OperandType.BranchTarget) {
                // TODO: If leaving try/handler block ... (missing) ... else:
                createEdge(node, end.<Instruction>getOperand(0), JumpType.Normal);
            }
            else if (endOpCode.getOperandType() == OperandType.Switch) {
                final SwitchInfo switchInfo = end.getOperand(0);

                createEdge(node, switchInfo.getDefaultTarget(), JumpType.Normal);

                for (final Instruction target : switchInfo.getTargets()) {
                    createEdge(node, target, JumpType.Normal);
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

            if (end != null && end.getOpCode().canThrow()) {
                createEdge(
                    node,
                    findInnermostExceptionHandlerNode(node.getEnd().getEndOffset()),
                    JumpType.JumpToExceptionHandler
                );
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
                    createEdge(
                        node,
                        findParentExceptionHandlerNode(node),
                        JumpType.JumpToExceptionHandler
                    );
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

                final ControlFlowEdge edge = node.getOutgoing().get(0);
                final ControlFlowNode target = edge.getTarget();

                target.getIncoming().remove(edge);
                node.getOutgoing().clear();

                final ControlFlowNode handler = findInnermostExceptionHandlerNode(end.getEndOffset());

                assert handler.getNodeType() == ControlFlowNodeType.FinallyHandler;

                createEdge(node, handler, JumpType.Normal);
                createEdge(handler.getEndFinallyNode(), target, JumpType.EndFinally);
            }
        }
    }

    private ControlFlowNode findParentExceptionHandlerNode(final ControlFlowNode node) {
        assert node.getNodeType() == ControlFlowNodeType.CatchHandler ||
               node.getNodeType() == ControlFlowNodeType.FinallyHandler;

        final int offset = node.getExceptionHandler().getTryBlock().getFirstInstruction().getOffset();

        for (int i = node.getBlockIndex(), n = _nodes.size(); i < n; i++) {
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

    private ControlFlowEdge createEdge(final ControlFlowNode fromNode, final Instruction toInstruction, final JumpType type) {
        for (final ControlFlowNode node : _nodes) {
            if (node.getStart() == toInstruction) {
                return createEdge(fromNode, node, type);
            }
        }
        throw new IllegalStateException("Could not find target node!");
    }

    private ControlFlowEdge createEdge(final ControlFlowNode fromNode, final ControlFlowNode toNode, final JumpType type) {
        final ControlFlowEdge edge = new ControlFlowEdge(fromNode, toNode, type);

        fromNode.getOutgoing().add(edge);
        toNode.getIncoming().add(edge);

        return edge;
    }
}
