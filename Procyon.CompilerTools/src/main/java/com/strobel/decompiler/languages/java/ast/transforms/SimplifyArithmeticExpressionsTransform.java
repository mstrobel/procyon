/*
 * SimplifyArithmeticExpressionsTransform.java
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

import com.strobel.assembler.metadata.CommonTypeReferences;
import com.strobel.assembler.metadata.JvmType;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.semantics.ResolveResult;

public class SimplifyArithmeticExpressionsTransform extends ContextTrackingVisitor<Void> {
    private final JavaResolver _resolver;

    public SimplifyArithmeticExpressionsTransform(final DecompilerContext context) {
        super(context);
        _resolver = new JavaResolver(context);
    }

    @Override
    public Void visitUnaryOperatorExpression(final UnaryOperatorExpression node, final Void data) {
        super.visitUnaryOperatorExpression(node, data);

        final UnaryOperatorType operator = node.getOperator();

        switch (operator) {
            case PLUS:
            case MINUS: {
                final boolean minus = operator == UnaryOperatorType.MINUS;

                if (node.getExpression() instanceof PrimitiveExpression) {
                    final PrimitiveExpression operand = (PrimitiveExpression) node.getExpression();
                    final boolean isNegative;

                    if (operand.getValue() instanceof Number) {
                        if (operand.getValue() instanceof Float || operand.getValue() instanceof Double) {
                            final double doubleValue = (double) JavaPrimitiveCast.cast(JvmType.Double, operand.getValue());

                            isNegative = doubleValue < 0d;
                        }
                        else {
                            final long longValue = (long) JavaPrimitiveCast.cast(JvmType.Long, operand.getValue());

                            isNegative = longValue < 0L;
                        }

                        if (minus == isNegative) {
                            operand.remove();
                            node.replaceWith(operand);
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Void visitBinaryOperatorExpression(final BinaryOperatorExpression node, final Void data) {
        super.visitBinaryOperatorExpression(node, data);

        final BinaryOperatorType operator = node.getOperator();

        switch (operator) {
            case ADD:
            case SUBTRACT: {
                final ResolveResult leftResult = _resolver.apply(node.getLeft());

                if (leftResult == null ||
                    leftResult.getType() == null ||
                    leftResult.getType().isEquivalentTo(CommonTypeReferences.String)) {

                    return null;
                }

                if (node.getRight() instanceof PrimitiveExpression) {
                    final PrimitiveExpression right = (PrimitiveExpression) node.getRight();
                    final boolean isNegative;

                    if (right.getValue() instanceof Number) {
                        final Number negatedValue;

                        if (right.getValue() instanceof Float || right.getValue() instanceof Double) {
                            final double value = (double) JavaPrimitiveCast.cast(JvmType.Double, right.getValue());

                            isNegative = value < 0d;

                            negatedValue = isNegative ? (Number) JavaPrimitiveCast.cast(JvmType.forValue(right.getValue(), true), -value)
                                                      : null;
                        }
                        else {
                            final long value = (long) JavaPrimitiveCast.cast(JvmType.Long, right.getValue());

                            isNegative = value < 0L;

                            negatedValue = isNegative ? (Number) JavaPrimitiveCast.cast(JvmType.forValue(right.getValue(), true), -value)
                                                      : null;
                        }

                        if (isNegative) {
                            right.setValue(negatedValue);

                            node.setOperator(
                                operator == BinaryOperatorType.ADD ? BinaryOperatorType.SUBTRACT
                                                                   : BinaryOperatorType.ADD
                            );
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Void visitAssignmentExpression(final AssignmentExpression node, final Void data) {
        super.visitAssignmentExpression(node, data);

        final AssignmentOperatorType operator = node.getOperator();

        switch (operator) {
            case ADD:
            case SUBTRACT: {
                final ResolveResult leftResult = _resolver.apply(node.getLeft());

                if (leftResult == null ||
                    leftResult.getType() == null ||
                    leftResult.getType().isEquivalentTo(CommonTypeReferences.String)) {

                    return null;
                }

                if (node.getRight() instanceof PrimitiveExpression) {
                    final PrimitiveExpression right = (PrimitiveExpression) node.getRight();
                    final boolean isNegative;

                    if (right.getValue() instanceof Number) {
                        final Number negatedValue;

                        if (right.getValue() instanceof Float || right.getValue() instanceof Double) {
                            final double value = (double) JavaPrimitiveCast.cast(JvmType.Double, right.getValue());

                            isNegative = value < 0d;

                            negatedValue = isNegative ? (Number) JavaPrimitiveCast.cast(JvmType.forValue(right.getValue(), true), -value)
                                                      : null;
                        }
                        else {
                            final long value = (long) JavaPrimitiveCast.cast(JvmType.Long, right.getValue());

                            isNegative = value < 0L;

                            negatedValue = isNegative ? (Number) JavaPrimitiveCast.cast(JvmType.forValue(right.getValue(), true), -value)
                                                      : null;
                        }

                        if (isNegative) {
                            right.setValue(negatedValue);

                            node.setOperator(
                                operator == AssignmentOperatorType.ADD ? AssignmentOperatorType.SUBTRACT
                                                                       : AssignmentOperatorType.ADD
                            );
                        }
                    }
                }
            }
        }

        return null;
    }
}
