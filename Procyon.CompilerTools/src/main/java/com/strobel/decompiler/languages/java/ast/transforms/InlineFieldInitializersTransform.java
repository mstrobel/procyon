/*
 * InlineFieldInitializersTransform.java
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

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.AnyNode;
import com.strobel.decompiler.patterns.Choice;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.MemberReferenceTypeNode;
import com.strobel.decompiler.patterns.Pattern;
import com.strobel.decompiler.patterns.TypedNode;

import java.util.HashMap;
import java.util.Map;

import static com.strobel.core.CollectionUtilities.firstOrDefault;

public class InlineFieldInitializersTransform extends ContextTrackingVisitor<Void> {
    private final Map<String, FieldDeclaration> _fieldDeclarations;
    private final Map<String, AssignmentExpression> _initializers;

    private MethodDefinition _currentInitializerMethod;

    public InlineFieldInitializersTransform(final DecompilerContext context) {
        super(context);

        _fieldDeclarations = new HashMap<>();
        _initializers = new HashMap<>();
    }

    @Override
    public void run(final AstNode compilationUnit) {
        new ContextTrackingVisitor<Void>(context) {
            @Override
            public Void visitFieldDeclaration(final FieldDeclaration node, final Void _) {
                final FieldDefinition field = node.getUserData(Keys.FIELD_DEFINITION);

                if (field != null) {
                    _fieldDeclarations.put(field.getFullName(), node);
                }

                return super.visitFieldDeclaration(node, _);
            }
        }.run(compilationUnit);

        super.run(compilationUnit);

        inlineInitializers();
    }

    private void inlineInitializers() {
        for (final String fieldName : _initializers.keySet()) {
            final FieldDeclaration declaration = _fieldDeclarations.get(fieldName);

            if (declaration != null &&
                declaration.getVariables().firstOrNullObject().getInitializer().isNull()) {

                final AssignmentExpression assignment = _initializers.get(fieldName);
                final Expression value = assignment.getRight();

                value.remove();
                declaration.getVariables().firstOrNullObject().setInitializer(value);

                final AstNode parent = assignment.getParent();

                if (parent instanceof ExpressionStatement) {
                    parent.remove();
                }
                else {
                    final Expression left = assignment.getLeft();

                    left.remove();
                    parent.replaceWith(left);
                }
            }
        }
    }

    @Override
    public Void visitConstructorDeclaration(final ConstructorDeclaration node, final Void _) {
        final MethodDefinition oldInitializer = _currentInitializerMethod;
        final MethodDefinition method = node.getUserData(Keys.METHOD_DEFINITION);

        if (method != null &&
            method.isConstructor() &&
            method.isSynthetic()) {

            _currentInitializerMethod = method;
        }
        else {
            _currentInitializerMethod = null;
        }

        try {
            return super.visitConstructorDeclaration(node, _);
        }
        finally {
            _currentInitializerMethod = oldInitializer;
        }
    }

    @Override
    public Void visitMethodDeclaration(final MethodDeclaration node, final Void _) {
        final MethodDefinition oldInitializer = _currentInitializerMethod;
        final MethodDefinition method = node.getUserData(Keys.METHOD_DEFINITION);

        if (method != null &&
            method.isTypeInitializer() &&
            method.getDeclaringType().isInterface()) {

            _currentInitializerMethod = method;
        }
        else {
            _currentInitializerMethod = null;
        }

        try {
            return super.visitMethodDeclaration(node, _);
        }
        finally {
            _currentInitializerMethod = oldInitializer;
        }
    }

    private final static INode FIELD_ASSIGNMENT;

    static {
        FIELD_ASSIGNMENT = new Choice(
            new AssignmentExpression(
                new MemberReferenceTypeNode(
                    "target",
                    new Choice(
                        new MemberReferenceExpression(
                            new Choice(
                                new TypedNode(AstType.class),
                                new TypedNode(ThisReferenceExpression.class)
                            ).toExpression(),
                            Pattern.ANY_STRING
                        ),
                        new IdentifierExpression(Pattern.ANY_STRING)
                    ).toExpression(),
                    FieldReference.class
                ).toExpression(),
                AssignmentOperatorType.ASSIGN,
                new AnyNode("value").toExpression()
            )
        );
    }

    @Override
    public Void visitAssignmentExpression(final AssignmentExpression node, final Void data) {
        super.visitAssignmentExpression(node, data);

        if (_currentInitializerMethod == null) {
            return null;
        }

        final Match match = FIELD_ASSIGNMENT.match(node);

        if (match.success()) {
            final Expression target = (Expression) firstOrDefault(match.get("target"));
            final MemberReference reference = target.getUserData(Keys.MEMBER_REFERENCE);

            _initializers.put(reference.getFullName(), node);
        }

        return null;
    }
}
