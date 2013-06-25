/*
 * PushNegationTransform.java
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

package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.functions.Function;

public class SimplifyBooleanExpressionsTransform extends ContextTrackingVisitor<AstNode> implements IAstTransform {
    private final static Function<AstNode, AstNode> NEGATE_FUNCTION = new Function<AstNode, AstNode>() {
        @Override
        public AstNode apply(final AstNode n) {
            if (n instanceof UnaryOperatorExpression) {
                final UnaryOperatorExpression unary = (UnaryOperatorExpression) n;

                if (unary.getOperator() == UnaryOperatorType.NOT) {
                    final Expression operand = unary.getExpression();
                    operand.remove();
                    return operand;
                }
            }
            return new UnaryOperatorExpression(UnaryOperatorType.NOT, (Expression) n);
        }
    };

    public SimplifyBooleanExpressionsTransform(final DecompilerContext context) {
        super(context);
    }

    private final static PrimitiveExpression TRUE_CONSTANT = new PrimitiveExpression(true);
    private final static PrimitiveExpression FALSE_CONSTANT = new PrimitiveExpression(false);

    @Override
    public AstNode visitConditionalExpression(final ConditionalExpression node, final Void data) {
        final Expression condition = node.getCondition();
        final Expression trueExpression = node.getTrueExpression();
        final Expression falseExpression = node.getFalseExpression();

        if (TRUE_CONSTANT.matches(trueExpression) &&
            FALSE_CONSTANT.matches(falseExpression)) {

            condition.remove();
            trueExpression.remove();
            falseExpression.remove();

            node.replaceWith(condition);

            return condition.acceptVisitor(this, data);
        }
        else if (TRUE_CONSTANT.matches(trueExpression) &&
                 FALSE_CONSTANT.matches(falseExpression)) {

            condition.remove();
            trueExpression.remove();
            falseExpression.remove();

            final Expression negatedCondition = new UnaryOperatorExpression(UnaryOperatorType.NOT, condition);

            node.replaceWith(negatedCondition);

            return negatedCondition.acceptVisitor(this, data);
        }

        return super.visitConditionalExpression(node, data);
    }

    @Override
    public AstNode visitBinaryOperatorExpression(final BinaryOperatorExpression node, final Void data) {
        final BinaryOperatorType operator = node.getOperator();

        if (operator == BinaryOperatorType.EQUALITY ||
            operator == BinaryOperatorType.INEQUALITY) {

            final Expression left = node.getLeft();
            final Expression right = node.getRight();

            if (TRUE_CONSTANT.matches(left) || FALSE_CONSTANT.matches(left)) {
                if (TRUE_CONSTANT.matches(right) || FALSE_CONSTANT.matches(right)) {
                    return new PrimitiveExpression(
                        (TRUE_CONSTANT.matches(left) == TRUE_CONSTANT.matches(right)) ^
                        operator == BinaryOperatorType.INEQUALITY
                    );
                }

                final boolean negate = FALSE_CONSTANT.matches(left) ^ operator == BinaryOperatorType.INEQUALITY;

                right.remove();

                final Expression replacement = negate ? new UnaryOperatorExpression(UnaryOperatorType.NOT, right)
                                                      : right;

                node.replaceWith(replacement);

                return replacement.acceptVisitor(this, data);
            }
            else if (TRUE_CONSTANT.matches(right) || FALSE_CONSTANT.matches(right)) {
                final boolean negate = FALSE_CONSTANT.matches(right) ^ operator == BinaryOperatorType.INEQUALITY;

                left.remove();

                final Expression replacement = negate ? new UnaryOperatorExpression(UnaryOperatorType.NOT, left)
                                                      : left;

                node.replaceWith(replacement);

                return replacement.acceptVisitor(this, data);
            }
        }

        return super.visitBinaryOperatorExpression(node, data);
    }

    @Override
    public AstNode visitUnaryOperatorExpression(final UnaryOperatorExpression node, final Void _) {
        if (node.getOperator() == UnaryOperatorType.NOT &&
            node.getExpression() instanceof BinaryOperatorExpression) {

            final BinaryOperatorExpression binary = (BinaryOperatorExpression) node.getExpression();

            boolean successful = true;

            switch (binary.getOperator()) {
                case EQUALITY:
                    binary.setOperator(BinaryOperatorType.INEQUALITY);
                    break;

                case INEQUALITY:
                    binary.setOperator(BinaryOperatorType.EQUALITY);
                    break;

                case GREATER_THAN:
                    binary.setOperator(BinaryOperatorType.LESS_THAN_OR_EQUAL);
                    break;

                case GREATER_THAN_OR_EQUAL:
                    binary.setOperator(BinaryOperatorType.LESS_THAN);
                    break;

                case LESS_THAN:
                    binary.setOperator(BinaryOperatorType.GREATER_THAN_OR_EQUAL);
                    break;

                case LESS_THAN_OR_EQUAL:
                    binary.setOperator(BinaryOperatorType.GREATER_THAN);
                    break;

                default:
                    successful = false;
                    break;
            }

            if (successful) {
                node.replaceWith(binary);
                return binary.acceptVisitor(this, _);
            }

            successful = true;

            switch (binary.getOperator()) {
                case LOGICAL_AND:
                    binary.setOperator(BinaryOperatorType.LOGICAL_OR);
                    break;

                case LOGICAL_OR:
                    binary.setOperator(BinaryOperatorType.LOGICAL_AND);
                    break;

                default:
                    successful = false;
                    break;
            }

            if (successful) {
                binary.getLeft().replaceWith(NEGATE_FUNCTION);
                binary.getRight().replaceWith(NEGATE_FUNCTION);
                node.replaceWith(binary);
                return binary.acceptVisitor(this, _);
            }
        }

        return super.visitUnaryOperatorExpression(node, _);
    }

    @Override
    public AstNode visitAssignmentExpression(final AssignmentExpression node, final Void data) {
        if (node.getOperator() == AssignmentOperatorType.ASSIGN) {
            final Expression left = node.getLeft();
            final Expression right = node.getRight();

            if (right instanceof BinaryOperatorExpression) {
                final BinaryOperatorExpression binary = (BinaryOperatorExpression) right;
                final Expression innerLeft = binary.getLeft();
                final Expression innerRight = binary.getRight();

                if (innerLeft.matches(left)) {
                    final BinaryOperatorType binaryOp = binary.getOperator();
                    final AssignmentOperatorType assignOp = AssignmentExpression.getCorrespondingAssignmentOperator(binaryOp);

                    if (assignOp != null) {
                        innerRight.remove();
                        right.replaceWith(innerRight);
                        node.setOperator(assignOp);
                    }
                }
                else if (innerRight.matches(left)) {
                    final BinaryOperatorType binaryOp = binary.getOperator();
                    final AssignmentOperatorType assignOp = AssignmentExpression.getCorrespondingAssignmentOperator(binaryOp);

                    if (assignOp != null) {
                        switch (assignOp) {
                            case ADD:
                            case MULTIPLY:
                            case BITWISE_AND:
                            case BITWISE_OR:
                            case EXCLUSIVE_OR: {
                                innerLeft.remove();
                                right.replaceWith(innerLeft);
                                node.setOperator(assignOp);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return super.visitAssignmentExpression(node, data);
    }
}
