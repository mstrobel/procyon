/*
 * GotoRemoval.java
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

import com.strobel.core.MutableInteger;
import com.strobel.core.Predicate;
import com.strobel.core.StrongBox;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.ITextOutput;
import com.strobel.util.ContractUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.strobel.core.CollectionUtilities.*;
import static com.strobel.decompiler.ast.PatternMatching.match;
import static com.strobel.decompiler.ast.PatternMatching.matchGetArgument;

final class GotoRemoval {
    private final static Node NULL_NODE = new Node() {
        @Override
        public void writeTo(final ITextOutput output) {
            throw ContractUtils.unreachable();
        }
    };

    final Map<Node, Label> labels = new IdentityHashMap<>();
    final Map<Node, Node> parentLookup = new IdentityHashMap<>();
    final Map<Node, Node> nextSibling = new IdentityHashMap<>();

    public final void removeGotos(final Block method) {
        parentLookup.put(method, NULL_NODE);

        for (final Node node : method.getSelfAndChildrenRecursive(Node.class)) {
            Node previousChild = null;

            for (final Node child : node.getChildren()) {
                if (parentLookup.containsKey(child)) {
                    throw Error.expressionLinkedFromMultipleLocations(child);
                }

                parentLookup.put(child, node);

                if (previousChild != null) {
                    if (previousChild instanceof Label) {
                        labels.put(child, (Label) previousChild);
                    }
                    nextSibling.put(previousChild, child);
                }

                previousChild = child;
            }

            if (previousChild != null) {
                nextSibling.put(previousChild, NULL_NODE);
            }
        }

        boolean modified;

        do {
            modified = false;

            for (final Expression e : method.getSelfAndChildrenRecursive(Expression.class)) {
                if (e.getCode() == AstCode.Goto) {
                    modified |= trySimplifyGoto(e);
                }
            }
        }
        while (modified);
    }

    private boolean trySimplifyGoto(final Expression gotoExpression) {
        assert gotoExpression.getCode() == AstCode.Goto;
        assert gotoExpression.getOperand() instanceof Label;

        final Node target = enter(gotoExpression, new LinkedHashSet<Node>());

        if (target == null) {
            return false;
        }

        //
        // The goto expression is marked as visited because we do not want to iterate over
        // nodes which we plan to modify.
        //
        // The simulated path always has to start in the same try block in order for the
        // same finally blocks to be executed.
        //

        final Set<Node> visitedNodes = new LinkedHashSet<>();

        visitedNodes.add(gotoExpression);

        if (target == exit(gotoExpression, visitedNodes)) {
            gotoExpression.setCode(AstCode.Nop);
            gotoExpression.setOperand(null);

            if (target instanceof Expression) {
                ((Expression) target).getRanges().addAll(gotoExpression.getRanges());
            }

            gotoExpression.getRanges().clear();
            return true;
        }

        Node breakBlock = null;

        for (final Node parent : getParents(gotoExpression)) {
            if (parent instanceof Loop || parent instanceof Switch) {
                breakBlock = parent;
                break;
            }
        }

        visitedNodes.clear();
        visitedNodes.add(gotoExpression);

        if (breakBlock != null && target == exit(breakBlock, visitedNodes)) {
            gotoExpression.setCode(AstCode.LoopOrSwitchBreak);
            gotoExpression.setOperand(null);
            return true;
        }

        Loop continueBlock = null;

        for (final Node parent : getParents(gotoExpression)) {
            if (parent instanceof Loop) {
                continueBlock = (Loop) parent;
                break;
            }
        }

        visitedNodes.clear();
        visitedNodes.add(gotoExpression);

        if (continueBlock != null && target == enter(continueBlock, visitedNodes)) {
            gotoExpression.setCode(AstCode.LoopContinue);
            gotoExpression.setOperand(null);
            return true;
        }

        return false;
    }

    private Iterable<Node> getParents(final Node node) {
        return getParents(node, Node.class);
    }

    private <T extends Node> Iterable<T> getParents(final Node node, final Class<T> parentType) {
        return new Iterable<T>() {
            @Override
            public final Iterator<T> iterator() {
                return new Iterator<T>() {
                    T current = updateCurrent(node);

                    @SuppressWarnings("unchecked")
                    private T updateCurrent(Node node) {
                        while (node != null && node != NULL_NODE) {
                            node = parentLookup.get(node);

                            if (parentType.isInstance(node)) {
                                return (T) node;
                            }
                        }

                        return null;
                    }

                    @Override
                    public final boolean hasNext() {
                        return current != null;
                    }

                    @Override
                    public final T next() {
                        final T next = current;

                        if (next == null) {
                            throw new NoSuchElementException();
                        }

                        current = updateCurrent(next);
                        return next;
                    }

                    @Override
                    public final void remove() {
                        throw ContractUtils.unsupported();
                    }
                };
            }
        };
    }

    private Node enter(final Node node, final Set<Node> visitedNodes) {
        VerifyArgument.notNull(node, "node");
        VerifyArgument.notNull(visitedNodes, "visitedNodes");

        if (!visitedNodes.add(node)) {
            //
            // Infinite loop.
            //
            return null;
        }

        if (node instanceof Label) {
            return exit(node, visitedNodes);
        }

        if (node instanceof Expression) {
            final Expression e = (Expression) node;

            switch (e.getCode()) {
                case Goto: {
                    final Label target = (Label) e.getOperand();

                    //
                    // Early exit -- same try block.
                    //
                    if (firstOrDefault(getParents(e, TryCatchBlock.class)) ==
                        firstOrDefault(getParents(target, TryCatchBlock.class))) {

                        return enter(target, visitedNodes);
                    }

                    //
                    // Make sure we are not entering a try block.
                    //
                    final List<TryCatchBlock> sourceTryBlocks = toList(getParents(e, TryCatchBlock.class));
                    final List<TryCatchBlock> targetTryBlocks = toList(getParents(target, TryCatchBlock.class));

                    Collections.reverse(sourceTryBlocks);
                    Collections.reverse(targetTryBlocks);

                    //
                    // Skip blocks we are already in.
                    //
                    int i = 0;

                    while (i < sourceTryBlocks.size() &&
                           i < targetTryBlocks.size() &&
                           sourceTryBlocks.get(i) == targetTryBlocks.get(i)) {
                        i++;
                    }

                    if (i == targetTryBlocks.size()) {
                        return enter(target, visitedNodes);
                    }

                    final TryCatchBlock targetTryBlock = targetTryBlocks.get(i);

                    //
                    // Check that the goto points to the start.
                    //
                    TryCatchBlock current = targetTryBlock;

                    while (current != null) {
                        for (final Node n : current.getTryBlock().getBody()) {
                            if (n instanceof Label) {
                                if (n == target) {
                                    return targetTryBlock;
                                }
                            }
                            else if (!match(n, AstCode.Nop)) {
                                current = n instanceof TryCatchBlock ? (TryCatchBlock) n : null;
                                break;
                            }
                        }
                    }

                    return null;
                }

                default: {
                    return e;
                }
            }
        }

        if (node instanceof Block) {
            final Block block = (Block) node;

            if (block.getEntryGoto() != null) {
                return enter(block.getEntryGoto(), visitedNodes);
            }

            if (block.getBody().isEmpty()) {
                return exit(block, visitedNodes);
            }

            return enter(block.getBody().get(0), visitedNodes);
        }

        if (node instanceof Condition) {
            return ((Condition) node).getCondition();
        }

        if (node instanceof Loop) {
            final Loop loop = (Loop) node;

            if (loop.getCondition() != null) {
                return loop.getCondition();
            }

            return enter(loop.getBody(), visitedNodes);
        }

        if (node instanceof TryCatchBlock) {
            return node;
        }

        if (node instanceof Switch) {
            return ((Switch) node).getCondition();
        }

        throw Error.unsupportedNode(node);
    }

    private Node exit(final Node node, final Set<Node> visitedNodes) {
        VerifyArgument.notNull(node, "node");
        VerifyArgument.notNull(visitedNodes, "visitedNodes");

        final Node parent = parentLookup.get(node);

        if (parent == null || parent == NULL_NODE) {
            //
            // Exited main body.
            //
            return null;
        }

        if (parent instanceof Block) {
            final Node nextNode = nextSibling.get(node);

            if (nextNode != null && nextNode != NULL_NODE) {
                return enter(nextNode, visitedNodes);
            }

            return exit(parent, visitedNodes);
        }

        if (parent instanceof Condition) {
            return exit(parent, visitedNodes);
        }

        if (parent instanceof TryCatchBlock) {
            //
            // Finally blocks are completely ignored.  We rely on the fact that try blocks
            // cannot be entered.
            //
            return exit(parent, visitedNodes);
        }

        if (parent instanceof Switch) {
            //
            // Implicit exit from switch is not allowed.
            //
            return null;
        }

        if (parent instanceof Loop) {
            return enter(parent, visitedNodes);
        }

        throw Error.unsupportedNode(parent);
    }

    @SuppressWarnings("ConstantConditions")
    public static void removeRedundantCode(final Block method) {
        final Map<Label, MutableInteger> labelReferenceCount = new IdentityHashMap<>();

        final List<Expression> branchExpressions = method.getSelfAndChildrenRecursive(
            Expression.class,
            new Predicate<Expression>() {
                @Override
                public boolean test(final Expression e) {
                    return e.isBranch();
                }
            }
        );

        for (final Expression e : branchExpressions) {
            for (final Label branchTarget : e.getBranchTargets()) {
                final MutableInteger referenceCount = labelReferenceCount.get(branchTarget);

                if (referenceCount == null) {
                    labelReferenceCount.put(branchTarget, new MutableInteger(1));
                }
                else {
                    referenceCount.increment();
                }
            }
        }

        for (final Block block : method.getSelfAndChildrenRecursive(Block.class)) {
            final List<Node> body = block.getBody();
            final List<Node> newBody = new ArrayList<>(body.size());

            for (int i = 0, n = body.size(); i < n; i++) {
                final Node node = body.get(i);
                final StrongBox<Label> target = new StrongBox<>();
                final StrongBox<Expression> popExpression = new StrongBox<>();

                if (PatternMatching.matchGetOperand(node, AstCode.Goto, target) &&
                    i + 1 < body.size() &&
                    body.get(i + 1) == target.get()) {

                    //
                    // Ignore the branch.
                    //
                    if (labelReferenceCount.get(target.get()).getValue() == 1) {
                        //
                        // Ignore the label as well.
                        //
                        i++;
                    }
                }
                else if (match(node, AstCode.Nop)) {
                    //
                    // Ignore NOP.
                    //
                }
                else if (PatternMatching.matchGetArgument(node, AstCode.Pop, popExpression)) {
                    final StrongBox<Variable> variable = new StrongBox<>();

                    if (!PatternMatching.matchGetOperand(popExpression.get(), AstCode.Load, variable)) {
                        throw new IllegalStateException("Pop should just have Load at this stage.");
                    }

                    //
                    // Best effort to move bytecode range to previous statement.
                    //

                    final StrongBox<Variable> previousVariable = new StrongBox<>();
                    final StrongBox<Expression> previousExpression = new StrongBox<>();

                    if (i - 1 >= 0 &&
                        matchGetArgument(body.get(i - 1), AstCode.Store, previousVariable, previousExpression) &&
                        previousVariable.get() == variable.get()) {

                        previousExpression.get().getRanges().addAll(((Expression) node).getRanges());

                        //
                        // Ignore POP.
                        //
                    }
                }
                else if (node instanceof Label) {
                    final Label label = (Label) node;
                    final MutableInteger referenceCount = labelReferenceCount.get(label);

                    if (referenceCount != null && referenceCount.getValue() > 0) {
                        newBody.add(label);
                    }
                }
                else {
                    newBody.add(node);
                }
            }

            body.clear();
            body.addAll(newBody);
        }

        //
        // DUP removal.
        //
        final StrongBox<Expression> child = new StrongBox<>();

        for (final Expression e : method.getSelfAndChildrenRecursive(Expression.class)) {
            final List<Expression> arguments = e.getArguments();

            for (int i = 0, n = arguments.size(); i < n; i++) {
                final Expression argument = arguments.get(i);

                if (PatternMatching.matchGetArgument(e, AstCode.Dup, child)) {
                    child.get().getRanges().addAll(argument.getRanges());
                    arguments.set(i, child.get());
                }
            }
        }
    }
}
