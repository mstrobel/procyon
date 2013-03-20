/*
 * PatternMatching.java
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

import com.strobel.core.Comparer;
import com.strobel.core.StrongBox;
import com.strobel.util.ContractUtils;

import java.util.ArrayList;
import java.util.List;

public final class PatternMatching {
    private PatternMatching() {
        throw ContractUtils.unreachable();
    }

    public static boolean match(final Node node, final AstCode code) {
        return node instanceof Expression &&
               ((Expression) node).getCode() == code;
    }

    public static <T> boolean matchGetOperand(final Node node, final AstCode code, final StrongBox<T> operand) {
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

    public static boolean matchGetArguments(final Node node, final AstCode code, final List<Expression> arguments) {
        if (node instanceof Expression) {
            final Expression expression = (Expression) node;

            if (expression.getCode() == code) {
                assert expression.getOperand() == null;
                arguments.addAll(expression.getArguments());
                return true;
            }
        }

        arguments.clear();
        return false;
    }

    public static <T> boolean matchGetArguments(final Node node, final AstCode code, final StrongBox<T> operand, final List<Expression> arguments) {
        if (node instanceof Expression) {
            final Expression expression = (Expression) node;

            if (expression.getCode() == code) {
                operand.set(expression.getOperand());
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
        final StrongBox<T> operand,
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
        final StrongBox<T> operand,
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
        final StrongBox<T> operand,
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
        final StrongBox<T> operand,
        final StrongBox<Expression> argument,
        final StrongBox<Label> label) {

        final List<Node> body = block.getBody();

        if (body.size() == 3 &&
            body.get(0) instanceof Label &&
            matchGetArgument(body.get(1), code, operand, argument) &&
            PatternMatching.matchGetOperand(body.get(2), AstCode.Goto, label)) {

            return true;
        }

        operand.set(null);
        argument.set(null);
        label.set(null);
        return false;
    }

    public static <T> boolean matchLast(
        final BasicBlock block,
        final AstCode code,
        final StrongBox<T> operand,
        final StrongBox<Expression> argument) {

        final List<Node> body = block.getBody();

        if (body.size() >= 2 &&
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
        final StrongBox<T> operand,
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
    
    public static boolean matchLoad(final Node node, final Variable expectedVariable) {
        final StrongBox<Variable> operand = new StrongBox<>();
        
        return matchGetOperand(node, AstCode.Load, operand) &&
               Comparer.equals(operand.get(), expectedVariable);
    }

    public static boolean matchLoad(final Node node, final Variable expectedVariable, final StrongBox<Expression> argument) {
        final StrongBox<Variable> operand = new StrongBox<>();
        
        return matchGetArgument(node, AstCode.Load, operand, argument) &&
               Comparer.equals(operand.get(), expectedVariable);
    }
}
