package com.strobel.assembler.metadata;

import com.strobel.annotations.NotNull;
import com.strobel.assembler.Collection;
import com.strobel.assembler.flowanalysis.ControlFlowEdge;
import com.strobel.assembler.flowanalysis.ControlFlowGraph;
import com.strobel.assembler.flowanalysis.ControlFlowNode;
import com.strobel.assembler.flowanalysis.ControlFlowNodeType;
import com.strobel.assembler.flowanalysis.JumpType;
import com.strobel.assembler.ir.ExceptionBlock;
import com.strobel.assembler.ir.ExceptionHandler;
import com.strobel.assembler.ir.FlowControl;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.InstructionCollection;
import com.strobel.assembler.ir.OpCode;
import com.strobel.assembler.ir.OperandType;
import com.strobel.assembler.ir.attributes.ExceptionTableEntry;
import com.strobel.core.Predicate;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.InstructionHelper;

import java.util.*;

import static com.strobel.core.CollectionUtilities.*;
import static java.lang.String.format;

@SuppressWarnings("ConstantConditions")
public final class ExceptionHandlerMapper {
    public static List<ExceptionHandler> run(final InstructionCollection instructions, final List<ExceptionTableEntry> tableEntries) {
        VerifyArgument.notNull(instructions, "instructions");
        VerifyArgument.notNull(tableEntries, "tableEntries");

        final ExceptionHandlerMapper builder = new ExceptionHandlerMapper(instructions, tableEntries);
        final ControlFlowGraph cfg = builder.build();

        final List<ExceptionHandler> handlers = new ArrayList<>();
        final Map<ExceptionTableEntry, ControlFlowNode> handlerStartNodes = new IdentityHashMap<>();

        for (final ExceptionTableEntry entry : builder._tableEntries) {
            final Instruction handlerStart = instructions.atOffset(entry.getHandlerOffset());
            final ControlFlowNode handlerStartNode = builder.findNode(handlerStart);

            if (handlerStartNode == null) {
                throw new IllegalStateException(
                    format(
                        "Could not find entry node for handler at offset %d.",
                        handlerStart.getOffset()
                    )
                );
            }

            if (handlerStartNode.getIncoming().isEmpty()) {
                builder.createEdge(cfg.getEntryPoint(), handlerStartNode, JumpType.Normal);
            }

            handlerStartNodes.put(entry, handlerStartNode);
        }

        cfg.computeDominance();
        cfg.computeDominanceFrontier();

        for (final ExceptionTableEntry entry : builder._tableEntries) {
            final ControlFlowNode handlerStart = handlerStartNodes.get(entry);
            final List<ControlFlowNode> dominatedNodes = new ArrayList<>();

            dominatedNodes.addAll(findDominatedNodes(cfg, handlerStart));

            Collections.sort(
                dominatedNodes,
                new Comparator<ControlFlowNode>() {
                    @Override
                    public int compare(@NotNull final ControlFlowNode o1, @NotNull final ControlFlowNode o2) {
                        return Integer.compare(o1.getBlockIndex(), o2.getBlockIndex());
                    }
                }
            );

            final Instruction lastInstruction = instructions.get(instructions.size() - 1);

            final ExceptionBlock tryBlock;

            if (entry.getEndOffset() == lastInstruction.getEndOffset()) {
                tryBlock = new ExceptionBlock(
                    instructions.atOffset(entry.getStartOffset()),
                    lastInstruction
                );
            }
            else {
                tryBlock = new ExceptionBlock(
                    instructions.atOffset(entry.getStartOffset()),
                    instructions.atOffset(entry.getEndOffset()).getPrevious()
                );
            }

            if (entry.getCatchType() == null) {
                handlers.add(
                    ExceptionHandler.createFinally(
                        tryBlock,
                        new ExceptionBlock(handlerStart.getStart(), lastOrDefault(dominatedNodes).getEnd())
                    )
                );
            }
            else {
                handlers.add(
                    ExceptionHandler.createCatch(
                        tryBlock,
                        new ExceptionBlock(handlerStart.getStart(), lastOrDefault(dominatedNodes).getEnd()),
                        entry.getCatchType()
                    )
                );
            }
        }

//        Collections.sort(handlers);
//        ControlFlowGraphBuilder.build(instructions, handlers).export(new File("w:/dump/try"));

        return handlers;
    }

