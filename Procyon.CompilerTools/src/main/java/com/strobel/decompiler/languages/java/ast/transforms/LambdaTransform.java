/*
 * LambdaTransform.java
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

import com.strobel.assembler.metadata.DynamicCallSite;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;

import java.util.HashMap;
import java.util.Map;

public class LambdaTransform extends ContextTrackingVisitor<Void> {
    private final Map<String, MethodDeclaration> _methodDeclarations;

    public LambdaTransform(final DecompilerContext context) {
        super(context);
        _methodDeclarations = new HashMap<>();
    }

    @Override
    public void run(final AstNode compilationUnit) {
        compilationUnit.acceptVisitor(
            new ContextTrackingVisitor<Void>(context) {
                @Override
                public Void visitMethodDeclaration(final MethodDeclaration node, final Void _) {
                    final MemberReference methodReference = node.getUserData(Keys.MEMBER_REFERENCE);

                    if (methodReference instanceof MethodReference) {
                        _methodDeclarations.put(makeMethodKey((MethodReference) methodReference), node);
                    }

                    return super.visitMethodDeclaration(node, _);
                }
            },
            null
        );

        super.run(compilationUnit);
    }

    @Override
    public Void visitMethodGroupExpression(final MethodGroupExpression node, final Void data) {
        final MemberReference reference = node.getUserData(Keys.MEMBER_REFERENCE);

        if (reference instanceof MethodReference) {
            final MethodReference method = (MethodReference) reference;
            final MethodDefinition resolvedMethod = method.resolve();
            final DynamicCallSite callSite = node.getUserData(Keys.DYNAMIC_CALL_SITE);

            if (resolvedMethod != null && resolvedMethod.isSynthetic() && callSite != null) {
                inlineLambda(node, resolvedMethod);
                return null;
            }
        }

        return super.visitMethodGroupExpression(node, data);
    }

    private void inlineLambda(final MethodGroupExpression methodGroup, final MethodDefinition method) {
        final MethodDeclaration declaration = _methodDeclarations.get(makeMethodKey(method));

        if (declaration == null) {
            return;
        }

        final BlockStatement body = (BlockStatement) declaration.getBody().clone();
        final AstNodeCollection<ParameterDeclaration> parameters = declaration.getParameters();
        final Map<String, String> renamedVariables = new HashMap<>();
        final AstNodeCollection<Expression> closureArguments = methodGroup.getClosureArguments();

        Expression a = closureArguments.firstOrNullObject();

        for (ParameterDeclaration p = parameters.firstOrNullObject();
             p != null && !p.isNull() && a != null && !a.isNull();
             p = (ParameterDeclaration) p.getNextSibling(), a = (Expression) a.getNextSibling()) {

            if (a instanceof IdentifierExpression) {
                renamedVariables.put(p.getName(), ((IdentifierExpression) a).getIdentifier());
            }
        }

        body.acceptVisitor(
            new ContextTrackingVisitor<Void>(context) {
                @Override
                public Void visitIdentifier(final Identifier node, final Void _) {
                    final String oldName = node.getName();

                    if (oldName != null) {
                        final String newName = renamedVariables.get(oldName);

                        if (newName != null) {
                            node.setName(newName);
                        }
                    }

                    return super.visitIdentifier(node, _);
                }

                @Override
                public Void visitIdentifierExpression(final IdentifierExpression node, final Void _) {
                    final String oldName = node.getIdentifier();

                    if (oldName != null) {
                        final String newName = renamedVariables.get(oldName);

                        if (newName != null) {
                            node.setIdentifier(newName);
                        }
                    }

                    return super.visitIdentifierExpression(node, _);
                }
            },
            null
        );

        final LambdaExpression lambda = new LambdaExpression();
        final DynamicCallSite callSite = methodGroup.getUserData(Keys.DYNAMIC_CALL_SITE);

        if (callSite != null) {
            lambda.putUserData(Keys.DYNAMIC_CALL_SITE, callSite);
        }

        body.remove();

        final Statement firstStatement = body.getStatements().firstOrNullObject();

        if (body.getStatements().size() == 1 &&
            (firstStatement instanceof ExpressionStatement || firstStatement instanceof ReturnStatement)) {

            final Expression simpleBody = firstStatement.getChildByRole(Roles.EXPRESSION);

            simpleBody.remove();
            lambda.setBody(simpleBody);
        }
        else {
            lambda.setBody(body);
        }

        int parametersToSkip = closureArguments.size();

        for (final ParameterDeclaration p : declaration.getParameters()) {
            if (parametersToSkip-- > 0) {
                continue;
            }

            final ParameterDeclaration lambdaParameter = (ParameterDeclaration) p.clone();

            lambdaParameter.setType(AstType.NULL);
            lambda.addChild(lambdaParameter, Roles.PARAMETER);
        }

        methodGroup.replaceWith(lambda);
    }

    private static String makeMethodKey(final MethodReference method) {
        return method.getFullName() + ":" + method.getErasedSignature();
    }
}
