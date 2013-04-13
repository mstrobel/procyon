/*
 * LoopsAndConditions.java
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

package com.strobel.decompiler.ast;

import com.strobel.assembler.Collection;
import com.strobel.assembler.flowanalysis.ControlFlowEdge;
import com.strobel.assembler.flowanalysis.ControlFlowGraph;
import com.strobel.assembler.flowanalysis.ControlFlowNode;
import com.strobel.assembler.flowanalysis.ControlFlowNodeType;
import com.strobel.assembler.flowanalysis.JumpType;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.InstructionCollection;
import com.strobel.assembler.metadata.MethodBody;
import com.strobel.assembler.metadata.SwitchInfo;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.StrongBox;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static com.strobel.decompiler.ast.AstOptimizer.*;
import static com.strobel.decompiler.ast.PatternMatching.*;

final class LoopsAndConditions {
    private final Map<Label, ControlFlowNode> labelsToNodes = new IdentityHashMap<>();
    private final DecompilerContext _context;

    private int _nextLabelIndex;

    public LoopsAndConditions(final DecompilerContext context) {
        _context = VerifyArgument.notNull(context, "context");
    }

    public final void findConditions(final Block block) {
        final List<Node> body = block.getBody();

        if (body.isEmpty()) {
            return;
        }

        final ControlFlowGraph graph = buildGraph(body, (Label) block.getEntryGoto().getOperand());

        graph.computeDominance();
        graph.computeDominanceFrontier();

        final Set<ControlFlowNode> cfNodes = new LinkedHashSet<>();
        final List<ControlFlowNode> graphNodes = graph.getNodes();

        for (int i = 3; i < graphNodes.size(); i++) {
            cfNodes.add(graphNodes.get(i));
        }

        final List<Node> newBody = findConditions(cfNodes, graph.getEntryPoint());

        block.getBody().clear();
        block.getBody().addAll(newBody);
    }

    public final void findLoops(final Block block) {
        final List<Node> body = block.getBody();

        if (body.isEmpty()) {
            return;
        }

        final ControlFlowGraph graph = buildGraph(body, (Label) block.getEntryGoto().getOperand());

        graph.computeDominance();
        graph.computeDominanceFrontier();

        final Set<ControlFlowNode> cfNodes = new LinkedHashSet<>();
        final List<ControlFlowNode> graphNodes = graph.getNodes();

        for (int i = 3; i < graphNodes.size(); i++) {
            cfNodes.add(graphNodes.get(i));
        }

        final List<Node> newBody = findLoops(cfNodes, graph.getEntryPoint(), false);

        block.getBody().clear();
        block.getBody().addAll(newBody);
    }

    private ControlFlowGraph buildGraph(final List<Node> nodes, final Label entryLabel) {
        int index = 0;

        final List<ControlFlowNode> cfNodes = new ArrayList<>();

        final ControlFlowNode entryPoint = new ControlFlowNode(index++, 0, ControlFlowNodeType.EntryPoint);
        final ControlFlowNode regularExit = new ControlFlowNode(index++, -1, ControlFlowNodeType.RegularExit);
        final ControlFlowNode exceptionalExit = new ControlFlowNode(index++, -1, ControlFlowNodeType.ExceptionalExit);

        cfNodes.add(entryPoint);
        cfNodes.add(regularExit);
        cfNodes.add(exceptionalExit);

        //
        // Create graph nodes.
        //

        labelsToNodes.clear();

        final Map<Node, ControlFlowNode> astNodesToControlFlowNodes = new IdentityHashMap<>();

        for (final Node node : nodes) {
            final ControlFlowNode cfNode = new ControlFlowNode(index++, -1, ControlFlowNodeType.Normal);

            cfNodes.add(cfNode);
            astNodesToControlFlowNodes.put(node, cfNode);
            cfNode.setUserData(node);

            //
            // Find all contained labels.
            //
            for (final Label label : node.getSelfAndChildrenRecursive(Label.class)) {
                labelsToNodes.put(label, cfNode);
            }
        }

        final ControlFlowNode entryNode = labelsToNodes.get(entryLabel);
        final ControlFlowEdge entryEdge = new ControlFlowEdge(entryPoint, entryNode, JumpType.Normal);

        entryPoint.getOutgoing().add(entryEdge);
        entryNode.getIncoming().add(entryEdge);

        //
        // Create edges.
        //

        for (final Node node : nodes) {
            final List<Node> nodeBody = ((BasicBlock) node).getBody();
            final ControlFlowNode source = astNodesToControlFlowNodes.get(node);

            //
            // Find all branches.
            //

            for (final Expression e : node.getSelfAndChildrenRecursive(Expression.class)) {
                if (!e.isBranch()) {
                    continue;
                }

                for (final Label target : e.getBranchTargets()) {
                    final ControlFlowNode destination = labelsToNodes.get(target);

                    if (destination != null &&
                        (destination != source || !nodeBody.isEmpty() && target == nodeBody.get(0))) {

                        final ControlFlowEdge edge = new ControlFlowEdge(source, destination, JumpType.Normal);

                        if (!source.getOutgoing().contains(edge)) {
                            source.getOutgoing().add(edge);
                        }

                        if (!destination.getIncoming().contains(edge)) {
                            destination.getIncoming().add(edge);
                        }
                    }
                }
            }
        }

        return new ControlFlowGraph(cfNodes.toArray(new ControlFlowNode[cfNodes.size()]));
    }

    @SuppressWarnings("ConstantConditions")
    private List<Node> findLoops(final Set<ControlFlowNode> scopeNodes, final ControlFlowNode entryPoint, final boolean excludeEntryPoint) {
        final List<Node> result = new ArrayList<>();
        final Set<ControlFlowNode> scope = new LinkedHashSet<>(scopeNodes);
        final ArrayDeque<ControlFlowNode> agenda = new ArrayDeque<>();

        agenda.addLast(entryPoint);

        while (!agenda.isEmpty()) {
            final ControlFlowNode node = agenda.pollFirst();

            //
            // If the node is a loop header...
            //
            if (scope.contains(node) &&
                node.getDominanceFrontier().contains(node) &&
                (node != entryPoint || !excludeEntryPoint)) {

                final Set<ControlFlowNode> loopContents = findLoopContents(scope, node);

                //
                // If the first expression is a loop condition...
                //
                final BasicBlock basicBlock = (BasicBlock) node.getUserData();
                final StrongBox<Expression> condition = new StrongBox<>();
                final StrongBox<Label> trueLabel = new StrongBox<>();
                final StrongBox<Label> falseLabel = new StrongBox<>();

                //
                // It has to be just IfTrue; any preceding code would introduce a goto.
                //
                if (matchSingleAndBreak(basicBlock, AstCode.IfTrue, trueLabel, condition, falseLabel)) {
                    final ControlFlowNode trueTarget = labelsToNodes.get(trueLabel.get());
                    final ControlFlowNode falseTarget = labelsToNodes.get(falseLabel.get());

                    //
                    // If one point inside the loop and the other outside...
                    //
                    if (loopContents.contains(falseTarget) && !loopContents.contains(trueTarget) ||
                        loopContents.contains(trueTarget) && !loopContents.contains(falseTarget)) {

                        removeOrThrow(loopContents, node);
                        removeOrThrow(scope, node);

                        //
                        // If false means enter the loop, negate the condition.
                        //
                        if (loopContents.contains(falseTarget) || falseTarget == node) {
                            final Label temp = trueLabel.get();

                            trueLabel.set(falseLabel.get());
                            falseLabel.set(temp);
                            condition.set(AstOptimizer.simplifyLogicalNot(new Expression(AstCode.LogicalNot, null, condition.get())));
                        }

                        final ControlFlowNode postLoopTarget = labelsToNodes.get(falseLabel.get());

                        if (postLoopTarget != null) {
                            //
                            // Pull more nodes into the loop.
                            //
                            final Set<ControlFlowNode> postLoopContents = findDominatedNodes(scope, postLoopTarget);
                            final LinkedHashSet<ControlFlowNode> pullIn = new LinkedHashSet<>(scope);

                            pullIn.removeAll(postLoopContents);

                            for (final ControlFlowNode n : pullIn) {
                                if (node.dominates(n)) {
                                    loopContents.add(n);
                                }
                            }
                        }

                        //
                        // Use loop to implement the IfTrue.
                        //
                        final List<Node> basicBlockBody = basicBlock.getBody();

                        removeTail(basicBlockBody, AstCode.IfTrue, AstCode.Goto);

                        final Loop loop = new Loop();
                        final Block bodyBlock = new Block();

                        loop.setCondition(condition.get());
                        loop.setBody(bodyBlock);

                        bodyBlock.setEntryGoto(new Expression(AstCode.Goto, trueLabel.get()));
                        bodyBlock.getBody().addAll(findLoops(loopContents, node, false));

                        basicBlockBody.add(loop);
                        basicBlockBody.add(new Expression(AstCode.Goto, falseLabel.get()));

                        result.add(basicBlock);
                        scope.removeAll(loopContents);
                    }
                }

                //
                // Fallback method: while (true) { ... }
                //
                if (scope.contains(node)) {
                    final BasicBlock block = new BasicBlock();
                    final List<Node> blockBody = block.getBody();
                    final Loop loop = new Loop();
                    final Block bodyBlock = new Block();

                    loop.setBody(bodyBlock);

                    bodyBlock.setEntryGoto(new Expression(AstCode.Goto, basicBlock.getBody().get(0)));
                    bodyBlock.getBody().addAll(findLoops(loopContents, node, true));

                    blockBody.add(new Label("Loop_" + _nextLabelIndex++));
                    blockBody.add(loop);

                    result.add(block);
                    scope.removeAll(loopContents);
                }
            }

            //
            // Using the dominator tree should ensure we find the widest loop first.
            //
            for (final ControlFlowNode child : node.getDominatorTreeChildren()) {
                agenda.addLast(child);
            }
        }

        //
        // Add whatever is left.
        //

        for (final ControlFlowNode node : scope) {
            result.add((Node) node.getUserData());
        }

        scope.clear();

        return result;
    }

    private static Set<ControlFlowNode> findLoopContents(final Set<ControlFlowNode> scope, final ControlFlowNode head) {
        final Set<ControlFlowNode> viaBackEdges = new LinkedHashSet<>();

        for (final ControlFlowNode predecessor : head.getPredecessors()) {
            if (head.dominates(predecessor)) {
                viaBackEdges.add(predecessor);
            }
        }

        final Set<ControlFlowNode> agenda = new LinkedHashSet<>(viaBackEdges);
        final Set<ControlFlowNode> result = new LinkedHashSet<>();

        while (!agenda.isEmpty()) {
            final ControlFlowNode addNode = agenda.iterator().next();

            agenda.remove(addNode);

            if (scope.contains(addNode) && head.dominates(addNode) && result.add(addNode)) {
                for (final ControlFlowNode predecessor : addNode.getPredecessors()) {
                    agenda.add(predecessor);
                }
            }
        }

        if (scope.contains(head)) {
            result.add(head);
        }

        return result;
    }

    @SuppressWarnings("ConstantConditions")
    private List<Node> findConditions(final Set<ControlFlowNode> scopeNodes, final ControlFlowNode entryNode) {
        final List<Node> result = new ArrayList<>();
        final Set<ControlFlowNode> scope = new HashSet<>(scopeNodes);
        final Stack<ControlFlowNode> agenda = new Stack<>();
        final MethodBody methodBody = _context.getCurrentMethod().getBody();
        final InstructionCollection instructions = methodBody.getInstructions();

        agenda.push(entryNode);

        while (!agenda.isEmpty()) {
            final ControlFlowNode node = agenda.pop();

            //
            // Find a block that represents a simple condition.
            //

            if (scope.contains(node)) {
                final BasicBlock block = (BasicBlock) node.getUserData();
                final List<Node> blockBody = block.getBody();

                final StrongBox<Label[]> caseLabels = new StrongBox<>();
                final StrongBox<Expression> switchArgument = new StrongBox<>();
                final StrongBox<Label> fallLabel = new StrongBox<>();
                final StrongBox<Label> tempTarget = new StrongBox<>();

                AstCode switchCode;

                if (matchLastAndBreak(block, switchCode = AstCode.TableSwitch, caseLabels, switchArgument, fallLabel) ||
                    matchLastAndBreak(block, switchCode = AstCode.LookupSwitch, caseLabels, switchArgument, fallLabel)) {

                    final Expression switchExpression = (Expression) blockBody.get(blockBody.size() - 2);
                    final Collection<Range> switchRanges = switchExpression.getArguments().get(0).getRanges();
                    final Instruction switchInstruction = instructions.atOffset(switchRanges.get(switchRanges.size() - 1).getStart());

                    //
                    // Replace the switch code with a Switch node.
                    //

                    final Switch switchNode = new Switch();

                    switchNode.setCondition(switchArgument.get());
                    removeTail(blockBody, switchCode, AstCode.Goto);
                    blockBody.add(switchNode);
                    result.add(block);

                    //
                    // Replace the item so it isn't picked up as content.
                    //

                    removeOrThrow(scope, node);

                    //
                    // Pull in code of cases.
                    //

                    final Label[] labels = caseLabels.get();
                    final SwitchInfo switchInfo = switchInstruction.getOperand(0);
                    final int lowValue = switchInfo.getLowValue();
                    final int[] keys = switchInfo.getKeys();
                    final Label defaultLabel = labels[0];
                    final ControlFlowNode defaultTarget = labelsToNodes.get(defaultLabel);

                    boolean defaultFollowsSwitch = false;
                    List<CaseBlock> defaultPredecessors = null;

                    for (int i = 1; i < labels.length; i++) {
                        final Label caseLabel = labels[i];
                        //
                        // Find or create a new case block.
                        //

                        CaseBlock caseBlock = null;

                        for (final CaseBlock cb : switchNode.getCaseBlocks()) {
                            if (cb.getEntryGoto().getOperand() == caseLabel) {
                                caseBlock = cb;
                                break;
                            }
                        }

                        if (caseBlock == null) {
                            caseBlock = new CaseBlock();

                            final ControlFlowNode caseTarget = labelsToNodes.get(caseLabel);
                            final List<Node> caseBody = caseBlock.getBody();

                            if (caseLabel == defaultLabel) {
                                if (defaultPredecessors == null) {
                                    defaultPredecessors = new ArrayList<>();
                                }

                                defaultPredecessors.add(caseBlock);

                                final BasicBlock gotoDefault = new BasicBlock();

                                gotoDefault.getBody().add(new Label("GotoDefault_" + _nextLabelIndex++));
                                gotoDefault.getBody().add(new Expression(AstCode.Goto, defaultLabel));

                                caseBody.add(gotoDefault);
                            }
                            else {
                                switchNode.getCaseBlocks().add(caseBlock);

                                if (caseTarget != null) {
                                    if (caseTarget.getDominanceFrontier().contains(defaultTarget)) {
                                        defaultFollowsSwitch = true;
                                    }

                                    caseBlock.setEntryGoto(new Expression(AstCode.Goto, caseLabel));

                                    final Set<ControlFlowNode> content = findDominatedNodes(scope, caseTarget);

                                    scope.removeAll(content);
                                    caseBody.addAll(findConditions(content, caseTarget));
                                }
                            }

                            if (caseBody.isEmpty() ||
                                !matchLast((BasicBlock) caseBody.get(caseBody.size() - 1), AstCode.Goto, tempTarget) ||
                                !ArrayUtilities.contains(labels, tempTarget.get())) {

                                //
                                // Add explicit break that should not be used by default, but which might be used
                                // by goto removal.
                                //

                                final BasicBlock explicitBreak = new BasicBlock();

                                explicitBreak.getBody().add(new Label("SwitchBreak_" + _nextLabelIndex++));
                                explicitBreak.getBody().add(new Expression(AstCode.LoopOrSwitchBreak, null));

                                caseBody.add(explicitBreak);
                            }
                        }

                        if (i != 0) {
                            if (switchCode == AstCode.TableSwitch) {
                                caseBlock.getValues().add(lowValue + i - 1);
                            }
                            else {
                                caseBlock.getValues().add(keys[i - 1]);
                            }
                        }
                    }

                    if (defaultPredecessors != null) {
                        for (final CaseBlock caseBlock : defaultPredecessors) {
                            switchNode.getCaseBlocks().add(caseBlock);
                        }
                    }

                    if (!defaultFollowsSwitch) {
                        final CaseBlock defaultBlock = new CaseBlock();

                        switchNode.getCaseBlocks().add(defaultBlock);

//                        defaultBlock.setEntryGoto(new Expression(AstCode.Goto, labels[0]));

                        final Set<ControlFlowNode> content = findDominatedNodes(scope, defaultTarget);

                        scope.removeAll(content);
                        defaultBlock.getBody().addAll(findConditions(content, defaultTarget));

                        //
                        // Add explicit break that should not be used by default, but which might be used
                        // by goto removal.
                        //

                        final BasicBlock explicitBreak = new BasicBlock();

                        explicitBreak.getBody().add(new Label("SwitchBreak_" + _nextLabelIndex++));
                        explicitBreak.getBody().add(new Expression(AstCode.LoopOrSwitchBreak, null));

                        defaultBlock.getBody().add(explicitBreak);
                    }

                    //
                    // TODO: Arrange the case blocks such that fall-throughs go to the right block.
                    //
                }

                //
                // Two-way branch...
                //
                final StrongBox<Expression> condition = new StrongBox<>();
                final StrongBox<Label> trueLabel = new StrongBox<>();
                final StrongBox<Label> falseLabel = new StrongBox<>();

                if (matchLastAndBreak(block, AstCode.IfTrue, trueLabel, condition, falseLabel)) {
                    //
                    // Flip bodies since that seems to be the Java compiler tradition.
                    //

                    final Label temp = trueLabel.get();

                    trueLabel.set(falseLabel.get());
                    falseLabel.set(temp);
                    condition.set(AstOptimizer.simplifyLogicalNot(new Expression(AstCode.LogicalNot, null, condition.get())));

                    //
                    // Convert IfTrue expression to Condition.
                    //

                    final Condition conditionNode = new Condition();
                    final Block trueBlock = new Block();
                    final Block falseBlock = new Block();

                    trueBlock.setEntryGoto(new Expression(AstCode.Goto, trueLabel.get()));
                    falseBlock.setEntryGoto(new Expression(AstCode.Goto, falseLabel.get()));

                    conditionNode.setCondition(condition.get());
                    conditionNode.setTrueBlock(trueBlock);
                    conditionNode.setFalseBlock(falseBlock);

                    removeTail(blockBody, AstCode.IfTrue, AstCode.Goto);
                    blockBody.add(conditionNode);
                    result.add(block);

                    //
                    // Remove the item immediately so it isn't picked up as content.
                    //
                    removeOrThrow(scope, node);

                    final ControlFlowNode trueTarget = labelsToNodes.get(trueLabel.get());
                    final ControlFlowNode falseTarget = labelsToNodes.get(falseLabel.get());

                    //
                    // Pull in the conditional code.
                    //

                    if (trueTarget != null && hasSingleEdgeEnteringBlock(trueTarget)) {
                        final Set<ControlFlowNode> content = findDominatedNodes(scope, trueTarget);
                        scope.removeAll(content);
                        conditionNode.getTrueBlock().getBody().addAll(findConditions(content, trueTarget));
                    }

                    if (falseTarget != null && hasSingleEdgeEnteringBlock(falseTarget)) {
                        final Set<ControlFlowNode> content = findDominatedNodes(scope, falseTarget);
                        scope.removeAll(content);
                        conditionNode.getFalseBlock().getBody().addAll(findConditions(content, falseTarget));
                    }
                }

                //
                // Add the node now so that we have good ordering.
                //
                if (scope.contains(node)) {
                    result.add((Node) node.getUserData());
                    scope.remove(node);
                }
            }

            //
            // Depth-first traversal of dominator tree.
            //

            final List<ControlFlowNode> dominatorTreeChildren = node.getDominatorTreeChildren();

            for (int i = dominatorTreeChildren.size() - 1; i >= 0; i--) {
                agenda.push(dominatorTreeChildren.get(i));
            }
        }

        //
        // Add whatever is left.
        //
        for (final ControlFlowNode node : scope) {
            result.add((Node) node.getUserData());
        }

        return result;
    }

    private static boolean hasSingleEdgeEnteringBlock(final ControlFlowNode node) {
        int count = 0;

        for (final ControlFlowEdge edge : node.getIncoming()) {
            if (!node.dominates(edge.getSource())) {
                if (++count > 1) {
                    return false;
                }
            }
        }

        return count == 1;
    }

    private static Set<ControlFlowNode> findDominatedNodes(final Set<ControlFlowNode> scope, final ControlFlowNode head) {
        final Set<ControlFlowNode> agenda = new LinkedHashSet<>();
        final Set<ControlFlowNode> result = new LinkedHashSet<>();

        agenda.add(head);

        while (!agenda.isEmpty()) {
            final ControlFlowNode addNode = agenda.iterator().next();

            agenda.remove(addNode);

            if (scope.contains(addNode) && head.dominates(addNode) && result.add(addNode)) {
                for (final ControlFlowNode successor : addNode.getSuccessors()) {
                    agenda.add(successor);
                }
            }
        }

        return result;
    }
}
