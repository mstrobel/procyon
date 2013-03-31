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
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.assembler.metadata.VariableDefinition;
import com.strobel.core.BooleanBox;
import com.strobel.core.CollectionUtilities;
import com.strobel.core.MutableInteger;
import com.strobel.core.Predicate;
import com.strobel.core.Predicates;
import com.strobel.core.StrongBox;
import com.strobel.core.VerifyArgument;
import com.strobel.core.delegates.Func;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.functions.Function;
import com.strobel.reflection.SimpleType;
import com.strobel.util.ContractUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.strobel.core.CollectionUtilities.*;
import static com.strobel.decompiler.ast.PatternMatching.*;

@SuppressWarnings("ConstantConditions")
public final class AstOptimizer {
    private int _nextLabelIndex;

    public static void optimize(final DecompilerContext context, final Block method) {
        optimize(context, method, context.getSettings().getAbortBeforeStep());
    }

    public static void optimize(final DecompilerContext context, final Block method, final AstOptimizationStep abortBeforeStep) {
        VerifyArgument.notNull(context, "context");
        VerifyArgument.notNull(method, "method");

        if (abortBeforeStep == AstOptimizationStep.RemoveRedundantCode) {
            return;
        }

        final AstOptimizer optimizer = new AstOptimizer();

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

                if (abortBeforeStep == AstOptimizationStep.SimplifyTernaryOperator) {
                    continue;
                }

                modified |= runOptimization(block, new SimplifyTernaryOperatorOptimization(context, method));

                if (abortBeforeStep == AstOptimizationStep.SimplifyLogicalNot) {
                    continue;
                }

                modified |= runOptimization(block, new SimplifyLogicalNotOptimization(context, method));

                if (abortBeforeStep == AstOptimizationStep.TransformObjectInitializers) {
                    continue;
                }

                modified |= runOptimization(block, new TransformObjectInitializersOptimization(context, method));

                if (abortBeforeStep == AstOptimizationStep.TransformArrayInitializers) {
                    continue;
                }

                modified |= runOptimization(block, new TransformArrayInitializersOptimization(context, method));

                if (abortBeforeStep == AstOptimizationStep.MakeAssignmentExpressions) {
                    continue;
                }

                modified |= runOptimization(block, new MakeAssignmentExpressionsOptimization(context, method));

                if (abortBeforeStep == AstOptimizationStep.InlineVariables2) {
                    continue;
                }

                modified |= new Inlining(method).inlineAllInBlock(block);
                new Inlining(method).copyPropagation();
            }
            while (modified);
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

        if (abortBeforeStep == AstOptimizationStep.FlattenNestedMovableBlocks) {
            return;
        }

        flattenBasicBlocks(method);

        if (abortBeforeStep == AstOptimizationStep.RemoveRedundantCode2) {
            return;
        }

        removeRedundantCode(method);

        if (abortBeforeStep == AstOptimizationStep.GotoRemoval) {
            return;
        }

        new GotoRemoval().removeGotos(method);

        if (abortBeforeStep == AstOptimizationStep.DuplicateReturns) {
            return;
        }

        duplicateReturnStatements(method);

        if (abortBeforeStep == AstOptimizationStep.GotoRemoval2) {
            return;
        }

        new GotoRemoval().removeGotos(method);

        if (abortBeforeStep == AstOptimizationStep.ReduceIfNesting) {
            return;
        }

        reduceIfNesting(method);

        if (abortBeforeStep == AstOptimizationStep.ReduceComparisonInstructionSet) {
            return;
        }

        for (final Expression e : method.getChildrenAndSelfRecursive(Expression.class)) {
            reduceComparisonInstructionSet(e);
        }

        if (abortBeforeStep == AstOptimizationStep.RecombineVariables) {
            return;
        }

        recombineVariables(method);

        if (abortBeforeStep == AstOptimizationStep.InlineVariables3) {
            return;
        }

        //
        // The second inlining pass is necessary because the DuplicateReturns step and the
        // introduction of ternary operators may open up additional inlining possibilities.
        //

        final Inlining inliningPhase3 = new Inlining(method);

        inliningPhase3.inlineAllVariables();

        if (abortBeforeStep == AstOptimizationStep.TypeInference2) {
            return;
        }