    private ControlFlowNode findNode(final Instruction instruction) {
        if (instruction == null) {
            return null;
        }

        return firstOrDefault(
            _nodes,
            new Predicate<ControlFlowNode>() {
                @Override
                public boolean test(final ControlFlowNode node) {
                    return node.getNodeType() == ControlFlowNodeType.Normal &&
                           instruction.getOffset() >= node.getStart().getOffset() &&
                           instruction.getOffset() < node.getEnd().getEndOffset();
                }
            }
        );
    }

    private static Set<ControlFlowNode> findDominatedNodes(final ControlFlowGraph cfg, final ControlFlowNode head) {
        final Set<ControlFlowNode> agenda = new LinkedHashSet<>();
        final Set<ControlFlowNode> result = new LinkedHashSet<>();

        agenda.add(head);

        while (!agenda.isEmpty()) {
            final ControlFlowNode addNode = agenda.iterator().next();

            agenda.remove(addNode);

            if (addNode.getNodeType() != ControlFlowNodeType.Normal) {
                continue;
            }

            if (!head.dominates(addNode) &&
                !shouldIncludeExceptionalExit(cfg, head, addNode)) {

                continue;
            }

            if (!result.add(addNode)) {
                continue;
            }

            for (final ControlFlowNode successor : addNode.getSuccessors()) {
                agenda.add(successor);
            }
        }

        return result;
    }

    private static boolean shouldIncludeExceptionalExit(final ControlFlowGraph cfg, final ControlFlowNode head, final ControlFlowNode node) {
        if (!node.getDominanceFrontier().contains(cfg.getExceptionalExit())) {
            return false;
        }

        if (node.getStart().getNext() != node.getEnd()) {
            return false;
        }

        if (head.getStart().getOpCode().isStore() &&
            node.getStart().getOpCode().isLoad() &&
            node.getEnd().getOpCode() == OpCode.ATHROW) {

            return InstructionHelper.getLoadOrStoreSlot(head.getStart()) ==
                   InstructionHelper.getLoadOrStoreSlot(node.getStart());
        }

        return false;
    }

    private final InstructionCollection _instructions;
    private final List<ExceptionTableEntry> _tableEntries;
    private final List<ControlFlowNode> _nodes = new Collection<>();
    private final int[] _offsets;
    private final boolean[] _hasIncomingJumps;
    private final ControlFlowNode _entryPoint;
    private final ControlFlowNode _regularExit;
    private final ControlFlowNode _exceptionalExit;

    private int _nextBlockId;
    boolean copyFinallyBlocks = false;

    private ExceptionHandlerMapper(final InstructionCollection instructions, final List<ExceptionTableEntry> tableEntries) {
        _instructions = VerifyArgument.notNull(instructions, "instructions");
        _tableEntries = VerifyArgument.notNull(tableEntries, "tableEntries");

        _offsets = new int[instructions.size()];
        _hasIncomingJumps = new boolean[instructions.size()];

        for (int i = 0; i < instructions.size(); i++) {
            _offsets[i] = instructions.get(i).getOffset();
        }

        _entryPoint = new ControlFlowNode(_nextBlockId++, 0, ControlFlowNodeType.EntryPoint);
        _regularExit = new ControlFlowNode(_nextBlockId++, -1, ControlFlowNodeType.RegularExit);
        _exceptionalExit = new ControlFlowNode(_nextBlockId++, -2, ControlFlowNodeType.ExceptionalExit);

        _nodes.add(_entryPoint);
        _nodes.add(_regularExit);
        _nodes.add(_exceptionalExit);
    }

    private ControlFlowGraph build() {
        calculateIncomingJumps();
        createNodes();
        createRegularControlFlow();
//        createExceptionalControlFlow();

        return new ControlFlowGraph(_nodes.toArray(new ControlFlowNode[_nodes.size()]));
    }

    private boolean isHandlerStart(final Instruction instruction) {
        for (final ExceptionTableEntry entry : _tableEntries) {
            if (entry.getHandlerOffset() == instruction.getOffset()) {
                return true;
            }
        }
        return false;
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

        for (final ExceptionTableEntry entry : _tableEntries) {
            _hasIncomingJumps[getInstructionIndex(_instructions.atOffset(entry.getHandlerOffset()))] = true;
        }
    }

