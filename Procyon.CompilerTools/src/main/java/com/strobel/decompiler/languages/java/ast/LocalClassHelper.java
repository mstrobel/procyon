/*
 * LocalClassHelper.java
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

package com.strobel.decompiler.languages.java.ast;

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.strobel.core.CollectionUtilities.getOrDefault;

public final class LocalClassHelper {
    public static void replaceClosureMembers(final DecompilerContext context, final AnonymousObjectCreationExpression node) {
        replaceClosureMembers(context, node.getTypeDeclaration(), Collections.singletonList(node));
    }

    public static void replaceClosureMembers(
        final DecompilerContext context,
        final TypeDeclaration declaration,
        final List<? extends ObjectCreationExpression> instantiations) {

        VerifyArgument.notNull(context, "context");

        final TypeDeclaration root = VerifyArgument.notNull(declaration, "declaration");
        final Map<String, Expression> initializers = new HashMap<>();
        final Map<String, Expression> replacements = new HashMap<>();
        final List<AstNode> nodesToRemove = new ArrayList<>();
        final List<ParameterDefinition> parametersToRemove = new ArrayList<>();
        final List<Expression> originalArguments;

        if (instantiations.isEmpty()) {
            originalArguments = Collections.emptyList();
        }
        else {
            originalArguments = new ArrayList<>(instantiations.get(0).getArguments());
        }

        new PhaseOneVisitor(context, originalArguments, replacements, initializers, parametersToRemove, nodesToRemove).run(root);
        new PhaseTwoVisitor(context, replacements, initializers).run(root);

        for (final ObjectCreationExpression instantiation : instantiations) {
            for (final ParameterDefinition p : parametersToRemove) {
                final Expression argumentToRemove = getOrDefault(instantiation.getArguments(), p.getPosition());

                if (argumentToRemove != null) {
                    instantiation.getArguments().remove(argumentToRemove);
                }
            }
        }

        for (final AstNode n : nodesToRemove) {
            if (n instanceof Expression) {
                final int argumentIndex = originalArguments.indexOf(n);

                if (argumentIndex >= 0) {
                    for (final ObjectCreationExpression instantiation : instantiations) {
                        final Expression argumentToRemove = getOrDefault(instantiation.getArguments(), argumentIndex);

                        if (argumentToRemove != null) {
                            argumentToRemove.remove();
                        }
                    }
                }
            }

            n.remove();
        }
    }

    private final static class PhaseOneVisitor extends ContextTrackingVisitor<Void> {
        private final Map<String, Expression> _replacements;
        private final List<Expression> _originalArguments;
        private final List<ParameterDefinition> _parametersToRemove;
        private final Map<String, Expression> _initializers;
        private final List<AstNode> _nodesToRemove;

        private boolean _baseConstructorCalled;

        public PhaseOneVisitor(
            final DecompilerContext context,
            final List<Expression> originalArguments,
            final Map<String, Expression> replacements,
            final Map<String, Expression> initializers,
            final List<ParameterDefinition> parametersToRemove,
            final List<AstNode> nodesToRemove) {

            super(context);

            _originalArguments = VerifyArgument.notNull(originalArguments, "originalArguments");
            _replacements = VerifyArgument.notNull(replacements, "replacements");
            _initializers = VerifyArgument.notNull(initializers, "initializers");
            _parametersToRemove = VerifyArgument.notNull(parametersToRemove, "parametersToRemove");
            _nodesToRemove = VerifyArgument.notNull(nodesToRemove, "nodesToRemove");
        }

        @Override
        public Void visitConstructorDeclaration(final ConstructorDeclaration node, final Void _) {
            final boolean wasDone = _baseConstructorCalled;

            _baseConstructorCalled = false;

            try {
                return super.visitConstructorDeclaration(node, _);
            }
            finally {
                _baseConstructorCalled = wasDone;
            }
        }

        @Override
        protected Void visitChildren(final AstNode node, final Void _) {
            final MethodDefinition currentMethod = context.getCurrentMethod();

            if (currentMethod != null && !(currentMethod.isConstructor()/* && currentMethod.isSynthetic()*/)) {
                return null;
            }

            return super.visitChildren(node, _);
        }

        @Override
        public Void visitSuperReferenceExpression(final SuperReferenceExpression node, final Void _) {
            super.visitSuperReferenceExpression(node, _);

            if (context.getCurrentMethod() != null &&
                context.getCurrentMethod().isConstructor() &&
                node.getParent() instanceof InvocationExpression) {

                //
                // We only care about field initializations that occur before the base constructor call.
                //
                _baseConstructorCalled = true;
            }

            return null;
        }

        @Override
        public Void visitAssignmentExpression(final AssignmentExpression node, final Void _) {
            super.visitAssignmentExpression(node, _);

            if (context.getCurrentMethod() == null || !context.getCurrentMethod().isConstructor()) {
                return null;
            }

            final Expression left = node.getLeft();
            final Expression right = node.getRight();

            if (left instanceof MemberReferenceExpression) {
                if (right instanceof IdentifierExpression) {
                    final Variable variable = right.getUserData(Keys.VARIABLE);

                    if (variable == null || !variable.isParameter()) {
                        return null;
                    }

                    final MemberReferenceExpression memberReference = (MemberReferenceExpression) left;
                    final MemberReference member = memberReference.getUserData(Keys.MEMBER_REFERENCE);

                    if (member instanceof FieldReference &&
                        memberReference.getTarget() instanceof ThisReferenceExpression) {

                        final FieldDefinition resolvedField = ((FieldReference) member).resolve();

                        if (resolvedField != null && resolvedField.isSynthetic()) {
                            final ParameterDefinition parameter = variable.getOriginalParameter();

                            int parameterIndex = parameter.getPosition();

                            if (parameter.getMethod().getParameters().size() > _originalArguments.size()) {
                                parameterIndex -= (parameter.getMethod().getParameters().size() - _originalArguments.size());
                            }

                            if (parameterIndex >= 0 && parameterIndex < _originalArguments.size()) {
                                final Expression argument = _originalArguments.get(parameterIndex);

                                if (argument == null) {
                                    return null;
                                }

                                _nodesToRemove.add(argument);

                                if (argument instanceof ThisReferenceExpression) {
                                    //
                                    // Don't replace outer class references; they will be rewritten later.
                                    //
                                    markConstructorParameterForRemoval(node, parameter);
                                    return null;
                                }

                                _parametersToRemove.add(parameter);

                                final String fullName = member.getFullName();

                                if (!hasSideEffects(argument)) {
                                    _replacements.put(fullName, argument);
                                }
                                else {
                                    context.getForcedVisibleMembers().add(resolvedField);
                                    _initializers.put(fullName, argument);
                                }

                                if (node.getParent() instanceof ExpressionStatement) {
                                    _nodesToRemove.add(node.getParent());
                                }

                                markConstructorParameterForRemoval(node, parameter);
                            }
                        }
                        else if (_baseConstructorCalled &&
                                 resolvedField != null &&
                                 context.getCurrentMethod().isConstructor() &&
                                 (!context.getCurrentMethod().isSynthetic() ||
                                  context.getSettings().getShowSyntheticMembers())) {

                            final MemberReferenceExpression leftMemberReference = (MemberReferenceExpression) left;
                            final MemberReference leftMember = leftMemberReference.getUserData(Keys.MEMBER_REFERENCE);
                            final Variable rightVariable = right.getUserData(Keys.VARIABLE);

                            if (rightVariable.isParameter()) {
                                final ParameterDefinition parameter = variable.getOriginalParameter();
                                final int parameterIndex = parameter.getPosition();

                                if (parameterIndex >= 0 && parameterIndex < _originalArguments.size()) {
                                    final Expression argument = _originalArguments.get(parameterIndex);

                                    if (parameterIndex == 0 &&
                                        argument instanceof ThisReferenceExpression &&
                                        isLocalOrAnonymous(context.getCurrentType())) {

                                        //
                                        // Don't replace outer class references; they will be rewritten later.
                                        //
                                        return null;
                                    }

                                    final FieldDefinition resolvedTargetField = ((FieldReference) leftMember).resolve();

                                    if (resolvedTargetField != null && !resolvedTargetField.isSynthetic()) {
                                        _parametersToRemove.add(parameter);
                                        _initializers.put(resolvedTargetField.getFullName(), argument);

                                        if (node.getParent() instanceof ExpressionStatement) {
                                            _nodesToRemove.add(node.getParent());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else if (_baseConstructorCalled && right instanceof MemberReferenceExpression) {
                    final MemberReferenceExpression leftMemberReference = (MemberReferenceExpression) left;
                    final MemberReference leftMember = leftMemberReference.getUserData(Keys.MEMBER_REFERENCE);
                    final MemberReferenceExpression rightMemberReference = (MemberReferenceExpression) right;
                    final MemberReference rightMember = right.getUserData(Keys.MEMBER_REFERENCE);

                    if (rightMember instanceof FieldReference &&
                        rightMemberReference.getTarget() instanceof ThisReferenceExpression) {

                        final FieldDefinition resolvedTargetField = ((FieldReference) leftMember).resolve();
                        final FieldDefinition resolvedSourceField = ((FieldReference) rightMember).resolve();

                        if (resolvedSourceField != null &&
                            resolvedTargetField != null &&
                            resolvedSourceField.isSynthetic() &&
                            !resolvedTargetField.isSynthetic()) {

                            final Expression initializer = _replacements.get(rightMember.getFullName());

                            if (initializer != null) {
                                _initializers.put(resolvedTargetField.getFullName(), initializer);

                                if (node.getParent() instanceof ExpressionStatement) {
                                    _nodesToRemove.add(node.getParent());
                                }
                            }
                        }
                    }
                }
            }

            return null;
        }

        private void markConstructorParameterForRemoval(final AssignmentExpression node, final ParameterDefinition parameter) {
            final ConstructorDeclaration constructorDeclaration = node.getParent(ConstructorDeclaration.class);

            if (constructorDeclaration != null) {
                final AstNodeCollection<ParameterDeclaration> parameters = constructorDeclaration.getParameters();

                for (final ParameterDeclaration p : parameters) {
                    if (p.getUserData(Keys.PARAMETER_DEFINITION) == parameter) {
                        _nodesToRemove.add(p);
                        break;
                    }
                }
            }
        }
    }

    private static boolean isLocalOrAnonymous(final TypeDefinition type) {
        if (type == null) {
            return false;
        }
        return type.isLocalClass() || type.isAnonymous();
    }

    private static boolean hasSideEffects(final Expression e) {
        if (e instanceof IdentifierExpression ||
            e instanceof PrimitiveExpression ||
            e instanceof ThisReferenceExpression ||
            e instanceof SuperReferenceExpression ||
            e instanceof NullReferenceExpression ||
            e instanceof ClassOfExpression) {

            return false;
        }

        return true;
    }

    private final static class PhaseTwoVisitor extends ContextTrackingVisitor<Void> {
        private final Map<String, Expression> _replacements;
        private final Map<String, Expression> _initializers;

        protected PhaseTwoVisitor(
            final DecompilerContext context,
            final Map<String, Expression> replacements,
            final Map<String, Expression> initializers) {

            super(context);

            _replacements = VerifyArgument.notNull(replacements, "replacements");
            _initializers = VerifyArgument.notNull(initializers, "initializers");
        }

        @Override
        public Void visitFieldDeclaration(final FieldDeclaration node, final Void data) {
            super.visitFieldDeclaration(node, data);

            final FieldDefinition field = node.getUserData(Keys.FIELD_DEFINITION);

            if (field != null &&
                !_initializers.isEmpty() &&
                node.getVariables().size() == 1 &&
                node.getVariables().firstOrNullObject().getInitializer().isNull()) {

                final Expression initializer = _initializers.get(field.getFullName());

                if (initializer != null) {
                    node.getVariables().firstOrNullObject().setInitializer(initializer.clone());
                }
            }

            return null;
        }

        @Override
        public Void visitMemberReferenceExpression(final MemberReferenceExpression node, final Void _) {
            super.visitMemberReferenceExpression(node, _);

            if (node.getParent() instanceof AssignmentExpression &&
                node.getRole() == AssignmentExpression.LEFT_ROLE) {

                return null;
            }

            if (node.getTarget() instanceof ThisReferenceExpression) {
                final MemberReference member = node.getUserData(Keys.MEMBER_REFERENCE);

                if (member instanceof FieldReference) {
                    final Expression replacement = _replacements.get(member.getFullName());

                    if (replacement != null) {
                        node.replaceWith(replacement.clone());
                    }
                }
            }

            return null;
        }
    }
}
