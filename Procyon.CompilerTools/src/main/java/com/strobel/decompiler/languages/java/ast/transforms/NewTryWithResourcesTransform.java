/*
 * NewTryWithResourcesTransform.java
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

import com.strobel.assembler.metadata.Flags;
import com.strobel.assembler.metadata.LanguageFeature;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.Predicate;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.*;
import com.strobel.decompiler.semantics.ResolveResult;

import static com.strobel.core.CollectionUtilities.*;
import static com.strobel.core.Comparer.coalesce;
import static com.strobel.decompiler.languages.java.ast.transforms.ConvertLoopsTransform.*;

public class NewTryWithResourcesTransform extends ContextTrackingVisitor<Void> {
    private final Statement _resourceDeclaration;
    private final TryCatchStatement _tryPattern;
    private final JavaResolver _resolver;

    private AstBuilder _builder;

    public NewTryWithResourcesTransform(final DecompilerContext context) {
        super(context);

        _resolver = new JavaResolver(context);

        final VariableDeclarationStatement rv = new VariableDeclarationStatement(
            new AnyNode().toType(),
            Pattern.ANY_STRING,
            new AnyNode().toExpression()
        );

        rv.setAnyModifiers(true);

        _resourceDeclaration = new Choice(new NamedNode("resource", rv),
                                          new NamedNode("assignment",
                                                        new ExpressionStatement(
                                                            new AssignmentExpression(new NamedNode("resource",
                                                                                                   new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                                                                                     new NamedNode("resourceInitializer", new AnyNode()).toExpression())
                                                        ))).toStatement();

        final TryCatchStatement tryPattern = new TryCatchStatement(Expression.MYSTERY_OFFSET);

        final TryCatchStatement nestedTryWithResourceDisposal = new TryCatchStatement();

        nestedTryWithResourceDisposal.setTryBlock(new AnyNode().toBlockStatement());
        nestedTryWithResourceDisposal.getCatchClauses().add(new Repeat(new AnyNode()).toCatchClause());

        final Expression resourceReference = new Choice(new DeclaredVariableBackReference("resource"),
                                                        new IdentifierBackReference("resource")).toExpression();

        nestedTryWithResourceDisposal.setFinallyBlock(
            new BlockStatement(
                new Repeat(new AnyNode()).toStatement(),
                new NamedNode(
                    "resourceDisposal",
                    new Choice(
                        new ExpressionStatement(resourceReference.invoke("close")),
                        new IfElseStatement(
                            Expression.MYSTERY_OFFSET,
                            new BinaryOperatorExpression(resourceReference.clone(),
                                                         BinaryOperatorType.INEQUALITY,
                                                         new NullReferenceExpression()),
                            new BlockStatement(new ExpressionStatement(resourceReference.clone().invoke("close")))
                        )
                    )
                ).toStatement()
            )
        );

        final BlockStatement tryContent = new NamedNode(
            "tryContent",
            new BlockStatement(
                new Repeat(new AnyNode()).toStatement(),
                new Choice(
                    new NamedNode(
                        "resourceDisposal",
                        new Choice(
                            new ExpressionStatement(resourceReference.clone().invoke("close")),
                            new IfElseStatement(
                                Expression.MYSTERY_OFFSET,
                                new BinaryOperatorExpression(resourceReference.clone(),
                                                             BinaryOperatorType.INEQUALITY,
                                                             new NullReferenceExpression()),
                                new BlockStatement(new ExpressionStatement(resourceReference.clone().invoke("close")))
                            )
                        )
                    ),
                    nestedTryWithResourceDisposal
                ).toStatement(),
                new Repeat(
                    new NamedNode(
                        "outerResourceDisposal",
                        new Choice(
                            new IfElseStatement(
                                Expression.MYSTERY_OFFSET,
                                new BinaryOperatorExpression(new NamedNode("otherId",
                                                                           new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                                                             BinaryOperatorType.INEQUALITY,
                                                             new NullReferenceExpression()),
                                new BlockStatement(new ExpressionStatement(new IdentifierBackReference("otherId").toExpression().invoke("close")))
                            ),
                            new ExpressionStatement(new IdentifierExpression(Pattern.ANY_STRING).invoke("close"))
                        ).toStatement()
                    )
                ).toStatement(),
                new OptionalNode(new AnyNode("finalStatement")).toStatement()
            )
        ).toBlockStatement();

        tryPattern.setTryBlock(tryContent);

        final TryCatchStatement disposeTry = new TryCatchStatement(Expression.MYSTERY_OFFSET);

        disposeTry.setTryBlock(
            new BlockStatement(
                new ExpressionStatement(new NamedNode("resourceToDispose", resourceReference.clone()).toExpression().invoke("close"))
            )
        );

        final Expression outerException = new NamedNode("error", new IdentifierExpression(Pattern.ANY_STRING)).toExpression();

        final CatchClause disposeCatch = new CatchClause(
            new BlockStatement(
                new ExpressionStatement(
                    outerException.invoke("addSuppressed",
                                          new NamedNode("innerError", new IdentifierExpression(Pattern.ANY_STRING)).toExpression())
                )
            )
        );

        disposeCatch.setVariableName(Pattern.ANY_STRING);
        disposeCatch.getExceptionTypes().add(new SimpleType("Throwable"));

        disposeTry.getCatchClauses().add(disposeCatch);

        final CatchClause catchClause = new CatchClause(
            new BlockStatement(
                new Choice(new NamedNode("disposeTry", disposeTry),
                           new IfElseStatement(Expression.MYSTERY_OFFSET,
                                               new BinaryOperatorExpression(new NamedNode("resourceToDispose", resourceReference.clone()).toExpression(),
                                                                            BinaryOperatorType.INEQUALITY,
                                                                            new NullReferenceExpression()),
                                               new BlockStatement(new NamedNode("disposeTry", disposeTry).toStatement()))).toStatement(),
                new ThrowStatement(new BackReference("error").toExpression())
            )
        );

        catchClause.setVariableName(Pattern.ANY_STRING);
        catchClause.getExceptionTypes().add(new SimpleType("Throwable"));
        tryPattern.getCatchClauses().add(catchClause);

        _tryPattern = tryPattern;
    }

    @Override
    public void run(final AstNode compilationUnit) {
        if (_tryPattern == null || !context.isSupported(LanguageFeature.TRY_WITH_RESOURCES)) {
            return;
        }

        final AstBuilder builder = context.getUserData(Keys.AST_BUILDER);

        if (builder == null) {
            return;
        }

        final AstBuilder oldBuilder = _builder;

        _builder = builder;

        try {
            super.run(compilationUnit);
            new EmptyTryWithResourcesRewriter().run(compilationUnit); // check for empty patterns last.
        }
        finally {
            _builder = oldBuilder;
        }
    }

    @Override
    public Void visitTryCatchStatement(final TryCatchStatement node, final Void data) {
        super.visitTryCatchStatement(node, data);

        if (!(node.getParent() instanceof BlockStatement) || node.getCatchClauses().firstOrNullObject().isNull()) {
            return null;
        }

        final BlockStatement parent = (BlockStatement) node.getParent();
        final Statement initResource = node.getPreviousSibling(BlockStatement.STATEMENT_ROLE);

        Match m = Match.createNew();

        m.add("resource", new IdentifierExpression(Pattern.ANY_STRING));

        if (_tryPattern.getCatchClauses().firstOrNullObject().matches(node.getCatchClauses().firstOrNullObject(), m)) {
            final AstNode declaration;
            final boolean isParameter;

            final IdentifierExpression resource = firstOrDefault(m.<IdentifierExpression>get("resourceToDispose"));

            if (resource == null) {
                return null;
            }

            m = Match.createNew();
            m.add("resource", resource);

            if (!_tryPattern.matches(node, m)) {
                return null;
            }

            if (initResource != null && _resourceDeclaration.matches(initResource, m)) {
                declaration = firstOrDefault(skip(m.<AstNode>get("resource"), 1));
                isParameter = false;
            }
            else {
                final ParameterDeclaration p = findDeclaration(resource, node);

                if (p == null || !context.isSupported(LanguageFeature.TRY_EXPRESSION_RESOURCE)) {
                    return null;
                }

                declaration = p;
                isParameter = true;
            }

            final Statement declarationStatement;
            final TypeReference resourceType;

            if (isParameter) {
                declarationStatement = null;
                resourceType = ((ParameterDeclaration) declaration).getType().toTypeReference();
            }
            else {
                if (!(declaration instanceof VariableDeclarationStatement || declaration instanceof IdentifierExpression)) {
                    return null;
                }

                declarationStatement = declaration instanceof Statement ? (Statement) declaration : declaration.getParent(Statement.class);

                if (declarationStatement == null ||
                    declarationStatement.isNull() ||
                    !(declarationStatement.getParent() instanceof BlockStatement) ||
                    !canMoveVariableDeclaration(declarationStatement, resource, node)) {
                    return null;
                }

                final ResolveResult resourceResult = _resolver.apply(declaration);

                if (resourceResult == null) {
                    return null;
                }

                resourceType = resourceResult.getType();
            }

            if (resourceType == null) {
                return null;
            }

            final BlockStatement tryContent = first(m.<BlockStatement>get("tryContent"));
            final IdentifierExpression caughtException = first(m.<IdentifierExpression>get("error"));
            final IdentifierExpression innerError = first(m.<IdentifierExpression>get("innerError"));

            final CatchClause caughtParent = firstOrDefault(caughtException.getAncestors(CatchClause.class),
                                                            new Predicate<CatchClause>() {
                                                                @Override
                                                                public boolean test(final CatchClause clause) {
                                                                    return StringUtilities.equals(caughtException.getIdentifier(), clause.getVariableName());
                                                                }
                                                            });

            final CatchClause innerErrorParent = firstOrDefault(innerError.getAncestors(CatchClause.class),
                                                                new Predicate<CatchClause>() {
                                                                    @Override
                                                                    public boolean test(final CatchClause clause) {
                                                                        return StringUtilities.equals(innerError.getIdentifier(), clause.getVariableName());
                                                                    }
                                                                });

            if (caughtParent == null || innerErrorParent == null || !caughtParent.isAncestorOf(innerErrorParent)) {
                return null;
            }

            if (notEffectivelyFinal(resource.getIdentifier(), tryContent, null)) {
                return null;
            }

            final VariableDeclarationStatement vd;

            if (isParameter) {
                final ParameterDeclaration pd = (ParameterDeclaration) declaration;
                final IdentifierExpression resourceId = new IdentifierExpression(pd.getName());
                final ParameterDefinition p = pd.getUserData(Keys.PARAMETER_DEFINITION);
                final Variable v = pd.getUserData(Keys.VARIABLE);

                if (p != null) {
                    resourceId.putUserData(Keys.PARAMETER_DEFINITION, p);
                }
                if (v != null) {
                    resourceId.putUserData(Keys.VARIABLE, v);
                }

                node.getExternalResources().add(resourceId);
            }
            else {
                if (declaration instanceof VariableDeclarationStatement) {
                    vd = (VariableDeclarationStatement) declaration;
                }
                else {
                    final IdentifierExpression identifier = (IdentifierExpression) declaration;
                    final AssignmentExpression assignment = declaration.getParent(AssignmentExpression.class);

                    if (assignment == null || assignment.isNull()) {
                        return null;
                    }

                    final Expression initializer = assignment.getRight();

                    initializer.remove();
                    vd = new VariableDeclarationStatement(_builder.convertType(resourceType), identifier.getIdentifier(), initializer);
                }

                vd.addModifier(Flags.Flag.FINAL);
                declarationStatement.remove();
                node.getDeclaredResources().add(vd);
            }

            caughtParent.remove();

            final AstNode resourceDisposal = firstOrDefault(m.<AstNode>get("resourceDisposal"));

            if (resourceDisposal != null) {
                resourceDisposal.remove();
            }

            for (final Statement outerResourceDisposal : m.<Statement>get("outerResourceDisposal")) {
                outerResourceDisposal.remove();
                parent.getStatements().insertAfter(node, outerResourceDisposal);
            }
        }

        return null;
    }

    private boolean notEffectivelyFinal(final String resourceName, final BlockStatement scope, final Statement startingPoint) {
        final Statement lastStatement;
        final Statement firstStatement = coalesce(startingPoint, firstOrDefault(scope.getStatements()));

        if (firstStatement == null || (lastStatement = lastOrDefault(scope.getStatements())) == null) {
            return false;
        }

        final DefiniteAssignmentAnalysis analysis = new DefiniteAssignmentAnalysis(context, scope);

        analysis.setAnalyzedRange(firstStatement, lastStatement);
        analysis.analyze(resourceName, DefiniteAssignmentStatus.DEFINITELY_NOT_ASSIGNED);

        // Resource declarations are effectively final; if it's reassigned, we can't rewrite.
        return analysis.isPotentiallyAssigned();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean canMoveVariableDeclaration(final Statement initializeResource, final IdentifierExpression resource, final Statement node) {
        final AstNode parent = node.getParent();
        final VariableDeclarationStatement resourceDeclaration = findVariableDeclaration(node, resource.getIdentifier());

        if (resourceDeclaration == null || !(resourceDeclaration.getParent() instanceof BlockStatement) || !(parent instanceof BlockStatement)) {
            return false;
        }

        final BlockStatement outerTemp = new BlockStatement();
        final BlockStatement temp = new BlockStatement();
        final BlockStatement placeholder = new BlockStatement();

        initializeResource.replaceWith(placeholder);

        node.replaceWith(outerTemp);

        temp.add(initializeResource);
        temp.add(node);

        outerTemp.add(temp);

        //
        // Now verify that we can move the variable declaration into the 'try'.
        //

        final Statement declarationPoint = canMoveVariableDeclarationIntoStatement(context, resourceDeclaration, node);

        node.remove();
        outerTemp.replaceWith(node);

        initializeResource.remove();
        placeholder.replaceWith(initializeResource);

        //
        // Can we move the declaration into the 'try' block?
        //
        return declarationPoint == outerTemp;
    }

    private static ParameterDeclaration findDeclaration(final IdentifierExpression id, final AstNode source) {
        if (id == null || StringUtilities.isNullOrEmpty(id.getIdentifier()) || Pattern.ANY_STRING.equals(id.getIdentifier())) {
            return null;
        }

        final NameResolveResult rr = JavaNameResolver.resolve(id.getIdentifier(), source);

        if (!rr.hasMatch() || rr.isAmbiguous()) {
            return null;
        }

        final Object c = first(rr.getCandidates());
        final boolean isParameter = c instanceof ParameterDefinition;

        if (!isParameter) {
            return null;
        }

        for (final MethodDeclaration md : source.getAncestors(MethodDeclaration.class)) {
            for (final ParameterDeclaration pd : md.getParameters()) {
                if (pd.getUserData(Keys.PARAMETER_DEFINITION) == c) {
                    return pd;
                }
            }
        }

        return null;
    }

    protected final class EmptyTryWithResourcesRewriter extends ContextTrackingVisitor<Void> {
        private final IfElseStatement _emptyPattern = new IfElseStatement(
            new BinaryOperatorExpression(new NamedNode("resource", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                                         BinaryOperatorType.INEQUALITY,
                                         new NullReferenceExpression()),
            new BlockStatement(
                new ExpressionStatement(new IdentifierBackReference("resource").toExpression().invoke("close"))
            )
        );

        public EmptyTryWithResourcesRewriter() {
            super(NewTryWithResourcesTransform.this.context);
        }

        @Override
        public Void visitIfElseStatement(final IfElseStatement node, final Void data) {
            super.visitIfElseStatement(node, data);

            final Match m = Match.createNew();

            if (!_emptyPattern.matches(node, m)) {
                return null;
            }

            final IdentifierExpression resource = first(m.<IdentifierExpression>get("resource"));
            final String resourceId = resource.getIdentifier();

            final MethodDeclaration md = node.getParent(MethodDeclaration.class);
            final BlockStatement body = md != null ? md.getBody() : null;

            if (body == null || body.isNull()) {
                return null;
            }

            final VariableDeclarationStatement vd = findVariableDeclaration(node, resourceId);
            final Statement declaration;
            final boolean isExternal;

            Expression initializer = null;

            if (vd != null) {
                final Statement prev = node.getPreviousStatement();
                final Match m2 = Match.createNew();

                if (_resourceDeclaration.matches(prev, m2)) {
                    declaration = coalesce(firstOrDefault(m2.<Statement>get("assignment")), firstOrDefault(m2.<Statement>get("resource")));
                    initializer = firstOrDefault(m2.<Expression>get("resourceInitializer"));
                }
                else {
                    declaration = vd;
                }

                if (!canMoveVariableDeclaration(declaration, resource, node) || notEffectivelyFinal(resourceId, body, node)) {
                    return null;
                }

                isExternal = false;
            }
            else {
                if (!context.isSupported(LanguageFeature.TRY_EXPRESSION_RESOURCE)) {
                    return null;
                }

                final NameResolveResult rr = JavaNameResolver.resolve(resourceId, node);

                if (!rr.hasMatch() || rr.isAmbiguous()) {
                    return null;
                }

                final Object d = first(rr.getCandidates());

                if (!(d instanceof ParameterDefinition) || notEffectivelyFinal(resourceId, body, null)) {
                    return null;
                }

                declaration = null;
                isExternal = true;
            }

            resource.remove();

            if (declaration != null) {
                declaration.remove();
            }

            final TryCatchStatement tryCatch = new TryCatchStatement();

            tryCatch.setTryBlock(new BlockStatement());

            if (isExternal) {
                tryCatch.getExternalResources().add(resource);
            }
            else {
                if (initializer != null) {
                    initializer.remove();
                    vd.getVariables().firstOrNullObject().setInitializer(initializer);
                }

                vd.remove();
                vd.addModifier(Flags.Flag.FINAL);
                tryCatch.getDeclaredResources().add(vd);
            }

            node.replaceWith(tryCatch);
            return null;
        }
    }
}
