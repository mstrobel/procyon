/*
 * Inlining.java
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
import com.strobel.core.StrongBox;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.strobel.decompiler.ast.PatternMatching.*;
import static java.lang.String.format;

final class Inlining {
    private final Block _method;

    final Map<Variable, MutableInteger> loadCounts = new IdentityHashMap<>();
    final Map<Variable, MutableInteger> storeCounts = new IdentityHashMap<>();

    public Inlining(final Block method) {
        _method = method;
        analyzeMethod();
    }

    // <editor-fold defaultstate="collapsed" desc="Load/Store Analysis">

    final void analyzeMethod() {
        loadCounts.clear();
        storeCounts.clear();

        analyzeNode(_method);
    }

    final void analyzeNode(final Node node) {
        if (node instanceof Expression) {
            final Expression expression = (Expression) node;
            final Object operand = expression.getOperand();

            if (operand instanceof Variable) {
                final AstCode code = expression.getCode();
                final Variable localVariable = (Variable) operand;

                if (code == AstCode.Load) {
                    increment(loadCounts, localVariable);
                }
                else if (code == AstCode.Store) {
                    increment(storeCounts, localVariable);
                }
                else {
                    throw new IllegalStateException("Unexpected AST op code: " + code.getName());
                }
            }

            for (final Expression argument : expression.getArguments()) {
                analyzeNode(argument);
            }
        }
        else {
            if (node instanceof CatchBlock) {
                final CatchBlock catchBlock = (CatchBlock) node;
                final Variable exceptionVariable = catchBlock.getExceptionVariable();

                if (exceptionVariable != null) {
                    increment(storeCounts, exceptionVariable);
                }
            }

            for (final Node child : node.getChildren()) {
                analyzeNode(child);
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Inlining">

    final boolean inlineAllVariables() {
        boolean modified = false;

        final Inlining inlining = new Inlining(_method);

        for (final Block block : _method.getSelfAndChildrenRecursive(Block.class)) {
            modified |= inlining.inlineAllInBlock(block);
        }

        return modified;
    }

    final boolean inlineAllInBlock(final Block block) {
        boolean modified = false;

        final List<Node> body = block.getBody();
        final StrongBox<Variable> tempVariable = new StrongBox<>();
        final StrongBox<Expression> tempExpression = new StrongBox<>();

        if (block instanceof CatchBlock && body.size() > 1) {
            final CatchBlock catchBlock = (CatchBlock) block;
            final Variable v = catchBlock.getExceptionVariable();

            if (v != null &&
                v.isGenerated() &&
                count(storeCounts, v) == 0 &&
                count(loadCounts, v) == 0) {

                if (matchGetArgument(body.get(0), AstCode.Store, tempVariable, tempExpression) &&
                    matchLoad(tempExpression.get(), v)) {

                    body.remove(0);
                    catchBlock.setExceptionVariable(tempVariable.get());
                    modified = true;
                }
            }
        }

        for (int i = 0; i < body.size() - 1; ) {
            final Node node = body.get(i);

            if (matchGetArgument(node, AstCode.Store, tempVariable, tempExpression) &&
                inlineOneIfPossible(block.getBody(), i, false)) {

                modified = true;
                i = Math.max(0, i - 1);
            }
            else {
                i++;
            }
        }

        for (final Node node : body) {
            if (node instanceof BasicBlock) {
                modified |= inlineAllInBasicBlock((BasicBlock) node);
            }
        }

        return modified;
    }

    final boolean inlineAllInBasicBlock(final BasicBlock basicBlock) {
        boolean modified = false;

        final List<Node> body = basicBlock.getBody();
        final StrongBox<Variable> tempVariable = new StrongBox<>();
        final StrongBox<Expression> tempExpression = new StrongBox<>();

        for (int i = 0; i < body.size(); ) {
            final Node node = body.get(i);

            if (matchGetArgument(node, AstCode.Store, tempVariable, tempExpression) &&
                inlineOneIfPossible(basicBlock.getBody(), i, false)) {

                modified = true;
                i = Math.max(0, i - 1);
            }
            else {
                i++;
            }
        }

        return modified;
    }

    final boolean inlineIfPossible(final List<Node> body, final MutableInteger position) {
        final int currentPosition = position.getValue();

        if (inlineOneIfPossible(body, currentPosition, true)) {
            position.setValue(currentPosition - inlineInto(body, currentPosition, false));
            return true;
        }

        return false;
    }

    final int inlineInto(final List<Node> body, final int position, final boolean aggressive) {
        if (position >= body.size()) {
            return 0;
        }

        int count = 0;
        int p = position;

        while (--p >= 0) {
            final Node node = body.get(p);

            if (node instanceof Expression) {
                final Expression e = (Expression) node;

                if (e.getCode() != AstCode.Store) {
                    break;
                }

                if (inlineOneIfPossible(body, p, aggressive)) {
                    ++count;
                }
            }
            else {
                break;
            }
        }

        return count;
    }

    final boolean inlineIfPossible(final Variable variable, final Expression inlinedExpression, final Node next, final boolean aggressive) {
        //
        // Ensure the variable is accessed only a single time.
        //
        final int storeCount = count(storeCounts, variable);
        final int loadCount = count(loadCounts, variable);

        if (storeCount != 1 || loadCount > 1) {
            return false;
        }

        Node n = next;

        if (n instanceof Condition) {
            n = ((Condition) n).getCondition();
        }
        else if (n instanceof Loop) {
            n = ((Loop) n).getCondition();
        }

        final StrongBox<Expression> parent = new StrongBox<>();
        final MutableInteger position = new MutableInteger();

        if (n instanceof Expression &&
            findLoadInNext((Expression) n, variable, inlinedExpression, parent, position) == Boolean.TRUE) {

            if (!aggressive &&
                !variable.isGenerated() &&
                !nonAggressiveInlineInto((Expression) n, parent.get())) {

                return false;
            }

            final List<Expression> parentArguments = parent.get().getArguments();

            //
            // Assign the ranges of the Load instruction.
            //
            inlinedExpression.getRanges().addAll(
                parentArguments.get(position.getValue()).getRanges()
            );

            parentArguments.set(position.getValue(), inlinedExpression);

            return true;
        }

        return false;
    }

    private boolean nonAggressiveInlineInto(final Expression next, final Expression parent) {
        switch (next.getCode()) {
            case Return:
            case IfTrue:
                return parent == next;

            case TableSwitch:
            case LookupSwitch:
                return parent == next ||
                       parent.getCode() == AstCode.Sub && parent == next.getArguments().get(0);

            default:
                return false;
        }
    }

    final Boolean findLoadInNext(
        final Expression expression,
        final Variable variable,
        final Expression expressionBeingMoved,
        final StrongBox<Expression> parent,
        final MutableInteger position) {

        parent.set(null);
        position.setValue(0);

        if (expression == null) {
            return Boolean.FALSE;
        }

        final AstCode code = expression.getCode();
        final List<Expression> arguments = expression.getArguments();

        for (int i = 0; i < arguments.size(); i++) {
            //
            // Stop when seeing an opcode that does not guarantee that its operands will be evaluated.
            // Inlining in that case might result in the inlined expression not being evaluated.
            //
            if (i == 1 &&
                (code == AstCode.LogicalAnd ||
                 code == AstCode.LogicalOr ||
                 code == AstCode.TernaryOp)) {

                return Boolean.FALSE;
            }

            final Expression argument = arguments.get(i);

            if (argument.getCode() == AstCode.Load &&
                argument.getOperand() == variable) {

                parent.set(expression);
                position.setValue(i);
                return Boolean.TRUE;
            }

            final Boolean result = findLoadInNext(argument, variable, expressionBeingMoved, parent, position);

            if (result != null) {
                return result;
            }
        }

        if (isSafeForInlineOver(expression, expressionBeingMoved)) {
            //
            // Continue searching.
            //
            return null;
        }

        //
        // Abort; inlining not possible.
        //
        return Boolean.FALSE;
    }

    final boolean isSafeForInlineOver(final Expression expression, final Expression expressionBeingMoved) {
        switch (expression.getCode()) {
            case Load: {
                final Variable loadedVariable = (Variable) expression.getOperand();

                for (final Expression potentialStore : expressionBeingMoved.getSelfAndChildrenRecursive(Expression.class)) {
                    if (potentialStore.getCode() == AstCode.Store &&
                        potentialStore.getOperand() == loadedVariable) {

                        return false;
                    }
                }

                //
                // The expression is loading a non-forbidden variable.
                //
                return true;
            }

            default: {
                //
                // Expressions with no side effects are safe (except for Load, which is handled above).
                //
                return hasNoSideEffect(expression);
            }
        }
    }

    final boolean inlineOneIfPossible(final List<Node> body, final int position, final boolean aggressive) {
        final StrongBox<Variable> variable = new StrongBox<>();
        final StrongBox<Expression> inlinedExpression = new StrongBox<>();

        final Node node = body.get(position);

        if (matchGetArgument(node, AstCode.Store, variable, inlinedExpression)) {
            if (position < body.size() - 1 &&
                inlineIfPossible(variable.get(), inlinedExpression.get(), body.get(position + 1), aggressive)) {
                //
                // Assign the ranges of the Store instruction.
                //
                inlinedExpression.get().getRanges().addAll(((Expression) node).getRanges());

                //
                // Remove the store instruction.
                //
                body.remove(position);
                return true;
            }

            if (count(loadCounts, variable.get()) == 0) {
                //
                // The variable is never loaded.
                //
                if (hasNoSideEffect(inlinedExpression.get())) {
                    //
                    // Remove the expression completely.
                    //
                    body.remove(position);
                    return true;
                }

                if (canBeExpressionStatement(inlinedExpression.get()) &&
                    variable.get().isGenerated()) {
                    //
                    // Assign the ranges of the Store instruction.
                    //
                    inlinedExpression.get().getRanges().addAll(((Expression) node).getRanges());

                    //
                    // Remove the store, but keep the inner expression;
                    //
                    body.set(position, inlinedExpression.get());
                    return true;
                }
            }
        }

        return false;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Copy Propagation">

    final void copyPropagation() {
        for (final Block block : _method.getSelfAndChildrenRecursive(Block.class)) {
            final List<Node> body = block.getBody();

            final StrongBox<Variable> variable = new StrongBox<>();
            final StrongBox<Expression> copiedExpression = new StrongBox<>();

            for (int i = 0; i < body.size(); i++) {
                if (matchGetArgument(body.get(i), AstCode.Store, variable, copiedExpression) &&
                    !variable.get().isParameter() &&
                    count(storeCounts, variable.get()) == 1 &&
                    canPerformCopyPropagation(copiedExpression.get(), variable.get())) {

                    //
                    // Un-inline the arguments of the Load instruction.
                    //

                    final List<Expression> arguments = copiedExpression.get().getArguments();
                    final Variable[] uninlinedArgs = new Variable[arguments.size()];

                    for (int j = 0; j < uninlinedArgs.length; j++) {
                        final Variable newVariable = new Variable();

                        newVariable.setGenerated(true);
                        newVariable.setName(format("%s_cp_%d", variable.get().getName(), j));

                        uninlinedArgs[j] = newVariable;

                        body.add(i++, new Expression(AstCode.Store, uninlinedArgs[j]));
                    }

                    //
                    // Perform copy propagation.
                    //

                    for (final Expression expression : _method.getSelfAndChildrenRecursive(Expression.class)) {
                        if (expression.getCode() == AstCode.Load &&
                            expression.getOperand() == variable.get()) {

                            expression.setCode(copiedExpression.get().getCode());
                            expression.setOperand(copiedExpression.get().getOperand());

                            for (final Variable uninlinedArg : uninlinedArgs) {
                                expression.getArguments().add(new Expression(AstCode.Load, uninlinedArg));
                            }
                        }
                    }

                    body.remove(i);

                    if (uninlinedArgs.length > 0) {
                        //
                        // If we un-inlined anything, we need to update the usage counters.
                        //
                        analyzeMethod();
                    }

                    //
                    // Inlining may be possible after removal of body.get(i).
                    //
                    inlineInto(body, i, false);

                    i -= uninlinedArgs.length + 1;
                }
            }
        }
    }

    final boolean canPerformCopyPropagation(final Expression expr, final Variable copyVariable) {
        switch (expr.getCode()) {
            case Load: {
                final Variable v = (Variable) expr.getOperand();

                if (v.isParameter()) {
                    //
                    // Parameters can be copied only if they aren't assigned to.
                    //
                    return count(storeCounts, v) == 0;
                }

                //
                // Variables can be copied only if both the variable and the target copy variable are generated,
                // and if the variable has only a single assignment.
                //
                return v.isGenerated() &&
                       copyVariable.isGenerated() &&
                       count(storeCounts, v) == 1;
            }

            default: {
                return false;
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Helper Methods">

    private static boolean hasNoSideEffect(final Expression expression) {
        switch (expression.getCode()) {
            case Load:
            case LoadElement:
            case AConstNull:
            case LdC:
                return true;

            default:
                return false;
        }
    }

    private static boolean canBeExpressionStatement(final Expression expression) {
        switch (expression.getCode()) {
            case InvokeVirtual:
            case InvokeSpecial:
            case InvokeStatic:
            case InvokeInterface:
            case InvokeDynamic:
            case New:
            case NewArray:
            case __NewArray:
            case __ANewArray:
            case MultiANewArray:
            case Store:
            case StoreElement:
                return true;

            default:
                return false;
        }
    }

    private static int count(final Map<Variable, MutableInteger> map, final Variable variable) {
        final MutableInteger count = map.get(variable);
        return count != null ? count.getValue() : 0;
    }

    private static void increment(final Map<Variable, MutableInteger> map, final Variable variable) {
        final MutableInteger count = map.get(variable);

        if (count == null) {
            map.put(variable, new MutableInteger(1));
        }
        else {
            count.increment();
        }
    }

    // </editor-fold>
}
