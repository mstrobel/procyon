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
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.CollectionUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.util.EmptyArrayCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StackMapAnalyzer {
    public static List<StackMapFrame> computeStackMapTable(final MethodBody body, final IMethodSignature signature) {
        VerifyArgument.notNull(body, "body");
        VerifyArgument.notNull(signature, "signature");

        final ControlFlowGraph cfg = ControlFlowGraphBuilder.build(body);

        cfg.computeDominance();

        final StackMappingVisitor stackMappingVisitor = new StackMappingVisitor();
        final InstructionVisitor instructionVisitor = stackMappingVisitor.visitBody(null);

        int local = 0;

        final ParameterDefinition thisParameter = body.getThisParameter();

        if (thisParameter != null) {
            stackMappingVisitor.set(local++, thisParameter.getParameterType());
        }

        for (final ParameterDefinition parameter : signature.getParameters()) {
            stackMappingVisitor.set(local++, parameter.getParameterType());
        }

        final List<ControlFlowNode> nodes = new ArrayList<>();

        populateNodes(cfg.getRegularExit(), nodes);
        populateNodes(cfg.getExceptionalExit(), nodes);

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
            computeFrames(stackMappingVisitor, instructionVisitor, node, entryFrames, exitFrames);
        }

        final StackMapFrame[] computedFrames = new StackMapFrame[nodeCount];

        int i = 0;

        for (final ControlFlowNode node : nodes) {
            if (node.getNodeType() == ControlFlowNodeType.Normal && node.getOffset() != 0) {
                final ControlFlowNode predecessor = CollectionUtilities.firstOrDefault(node.getPredecessors());

                final Frame currentEntry = entryFrames[node.getBlockIndex()];
                final Frame previousEntry = entryFrames[predecessor.getBlockIndex()];
                final Frame previousExit = entryFrames[predecessor.getBlockIndex()];

                computedFrames[i++] = new StackMapFrame(
                    Frame.merge(
                        previousEntry,
                        previousExit,
                        currentEntry,
                        Collections.<FrameValue, TypeReference>emptyMap()
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

        for (final ControlFlowNode n : node.getPredecessors()) {
            populateNodes(n, nodes);
        }

        nodes.add(node);
    }

    private static void computeFrames(
        final StackMappingVisitor smv,
        final InstructionVisitor v,
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
            final ControlFlowNode predecessor = CollectionUtilities.firstOrDefault(node.getPredecessors());

            if (predecessor == null) {
                predecessorExit = exitFrames[0];
            }
            else {
                computeFrames(smv, v, predecessor, entryFrames, exitFrames);
                predecessorExit = exitFrames[predecessor.getBlockIndex()];
            }

            entryFrame = predecessorExit;
        }

        switch (node.getNodeType()) {
            case Normal: {
                smv.visitFrame(entryFrame);

                for (Instruction instruction = node.getStart();
                     instruction != null && instruction.getOffset() <= node.getEnd().getOffset();
                     instruction = instruction.getNext()) {

                    v.visit(instruction);

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
}
