/*
 * ControlFlowGraph.java
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

import com.strobel.core.ArrayUtilities;
import com.strobel.core.BooleanBox;
import com.strobel.core.VerifyArgument;
import com.strobel.functions.Block;
import com.strobel.functions.Function;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;

public final class ControlFlowGraph {
    private final List<ControlFlowNode> _nodes;

    public final ControlFlowNode getEntryPoint() {
        return _nodes.get(0);
    }

    public final ControlFlowNode getRegularExit() {
        return _nodes.get(1);
    }

    public final ControlFlowNode getExceptionalExit() {
        return _nodes.get(2);
    }

    public final List<ControlFlowNode> getNodes() {
        return _nodes;
    }

    public ControlFlowGraph(final ControlFlowNode... nodes) {
        _nodes = ArrayUtilities.asUnmodifiableList(VerifyArgument.noNullElements(nodes, "nodes"));

        assert nodes.length >= 3;
        assert getEntryPoint().getNodeType() == ControlFlowNodeType.EntryPoint;
        assert getRegularExit().getNodeType() == ControlFlowNodeType.RegularExit;
        assert getExceptionalExit().getNodeType() == ControlFlowNodeType.ExceptionalExit;
    }

    public final void resetVisited() {
        for (final ControlFlowNode node : _nodes) {
            node.setVisited(false);
        }
    }

    public final void computeDominance() {
        computeDominance(new BooleanBox());
    }

    public final void computeDominance(final BooleanBox cancelled) {
        final ControlFlowNode entryPoint = getEntryPoint();

        entryPoint.setImmediateDominator(entryPoint);

        final BooleanBox changed = new BooleanBox(true);

        while (changed.get()) {
            changed.set(false);
            resetVisited();

            if (cancelled.get()) {
                throw new CancellationException();
            }

            entryPoint.traversePreOrder(
                new Function<ControlFlowNode, Iterable<ControlFlowNode>>() {
                    @Override
                    public final Iterable<ControlFlowNode> apply(final ControlFlowNode input) {
                        return input.getSuccessors();
                    }
                },
                new Block<ControlFlowNode>() {
                    @Override
                    public final void accept(final ControlFlowNode b) {
                        if (b == entryPoint) {
                            return;
                        }

                        ControlFlowNode newImmediateDominator = null;

                        for (final ControlFlowNode p : b.getPredecessors()) {
                            if (p.isVisited() && p != b) {
                                newImmediateDominator = p;
                                break;
                            }
                        }

                        if (newImmediateDominator == null) {
                            throw new IllegalStateException("Could not compute new immediate dominator!");
                        }

                        for (final ControlFlowNode p : b.getPredecessors()) {
                            if (p != b && p.getImmediateDominator() != null) {
                                newImmediateDominator = findCommonDominator(p, newImmediateDominator);
                            }
                        }

                        if (b.getImmediateDominator() != newImmediateDominator) {
                            b.setImmediateDominator(newImmediateDominator);
                            changed.set(true);
                        }
                    }
                }
            );
        }

        entryPoint.setImmediateDominator(null);

        for (final ControlFlowNode node : _nodes) {
            final ControlFlowNode immediateDominator = node.getImmediateDominator();

            if (immediateDominator != null) {
                immediateDominator.getDominatorTreeChildren().add(node);
            }
        }
    }

    public final void computeDominanceFrontier() {
        resetVisited();

        getEntryPoint().traversePostOrder(
            new Function<ControlFlowNode, Iterable<ControlFlowNode>>() {
                @Override
                public final Iterable<ControlFlowNode> apply(final ControlFlowNode input) {
                    return input.getDominatorTreeChildren();
                }
            },
            new Block<ControlFlowNode>() {
                @Override
                public void accept(final ControlFlowNode n) {
                    final Set<ControlFlowNode> dominanceFrontier = n.getDominanceFrontier();

                    dominanceFrontier.clear();

                    for (final ControlFlowNode s : n.getSuccessors()) {
                        if (s.getImmediateDominator() != n) {
                            dominanceFrontier.add(s);
                        }
                    }

                    for (final ControlFlowNode child : n.getDominatorTreeChildren()) {
                        for (final ControlFlowNode p : child.getDominanceFrontier()) {
                            if (p.getImmediateDominator() != n) {
                                dominanceFrontier.add(p);
                            }
                        }
                    }
                }
            }
        );
    }

    static ControlFlowNode findCommonDominator(final ControlFlowNode a, final ControlFlowNode b) {
        final HashSet<ControlFlowNode> path1 = new HashSet<>();

        ControlFlowNode node1 = a;
        ControlFlowNode node2 = b;

        while (node1 != null && path1.add(node1)) {
            node1 = node1.getImmediateDominator();
        }

        while (node2 != null) {
            if (path1.contains(node2)) {
                return node2;
            }
            node2 = node2.getImmediateDominator();
        }

        throw new IllegalStateException("No common dominator found!");
    }
}
