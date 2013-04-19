/*
 * StackMapAnalyzer.java
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

package com.strobel.assembler.ir;

import com.strobel.assembler.flowanalysis.ControlFlowGraph;
import com.strobel.assembler.flowanalysis.ControlFlowGraphBuilder;
import com.strobel.assembler.flowanalysis.ControlFlowNode;
import com.strobel.assembler.flowanalysis.ControlFlowNodeType;
import com.strobel.assembler.metadata.IMethodSignature;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.MethodBody;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.VariableDefinition;
import com.strobel.assembler.metadata.VariableDefinitionCollection;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.CollectionUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.util.EmptyArrayCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class StackMapAnalyzer {
    public static List<StackMapFrame> computeStackMapTable(final MethodBody body, final IMethodSignature signature) {
        VerifyArgument.notNull(body, "body");
        VerifyArgument.notNull(signature, "signature");

        final ControlFlowGraph cfg = ControlFlowGraphBuilder.build(body);

        cfg.computeDominance();

        final StackMappingVisitor stackMappingVisitor = new StackMappingVisitor();
        final InstructionVisitor instructionVisitor = stackMappingVisitor.visitBody(body);

        final ParameterDefinition thisParameter = body.getThisParameter();
        final boolean hasThis = thisParameter != null;

        if (hasThis) {
            stackMappingVisitor.set(0, thisParameter.getParameterType());
        }

        for (final ParameterDefinition parameter : signature.getParameters()) {
            stackMappingVisitor.set(parameter.getSlot(), parameter.getParameterType());
        }

        final List<ControlFlowNode> nodes = new ArrayList<>();

        populateNodes(cfg.getRegularExit(), nodes);
        populateNodes(cfg.getExceptionalExit(), nodes);
//        populateNodes(cfg.getEntryPoint(), nodes);

        Collections.sort(
            nodes,
            new Comparator<ControlFlowNode>() {
                @Override
                public int compare(final ControlFlowNode o1, final ControlFlowNode o2) {
                    return Integer.compare(o1.getBlockIndex(), o2.getBlockIndex());
                }
            }
        );
/*
        Collections.sort(
            nodes,
            new Comparator<ControlFlowNode>() {
                @Override
                public int compare(final ControlFlowNode o1, final ControlFlowNode o2) {
                    Instruction start1 = o1.getStart();
                    Instruction start2 = o2.getStart();

                    if (start1 == null) {
                        switch (o1.getNodeType()) {
                            case EntryPoint:
                                return -1;
                            case RegularExit:
                                return 1;
                            case ExceptionalExit:
                                return 1;
                            case CatchHandler:
                            case FinallyHandler:
                                start1 = o1.getExceptionHandler().getHandlerBlock().getFirstInstruction();
                                break;
                            case EndFinally:
                                start1 = o1.getEndFinallyNode().getStart();
                                break;
                        }
                    }

                    if (start2 == null) {
                        switch (o2.getNodeType()) {
                            case EntryPoint:
                                return 1;
                            case RegularExit:
                                return -1;
                            case ExceptionalExit:
                                return -1;
                            case CatchHandler:
                            case FinallyHandler:
                                start2 = o2.getExceptionHandler().getHandlerBlock().getFirstInstruction();
                                break;
                            case EndFinally:
                                start2 = o2.getEndFinallyNode().getStart();
                                break;
                        }
                    }

                    assert start1 != null && start2 != null;

                    return Integer.compare(start1.getOffset(), start2.getOffset());
                }
            }
        );
*/

        final Frame[] entryFrames = new Frame[nodes.size()];
        final Frame[] exitFrames = new Frame[nodes.size()];

        final Frame initialFrame = stackMappingVisitor.buildFrame();

        entryFrames[0] = initialFrame;
        exitFrames[0] = initialFrame;

        int nodeCount = 0;

        for (final ControlFlowNode node : nodes) {
            if (node.getNodeType() == ControlFlowNodeType.Normal && node.getOffset() != 0) {
                ++nodeCount;
            }
            computeFrames(body, stackMappingVisitor, instructionVisitor, node, entryFrames, exitFrames);
        }

        final StackMapFrame[] computedFrames = new StackMapFrame[nodeCount];

        int i = 0;

        for (final ControlFlowNode node : nodes) {
            if (node.getNodeType() == ControlFlowNodeType.Normal &&
                node.getOffset() != 0) {

                final ControlFlowNode predecessor = findPredecessor(node);

                final Frame currentEntry = entryFrames[node.getBlockIndex()];
                final Frame previousEntry = entryFrames[predecessor.getBlockIndex()];
                final Frame previousExit = exitFrames[predecessor.getBlockIndex()];

                computedFrames[i++] = new StackMapFrame(
                    Frame.merge(
                        previousEntry,
                        previousExit,
                        currentEntry,
                        stackMappingVisitor.getInitializations()
                    ),
                    node.getStart()
                );
            }
        }

        return ArrayUtilities.asUnmodifiableList(computedFrames);
    }

    private static void populateNodes(final ControlFlowNode node, final List<ControlFlowNode> nodes) {
        if (nodes.contains(node)) {
            return;
        }

        final ControlFlowNode immediateDominator = node.getImmediateDominator();

        if (immediateDominator != null) {
            populateNodes(immediateDominator, nodes);
        }

        nodes.add(node);

        for (final ControlFlowNode n : node.getPredecessors()) {
            populateNodes(n, nodes);
        }
/*
        nodes.add(node);

        for (final ControlFlowNode successor : node.getSuccessors()) {
            populateNodes(successor, nodes);
        }
*/
    }

    private static void computeFrames(
        final MethodBody body,
        final StackMappingVisitor smv,
        final InstructionVisitor visitor,
        final ControlFlowNode node,
        final Frame[] entryFrames,
        final Frame[] exitFrames) {

        final int index = node.getBlockIndex();

        if (exitFrames[index] != null) {
            return;
        }

        Frame entryFrame = entryFrames[index];

        if (entryFrame == null) {
            final Frame predecessorExit;
            final ControlFlowNode predecessor = findPredecessor(node);

            if (predecessor == null) {
                predecessorExit = exitFrames[0];
            }
            else {
                computeFrames(body, smv, visitor, predecessor, entryFrames, exitFrames);
                predecessorExit = exitFrames[predecessor.getBlockIndex()];
            }

            entryFrame = predecessorExit;
        }

        switch (node.getNodeType()) {
            case Normal: {
                smv.visitFrame(entryFrame);

                final int startOffset = node.getStart().getOffset();
                final VariableDefinitionCollection variables = body.getVariables();

                boolean entryFrameModified = false;

                for (int i = smv.getLocalCount() - 1; i >= 0; i--) {
                    VariableDefinition v = variables.tryFind(i, startOffset);

                    if (v == null) {
                        if (i > 0) {
                            v = variables.tryFind(i - 1, startOffset);

                            if (v != null && v.getVariableType().getSimpleType().isDoubleWord()) {
                                i--;
                                continue;
                            }
                        }

                        smv.set(i, FrameValue.OUT_OF_SCOPE);
                        entryFrameModified = true;
                    }
                }

                if (entryFrameModified) {
                    smv.pruneLocals();
                    entryFrame = smv.buildFrame();
                }

                final int endOffset = node.getEnd().getOffset();

                for (Instruction instruction = node.getStart();
                     instruction != null && instruction.getOffset() <= endOffset;
                     instruction = instruction.getNext()) {

                    visitor.visit(instruction);

                    final OpCode opCode = instruction.getOpCode();

                    if (opCode.getOperandType() == OperandType.BranchTarget) {
                        for (final ControlFlowNode successor : node.getSuccessors()) {
                            if (successor.getStart() == instruction.<Instruction>getOperand(0) &&
                                entryFrames[successor.getBlockIndex()] == null) {

                                entryFrames[successor.getBlockIndex()] = smv.buildFrame();
                            }
                        }
                    }
                }

                break;
            }

            case CatchHandler: {
                entryFrame = new Frame(
                    FrameType.New,
                    entryFrame.getLocalValues().toArray(new FrameValue[entryFrame.getLocalValues().size()]),
                    EmptyArrayCache.fromElementType(FrameValue.class)
                );

                smv.visitFrame(entryFrame);
                smv.push(node.getExceptionHandler().getCatchType());

                break;
            }

            case FinallyHandler: {
                entryFrame = new Frame(
                    FrameType.New,
                    entryFrame.getLocalValues().toArray(new FrameValue[entryFrame.getLocalValues().size()]),
                    EmptyArrayCache.fromElementType(FrameValue.class)
                );

                smv.visitFrame(entryFrame);
                smv.push(MetadataSystem.instance().lookupType("java/lang/Throwable"));

                break;
            }
        }

        final Frame exitFrame = smv.buildFrame();

        entryFrames[index] = entryFrame;
        exitFrames[index] = exitFrame;
    }

    private static ControlFlowNode findPredecessor(final ControlFlowNode node) {
        ControlFlowNode predecessor = node.getImmediateDominator();

        if (predecessor == null || !predecessor.precedes(node)) {
            predecessor = CollectionUtilities.firstOrDefault(node.getPredecessors());
        }

        return predecessor;
    }
}
