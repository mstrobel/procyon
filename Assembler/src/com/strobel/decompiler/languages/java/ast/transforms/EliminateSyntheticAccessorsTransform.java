/*
 * EliminateOuterClassAccessMethodsTransform.java
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
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.strobel.core.CollectionUtilities.getOrDefault;

public class EliminateSyntheticAccessorsTransform extends ContextTrackingVisitor<Void> {
    private final List<AstNode> _nodesToRemove;
    private final Set<ParameterDefinition> _parametersToRemove;
    private final Map<String, MethodDeclaration> _accessMethodDeclarations;

    public EliminateSyntheticAccessorsTransform(final DecompilerContext context) {
        super(context);

        _nodesToRemove = new ArrayList<>();
        _parametersToRemove = new HashSet<>();
        _accessMethodDeclarations = new HashMap<>();
    }

    @Override
    public void run(final AstNode compilationUnit) {
        //
        // First run through and locate any outer class member access methods.
        //
        new PhaseOneVisitor().run(compilationUnit);

        super.run(compilationUnit);

        for (final AstNode node : _nodesToRemove) {
            node.remove();
        }
    }

    private static String makeMethodKey(final MethodReference method) {
        return method.getFullName() + ":" + method.getErasedSignature();
    }

    @Override
    public Void visitInvocationExpression(final InvocationExpression node, final Void data) {
        super.visitInvocationExpression(node, data);

        final Expression target = node.getTarget();
        final AstNodeCollection<Expression> arguments = node.getArguments();

        if (target instanceof MemberReferenceExpression && arguments.size() == 1) {
            final MemberReferenceExpression memberReference = (MemberReferenceExpression) target;

            MemberReference reference = memberReference.getUserData(Keys.MEMBER_REFERENCE);

            if (reference == null) {
                reference = node.getUserData(Keys.MEMBER_REFERENCE);
            }

            if (reference instanceof MethodReference) {
                final MethodReference method = (MethodReference) reference;
                final MethodDefinition resolvedMethod = method.resolve();

                if (resolvedMethod != null && resolvedMethod.isConstructor()) {
                    final TypeDefinition declaringType = resolvedMethod.getDeclaringType();

                    if (declaringType.isInnerClass() || declaringType.isAnonymous()) {
                        for (final ParameterDefinition p : resolvedMethod.getParameters()) {
                            if (_parametersToRemove.contains(p)) {
                                final int parameterIndex = p.getPosition();
                                final Expression argumentToRemove = getOrDefault(arguments, parameterIndex);

                                if (argumentToRemove != null) {
                                    _nodesToRemove.add(argumentToRemove);
                                }
                            }
                        }
                    }
                }

                final String key = makeMethodKey(resolvedMethod != null ? resolvedMethod : method);
                final MethodDeclaration declaration = _accessMethodDeclarations.get(key);

                if (declaration != null) {
                    final MethodDefinition definition = declaration.getUserData(Keys.METHOD_DEFINITION);

                    if (definition != null && definition.getParameters().size() == 1) {
                        final AstNode inlinedBody = InliningHelper.inlineMethod(
                            declaration,
                            Collections.singletonMap(
                                definition.getParameters().get(0),
                                arguments.firstOrNullObject()
                            )
                        );

                        if (inlinedBody instanceof Expression) {
                            node.replaceWith(inlinedBody);
                        }
                    }
                }
            }
        }

        return null;
    }

    // <editor-fold defaultstate="collapsed" desc="PhaseOneVisitor Class">

    private class PhaseOneVisitor extends ContextTrackingVisitor<Void> {
        private PhaseOneVisitor() {
            super(EliminateSyntheticAccessorsTransform.this.context);
        }

        @Override
        public Void visitMethodDeclaration(final MethodDeclaration node, final Void _) {
            final MethodDefinition method = node.getUserData(Keys.METHOD_DEFINITION);

            if (method != null &&
                method.isSynthetic() &&
                StringUtilities.startsWith(method.getName(), "access$") &&
                node.getBody().getStatements().size() == 1) {

                final Statement firstStatement = node.getBody().getStatements().firstOrNullObject();

                if (firstStatement instanceof ReturnStatement &&
                    ((ReturnStatement) firstStatement).getExpression() instanceof MemberReferenceExpression) {

                    if (method.isSynthetic() && method.isStatic()) {
                        final List<ParameterDefinition> p = method.getParameters();

                        if (p.size() == 1) {
                            _accessMethodDeclarations.put(makeMethodKey(method), node);
                        }
                    }
                }
            }

            return super.visitMethodDeclaration(node, _);
        }
    }

    // </editor-fold>
}