    private void createNodes() {
        //
        // Step 2a: Find basic blocks and create nodes for them.
        //

        final InstructionCollection instructions = _instructions;

        for (int i = 0, n = instructions.size(); i < n; i++) {
            final Instruction blockStart = instructions.get(i);
            final ExceptionTableEntry blockStartExceptionHandler = findInnermostExceptionHandler(blockStart.getOffset());

            //
            // See how big we can make that block...
            //
            for (; i + 1 < n; i++) {
                final Instruction instruction = instructions.get(i);
                final OpCode opCode = instruction.getOpCode();

                if (opCode.isBranch() /*|| opCode.canThrow()*/ || _hasIncomingJumps[i + 1]) {
                    break;
                }

                final Instruction next = instruction.getNext();

                if (next != null) {
                    //
                    // Ensure that blocks never contain instructions from different try blocks.
                    //
                    final ExceptionTableEntry innermostExceptionHandler = findInnermostExceptionHandler(next.getOffset());

                    if (innermostExceptionHandler != blockStartExceptionHandler) {
                        break;
                    }
                }
            }

            final ControlFlowNode node = new ControlFlowNode(_nodes.size(), blockStart, instructions.get(i));

            node.setUserData(blockStartExceptionHandler);

            _nodes.add(node);
        }
    }

    private void createRegularControlFlow() {
        //
        // Step 3: Create edges for the normal control flow (assuming no exceptions thrown).
        //

        final InstructionCollection instructions = _instructions;

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
                final Instruction next = end.getNext();

                if (next != null && !isHandlerStart(next)) {
                    createEdge(node, next, JumpType.Normal);
                }
            }

            //
            // Create edges for branch instructions.
            //
            for (Instruction instruction = node.getStart();
                 instruction != null && instruction.getOffset() <= end.getOffset();
                 instruction = instruction.getNext()) {

                final OpCode opCode = instruction.getOpCode();

                if (opCode.getOperandType() == OperandType.BranchTarget) {
                    createEdge(node, instruction.<Instruction>getOperand(0), JumpType.Normal);
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
            else if (endOpCode.getFlowControl() == FlowControl.Throw) {
                createEdge(node, _exceptionalExit, JumpType.JumpToExceptionHandler);
            }
        }
    }

//    private void createExceptionalControlFlow() {
//        //
//        // Step 4: Create edges for the exceptional control flow.
//        //
//
//        for (final ControlFlowNode node : _nodes) {
//            if (node.getNodeType() != ControlFlowNodeType.Normal) {
//                continue;
//            }
//
//            final Instruction end = node.getEnd();
//            final ExceptionTableEntry entry = (ExceptionTableEntry) node.getUserData();
//
//            if (entry != null && end.getEndOffset() == entry.getEndOffset()) {
//                final ControlFlowNode innermostHandler = findInnermostExceptionHandlerNode(node.getEnd().getOffset());
//
//                if (innermostHandler != null) {
//                    for (final ExceptionTableEntry handler : _tableEntries) {
//                        if (handler.getStartOffset() == entry.getStartOffset() &&
//                            handler.getEndOffset() == entry.getEndOffset()) {
//
//                            final ControlFlowNode handlerNode = firstOrDefault(
//                                _nodes,
//                                new Predicate<ControlFlowNode>() {
//                                    @Override
//                                    public boolean test(final ControlFlowNode node) {
//                                        return node.getNodeType() == ControlFlowNodeType.Normal &&
//                                               node.getStart().getOffset() == handler.getHandlerOffset();
//                                    }
//                                }
//                            );
//
//                            if (node != handlerNode) {
//                                createEdge(node, handlerNode, JumpType.JumpToExceptionHandler);
//                            }
//                        }
//                    }
//                }
//            }
//
///*
//            if (entry != null) {
//                createEdge(
//                    node,
//                    _instructions.atOffset(entry.getHandlerOffset()),
//                    JumpType.Normal
//                );
//            }
//*/
//        }
//    }

//    private ControlFlowNode findInnermostExceptionHandlerNode(final int offsetInTryBlock) {
//        final ExceptionTableEntry entry = findInnermostExceptionHandler(offsetInTryBlock);
//
//        if (entry == null) {
//            return null;
//        }
//
//        final Instruction nodeStart = _instructions.atOffset(entry.getHandlerOffset());
//
//        for (final ControlFlowNode node : _nodes) {
//            if (node.getStart() == nodeStart) {
//                return node;
//            }
//        }
//
//        return null;
//    }

    private ExceptionTableEntry findInnermostExceptionHandler(final int offsetInTryBlock) {
        for (final ExceptionTableEntry entry : _tableEntries) {
            if (entry.getStartOffset() <= offsetInTryBlock &&
                offsetInTryBlock < entry.getEndOffset()) {

                return entry;
            }
        }

        return null;
    }

    private int getInstructionIndex(final Instruction instruction) {
        final int index = Arrays.binarySearch(_offsets, instruction.getOffset());
        assert index >= 0;
        return index;
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
}

