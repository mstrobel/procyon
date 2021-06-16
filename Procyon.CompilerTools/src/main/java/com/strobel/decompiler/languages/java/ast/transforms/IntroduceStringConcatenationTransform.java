/*
 * IntroduceStringConcatenationTransform.java
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
import com.strobel.assembler.metadata.DynamicCallSite;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.OptionalNode;
import com.strobel.decompiler.patterns.TypedExpression;
import com.strobel.decompiler.semantics.ResolveResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.strobel.core.CollectionUtilities.*;

public class IntroduceStringConcatenationTransform extends ContextTrackingVisitor<Void> {
    private final static char ARGUMENT_MARKER = '\u0001';
    private final static char CONSTANT_MARKER = '\u0002';

    private final INode _stringBuilderArgumentPattern;

    public IntroduceStringConcatenationTransform(final DecompilerContext context) {
        super(context);

        _stringBuilderArgumentPattern = new OptionalNode(
            new TypedExpression(
                "firstArgument",
                CommonTypeReferences.String,
                new JavaResolver(context)
            )
        );
    }

    @Override
    public Void visitInlinedBytecode(final InlinedBytecodeExpression node, final Void data) {
        super.visitInlinedBytecode(node, data);

        if (!(node.getParent() instanceof InvocationExpression)) {
            return null;
        }

        final InvocationExpression parent = (InvocationExpression) node.getParent();
        final BytecodeConstant operandConstant = firstOrDefault(ofType(node.getOperands(), BytecodeConstant.class));
        final Object operand = operandConstant != null ? operandConstant.getConstantValue() : null;

        if (!(operand instanceof DynamicCallSite)) {
            return null;
        }

        final DynamicCallSite callSite = (DynamicCallSite) operand;
        final AstNodeCollection<Expression> arguments = parent.getArguments();
        final MethodReference bootstrapMethod = callSite.getBootstrapMethod();

        if ("java/lang/invoke/StringConcatFactory".equals(bootstrapMethod.getDeclaringType().getInternalName())) {
            if ("makeConcat".equals(bootstrapMethod.getName())) {
                handleIndyConcatWithConstants(parent, callSite, arguments);
            }
            else if ("makeConcatWithConstants".equals(bootstrapMethod.getName())) {
                handleIndyConcatWithConstants(parent, callSite, arguments);
            }
        }

        return null;
    }

    private void handleIndyConcat(final InvocationExpression parent, final AstNodeCollection<Expression> arguments) {
        if (arguments.isEmpty()) {
            return;
        }

        final List<Expression> operands = new ArrayList<>(arguments);

        Expression concatenation = null;

        if (!anyIsString(operands, 0, 2)) {
            concatenation = new PrimitiveExpression(Expression.MYSTERY_OFFSET, "");
        }

        for (int i = 0, n = operands.size(); i < n; i++) {
            final Expression operand = operands.get(i);

            operand.remove();

            concatenation = concatenation != null ? new BinaryOperatorExpression(concatenation, BinaryOperatorType.ADD, operand)
                                                  : operand;
        }

        parent.replaceWith(concatenation);
    }

    private void handleIndyConcatWithConstants(final InvocationExpression parent,
                                               final DynamicCallSite callSite,
                                               final AstNodeCollection<Expression> arguments) {
        final ArrayDeque<Object> constants = new ArrayDeque<>(callSite.getBootstrapArguments());
        final ArrayDeque<Expression> formalArguments = new ArrayDeque<>(arguments != null ? arguments : Collections.<Expression>emptyList());

        if (!(constants.peekFirst() instanceof String)) {
            return;
        }

        final List<Expression> operands = new ArrayList<>(Math.max(16, constants.size() + formalArguments.size()));

        final String pattern = (String) constants.removeFirst();

        int i = 0;

        while (i < pattern.length()) {
            final int nextMarker = nextMarker(pattern, i);

            if (nextMarker < 0) {
                if (i < pattern.length() - 1) {
                    operands.add(new PrimitiveExpression(Expression.MYSTERY_OFFSET, pattern.substring(i)));
                }
                break;
            }

            if (nextMarker > i) {
                operands.add(new PrimitiveExpression(Expression.MYSTERY_OFFSET, pattern.substring(i, nextMarker)));
            }

            if (pattern.charAt(nextMarker) == CONSTANT_MARKER) {
                if (constants.isEmpty()) {
                    return;
                }
                operands.add(new PrimitiveExpression(Expression.MYSTERY_OFFSET, constants.removeFirst()));
            }
            else {
                if (formalArguments.isEmpty()) {
                    return;
                }
                operands.add(formalArguments.removeFirst());
            }

            i = nextMarker + 1;
        }

        if (operands.isEmpty() || !constants.isEmpty() || !formalArguments.isEmpty()) {
            return; // FAIL.
        }

        if (!anyIsString(operands, 0, 2)) {
            operands.add(0, new PrimitiveExpression(Expression.MYSTERY_OFFSET, ""));
        }

        Expression concatenation = operands.get(0);

        concatenation.remove();

        for (int j = 1, n = operands.size(); j < n; j++) {
            final Expression operand = operands.get(j);
            operand.remove();
            concatenation = new BinaryOperatorExpression(concatenation, BinaryOperatorType.ADD, operand);
        }

        parent.replaceWith(concatenation);
    }

    private static int nextMarker(final String pattern, final int start) {
        if (start < 0 || start >= pattern.length()) {
            return -1;
        }

        final int aNext = pattern.indexOf(ARGUMENT_MARKER, start);
        final int cNext = pattern.indexOf(CONSTANT_MARKER, start);

        return aNext < 0 ? cNext
                         : cNext < 0 ? aNext : Math.min(aNext, cNext);
    }

    @Override
    public Void visitObjectCreationExpression(final ObjectCreationExpression node, final Void data) {
        final AstNodeCollection<Expression> arguments = node.getArguments();

        if (arguments.isEmpty() ||
            arguments.hasSingleElement()) {

            final Expression firstArgument;

            if (arguments.hasSingleElement()) {
                final Match m = _stringBuilderArgumentPattern.match(arguments.firstOrNullObject());

                if (!m.success()) {
                    return super.visitObjectCreationExpression(node, data);
                }

                firstArgument = firstOrDefault(m.<Expression>get("firstArgument"));
            }
            else {
                firstArgument = null;
            }

            final TypeReference typeReference = node.getType().toTypeReference();

            if (typeReference != null &&
                isStringBuilder(typeReference)) {

                convertStringBuilderToConcatenation(node, firstArgument);
            }
        }

        return super.visitObjectCreationExpression(node, data);
    }

    private boolean isStringBuilder(final TypeReference typeReference) {
        if (CommonTypeReferences.StringBuilder.isEquivalentTo(typeReference)) {
            return true;
        }

        return context.getCurrentType() != null &&
               context.getCurrentType().getCompilerMajorVersion() < 49 &&
               CommonTypeReferences.StringBuffer.isEquivalentTo(typeReference);
    }

    private void convertStringBuilderToConcatenation(final ObjectCreationExpression node, final Expression firstArgument) {
        if (node.getParent() == null || node.getParent().getParent() == null) {
            return;
        }

        final ArrayList<Expression> operands = new ArrayList<>();

        if (firstArgument != null) {
            operands.add(firstArgument);
        }

        AstNode current;
        AstNode parent;

        for (current = node.getParent(), parent = current.getParent();
             current instanceof MemberReferenceExpression && parent instanceof InvocationExpression && parent.getParent() != null;
             current = parent.getParent(), parent = current.getParent()) {

            final String memberName = ((MemberReferenceExpression) current).getMemberName();
            final AstNodeCollection<Expression> arguments = ((InvocationExpression) parent).getArguments();

            if (StringUtilities.equals(memberName, "append") && arguments.size() == 1) {
                operands.add(arguments.firstOrNullObject());
            }
            else {
                break;
            }
        }

        if (operands.size() > 1 &&
            anyIsString(operands.subList(0, 2)) &&
            current instanceof MemberReferenceExpression &&
            parent instanceof InvocationExpression &&
            !(parent.getParent() instanceof ExpressionStatement) &&
            StringUtilities.equals(((MemberReferenceExpression) current).getMemberName(), "toString") &&
            ((InvocationExpression) parent).getArguments().isEmpty()) {

            for (final Expression operand : operands) {
                operand.remove();
            }

            Expression concatenation = new BinaryOperatorExpression(operands.get(0), BinaryOperatorType.ADD, operands.get(1));

            for (int i = 2; i < operands.size(); i++) {
                concatenation = new BinaryOperatorExpression(concatenation, BinaryOperatorType.ADD, operands.get(i));
            }

            parent.replaceWith(concatenation);
        }
    }

    private boolean anyIsString(final List<Expression> expressions) {
        return anyIsString(expressions, 0, expressions.size());
    }

    @SuppressWarnings("SameParameterValue")
    private boolean anyIsString(final List<Expression> expressions, final int start, final int end) {
        final JavaResolver resolver = new JavaResolver(context);

        for (int i = start, n = Math.min(expressions.size(), end); i < n; i++) {
            final ResolveResult result = resolver.apply(expressions.get(i));

            if (result != null &&
                result.getType() != null &&
                CommonTypeReferences.String.isEquivalentTo(result.getType())) {

                return true;
            }
        }

        return false;
    }
}