        TypeAnalysis.reset(method);
        TypeAnalysis.run(context, method);

        if (abortBeforeStep == AstOptimizationStep.RemoveRedundantCode3) {
            return;
        }

        GotoRemoval.removeRedundantCode(method);
    }

    // <editor-fold defaultstate="collapsed" desc="RemoveRedundantCode Step">

    @SuppressWarnings("ConstantConditions")
    static void removeRedundantCode(final Block method) {
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

        for (final Expression e : method.getSelfAndChildrenRecursive(Expression.class)) {
            final List<Expression> arguments = e.getArguments();

            for (int i = 0, n = arguments.size(); i < n; i++) {
                final Expression argument = arguments.get(i);

                switch (argument.getCode()) {
                    case Dup:
                    case Dup2:
                    case DupX1:
                    case DupX2:
                    case Dup2X1:
                    case Dup2X2:
                        final Expression firstArgument = argument.getArguments().get(0);
                        firstArgument.getRanges().addAll(argument.getRanges());
                        arguments.set(i, firstArgument);
                        break;
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

    // <editor-fold defaultstate="collapsed" desc="ReduceComparisonInstructionSet Step">

    private static void reduceComparisonInstructionSet(final Expression expression) {
        final List<Expression> arguments = expression.getArguments();
        final Expression firstArgument = arguments.isEmpty() ? null : arguments.get(0);

        if (matchSimplifiableComparison(expression)) {
            arguments.clear();
            arguments.addAll(firstArgument.getArguments());
            expression.getRanges().addAll(firstArgument.getRanges());
        }

        if (matchReversibleComparison(expression)) {
            final AstCode reversedCode;

            switch (firstArgument.getCode()) {
                case CmpEq:
                    reversedCode = AstCode.CmpNe;
                    break;
                case CmpNe:
                    reversedCode = AstCode.CmpEq;
                    break;
                case CmpLt:
                    reversedCode = AstCode.CmpGe;
                    break;
                case CmpGe:
                    reversedCode = AstCode.CmpLt;
                    break;
                case CmpGt:
                    reversedCode = AstCode.CmpLe;
                    break;
                case CmpLe:
                    reversedCode = AstCode.CmpGt;
                    break;

                default:
                    throw ContractUtils.unreachable();
            }

            expression.setCode(reversedCode);
            expression.getRanges().addAll(firstArgument.getRanges());

            arguments.clear();
            arguments.addAll(firstArgument.getArguments());
        }
    }

    // </editor-fold>

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="SplitToMovableBlocks Step">

    private void splitToMovableBlocks(final Block block) {
        final List<Node> basicBlocks = new ArrayList<>();

        final List<Node> body = block.getBody();
        final Object firstNode = firstOrDefault(body);

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

                    //noinspection SuspiciousMethodCalls
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
                            logicExpression = makeLeftAssociativeShortCircuit(
                                AstCode.LogicalAnd,
                                negate ? new Expression(AstCode.LogicalNot, null, condition.get()) : condition.get(),
                                nextCondition.get()
                            );
                        }
                        else {
                            logicExpression = makeLeftAssociativeShortCircuit(
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

    // <editor-fold defaultstate="collapsed" desc="SimplifyTernaryOperator Optimization">

    private final static class SimplifyTernaryOperatorOptimization extends AbstractBasicBlockOptimization {
        protected SimplifyTernaryOperatorOptimization(final DecompilerContext context, final Block method) {
            super(context, method);
        }

        @Override
        public final boolean run(final List<Node> body, final BasicBlock head, final int position) {
            final StrongBox<Expression> condition = new StrongBox<>();
            final StrongBox<Label> trueLabel = new StrongBox<>();
            final StrongBox<Label> falseLabel = new StrongBox<>();
            final StrongBox<Variable> trueVariable = new StrongBox<>();
            final StrongBox<Expression> trueExpression = new StrongBox<>();
            final StrongBox<Label> trueFall = new StrongBox<>();
            final StrongBox<Variable> falseVariable = new StrongBox<>();
            final StrongBox<Expression> falseExpression = new StrongBox<>();
            final StrongBox<Label> falseFall = new StrongBox<>();
            final StrongBox<Object> unused = new StrongBox<>();

            if (matchLastAndBreak(head, AstCode.IfTrue, trueLabel, condition, falseLabel) &&
                labelGlobalRefCount.get(trueLabel.get()).getValue() == 1 &&
                labelGlobalRefCount.get(falseLabel.get()).getValue() == 1 &&
                ((matchSingleAndBreak(labelToBasicBlock.get(trueLabel.get()), AstCode.Store, trueVariable, trueExpression, trueFall) &&
                  matchSingleAndBreak(labelToBasicBlock.get(falseLabel.get()), AstCode.Store, falseVariable, falseExpression, falseFall) &&
                  trueVariable.get() == falseVariable.get() &&
                  trueFall.get() == falseFall.get()) ||
                 (matchSingle(labelToBasicBlock.get(trueLabel.get()), AstCode.Return, unused, trueExpression) &&
                  matchSingle(labelToBasicBlock.get(falseLabel.get()), AstCode.Return, unused, falseExpression))) &&
                body.contains(labelToBasicBlock.get(trueLabel.get())) &&
                body.contains(labelToBasicBlock.get(falseLabel.get()))) {

                final boolean isStore = trueVariable.get() != null;
                final AstCode opCode = isStore ? AstCode.Store : AstCode.Return;
                final TypeReference returnType = isStore ? trueVariable.get().getType() : context.getCurrentMethod().getReturnType();

                final boolean returnTypeIsBoolean = TypeAnalysis.isBoolean(returnType) ||
                                                    isStore && trueVariable.get().getType().getSimpleType() == SimpleType.Integer;

                final StrongBox<Integer> leftBooleanValue = new StrongBox<>();
                final StrongBox<Integer> rightBooleanValue = new StrongBox<>();

                final Expression newExpression;

                // a ? true:false  is equivalent to  a
                // a ? false:true  is equivalent to  !a
                // a ? true : b    is equivalent to  a || b
                // a ? b : true    is equivalent to  !a || b
                // a ? b : false   is equivalent to  a && b
                // a ? false : b   is equivalent to  !a && b

                if (returnTypeIsBoolean &&
                    matchGetOperand(trueExpression.get(), AstCode.LdC, Integer.class, leftBooleanValue) &&
                    matchGetOperand(falseExpression.get(), AstCode.LdC, Integer.class, rightBooleanValue) &&
                    (leftBooleanValue.get() != 0 && rightBooleanValue.get() == 0 ||
                     leftBooleanValue.get() == 0 && rightBooleanValue.get() != 0)) {

                    //
                    // It can be expressed as a trivial expression.
                    //
                    if (leftBooleanValue.get() != 0) {
                        newExpression = condition.get();
                    }
                    else {
                        newExpression = new Expression(AstCode.LogicalNot, null, condition.get());
                        newExpression.setInferredType(BuiltinTypes.Boolean);
                    }
                }
                else if ((returnTypeIsBoolean || TypeAnalysis.isBoolean(falseExpression.get().getInferredType())) &&
                         matchGetOperand(trueExpression.get(), AstCode.LdC, Integer.class, leftBooleanValue) &&
                         (leftBooleanValue.get() == 0 || leftBooleanValue.get() == 1)) {

                    //
                    // It can be expressed as a logical expression.
                    //
                    if (leftBooleanValue.get() != 0) {
                        newExpression = makeLeftAssociativeShortCircuit(
                            AstCode.LogicalOr,
                            condition.get(),
                            falseExpression.get()
                        );
                    }
                    else {
                        newExpression = makeLeftAssociativeShortCircuit(
                            AstCode.LogicalAnd,
                            new Expression(AstCode.LogicalNot, null, condition.get()),
                            falseExpression.get()
                        );
                    }
                }
                else if ((returnTypeIsBoolean || TypeAnalysis.isBoolean(trueExpression.get().getInferredType())) &&
                         matchGetOperand(falseExpression.get(), AstCode.LdC, Integer.class, rightBooleanValue) &&
                         (rightBooleanValue.get() == 0 || rightBooleanValue.get() == 1)) {

                    //
                    // It can be expressed as a logical expression.
                    //
                    if (rightBooleanValue.get() != 0) {
                        newExpression = makeLeftAssociativeShortCircuit(
                            AstCode.LogicalOr,
                            new Expression(AstCode.LogicalNot, null, condition.get()),
                            trueExpression.get()
                        );
                    }
                    else {
                        newExpression = makeLeftAssociativeShortCircuit(
                            AstCode.LogicalAnd,
                            condition.get(),
                            trueExpression.get()
                        );
                    }
                }
                else {
                    //
                    // Ternary operator tends to create long, complicated return statements.
                    //
                    if (opCode == AstCode.Return) {
                        return false;
                    }

                    //
                    // Only simplify generated variables.
                    //
                    if (opCode == AstCode.Store && !trueVariable.get().isGenerated()) {
                        return false;
                    }

                    //
                    // Create ternary expression.
                    //
                    newExpression = new Expression(
                        AstCode.TernaryOp,
                        null,
                        condition.get(),
                        trueExpression.get(),
                        falseExpression.get()
                    );
                }

                final List<Node> headBody = head.getBody();

                removeTail(headBody, AstCode.IfTrue, AstCode.Goto);
                headBody.add(new Expression(opCode, trueVariable.get(), newExpression));

                if (isStore) {
                    headBody.add(new Expression(AstCode.Goto, trueFall.get()));
                }

                //
                // Remove the old basic blocks.
                //

                removeOrThrow(body, labelToBasicBlock.get(trueLabel.get()));
                removeOrThrow(body, labelToBasicBlock.get(falseLabel.get()));

                return true;
            }

            return false;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="SimplifyLogicalNot Optimization">

    private final static class SimplifyLogicalNotOptimization extends AbstractExpressionOptimization {
        protected SimplifyLogicalNotOptimization(final DecompilerContext context, final Block method) {
            super(context, method);
        }

        @Override
        public final boolean run(final List<Node> body, final Expression head, final int position) {
            final BooleanBox modified = new BooleanBox();
            final Expression simplified = simplifyLogicalNot(head, modified);

            assert simplified == null;

            return modified.get();
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="TransformArrayInitializers Step">

    private final static class TransformObjectInitializersOptimization extends AbstractExpressionOptimization {
        protected TransformObjectInitializersOptimization(final DecompilerContext context, final Block method) {
            super(context, method);
        }

        @Override
        public boolean run(final List<Node> body, final Expression head, final int position) {
            if (position >= body.size() - 1) {
                return false;
            }

            final StrongBox<Variable> v = new StrongBox<>();
            final StrongBox<Expression> newObject = new StrongBox<>();
            final StrongBox<TypeReference> objectType = new StrongBox<>();

            if (position < body.size() - 1 &&
                matchGetArgument(head, AstCode.Store, v, newObject) &&
                matchGetOperand(newObject.get(), AstCode.__New, objectType)) {

                final Node next = body.get(position + 1);
                final StrongBox<MethodReference> constructor = new StrongBox<>();
                final List<Expression> arguments = new ArrayList<>();
                final StrongBox<Variable> v2 = new StrongBox<>();

                if (matchGetArguments(next, AstCode.InvokeSpecial, constructor, arguments) &&
                    !arguments.isEmpty() &&
                    matchGetOperand(arguments.get(0), AstCode.Load, v2) &&
                    v2.get() == v.get()) {

                    final Expression initExpression = new Expression(AstCode.InitObject, constructor.get());

                    arguments.remove(0);
                    initExpression.getArguments().addAll(arguments);
                    initExpression.getRanges().addAll(((Expression) next).getRanges());
                    head.getArguments().set(0, initExpression);
                    body.remove(position + 1);

                    return true;
                }
            }

            return false;
        }
    }

    private final static class TransformArrayInitializersOptimization extends AbstractExpressionOptimization {
        protected TransformArrayInitializersOptimization(final DecompilerContext context, final Block method) {
            super(context, method);
        }

        @Override
        public boolean run(final List<Node> body, final Expression head, final int position) {
            final StrongBox<Variable> v = new StrongBox<>();
            final StrongBox<Variable> v3 = new StrongBox<>();
            final StrongBox<Expression> newArray = new StrongBox<>();
            final StrongBox<TypeReference> elementType = new StrongBox<>();
            final StrongBox<Expression> lengthExpression = new StrongBox<>();
            final StrongBox<Number> arrayLength = new StrongBox<>();

            if (matchGetArgument(head, AstCode.Store, v, newArray) &&
                matchGetArgument(newArray.get(), AstCode.NewArray, elementType, lengthExpression) &&
                matchGetOperand(lengthExpression.get(), AstCode.LdC, Number.class, arrayLength) &&
                arrayLength.get().intValue() > 0) {

                final StrongBox<Number> arrayPosition = new StrongBox<>();
                final List<Expression> initializers = new ArrayList<>();

                int instructionsToRemove = 0;

                for (int j = position + 1; j < body.size(); j++) {
                    final Node node = body.get(j);

                    if (!(node instanceof Expression)) {
                        continue;
                    }

                    final Expression next = (Expression) node;

                    if (next.getCode() == AstCode.StoreElement &&
                        matchGetOperand(next.getArguments().get(0), AstCode.Load, v3) &&
                        v3.get() == v.get() &&
                        matchGetOperand(next.getArguments().get(1), AstCode.LdC, Number.class, arrayPosition) &&
                        arrayPosition.get().intValue() >= initializers.size() &&
                        !next.getArguments().get(2).containsReferenceTo(v3.get())) {

                        while (initializers.size() < arrayPosition.get().intValue()) {
                            initializers.add(new Expression(AstCode.DefaultValue, elementType));
                        }

                        initializers.add(next.getArguments().get(2));
                        instructionsToRemove++;
                    }
                    else {
                        break;
                    }
                }

                if (initializers.size() == arrayLength.get().intValue()) {
                    final TypeReference arrayType = elementType.get().makeArrayType();

                    head.getArguments().set(0, new Expression(AstCode.InitArray, arrayType, initializers));

                    for (int i = 0; i < instructionsToRemove; i++) {
                        body.remove(position + 1);
                    }

                    new Inlining(method).inlineIfPossible(body, new MutableInteger(position));
                    return true;
                }
            }

            return false;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="MakeAssignmentExpressions Optimization">

    private final static class MakeAssignmentExpressionsOptimization extends AbstractExpressionOptimization {
        protected MakeAssignmentExpressionsOptimization(final DecompilerContext context, final Block method) {
            super(context, method);
        }

        @Override
        public boolean run(final List<Node> body, final Expression head, final int position) {
            //
            // ev = ...; store(v, ev) => ev = store(v, ...)
            //

            final StrongBox<Variable> ev = new StrongBox<>();
            final StrongBox<Expression> initializer = new StrongBox<>();

            if (!matchGetArgument(head, AstCode.Store, ev, initializer)/* || !ev.get().isGenerated()*/) {
                return false;
            }

            final Node next = getOrDefault(body, position + 1);
            final StrongBox<Variable> v = new StrongBox<>();
            final StrongBox<Expression> storeArgument = new StrongBox<>();

            if (matchGetArgument(next, AstCode.Store, v, storeArgument) &&
                matchLoad(storeArgument.get(), ev.get())) {

                final Expression nextExpression = (Expression) next;
                final Node store2 = getOrDefault(body, position + 2);

                if (canConvertStoreToAssignment(store2, ev.get())) {
                    //
                    // e = ...; store(v1, e); anyStore(v2, e) => store(v1, anyStore(v2, ...)
                    //

                    final Inlining inlining = new Inlining(method);
                    final MutableInteger loadCounts = inlining.loadCounts.get(ev.get());
                    final MutableInteger storeCounts = inlining.storeCounts.get(ev.get());

                    if (loadCounts != null &&
                        loadCounts.getValue() == 2 &&
                        storeCounts != null &&
                        storeCounts.getValue() == 1) {

                        final Expression storeExpression = (Expression) store2;

                        body.remove(position + 2);  // remove store2
                        body.remove(position);      // remove ev = ...

                        nextExpression.getArguments().set(0, storeExpression);
                        storeExpression.getArguments().set(storeExpression.getArguments().size() - 1, initializer.get());

                        inlining.inlineIfPossible(body, new MutableInteger(position));

                        return true;
                    }
                }

                body.remove(position + 1);  // remove store

                nextExpression.getArguments().set(0, initializer.get());
                ((Expression) body.get(position)).getArguments().set(0, nextExpression);

                return true;
            }

            if (match(next, AstCode.PutStatic)) {
                final Expression nextExpression = (Expression) next;

                //
                // ev = ...; putstatic(f, ev) => ev = putstatic(f, ...)
                //

                if (matchLoad(nextExpression.getArguments().get(0), ev.get())) {
                    body.remove(position + 1);  // remove putstatic

                    nextExpression.getArguments().set(0, initializer.get());
                    ((Expression) body.get(position)).getArguments().set(0, nextExpression);

                    return true;
                }
            }

            return false;
        }

        private boolean canConvertStoreToAssignment(final Node store, final Variable variable) {
            if (store instanceof Expression) {
                final Expression storeExpression = (Expression) store;

                switch (storeExpression.getCode()) {
                    case Store:
                    case PutStatic:
                    case PutField:
                    case StoreElement:
                        return matchLoad(lastOrDefault(storeExpression.getArguments()), variable);
                }
            }

            return false;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="FlattenBasicBlocks Step">

    private static void flattenBasicBlocks(final Node node) {
        if (node instanceof Block) {
            final Block block = (Block) node;
            final List<Node> flatBody = new ArrayList<>();

            for (final Node child : block.getChildren()) {
                flattenBasicBlocks(child);

                if (child instanceof BasicBlock) {
                    final BasicBlock childBasicBlock = (BasicBlock) child;
                    final Node firstChild = firstOrDefault(childBasicBlock.getBody());
                    final Node lastChild = lastOrDefault(childBasicBlock.getBody());

                    if (!(firstChild instanceof Label)) {
                        throw new IllegalStateException("Basic block must start with a label.");
                    }

                    if (lastChild instanceof Expression && !lastChild.isUnconditionalControlFlow()) {
                        throw new IllegalStateException("Basic block must end with an unconditional branch.");
                    }

                    flatBody.addAll(childBasicBlock.getBody());
                }
                else {
                    flatBody.add(child);
                }
            }

            block.setEntryGoto(null);
            block.getBody().clear();
            block.getBody().addAll(flatBody);
        }
        else if (node instanceof Expression) {
            //
            // Optimization: no need to check expressions.
            //
        }
        else if (node != null) {
            for (final Node child : node.getChildren()) {
                flattenBasicBlocks(child);
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="DuplicateReturns Step">

    private static void duplicateReturnStatements(final Block method) {
        final List<Node> methodBody = method.getBody();
        final Map<Node, Node> nextSibling = new IdentityHashMap<>();
        final StrongBox<Object> constant = new StrongBox<>();
        final StrongBox<Variable> localVariable = new StrongBox<>();
        final StrongBox<Label> targetLabel = new StrongBox<>();
        final List<Expression> returnArguments = new ArrayList<>();

        //
        // Build navigation data.
        //
        for (final Block block : method.getSelfAndChildrenRecursive(Block.class)) {
            final List<Node> body = block.getBody();

            for (int i = 0; i < body.size() - 1; i++) {
                final Node current = body.get(i);

                if (current instanceof Label) {
                    nextSibling.put(current, body.get(i + 1));
                }
            }
        }

        //
        // Duplicate returns.
        //
        for (final Block block : method.getSelfAndChildrenRecursive(Block.class)) {
            final List<Node> body = block.getBody();

            for (int i = 0; i < body.size(); i++) {
                final Node node = body.get(i);

                if (matchGetOperand(node, AstCode.Goto, targetLabel)) {
                    //
                    // Skip extra labels.
                    //
                    while (nextSibling.get(targetLabel.get()) instanceof Label) {
                        targetLabel.accept((Label) nextSibling.get(targetLabel.get()));
                    }

                    //
                    // Inline return statement.
                    //
                    final Node target = nextSibling.get(targetLabel.get());

                    if (target != null &&
                        matchGetArguments(target, AstCode.Return, returnArguments)) {

                        if (returnArguments.isEmpty()) {
                            body.set(
                                i,
                                new Expression(AstCode.Return, null)
                            );
                        }
                        else if (matchGetOperand(returnArguments.get(0), AstCode.Load, localVariable)) {
                            body.set(
                                i,
                                new Expression(AstCode.Return, null, new Expression(AstCode.Load, localVariable.get()))
                            );
                        }
                        else if (matchGetOperand(returnArguments.get(0), AstCode.LdC, constant)) {
                            body.set(
                                i,
                                new Expression(AstCode.Return, null, new Expression(AstCode.LdC, constant.get()))
                            );
                        }
                    }
                    else if (!methodBody.isEmpty() && methodBody.get(methodBody.size() - 1) == targetLabel.get()) {
                        //
                        // It exits the main method, so it is effectively a return.
                        //
                        body.set(i, new Expression(AstCode.Return, null));
                    }
                }
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="ReduceIfNesting Step">

    private static void reduceIfNesting(final Node node) {
        if (node instanceof Block) {
            final Block block = (Block) node;
            final List<Node> blockBody = block.getBody();

            for (int i = 0; i < blockBody.size(); i++) {
                final Node n = blockBody.get(i);

                if (!(n instanceof Condition)) {
                    continue;
                }

                final Condition condition = (Condition) n;

                final Node trueEnd = lastOrDefault(condition.getTrueBlock().getBody());
                final Node falseEnd = lastOrDefault(condition.getFalseBlock().getBody());

                final boolean trueExits = trueEnd != null && trueEnd.isUnconditionalControlFlow();
                final boolean falseExits = falseEnd != null && falseEnd.isUnconditionalControlFlow();

                if (trueExits) {
                    //
                    // Move the false block after the condition.
                    //
                    blockBody.addAll(i + 1, condition.getFalseBlock().getChildren());
                    condition.setFalseBlock(new Block());
                }
                else if (falseExits) {
                    //
                    // Move the true block after the condition.
                    //
                    blockBody.addAll(i + 1, condition.getTrueBlock().getChildren());
                    condition.setTrueBlock(new Block());
                }

                //
                // Eliminate empty true block.
                //
                if (condition.getTrueBlock().getChildren().isEmpty() && !condition.getFalseBlock().getChildren().isEmpty()) {
                    final Block temp = condition.getTrueBlock();
                    final Expression conditionExpression = condition.getCondition();

                    condition.setTrueBlock(condition.getFalseBlock());
                    condition.setFalseBlock(temp);

                    if (conditionExpression.getCode() == AstCode.LogicalNot) {
                        condition.setCondition(conditionExpression.getArguments().get(0));
                    }
                    else {
                        final Expression notExpression = new Expression(AstCode.LogicalNot, null, conditionExpression);

                        if (simplifyLogicalNotArgument(notExpression)) {
                            condition.setCondition(notExpression.getArguments().get(0));
                        }
                        else {
                            condition.setCondition(notExpression);
                        }
                    }
                }
            }
        }

        for (final Node child : node.getChildren()) {
            if (child != null && !(child instanceof Expression)) {
                reduceIfNesting(child);
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="RecombineVariables Step">

    private static void recombineVariables(final Block method) {
        final Map<VariableDefinition, Variable> map = new IdentityHashMap<>();

        replaceVariables(
            method,
            new Function<Variable, Variable>() {
                @Override
                public final Variable apply(final Variable v) {
                    final VariableDefinition originalVariable = v.getOriginalVariable();

                    if (originalVariable == null) {
                        return v;
                    }

                    Variable combinedVariable = map.get(originalVariable);

                    if (combinedVariable == null) {
                        map.put(originalVariable, v);
                        combinedVariable = v;
                    }

                    return combinedVariable;
                }
            }
        );
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
        protected final Block method;

        protected AbstractBasicBlockOptimization(final DecompilerContext context, final Block method) {
            this.context = VerifyArgument.notNull(context, "context");
            this.metadataSystem = MetadataSystem.instance();
            this.method = VerifyArgument.notNull(method, "method");

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

    @SuppressWarnings("ProtectedField")
    private static abstract class AbstractExpressionOptimization implements ExpressionOptimization {
        protected final DecompilerContext context;
        protected final MetadataSystem metadataSystem;
        protected final Block method;

        protected AbstractExpressionOptimization(final DecompilerContext context, final Block method) {
            this.context = VerifyArgument.notNull(context, "context");
            this.metadataSystem = MetadataSystem.instance();
            this.method = VerifyArgument.notNull(method, "method");
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
                if (i >= body.size()) {
                    continue;
                }

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

    public static void replaceVariables(final Node node, final Function<Variable, Variable> mapping) {
        if (node instanceof Expression) {
            final Expression expression = (Expression) node;
            final Object operand = expression.getOperand();

            if (operand instanceof Variable) {
                expression.setOperand(mapping.apply((Variable) operand));
            }

            for (final Expression argument : expression.getArguments()) {
                replaceVariables(argument, mapping);
            }
        }
        else {
            if (node instanceof CatchBlock) {
                final CatchBlock catchBlock = (CatchBlock) node;
                final Variable exceptionVariable = catchBlock.getExceptionVariable();

                if (exceptionVariable != null) {
                    catchBlock.setExceptionVariable(mapping.apply(exceptionVariable));
                }
            }

            for (final Node child : node.getChildren()) {
                replaceVariables(child, mapping);
            }
        }
    }

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

        //noinspection UnusedDeclaration
        for (final AstCode code : codes) {
            body.remove(body.size() - 1);
        }
    }

    static Expression makeLeftAssociativeShortCircuit(final AstCode code, final Expression left, final Expression right) {
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

    static Expression simplifyLogicalNot(final Expression expression, final BooleanBox modified) {
        Expression a;
        Expression e = expression;

        //
        // CmpEq(a, ldc, 0) becomes LogicalNot(a) if the inferred type for expression 'a' is boolean.
        //

        List<Expression> arguments = e.getArguments();

        final Expression operand = arguments.isEmpty() ? null : arguments.get(0);

        if (e.getCode() == AstCode.CmpEq &&
            TypeAnalysis.isBoolean(operand.getInferredType()) &&
            (a = arguments.get(1)).getCode() == AstCode.LdC &&
            ((Number) a.getOperand()).intValue() == 0) {

            e.setCode(AstCode.LogicalNot);
            e.getRanges().addAll(a.getRanges());

            arguments.remove(1);
            modified.set(true);
        }

        Expression result = null;

        if (e.getCode() == AstCode.CmpNe &&
            TypeAnalysis.isBoolean(operand.getInferredType()) &&
            (a = arguments.get(1)).getCode() == AstCode.LdC &&
            ((Number) a.getOperand()).intValue() == 0) {

            modified.set(true);
            return e.getArguments().get(0);
        }

        while (e.getCode() == AstCode.LogicalNot) {
            a = operand;

            //
            // Remove double negation.
            //
            if (a.getCode() == AstCode.LogicalNot) {
                result = a.getArguments().get(0);
                result.getRanges().addAll(e.getRanges());
                result.getRanges().addAll(a.getRanges());
                e = result;
                arguments = e.getArguments();
            }
            else {
                if (simplifyLogicalNotArgument(e)) {
                    result = e = a;
                    arguments = e.getArguments();
                }
                break;
            }
        }

        for (int i = 0; i < arguments.size(); i++) {
            a = simplifyLogicalNot(arguments.get(i), modified);

            if (a != null) {
                arguments.set(i, a);
                modified.set(true);
            }
        }

        return result;
    }

    private static boolean simplifyLogicalNotArgument(final Expression e) {
        final Expression a = e.getArguments().get(0);

        final AstCode c;

        switch (a.getCode()) {
            case CmpEq:
            case CmpNe:
            case CmpLt:
            case CmpGe:
            case CmpGt:
            case CmpLe:
                c = a.getCode().reverse();
                break;

            case LogicalAnd:
            case LogicalOr:
                final List<Expression> arguments = a.getArguments();

                if (arguments.get(0).getCode().isComparison() &&
                    arguments.get(1).getCode().isComparison()) {

                    arguments.get(0).setCode(arguments.get(0).getCode().reverse());
                    arguments.get(1).setCode(arguments.get(1).getCode().reverse());
                    a.setCode(a.getCode().reverse());

                    return true;
                }

                return false;

            default:
                return false;
        }

        a.setCode(c);
        a.getRanges().addAll(e.getRanges());

        return true;
    }

    // </editor-fold>
}
