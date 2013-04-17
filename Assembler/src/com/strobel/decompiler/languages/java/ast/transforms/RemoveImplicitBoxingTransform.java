/*
 * AutoUnboxingTransform.java
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

import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;

import java.util.HashSet;
import java.util.Set;

public class RemoveImplicitBoxingTransform extends ContextTrackingVisitor<Void> {
    private final static Set<String> BOX_METHODS;
    private final static Set<String> UNBOX_METHODS;

    static {
        BOX_METHODS = new HashSet<>();
        UNBOX_METHODS = new HashSet<>();

        final String[] boxTypes = {
            "java/lang/Byte",
            "java/lang/Short",
            "java/lang/Integer",
            "java/lang/Long",
            "java/lang/Float",
            "java/lang/Double"
        };

        final String[] unboxMethods = {
            "byteValue:()B",
            "shortValue:()S",
            "intValue:()I",
            "longValue:()J",
            "floatValue:()F",
            "doubleValue:()D"
        };

        final String[] boxMethods = {
            "java/lang/Boolean.valueOf:(Z)Ljava/lang/Boolean;",
            "java/lang/Character.valueOf:(C)Ljava/lang/Character;",
            "java/lang/Byte.valueOf:(B)Ljava/lang/Byte;",
            "java/lang/Short.valueOf:(S)Ljava/lang/Short;",
            "java/lang/Integer.valueOf:(I)Ljava/lang/Integer;",
            "java/lang/Long.valueOf:(J)Ljava/lang/Long;",
            "java/lang/Float.valueOf:(F)Ljava/lang/Float;",
            "java/lang/Double.valueOf:(D)Ljava/lang/Double;"
        };

        for (final String boxMethod : boxMethods) {
            BOX_METHODS.add(boxMethod);
        }

        for (final String unboxMethod : unboxMethods) {
            UNBOX_METHODS.add("java/lang/Number." + unboxMethod);
        }

        for (final String boxType : boxTypes) {
            for (final String unboxMethod : unboxMethods) {
                UNBOX_METHODS.add(boxType + "." + unboxMethod);
            }
        }

        UNBOX_METHODS.add("java/lang/Character.charValue:()C");
        UNBOX_METHODS.add("java/lang/Boolean.booleanValue:()Z");
    }

    public RemoveImplicitBoxingTransform(final DecompilerContext context) {
        super(context);
    }

    @Override
    public Void visitInvocationExpression(final InvocationExpression node, final Void data) {
        if (node.getArguments().size() == 1 &&
            node.getTarget() instanceof MemberReferenceExpression &&
            isValidPrimitiveParent(node.getParent())) {

            removeBoxing(node);
        }

        return super.visitInvocationExpression(node, data);
    }

    private boolean isValidPrimitiveParent(final AstNode parent) {
        if (parent instanceof BinaryOperatorExpression) {
            final BinaryOperatorExpression binary = (BinaryOperatorExpression) parent;

            if (binary.getLeft() instanceof NullReferenceExpression ||
                binary.getRight() instanceof NullReferenceExpression) {

                return false;
            }

            return true;
        }

        return !(
            parent instanceof MemberReferenceExpression ||
            parent instanceof ClassOfExpression ||
            parent instanceof MethodGroupExpression ||
            parent instanceof InvocationExpression ||
            parent instanceof IndexerExpression ||
            parent instanceof SynchronizedStatement ||
            parent instanceof ThrowStatement
        );
    }

    @Override
    public Void visitUnaryOperatorExpression(final UnaryOperatorExpression node, final Void data) {
        unbox(node.getExpression());

        return super.visitUnaryOperatorExpression(node, data);
    }

    @Override
    public Void visitBinaryOperatorExpression(final BinaryOperatorExpression node, final Void data) {
        unbox(node.getLeft());
        unbox(node.getRight());

        return super.visitBinaryOperatorExpression(node, data);
    }

    private void unbox(final Expression e) {
        if (e == null || e.isNull()) {
            return;
        }

        if (!(e instanceof InvocationExpression)) {
            return;
        }

        final Expression target = ((InvocationExpression) e).getTarget();

        if (!(target instanceof MemberReferenceExpression)) {
            return;
        }

        final MemberReference reference = e.getUserData(Keys.MEMBER_REFERENCE);

        if (!(reference instanceof MethodReference)) {
            return;
        }

        final String key = reference.getFullName() + ":" + reference.getSignature();

        if (UNBOX_METHODS.contains(key)) {
            final Expression boxedValue = ((MemberReferenceExpression) target).getTarget();
            boxedValue.remove();
            e.replaceWith(boxedValue);
        }
    }

    private void removeBoxing(final InvocationExpression node) {
        final MemberReference reference = node.getUserData(Keys.MEMBER_REFERENCE);

        if (!(reference instanceof MethodReference)) {
            return;
        }

        final String key = reference.getFullName() + ":" + reference.getSignature();

        if (BOX_METHODS.contains(key)) {
            final Expression underlyingValue = node.getArguments().firstOrNullObject();
            underlyingValue.remove();
            node.replaceWith(underlyingValue);
        }
    }
}
