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

import com.strobel.assembler.metadata.IMetadataResolver;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.Predicate;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.*;
import com.strobel.decompiler.semantics.ResolveResult;

import javax.lang.model.element.Modifier;

import static com.strobel.core.CollectionUtilities.*;

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
                                          new ExpressionStatement(
                                              new AssignmentExpression(new NamedNode("resource",
                                                                                     new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                                                                       new AnyNode().toExpression())
                                          )).toStatement();

        final TryCatchStatement tryPattern = new TryCatchStatement(Expression.MYSTERY_OFFSET);

        final TryCatchStatement nestedTryWithResourceDisposal = new TryCatchStatement();

        nestedTryWithResourceDisposal.setTryBlock(new AnyNode().toBlockStatement());
        nestedTryWithResourceDisposal.getCatchClauses().add(new Repeat(new AnyNode()).toCatchClause());

        final Expression resourceReference = new Choice(new DeclaredVariableBackReference("resource"),
                                                        new IdentifierExpressionBackReference("resource")).toExpression();

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
                                new BlockStatement(new ExpressionStatement(new IdentifierExpressionBackReference("otherId").toExpression().invoke("close")))
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

        disposeTry.setTryBlock(new BlockStatement(new ExpressionStatement(resourceReference.clone().invoke("close"))));

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
                                               new BinaryOperatorExpression(resourceReference.clone(),
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
        if (_tryPattern == null) {
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
        }
        finally {
            _builder = oldBuilder;
        }
    }

    @Override
    public Void visitTryCatchStatement(final TryCatchStatement node, final Void data) {
        super.visitTryCatchStatement(node, data);

        if (!(node.getParent() instanceof BlockStatement)) {
            return null;
        }

        final BlockStatement parent = (BlockStatement) node.getParent();
        final Statement initializeResource = node.getPreviousSibling(BlockStatement.STATEMENT_ROLE);

        if (initializeResource == null) {
            return null;
        }

        final Match m = Match.createNew();

        if (_resourceDeclaration.matches(initializeResource, m) &&
            _tryPattern.matches(node, m)) {

            final AstNode declaration = first(m.<AstNode>get("resource"));

            if (!(declaration instanceof VariableDeclarationStatement || declaration instanceof IdentifierExpression)) {
                return null;
            }

            final Statement declarationStatement = declaration instanceof Statement ? (Statement) declaration : declaration.getParent(Statement.class);

            if (declarationStatement == null || declarationStatement.isNull() || !(declarationStatement.getParent() instanceof BlockStatement)) {
                return null;
            }

            final TypeReference resourceType;
            final ResolveResult resourceResult = _resolver.apply(declaration);

            if (resourceResult == null || (resourceType = resourceResult.getType()) == null || isDefinitelyNotCloseable(resourceType)) {
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

            final Statement lastStatement;
            final Statement firstStatement = firstOrDefault(tryContent.getStatements());

            if (firstStatement != null && (lastStatement = lastOrDefault(tryContent.getStatements())) != null) {
                final DefiniteAssignmentAnalysis analysis = new DefiniteAssignmentAnalysis(context, tryContent);

                analysis.setAnalyzedRange(firstStatement, lastStatement);

                final String resourceName;

                if (declaration instanceof VariableDeclarationStatement) {
                    resourceName = ((VariableDeclarationStatement) declaration).getVariables().firstOrNullObject().getName();
                }
                else {
                    resourceName = ((IdentifierExpression) declaration).getIdentifier();
                }

                analysis.analyze(resourceName, DefiniteAssignmentStatus.DEFINITELY_NOT_ASSIGNED);

                if (analysis.isPotentiallyAssigned()) {
                    // Resource declarations are effectively final; if it's reassigned, we can't rewrite.
                    return null;
                }
            }

            final VariableDeclarationStatement vd;

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

            vd.addModifier(Modifier.FINAL);
            declarationStatement.remove();
            node.getResources().add(vd);
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

    static boolean isDefinitelyNotCloseable(final TypeReference t) {
        if (t == null) {
            return true;
        }

        final TypeDefinition resolved = t.resolve();

        if (resolved == null) {
            return false;
        }

        final IMetadataResolver resolver = resolved.getResolver();
        final TypeReference autoCloseable = resolver.lookupType("java/lang/AutoCloseable");
        final TypeDefinition acResolved;

        return autoCloseable != null &&
               (acResolved = autoCloseable.resolve()) != null &&
               !MetadataHelper.isAssignableFrom(acResolved, resolved);
    }
}

