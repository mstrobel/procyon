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

import com.strobel.assembler.metadata.*;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.strobel.core.CollectionUtilities.first;

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

                final MemberReference member = left.getUserData(Keys.MEMBER_REFERENCE);

                if (member instanceof FieldReference) {
                    final FieldDefinition resolvedField = ((FieldReference) member).resolve();

                    if (resolvedField != null &&
                        (right instanceof ObjectCreationExpression ||
                         right instanceof ArrayCreationExpression)) {

                        final String fieldName = resolvedField.getName();

                        if (resolvedField.isEnumConstant() &&
                            right instanceof ObjectCreationExpression &&
                            MetadataResolver.areEquivalent(currentType, resolvedField.getFieldType())) {

                            _valueInitializers.put(fieldName, (ObjectCreationExpression) right);
                        }
                        else if (resolvedField.isSynthetic() &&
                                 !context.getSettings().getShowSyntheticMembers() &&
                                 matchesValuesField(fieldName) &&
                                 MetadataResolver.areEquivalent(currentType.makeArrayType(), resolvedField.getFieldType())) {

                            final Statement parentStatement = findStatement(node);

                            if (parentStatement != null) {
                                parentStatement.remove();
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
            final MethodDefinition constructor = node.getUserData(Keys.METHOD_DEFINITION);

            if (currentType != null && currentType.isEnum()) {
                final List<ParameterDefinition> pDefinitions = constructor.getParameters();
                final AstNodeCollection<ParameterDeclaration> pDeclarations = node.getParameters();

                for (int i = 0; i < pDefinitions.size() && i < pDeclarations.size() && pDefinitions.get(i).isSynthetic(); i++) {
                    pDeclarations.firstOrNullObject().remove();
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
                                MetadataResolver.areEquivalent(currentType.makeArrayType(), method.getReturnType())) {

                                node.remove();
                            }
                            break;
                        }

                        case "valueOf": {
                            if (currentType.equals(method.getReturnType().resolve()) &&
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

//            assert valueFields.size() == valueInitializers.size();

            if (valueFields.isEmpty() || valueFields.size() != valueInitializers.size()) {
                return;
            }

            final MethodDeclaration typeInitializer = findMethodDeclaration(first(valueInitializers.values()));

            for (final String name : valueFields.keySet()) {
                final FieldDeclaration field = valueFields.get(name);
                final ObjectCreationExpression initializer = valueInitializers.get(name);

                assert field != null && initializer != null;

                final MethodReference constructor = (MethodReference) initializer.getUserData(Keys.MEMBER_REFERENCE);
                final MethodDefinition resolvedConstructor = constructor.resolve();

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
                    if (resolvedConstructor != null && resolvedConstructor.isSynthetic() && i++ < 2) {
                        continue;
                    }

                    argument.remove();
                    enumDeclaration.getArguments().add(argument);
                }

                if (initializer instanceof AnonymousObjectCreationExpression) {
                    final AnonymousObjectCreationExpression creation = (AnonymousObjectCreationExpression) initializer;

                    for (final EntityDeclaration member: creation.getTypeDeclaration().getMembers()){
                        member.remove();
                        enumDeclaration.getMembers().add(member);
                    }
                }

                field.replaceWith(enumDeclaration);
            }

            if (typeInitializer != null && typeInitializer.getBody().getStatements().isEmpty()) {
                typeInitializer.remove();
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

        private MethodDeclaration findMethodDeclaration(final AstNode node) {
            for (AstNode current = node; current != null; current = current.getParent()) {
                if (current instanceof MethodDeclaration) {
                    return (MethodDeclaration) current;
                }
            }
            return null;
        }
    }

    private static boolean matchesValuesField(final String fieldName) {
        return StringUtilities.equals(fieldName, "$VALUES") ||
               StringUtilities.equals(fieldName, "ENUM$VALUES");
    }
}
