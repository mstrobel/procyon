/*
 * PatternMatching.java
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

import com.strobel.core.Comparer;
import com.strobel.core.Predicate;
import com.strobel.core.StrongBox;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;

import java.util.ArrayList;
import java.util.List;

import static com.strobel.core.CollectionUtilities.any;

public final class PatternMatching {
    private PatternMatching() {
        throw ContractUtils.unreachable();
    }

    public static boolean match(final Node node, final AstCode code) {
        return node instanceof Expression &&
               ((Expression) node).getCode() == code;
    }

    public static <T> boolean matchGetOperand(final Node node, final AstCode code, final StrongBox<? super T> operand) {
        if (node instanceof Expression) {
            final Expression expression = (Expression) node;

            if (expression.getCode() == code &&
                expression.getArguments().isEmpty()) {

                operand.set(expression.getOperand());
                return true;
            }
        }

        operand.set(null);
        return false;
    }

    public static <T> boolean matchGetOperand(final Node node, final AstCode code, final Class<T> operandType, final StrongBox<? super T> operand) {
        if (node instanceof Expression) {
            final Expression expression = (Expression) node;

            if (expression.getCode() == code &&
                expression.getArguments().isEmpty() &&
                operandType.isInstance(expression.getOperand())) {

                operand.set(expression.getOperand());
                return true;
            }
        }

        operand.set(null);
        return false;
    }

    public static boolean matchGetArguments(final Node node, final AstCode code, final List<Expression> arguments) {
        if (node instanceof Expression) {
            final Expression expression = (Expression) node;

            if (expression.getCode() == code) {
                assert expression.getOperand() == null;
                arguments.clear();
                arguments.addAll(expression.getArguments());
                return true;
            }
        }

        arguments.clear();
        return false;
    }

    public static <T> boolean matchGetArguments(final Node node, final AstCode code, final StrongBox<? super T> operand, final List<Expression> arguments) {
        if (node instanceof Expression) {
            final Expression expression = (Expression) node;

            if (expression.getCode() == code) {
                operand.set(expression.getOperand());
                arguments.clear();
                arguments.addAll(expression.getArguments());
                return true;
            }
        }

        operand.set(null);
        arguments.clear();
        return false;
    }

    public static boolean matchGetArgument(final Node node, final AstCode code, final StrongBox<Expression> argument) {
        final ArrayList<Expression> arguments = new ArrayList<>(1);

        if (matchGetArguments(node, code, arguments) && arguments.size() == 1) {
            argument.set(arguments.get(0));
            return true;
        }

        argument.set(null);
        return false;
    }

    public static <T> boolean matchGetArgument(
        final Node node,
        final AstCode code,
        final StrongBox<? super T> operand,
        final StrongBox<Expression> argument) {

        final ArrayList<Expression> arguments = new ArrayList<>(1);

        if (matchGetArguments(node, code, operand, arguments) && arguments.size() == 1) {
            argument.set(arguments.get(0));
            return true;
        }

        argument.set(null);
        return false;
    }

    public static <T> boolean matchGetArguments(
        final Node node,
        final AstCode code,
        final StrongBox<? super T> operand,
        final StrongBox<Expression> argument1,
        final StrongBox<Expression> argument2) {

        final ArrayList<Expression> arguments = new ArrayList<>(2);

        if (matchGetArguments(node, code, operand, arguments) && arguments.size() == 2) {
            argument1.set(arguments.get(0));
            argument2.set(arguments.get(1));
            return true;
        }

        argument1.set(null);
        argument2.set(null);
        return false;
    }

    public static <T> boolean matchSingle(
        final BasicBlock block,
        final AstCode code,
        final StrongBox<? super T> operand,
        final StrongBox<Expression> argument) {

        final List<Node> body = block.getBody();

        if (body.size() == 2 &&
            body.get(0) instanceof Label &&
            matchGetArgument(body.get(1), code, operand, argument)) {

            return true;
        }

        operand.set(null);
        argument.set(null);
        return false;
    }

    public static <T> boolean matchSingleAndBreak(
        final BasicBlock block,
        final AstCode code,
        final StrongBox<? super T> operand,
        final StrongBox<Expression> argument,
        final StrongBox<Label> label) {

        final List<Node> body = block.getBody();

        if (body.size() == 3 &&
            body.get(0) instanceof Label &&
            matchGetArgument(body.get(1), code, operand, argument) &&
            matchGetOperand(body.get(2), AstCode.Goto, label)) {

            return true;
        }

        operand.set(null);
        argument.set(null);
        label.set(null);
        return false;
    }

    public static boolean matchLast(final BasicBlock block, final AstCode code) {
        final List<Node> body = block.getBody();

        return body.size() >= 1 &&
               match(body.get(body.size() - 1), code);
    }

    public static boolean matchLast(final Block block, final AstCode code) {
        final List<Node> body = block.getBody();

        return body.size() >= 1 &&
               match(body.get(body.size() - 1), code);
    }

    public static <T> boolean matchLast(
        final BasicBlock block,
        final AstCode code,
        final StrongBox<? super T> operand) {

        final List<Node> body = block.getBody();

        if (body.size() >= 1 &&
            matchGetOperand(body.get(body.size() - 1), code, operand)) {

            return true;
        }

        operand.set(null);
        return false;
    }

    public static <T> boolean matchLast(
        final Block block,
        final AstCode code,
        final StrongBox<? super T> operand) {

        final List<Node> body = block.getBody();

        if (body.size() >= 1 &&
            matchGetOperand(body.get(body.size() - 1), code, operand)) {

            return true;
        }

        operand.set(null);
        return false;
    }

    public static <T> boolean matchLast(
        final Block block,
        final AstCode code,
        final StrongBox<? super T> operand,
        final StrongBox<Expression> argument) {

        final List<Node> body = block.getBody();

        if (body.size() >= 1 &&
            matchGetArgument(body.get(body.size() - 1), code, operand, argument)) {

            return true;
        }

        operand.set(null);
        argument.set(null);
        return false;
    }

    public static <T> boolean matchLast(
        final BasicBlock block,
        final AstCode code,
        final StrongBox<? super T> operand,
        final StrongBox<Expression> argument) {

        final List<Node> body = block.getBody();

        if (body.size() >= 1 &&
            matchGetArgument(body.get(body.size() - 1), code, operand, argument)) {

            return true;
        }

        operand.set(null);
        argument.set(null);
        return false;
    }

    public static <T> boolean matchLastAndBreak(
        final BasicBlock block,
        final AstCode code,
        final StrongBox<? super T> operand,
        final StrongBox<Expression> argument,
        final StrongBox<Label> label) {

        final List<Node> body = block.getBody();

        if (body.size() >= 2 &&
            matchGetArgument(body.get(body.size() - 2), code, operand, argument) &&
            PatternMatching.matchGetOperand(body.get(body.size() - 1), AstCode.Goto, label)) {

            return true;
        }

        operand.set(null);
        argument.set(null);
        label.set(null);
        return false;
    }

    public static boolean matchThis(final Node node) {
        final StrongBox<Variable> operand = new StrongBox<>();

        return matchGetOperand(node, AstCode.Load, operand) &&
               operand.get().isParameter() &&
               operand.get().getOriginalParameter().getPosition() == -1;
    }

    public static boolean matchLoadAny(final Node node, final Iterable<Variable> expectedVariables) {
        return any(
            expectedVariables,
            new Predicate<Variable>() {
                @Override
                public boolean test(final Variable variable) {
                    return matchLoad(node, variable);
                }
            }
        );
    }

    public static boolean matchLoad(final Node node, final Variable expectedVariable) {
        final StrongBox<Variable> operand = new StrongBox<>();

        return matchGetOperand(node, AstCode.Load, operand) &&
               Comparer.equals(operand.get(), expectedVariable);
    }

    public static boolean matchStore(final Node node, final Variable expectedVariable) {
        return match(node, AstCode.Store) &&
               Comparer.equals(((Expression) node).getOperand(), expectedVariable);
    }

    public static boolean matchLoad(final Node node, final Variable expectedVariable, final StrongBox<Expression> argument) {
        final StrongBox<Variable> operand = new StrongBox<>();

        return matchGetArgument(node, AstCode.Load, operand, argument) &&
               Comparer.equals(operand.get(), expectedVariable);
    }

    public static boolean matchLoadStore(final Node node, final Variable expectedVariable, final StrongBox<Variable> targetVariable) {
        final StrongBox<Expression> temp = new StrongBox<>();

        if (matchGetArgument(node, AstCode.Store, targetVariable, temp) &&
            matchLoad(temp.get(), expectedVariable)) {

            return true;
        }

        targetVariable.set(null);
        return false;
    }

    public static boolean matchSimplifiableComparison(final Node node) {
        if (node instanceof Expression) {
            final Expression e = (Expression) node;

            switch (e.getCode()) {
                case CmpEq:
                case CmpNe:
                case CmpLt:
                case CmpGe:
                case CmpGt:
                case CmpLe: {
                    final Expression comparisonArgument = e.getArguments().get(0);

                    switch (comparisonArgument.getCode()) {
                        case __LCmp:
                        case __FCmpL:
                        case __FCmpG:
                        case __DCmpL:
                        case __DCmpG:
                            final Expression constantArgument = e.getArguments().get(1);
                            final StrongBox<Integer> comparand = new StrongBox<>();

                            return matchGetOperand(constantArgument, AstCode.LdC, Integer.class, comparand) &&
                                   comparand.get() == 0;
                    }
                }
            }
        }

        return false;
    }

    public static boolean matchReversibleComparison(final Node node) {
        if (match(node, AstCode.LogicalNot)) {
            switch (((Expression) node).getArguments().get(0).getCode()) {
                case CmpEq:
                case CmpNe:
                case CmpLt:
                case CmpGe:
                case CmpGt:
                case CmpLe:
                    return true;
            }
        }

        return false;
    }

    public static Boolean matchBooleanConstant(final Node node) {
        if (match(node, AstCode.LdC)) {
            final Object operand = ((Expression) node).getOperand();

            if (operand instanceof Integer) {
                final int intValue = (Integer) operand;

                if (intValue == 0) {
                    return Boolean.FALSE;
                }

                if (intValue == 1) {
                    return Boolean.TRUE;
                }
            }
        }

        return null;
    }

    public static boolean matchUnconditionalBranch(final Node node) {
        return node instanceof Expression &&
               ((Expression) node).getCode().isUnconditionalControlFlow();
    }

    public static boolean matchLock(final List<Node> body, final int position, final StrongBox<LockInfo> result) {
        VerifyArgument.notNull(body, "body");
        VerifyArgument.notNull(result, "result");

        result.set(null);

        int head = position;

        if (head < 0 || head >= body.size()) {
            return false;
        }

        final List<Expression> a = new ArrayList<>();
        final Label leadingLabel;

        if (body.get(head) instanceof Label) {
            leadingLabel = (Label) body.get(head);
            ++head;
        }
        else {
            leadingLabel = null;
        }

        if (head >= body.size()) {
            return false;
        }

        if (matchGetArguments(body.get(head), AstCode.MonitorEnter, a)) {
            if (!match(a.get(0), AstCode.Load)) {
                return false;
            }

            result.set(new LockInfo(leadingLabel, (Expression) body.get(head)));
            return true;
        }

        final StrongBox<Variable> v = new StrongBox<>();

        final Variable lockVariable;
        final Expression lockInit;
        final Expression lockStore;
        final Expression lockStoreCopy;

        if (head < body.size() - 1 &&
            matchGetArguments(body.get(head), AstCode.Store, v, a)) {

            lockVariable = v.get();
            lockInit = a.get(0);
            lockStore = (Expression) body.get(head++);

            if (matchLoadStore(body.get(head), lockVariable, v)) {
                lockStoreCopy = (Expression) body.get(head++);
            }
            else {
                lockStoreCopy = null;
            }

            if (head < body.size() &&
                matchGetArguments(body.get(head), AstCode.MonitorEnter, a)) {

                if (!matchLoad(a.get(0), lockVariable)) {
                    return false;
                }

                result.set(
                    new LockInfo(
                        leadingLabel,
                        lockInit,
                        lockStore,
                        lockStoreCopy,
                        (Expression) body.get(head)
                    )
                );

                return true;
            }
        }

        return false;
    }

    public static boolean matchUnlock(final Node e, final LockInfo lockInfo) {
        if (lockInfo == null) {
            return false;
        }

        final StrongBox<Expression> a = new StrongBox<>();

        return matchGetArgument(e, AstCode.MonitorExit, a) &&
               (matchLoad(a.get(), lockInfo.lock) ||
                lockInfo.lockCopy != null && matchLoad(a.get(), lockInfo.lockCopy));
    }
}
