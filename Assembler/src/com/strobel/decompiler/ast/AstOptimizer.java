/*
 * AstOptimizer.java
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

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.core.CollectionUtilities;
import com.strobel.core.MutableInteger;
import com.strobel.core.Predicate;
import com.strobel.core.Predicates;
import com.strobel.core.StrongBox;
import com.strobel.core.VerifyArgument;
import com.strobel.core.delegates.Func;
import com.strobel.decompiler.DecompilerContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.strobel.decompiler.ast.PatternMatching.*;

@SuppressWarnings("ConstantConditions")
public final class AstOptimizer {
    int _nextLabelIndex;
    DecompilerContext _context;
    MetadataSystem _metadataSystem;
    Block _method;

    public static void optimize(final DecompilerContext context, final Block method) {
        optimize(context, method, context.getSettings().getAbortBeforeStep());
    }

    public static void optimize(final DecompilerContext context, final Block method, final AstOptimizationStep abortBeforeStep) {
        if (abortBeforeStep == AstOptimizationStep.RemoveRedundantCode) {
            return;
        }

        final AstOptimizer optimizer = new AstOptimizer();

        optimizer._context = context;
        optimizer._metadataSystem = MetadataSystem.instance();
        optimizer._method = method;

        removeRedundantCode(method);

        if (abortBeforeStep == AstOptimizationStep.ReduceBranchInstructionSet) {
            return;
        }

        for (final Block block : method.getSelfAndChildrenRecursive(Block.class)) {
            reduceBranchInstructionSet(block);
        }

        if (abortBeforeStep == AstOptimizationStep.InlineVariables) {
            return;
        }

        final Inlining inliningPhase1 = new Inlining(method);

        inliningPhase1.inlineAllVariables();

        if (abortBeforeStep == AstOptimizationStep.CopyPropagation) {
            return;
        }

        inliningPhase1.copyPropagation();

        if (abortBeforeStep == AstOptimizationStep.SplitToMovableBlocks) {
            return;
        }

        for (final Block block : method.getSelfAndChildrenRecursive(Block.class)) {
            optimizer.splitToMovableBlocks(block);
        }

        if (abortBeforeStep == AstOptimizationStep.TypeInference) {
            return;
        }

        TypeAnalysis.run(context, method);

        for (final Block block : method.getSelfAndChildrenRecursive(Block.class)) {
            boolean modified;

            do {
                modified = false;

                if (abortBeforeStep == AstOptimizationStep.SimplifyShortCircuit) {
                    continue;
                }

                modified |= runOptimization(block, new SimplifyShortCircuitOptimization(context, method));

                if (abortBeforeStep == AstOptimizationStep.JoinBasicBlocks) {
                    continue;
                }

                modified |= runOptimization(block, new JoinBasicBlocksOptimization(context, method));

                if (abortBeforeStep == AstOptimizationStep.InlineVariables2) {
                    continue;
                }

                modified |= new Inlining(method).inlineAllInBlock(block);
                new Inlining(method).copyPropagation();
            } while (modified);
        }

        if (abortBeforeStep == AstOptimizationStep.FindLoops) {
            return;
        }

        for (final Block block : method.getSelfAndChildrenRecursive(Block.class)) {
            new LoopsAndConditions(context).findLoops(block);
        }

        if (abortBeforeStep == AstOptimizationStep.FindConditions) {
            return;
        }

        for (final Block block : method.getSelfAndChildrenRecursive(Block.class)) {
            new LoopsAndConditions(context).findConditions(block);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="RemoveRedundantCode Step">

    private static void removeRedundantCode(final Block method) {
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

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="ReduceBranchInstructionSet Step">

    private static void reduceBranchInstructionSet(final Block block) {
        final List<Node> body = block.getBody();

        for (int i = 0; i < body.size(); i++) {
            final Node node = body.get(i);

            if (!(node instanceof Expression)) {
                continue;
            }

            final Expression e = (Expression) node;
            final AstCode code;

            switch (e.getCode()) {
                case TableSwitch:
                case LookupSwitch: {
                    e.getArguments().get(0).getRanges().addAll(e.getRanges());
                    e.getRanges().clear();
                    continue;
                }

                case __LCmp:
                case __FCmpL:
                case __FCmpG:
                case __DCmpL:
                case __DCmpG: {
                    if (i == body.size() - 1 || !(body.get(i + 1) instanceof Expression)) {
                        continue;
                    }

                    final Expression next = (Expression) body.get(i + 1);

                    switch (next.getCode()) {
                        case __IfEq:
                            code = AstCode.CmpEq;
                            break;
                        case __IfNe:
                            code = AstCode.CmpNe;
                            break;
                        case __IfLt:
                            code = AstCode.CmpLt;
                            break;
                        case __IfGe:
                            code = AstCode.CmpGe;
                            break;
                        case __IfGt:
                            code = AstCode.CmpGt;
                            break;
                        case __IfLe:
                            code = AstCode.CmpLe;
                            break;
                        default:
                            continue;
                    }

                    body.remove(i);
                    break;
                }

/*
                case __IfEq:
                    code = AstCode.LogicalNot;
                    break;
                case __IfNe:
                    e.setCode(AstCode.IfTrue);
                    continue;
*/
                case __IfEq:
                    e.getArguments().add(new Expression(AstCode.LdC, 0));
                    code = AstCode.CmpEq;
                    break;

                case __IfNe:
                    e.getArguments().add(new Expression(AstCode.LdC, 0));
                    code = AstCode.CmpNe;
                    break;

                case __IfLt:
                    e.getArguments().add(new Expression(AstCode.LdC, 0));
                    code = AstCode.CmpLt;
                    break;
                case __IfGe:
                    e.getArguments().add(new Expression(AstCode.LdC, 0));
                    code = AstCode.CmpGe;
                    break;
                case __IfGt:
                    e.getArguments().add(new Expression(AstCode.LdC, 0));
                    code = AstCode.CmpGt;
                    break;
                case __IfLe:
                    e.getArguments().add(new Expression(AstCode.LdC, 0));
                    code = AstCode.CmpLe;
                    break;

                case __IfICmpEq:
                    code = AstCode.CmpEq;
                    break;
                case __IfICmpNe:
                    code = AstCode.CmpNe;
                    break;
                case __IfICmpLt:
                    code = AstCode.CmpLt;
                    break;
                case __IfICmpGe:
                    code = AstCode.CmpGe;
                    break;
                case __IfICmpGt:
                    code = AstCode.CmpGt;
                    break;
                case __IfICmpLe:
                    code = AstCode.CmpLe;
                    break;
                case __IfACmpEq:
                    code = AstCode.CmpEq;
                    break;
                case __IfACmpNe:
                    code = AstCode.CmpNe;
                    break;

                case __IfNull:
                    e.getArguments().add(new Expression(AstCode.AConstNull, null));
                    code = AstCode.CmpEq;
                    break;
                case __IfNonNull:
                    e.getArguments().add(new Expression(AstCode.AConstNull, null));
                    code = AstCode.CmpNe;
                    break;

                default:
                    continue;
            }

            final Expression newExpression = new Expression(code, null, e.getArguments());

            body.set(i, new Expression(AstCode.IfTrue, e.getOperand(), newExpression));
            newExpression.getRanges().addAll(e.getRanges());
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="SplitToMovableBlocks Step">

    private void splitToMovableBlocks(final Block block) {
        final List<Node> basicBlocks = new ArrayList<>();

        final List<Node> body = block.getBody();
        final Object firstNode = CollectionUtilities.firstOrDefault(body);

        final Label entryLabel;

        if (firstNode instanceof Label) {
            entryLabel = (Label) firstNode;
        }
        else {
            entryLabel = new Label();
            entryLabel.setName("Block_" + (_nextLabelIndex++));
        }

        BasicBlock basicBlock = new BasicBlock();
        List<Node> basicBlockBody = basicBlock.getBody();

        basicBlocks.add(basicBlock);
        basicBlockBody.add(entryLabel);

        block.setEntryGoto(new Expression(AstCode.Goto, entryLabel));

        if (!body.isEmpty()) {
            if (body.get(0) != entryLabel) {
                basicBlockBody.add(body.get(0));
            }

            for (int i = 1; i < body.size(); i++) {
                final Node lastNode = body.get(i - 1);
                final Node currentNode = body.get(i);

                //
                // Start a new basic block if necessary.
                //
                if (currentNode instanceof Label ||
                    currentNode instanceof TryCatchBlock ||
                    lastNode.isConditionalControlFlow() ||
                    lastNode.isUnconditionalControlFlow()) {

                    //
                    // Try to reuse the label.
                    //
                    final Label label = currentNode instanceof Label ? (Label) currentNode
                                                                     : new Label("Block_" + (_nextLabelIndex++));

                    //
                    // Terminate the last block.
                    //
                    if (!lastNode.isUnconditionalControlFlow()) {
                        basicBlockBody.add(new Expression(AstCode.Goto, label));
                    }

                    //
                    // Start the new block.
                    //
                    basicBlock = new BasicBlock();
                    basicBlocks.add(basicBlock);
                    basicBlockBody = basicBlock.getBody();
                    basicBlockBody.add(label);

                    //
                    // Add the node to the basic block.
                    //
                    if (currentNode != label) {
                        basicBlockBody.add(currentNode);
                    }
                }
                else {
                    basicBlockBody.add(currentNode);
                }
            }
        }

        body.clear();
        body.addAll(basicBlocks);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="SimplifyShortCircuit Optimization">

    private static final class SimplifyShortCircuitOptimization extends AbstractBasicBlockOptimization {
        public SimplifyShortCircuitOptimization(final DecompilerContext context, final Block method) {
            super(context, method);
        }

        @Override
        public final boolean run(final List<Node> body, final BasicBlock head, final int position) {
            assert body.contains(head);

            final StrongBox<Expression> condition = new StrongBox<>();
            final StrongBox<Label> trueLabel = new StrongBox<>();
            final StrongBox<Label> falseLabel = new StrongBox<>();

            if (matchLastAndBreak(head, AstCode.IfTrue, trueLabel, condition, falseLabel)) {
                for (int pass = 0; pass < 2; pass++) {
                    //
                    // On second pass, swap labels and negate expression of the first branch.
                    // It is slightly ugly, but much better than copy-pasting the whole block.
                    //

                    final Label nextLabel = pass == 0 ? trueLabel.get() : falseLabel.get();
                    final Label otherLabel = pass == 0 ? falseLabel.get() : trueLabel.get();
                    final boolean negate = pass == 1;

                    final BasicBlock nextBasicBlock = labelToBasicBlock.get(nextLabel);

                    final StrongBox<Expression> nextCondition = new StrongBox<>();
                    final StrongBox<Label> nextTrueLabel = new StrongBox<>();
                    final StrongBox<Label> nextFalseLabel = new StrongBox<>();

                    if (body.contains(nextBasicBlock) &&
                        nextBasicBlock != head &&
                        labelGlobalRefCount.get(nextBasicBlock.getBody().get(0)).getValue() == 1 &&
                        matchSingleAndBreak(nextBasicBlock, AstCode.IfTrue, nextTrueLabel, nextCondition, nextFalseLabel) &&
                        (otherLabel == nextFalseLabel.get() || otherLabel == nextTrueLabel.get())) {

                        //
                        // Create short circuit branch.
                        //
                        final Expression logicExpression;

                        if (otherLabel == nextFalseLabel.get()) {
                            logicExpression = matchLeftAssociativeShortCircuit(
                                AstCode.LogicalAnd,
                                negate ? new Expression(AstCode.LogicalNot, null, condition.get()) : condition.get(),
                                nextCondition.get()
                            );
                        }
                        else {
                            logicExpression = matchLeftAssociativeShortCircuit(
                                AstCode.LogicalOr,
                                negate ? condition.get() : new Expression(AstCode.LogicalNot, null, condition.get()),
                                nextCondition.get()
                            );
                        }

                        final List<Node> headBody = head.getBody();

                        removeTail(headBody, AstCode.IfTrue, AstCode.Goto);

                        headBody.add(new Expression(AstCode.IfTrue, nextTrueLabel.get(), logicExpression));
                        headBody.add(new Expression(AstCode.Goto, nextFalseLabel.get()));

                        //
                        // Remove the inlined branch from scope.
                        //
                        removeOrThrow(body, nextBasicBlock);

                        return true;
                    }
                }
            }

            return false;
        }

        private Expression matchLeftAssociativeShortCircuit(final AstCode code, final Expression left, final Expression right) {
            //
            // Assuming that the inputs are already left-associative.
            //
            if (match(right, code)) {
                //
                // Find the leftmost logical expression.
                //
                Expression current = right;

                while (match(current.getArguments().get(0), code)) {
                    current = current.getArguments().get(0);
                }

                final Expression newArgument = new Expression(code, null, left, current.getArguments().get(0));

                newArgument.setInferredType(BuiltinTypes.Boolean);
                current.getArguments().set(0, newArgument);

                return right;
            }
            else {
                final Expression newExpression = new Expression(code, null, left, right);
                newExpression.setInferredType(BuiltinTypes.Boolean);
                return newExpression;
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="JoinBasicBlocks Optimization">

    private final static class JoinBasicBlocksOptimization extends AbstractBasicBlockOptimization {
        protected JoinBasicBlocksOptimization(final DecompilerContext context, final Block method) {
            super(context, method);
        }

        @Override
        public final boolean run(final List<Node> body, final BasicBlock head, final int position) {
            final StrongBox<Label> nextLabel = new StrongBox<>();
            final List<Node> headBody = head.getBody();
            final BasicBlock nextBlock;

            final Node secondToLast = CollectionUtilities.getOrDefault(headBody, headBody.size() - 2);

            if (secondToLast != null &&
                !secondToLast.isConditionalControlFlow() &&
                matchGetOperand(headBody.get(headBody.size() - 1), AstCode.Goto, nextLabel) &&
                labelGlobalRefCount.get(nextLabel.get()).getValue() == 1 &
                (nextBlock = labelToBasicBlock.get(nextLabel.get())) != null &&
                body.contains(nextBlock) &&
                nextBlock.getBody().get(0) == nextLabel.get() &&
                !CollectionUtilities.any(nextBlock.getBody(), Predicates.instanceOf(BasicBlock.class))) {

                removeTail(headBody, AstCode.Goto);
                nextBlock.getBody().remove(0);
                headBody.addAll(nextBlock.getBody());
                removeOrThrow(body, nextBlock);
                return true;
            }

            return false;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Optimization Helpers">

    private interface BasicBlockOptimization {
        boolean run(final List<Node> body, final BasicBlock head, final int position);
    }

    private interface ExpressionOptimization {
        boolean run(final List<Node> body, final Expression head, final int position);
    }

    @SuppressWarnings("ProtectedField")
    private static abstract class AbstractBasicBlockOptimization implements BasicBlockOptimization {
        protected final Map<Label, MutableInteger> labelGlobalRefCount = new DefaultMap<>(
            new Func<MutableInteger>() {
                @Override
                public MutableInteger invoke() {
                    return new MutableInteger(0);
                }
            }
        );

        protected final Map<Label, BasicBlock> labelToBasicBlock = new IdentityHashMap<>();

        protected final DecompilerContext context;
        protected final MetadataSystem metadataSystem;

        protected AbstractBasicBlockOptimization(final DecompilerContext context, final Block method) {
            this.context = VerifyArgument.notNull(context, "context");
            this.metadataSystem = MetadataSystem.instance();

            for (final Expression e : method.getSelfAndChildrenRecursive(Expression.class)) {
                if (e.isBranch()) {
                    for (final Label target : e.getBranchTargets()) {
                        labelGlobalRefCount.get(target).increment();
                    }
                }
            }

            for (final BasicBlock basicBlock : method.getSelfAndChildrenRecursive(BasicBlock.class)) {
                for (final Node child : basicBlock.getChildren()) {
                    if (child instanceof Label) {
                        labelToBasicBlock.put((Label) child, basicBlock);
                    }
                }
            }
        }
    }

    private static boolean runOptimization(final Block block, final BasicBlockOptimization optimization) {
        boolean modified = false;

        final List<Node> body = block.getBody();

        for (int i = body.size() - 1; i >= 0; i--) {
            if (i < body.size() && optimization.run(body, (BasicBlock) body.get(i), i)) {
                modified = true;
            }
        }

        return modified;
    }

    private static boolean runOptimization(final Block block, final ExpressionOptimization optimization) {
        boolean modified = false;

        for (final Node node : block.getBody()) {
            final BasicBlock basicBlock = (BasicBlock) node;
            final List<Node> body = basicBlock.getBody();

            for (int i = body.size() - 1; i >= 0; i--) {
                final Node n = body.get(i);
                if (n instanceof Expression && optimization.run(body, (Expression) n, i)) {
                    modified = true;
                }
            }
        }

        return modified;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Utility Methods">

    static <T> void removeOrThrow(final Collection<T> collection, final T item) {
        if (!collection.remove(item)) {
            throw new IllegalStateException("The item was not found in the collection.");
        }
    }

    static void removeTail(final List<Node> body, final AstCode... codes) {
        for (int i = 0; i < codes.length; i++) {
            if (((Expression) body.get(body.size() - codes.length + i)).getCode() != codes[i]) {
                throw new IllegalStateException("Tailing code does not match expected.");
            }
        }

        for (int i = 0; i < codes.length; i++) {
            body.remove(body.size() - 1);
        }
    }

    // </editor-fold>
}
