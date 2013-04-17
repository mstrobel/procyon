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
import com.strobel.decompiler.languages.java.ast.BinaryOperatorExpression;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.Expression;
import com.strobel.decompiler.languages.java.ast.InvocationExpression;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MemberReferenceExpression;
import com.strobel.decompiler.languages.java.ast.UnaryOperatorExpression;

import java.util.HashSet;
import java.util.Set;

public class AutoUnboxingTransform extends ContextTrackingVisitor<Void> {
    private final static Set<String> UNBOX_METHODS;

    static {
        UNBOX_METHODS = new HashSet<>();

        final String[] boxTypes = {
            "java/lang/Number",
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

        for (final String boxType : boxTypes) {
            for (final String unboxMethod : unboxMethods) {
                UNBOX_METHODS.add(boxType + "." + unboxMethod);
            }
        }

        UNBOX_METHODS.add("java/lang/Character.charValue:()C");
        UNBOX_METHODS.add("java/lang/Boolean.booleanValue:()Z");
    }

    public AutoUnboxingTransform(final DecompilerContext context) {
        super(context);
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
        if (e == null || e.isNull())
            return;

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
}
