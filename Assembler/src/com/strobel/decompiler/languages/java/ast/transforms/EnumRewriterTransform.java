/*
 * EnumRewriterTransform.java
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
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class EnumRewriterTransform implements IAstTransform {
    private final DecompilerContext _context;

    public EnumRewriterTransform(final DecompilerContext context) {
        _context = VerifyArgument.notNull(context, "context");
    }

    @Override
    public void run(final AstNode compilationUnit) {
        compilationUnit.acceptVisitor(new Visitor(_context), null);
    }

    private final static class Visitor extends ContextTrackingVisitor<Void> {
        private Map<String, FieldDeclaration> _valueFields = new LinkedHashMap<>();
        private Map<String, ObjectCreationExpression> _valueInitializers = new LinkedHashMap<>();

        protected Visitor(final DecompilerContext context) {
            super(context);
        }

        @Override
        public Void visitTypeDeclaration(final TypeDeclaration typeDeclaration, final Void _) {
            final Map<String, FieldDeclaration> oldValueFields = _valueFields;
            final Map<String, ObjectCreationExpression> oldValueInitializers = _valueInitializers;

            final LinkedHashMap<String, FieldDeclaration> valueFields = new LinkedHashMap<>();
            final LinkedHashMap<String, ObjectCreationExpression> valueInitializers = new LinkedHashMap<>();

            _valueFields = valueFields;
            _valueInitializers = valueInitializers;

            try {
                super.visitTypeDeclaration(typeDeclaration, _);
            }
            finally {
                _valueFields = oldValueFields;
                _valueInitializers = oldValueInitializers;
            }

            rewrite(valueFields, valueInitializers);

            return null;
        }

        @Override
        public Void visitFieldDeclaration(final FieldDeclaration node, final Void data) {
            final TypeDefinition currentType = context.getCurrentType();

            if (currentType != null && currentType.isEnum()) {
                final FieldDefinition field = node.getUserData(Keys.FIELD_DEFINITION);

                if (field != null) {
                    if (field.isEnumConstant()) {
                        _valueFields.put(field.getName(), node);
                    }
                }
            }

            return super.visitFieldDeclaration(node, data);
        }

        @Override
        public Void visitAssignmentExpression(final AssignmentExpression node, final Void data) {
            final TypeDefinition currentType = context.getCurrentType();
            final MethodDefinition currentMethod = context.getCurrentMethod();

            if (currentType != null &&
                currentMethod != null &&
                currentType.isEnum() &&
                currentMethod.isTypeInitializer()) {

                final Expression left = node.getLeft();
                final Expression right = node.getRight();

                if (left instanceof IdentifierExpression &&
                    (right instanceof ObjectCreationExpression ||
                     right instanceof ArrayCreationExpression)) {

                    final MemberReference member = left.getUserData(Keys.MEMBER_REFERENCE);

                    if (member instanceof FieldReference) {
                        final FieldDefinition resolvedField = ((FieldReference) member).resolve();

                        if (resolvedField != null) {
                            if (resolvedField.isEnumConstant() && right instanceof ObjectCreationExpression) {
                                _valueInitializers.put(resolvedField.getName(), (ObjectCreationExpression) right);
                            }
                            else if (resolvedField.isSynthetic() && "$VALUES".equals(resolvedField.getName())) {
                                final Statement parentStatement = findStatement(node);

                                if (parentStatement != null) {
                                    parentStatement.remove();
                                }
                            }
                        }
                    }
                }
            }

            return super.visitAssignmentExpression(node, data);
        }

        @Override
        public Void visitConstructorDeclaration(final ConstructorDeclaration node, final Void _) {
            final TypeDefinition currentType = context.getCurrentType();

            if (currentType != null && currentType.isEnum()) {
                final AstNodeCollection<ParameterDeclaration> parameters = node.getParameters();

                for (int i = 0; i < 2 && !parameters.isEmpty(); i++) {
                    parameters.firstOrNullObject().remove();
                }

                final Statement firstStatement = node.getBody().getStatements().firstOrNullObject();

                if (firstStatement instanceof ExpressionStatement) {
                    final Expression e = ((ExpressionStatement) firstStatement).getExpression();

                    if (e instanceof InvocationExpression && ((InvocationExpression) e).getTarget() instanceof SuperReferenceExpression) {
                        firstStatement.remove();
                    }
                }

                if (node.getBody().getStatements().isEmpty()) {
                    node.remove();
                }
            }

            return super.visitConstructorDeclaration(node, _);
        }

        @Override
        public Void visitMethodDeclaration(final MethodDeclaration node, final Void _) {
            final TypeDefinition currentType = context.getCurrentType();

            if (currentType != null && currentType.isEnum() && !context.getSettings().getShowSyntheticMembers()) {
                final MethodDefinition method = node.getUserData(Keys.METHOD_DEFINITION);

                if (method != null &&
                    method.isPublic() &&
                    method.isStatic()) {

                    switch (method.getName()) {
                        case "values": {
                            if (method.getParameters().isEmpty() &&
                                currentType.makeArrayType().equals(method.getReturnType())) {

                                node.remove();
                            }
                            break;
                        }

                        case "valueOf": {
                            if (currentType.equals(method.getReturnType()) &&
                                method.getParameters().size() == 1) {

                                final ParameterDefinition p = method.getParameters().get(0);

                                if ("java/lang/String".equals(p.getParameterType().getInternalName())) {
                                    node.remove();
                                }
                            }
                            break;
                        }
                    }
                }
            }

            return super.visitMethodDeclaration(node, _);
        }

        private void rewrite(
            final LinkedHashMap<String, FieldDeclaration> valueFields,
            final LinkedHashMap<String, ObjectCreationExpression> valueInitializers) {

            assert valueFields.size() == valueInitializers.size();

            if (valueFields.isEmpty()) {
                return;
            }

            for (final String name : valueFields.keySet()) {
                final FieldDeclaration field = valueFields.get(name);
                final ObjectCreationExpression initializer = valueInitializers.get(name);

                assert field != null && initializer != null;

                final EnumValueDeclaration enumDeclaration = new EnumValueDeclaration();
                final Statement initializerStatement = findStatement(initializer);

                assert initializerStatement != null;

                initializerStatement.remove();

                enumDeclaration.setName(name);
                enumDeclaration.putUserData(Keys.FIELD_DEFINITION, field.getUserData(Keys.FIELD_DEFINITION));
                enumDeclaration.putUserData(Keys.MEMBER_REFERENCE, field.getUserData(Keys.MEMBER_REFERENCE));

                int i = 0;

                final AstNodeCollection<Expression> arguments = initializer.getArguments();

                for (final Expression argument : arguments) {
                    if (i++ < 2) {
                        continue;
                    }

                    argument.remove();
                    enumDeclaration.getArguments().add(argument);
                }

                field.replaceWith(enumDeclaration);
            }
        }

        private Statement findStatement(final AstNode node) {
            for (AstNode current = node; current != null; current = current.getParent()) {
                if (current instanceof Statement) {
                    return (Statement) current;
                }
            }
            return null;
        }
    }
}
