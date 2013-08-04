/*
 * PatternStatementTransform.java
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

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.CollectionUtilities;
import com.strobel.core.Predicate;
import com.strobel.core.StringUtilities;
import com.strobel.core.StrongBox;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.java.analysis.ControlFlowEdge;
import com.strobel.decompiler.languages.java.analysis.ControlFlowGraphBuilder;
import com.strobel.decompiler.languages.java.analysis.ControlFlowNode;
import com.strobel.decompiler.languages.java.analysis.ControlFlowNodeType;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.AnyNode;
import com.strobel.decompiler.patterns.BackReference;
import com.strobel.decompiler.patterns.Choice;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.NamedNode;
import com.strobel.decompiler.patterns.Pattern;
import com.strobel.decompiler.patterns.Repeat;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import static com.strobel.core.CollectionUtilities.*;
import static com.strobel.decompiler.languages.java.analysis.Correlator.areCorrelated;

public final class PatternStatementTransform extends ContextTrackingVisitor<AstNode> {
    public PatternStatementTransform(final DecompilerContext context) {
        super(context);
    }

    // <editor-fold defaultstate="collapsed" desc="Visitor Overrides">

    @Override
    protected AstNode visitChildren(final AstNode node, final Void data) {
        AstNode next;

        for (AstNode child = node.getFirstChild(); child != null; child = next) {
            next = child.getNextSibling();

            final AstNode childResult = child.acceptVisitor(this, data);

            if (childResult != null && childResult != child) {
                next = childResult;
            }
        }

        return node;
    }

    @Override
    public AstNode visitExpressionStatement(final ExpressionStatement node, final Void data) {
        super.visitExpressionStatement(node, data);

        final AstNode result = transformForEach(node);

        if (result != null) {
            return result.acceptVisitor(this, data);
        }

        return node;
    }

    @Override
    public AstNode visitWhileStatement(final WhileStatement node, final Void data) {
        super.visitWhileStatement(node, data);

        final ForStatement forLoop = transformFor(node);

        if (forLoop != null) {
            final AstNode forEachInArray = transformForEachInArray(forLoop);

            if (forEachInArray != null) {
                return forEachInArray.acceptVisitor(this, data);
            }

            return forLoop.acceptVisitor(this, data);
        }

        final DoWhileStatement doWhile = transformDoWhile(node);

        if (doWhile != null) {
            return doWhile.acceptVisitor(this, data);
        }

        return transformContinueOuter(node);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="For Loop Transform">

    @SuppressWarnings("ConstantConditions")
    public final ForStatement transformFor(final WhileStatement node) {
        final Expression condition = node.getCondition();

        if (condition == null || condition.isNull()) {
            return null;
        }

        final ControlFlowGraphBuilder graphBuilder = new ControlFlowGraphBuilder();
        final List<ControlFlowNode> nodes = graphBuilder.buildControlFlowGraph(node, new JavaResolver(context));

        if (nodes.size() < 2) {
            return null;
        }

        final ControlFlowNode conditionNode = firstOrDefault(
            nodes,
            new Predicate<ControlFlowNode>() {
                @Override
                public boolean test(final ControlFlowNode n) {
                    return n.getType() == ControlFlowNodeType.LoopCondition;
                }
            }
        );

        if (conditionNode == null) {
            return null;
        }

        final List<ControlFlowNode> bodyNodes = new ArrayList<>();

        for (final ControlFlowEdge edge : conditionNode.getIncoming()) {
            final ControlFlowNode from = edge.getFrom();
            final Statement statement = from.getPreviousStatement();

            if (statement != null && node.getEmbeddedStatement().isAncestorOf(statement)) {
                bodyNodes.add(from);
            }
        }

        if (bodyNodes.size() != 1) {
            return null;
        }

        final List<Statement> iterators = new ArrayList<>();
        final ControlFlowNode iteratorNode = bodyNodes.get(0);
        final AstNodeCollection<Statement> loopBody = iteratorNode.getPreviousStatement()
                                                                  .getChildrenByRole(BlockStatement.STATEMENT_ROLE);

        final Statement firstIterator = firstOrDefault(
            loopBody,
            new Predicate<Statement>() {
                @Override
                public boolean test(final Statement s) {
                    return s.isEmbeddable() && areCorrelated(condition, s);
                }
            }
        );

        if (firstIterator == null) {
            return null;
        }

        iterators.add(firstIterator);

        for (Statement s = firstIterator.getNextStatement(); s != null; s = s.getNextStatement()) {
            if (s.isEmbeddable()) {
                iterators.add(s);
            }
            else {
                return null;
            }
        }

/*
        if (loopBody.size() - iterators.size() == 0) {
            //
            // Don't transform a 'while' loop into a 'for' loop with an empty body.
            //
            return null;
        }
*/

        final ForStatement forLoop = new ForStatement();
        final Stack<Statement> initializers = new Stack<>();

        for (Statement s = node.getPreviousStatement(); s instanceof ExpressionStatement; s = s.getPreviousStatement()) {
            final Statement fs = s;
            final Expression e = ((ExpressionStatement) s).getExpression();
            final Expression left;

            final boolean canExtract =
                e instanceof AssignmentExpression &&
                (left = e.getChildByRole(AssignmentExpression.LEFT_ROLE)) instanceof IdentifierExpression &&
                (areCorrelated(condition, s) ||
                 any(
                     iterators,
                     new Predicate<Statement>() {
                         @Override
                         public boolean test(final Statement i) {
                             return (i instanceof ExpressionStatement &&
                                     areCorrelated(((ExpressionStatement) i).getExpression(), fs)) ||
                                    areCorrelated(left, i);
                         }
                     }
                 ));

            if (canExtract) {
                initializers.add(s);
            }
            else {
                break;
            }
        }

        final Statement body = node.getEmbeddedStatement();

        condition.remove();
        body.remove();

        forLoop.setCondition(condition);

        if (body instanceof BlockStatement) {
            for (final Statement s : ((BlockStatement) body).getStatements()) {
                if (iterators.contains(s)) {
                    s.remove();
                }
            }

            forLoop.setEmbeddedStatement(body);
        }

        forLoop.getIterators().addAll(iterators);

        while (!initializers.isEmpty()) {
            final Statement initializer = initializers.pop();
            initializer.remove();
            forLoop.getInitializers().add(initializer);
        }

        node.replaceWith(forLoop);

        final Statement firstInlinableInitializer = canInlineInitializerDeclarations(forLoop);

        if (firstInlinableInitializer != null) {
            final BlockStatement parent = (BlockStatement) forLoop.getParent();
            final VariableDeclarationStatement newDeclaration = new VariableDeclarationStatement();
            final List<Statement> forInitializers = new ArrayList<>(forLoop.getInitializers());
            final int firstInlinableInitializerIndex = forInitializers.indexOf(firstInlinableInitializer);

            forLoop.getInitializers().clear();
            forLoop.getInitializers().add(newDeclaration);

            for (int i = 0; i < forInitializers.size(); i++) {
                final Statement initializer = forInitializers.get(i);

                if (i < firstInlinableInitializerIndex) {
                    parent.insertChildBefore(forLoop, initializer, BlockStatement.STATEMENT_ROLE);
                    continue;
                }

                final AssignmentExpression assignment = (AssignmentExpression) ((ExpressionStatement) initializer).getExpression();
                final IdentifierExpression variable = (IdentifierExpression) assignment.getLeft();
                final String variableName = variable.getIdentifier();
                final VariableDeclarationStatement declaration = findVariableDeclaration(forLoop, variableName);
//                final VariableInitializer oldInitializer = declaration.getVariable(variableName);
                final Expression initValue = assignment.getRight();

                initValue.remove();
                newDeclaration.getVariables().add(new VariableInitializer(variableName, initValue));

                final AstType newDeclarationType = newDeclaration.getType();

                if (newDeclarationType == null || newDeclarationType.isNull()) {
                    newDeclaration.setType(declaration.getType().clone());
                }

/*
                if (oldInitializer != null && !oldInitializer.isNull()) {
                    oldInitializer.remove();

                    if (declaration.getVariables().isEmpty()) {
                        declaration.remove();
                    }
                }
*/
            }
        }

        return forLoop;
    }

    private Statement canInlineInitializerDeclarations(final ForStatement forLoop) {
        TypeReference variableType = null;

        final BlockStatement tempOuter = new BlockStatement();
        final BlockStatement temp = new BlockStatement();
        final Statement[] initializers = forLoop.getInitializers().toArray(new Statement[forLoop.getInitializers().size()]);

        Statement firstInlinableInitializer = null;

        forLoop.getParent().insertChildBefore(forLoop, tempOuter, BlockStatement.STATEMENT_ROLE);
        forLoop.remove();

        for (final Statement initializer : initializers) {
            initializer.remove();
            temp.getStatements().add(initializer);
        }

        temp.getStatements().add(forLoop);
        tempOuter.getStatements().add(temp);

        try {
            for (final Statement initializer : initializers) {
                final AssignmentExpression assignment = (AssignmentExpression) ((ExpressionStatement) initializer).getExpression();
                final IdentifierExpression variable = (IdentifierExpression) assignment.getLeft();
                final String variableName = variable.getIdentifier();
                final VariableDeclarationStatement declaration = findVariableDeclaration(forLoop, variableName);

                if (declaration == null) {
                    return null;
                }

                final Variable underlyingVariable = declaration.getUserData(Keys.VARIABLE);

                if (underlyingVariable == null || underlyingVariable.isParameter()) {
                    return null;
                }

                if (variableType == null) {
                    variableType = underlyingVariable.getType();
                }
                else if (!variableType.equals(underlyingVariable.getType())) {
                    variableType = underlyingVariable.getType();
                    firstInlinableInitializer = null;
                }

                if (!(declaration.getParent() instanceof BlockStatement)) {
                    return null;
                }

                final Statement declarationPoint = canMoveVariableDeclarationIntoStatement(declaration, forLoop);

                if (declarationPoint != tempOuter) {
                    variableType = null;
                    firstInlinableInitializer = null;
                }
                else if (firstInlinableInitializer == null) {
                    firstInlinableInitializer = initializer;
                }
            }

            return firstInlinableInitializer;
        }
        finally {
            forLoop.remove();
            tempOuter.replaceWith(forLoop);

            for (final Statement initializer : initializers) {
                initializer.remove();
                forLoop.getInitializers().add(initializer);
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="For Each Loop Transform (Arrays)">

    private final static ExpressionStatement ARRAY_INIT_PATTERN;
    private final static ForStatement FOR_ARRAY_PATTERN_1;
    private final static ForStatement FOR_ARRAY_PATTERN_2;
    private final static ForStatement FOR_ARRAY_PATTERN_3;

    static {
        ARRAY_INIT_PATTERN = new ExpressionStatement(
            new AssignmentExpression(
                new NamedNode("array", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                new AnyNode("initializer").toExpression()
            )
        );

        final ForStatement forArrayPattern1 = new ForStatement();
        final VariableDeclarationStatement declaration1 = new VariableDeclarationStatement();
        final SimpleType variableType1 = new SimpleType("int");

        variableType1.putUserData(Keys.TYPE_REFERENCE, BuiltinTypes.Integer);

        declaration1.setType(variableType1);

        declaration1.getVariables().add(
            new VariableInitializer(
                Pattern.ANY_STRING,
                new NamedNode("array", new IdentifierExpression(Pattern.ANY_STRING)).toExpression().member("length")
            )
        );

        declaration1.getVariables().add(
            new VariableInitializer(
                Pattern.ANY_STRING,
                new PrimitiveExpression(0)
            )
        );

        forArrayPattern1.getInitializers().add(
            new NamedNode("declaration", declaration1).toStatement()
        );

        forArrayPattern1.setCondition(
            new BinaryOperatorExpression(
                new NamedNode("index", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                BinaryOperatorType.LESS_THAN,
                new NamedNode("length", new IdentifierExpression(Pattern.ANY_STRING)).toExpression()
            )
        );

        forArrayPattern1.getIterators().add(
            new ExpressionStatement(
                new UnaryOperatorExpression(
                    UnaryOperatorType.INCREMENT,
                    new BackReference("index").toExpression()
                )
            )
        );

        final BlockStatement embeddedStatement1 = new BlockStatement();

        embeddedStatement1.add(
            new ExpressionStatement(
                new AssignmentExpression(
                    new NamedNode("item", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                    AssignmentOperatorType.ASSIGN,
                    new IndexerExpression(
                        new BackReference("array").toExpression(),
                        new BackReference("index").toExpression()
                    )
                )
            )
        );

        embeddedStatement1.add(
            new Repeat(
                new AnyNode("statement")
            ).toStatement()
        );

        forArrayPattern1.setEmbeddedStatement(embeddedStatement1);

        FOR_ARRAY_PATTERN_1 = forArrayPattern1;

        final ForStatement forArrayPattern2 = new ForStatement();
        final VariableDeclarationStatement declaration2 = new VariableDeclarationStatement();
        final SimpleType variableType2 = new SimpleType("int");

        variableType2.putUserData(Keys.TYPE_REFERENCE, BuiltinTypes.Integer);

        declaration2.setType(variableType2);

        declaration2.getVariables().add(
            new VariableInitializer(
                Pattern.ANY_STRING,
                new PrimitiveExpression(0)
            )
        );

        forArrayPattern2.getInitializers().add(
            new NamedNode("declaration", declaration2).toStatement()
        );

        forArrayPattern2.setCondition(
            new BinaryOperatorExpression(
                new NamedNode("index", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                BinaryOperatorType.LESS_THAN,
                new NamedNode("length", new IdentifierExpression(Pattern.ANY_STRING)).toExpression()
            )
        );

        forArrayPattern2.getIterators().add(
            new ExpressionStatement(
                new UnaryOperatorExpression(
                    UnaryOperatorType.INCREMENT,
                    new BackReference("index").toExpression()
                )
            )
        );

        final BlockStatement embeddedStatement2 = new BlockStatement();

        embeddedStatement2.add(
            new ExpressionStatement(
                new AssignmentExpression(
                    new NamedNode("item", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                    AssignmentOperatorType.ASSIGN,
                    new IndexerExpression(
                        new NamedNode("array", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                        new BackReference("index").toExpression()
                    )
                )
            )
        );

        embeddedStatement2.add(
            new Repeat(
                new AnyNode("statement")
            ).toStatement()
        );

        forArrayPattern2.setEmbeddedStatement(embeddedStatement2);

        FOR_ARRAY_PATTERN_2 = forArrayPattern2;

        final ForStatement altForArrayPattern = new ForStatement();

        altForArrayPattern.getInitializers().add(
            new ExpressionStatement(
                new AssignmentExpression(
                    new NamedNode("length", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                    AssignmentOperatorType.ASSIGN,
                    new NamedNode("array", new IdentifierExpression(Pattern.ANY_STRING)).toExpression().member("length")
                )
            )
        );

        altForArrayPattern.getInitializers().add(
            new ExpressionStatement(
                new AssignmentExpression(
                    new NamedNode("index", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                    AssignmentOperatorType.ASSIGN,
                    new PrimitiveExpression(0)
                )
            )
        );

        altForArrayPattern.setCondition(
            new BinaryOperatorExpression(
                new BackReference("index").toExpression(),
                BinaryOperatorType.LESS_THAN,
                new BackReference("length").toExpression()
            )
        );

        altForArrayPattern.getIterators().add(
            new ExpressionStatement(
                new UnaryOperatorExpression(
                    UnaryOperatorType.INCREMENT,
                    new BackReference("index").toExpression()
                )
            )
        );

        final BlockStatement altEmbeddedStatement = new BlockStatement();

        altEmbeddedStatement.add(
            new ExpressionStatement(
                new AssignmentExpression(
                    new NamedNode("item", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                    AssignmentOperatorType.ASSIGN,
                    new IndexerExpression(
                        new BackReference("array").toExpression(),
                        new BackReference("index").toExpression()
                    )
                )
            )
        );

        altEmbeddedStatement.add(
            new Repeat(
                new AnyNode("statement")
            ).toStatement()
        );

        altForArrayPattern.setEmbeddedStatement(altEmbeddedStatement);

        FOR_ARRAY_PATTERN_3 = altForArrayPattern;
    }

    public final ForEachStatement transformForEachInArray(final ForStatement loop) {
        Match m = FOR_ARRAY_PATTERN_1.match(loop);

        if (!m.success()) {
            m = FOR_ARRAY_PATTERN_2.match(loop);

            if (!m.success()) {
                m = FOR_ARRAY_PATTERN_3.match(loop);

                if (!m.success()) {
                    return null;
                }
            }
        }

        final IdentifierExpression array = m.<IdentifierExpression>get("array").iterator().next();
        final IdentifierExpression item = m.<IdentifierExpression>get("item").iterator().next();

        //
        // Find the declaration of the item variable.  Because we look only outside the loop,
        // we won't make the mistake of moving a captured variable across the loop boundary.
        //

        final VariableDeclarationStatement itemDeclaration = findVariableDeclaration(loop, item.getIdentifier());

        if (itemDeclaration == null || !(itemDeclaration.getParent() instanceof BlockStatement)) {
            return null;
        }

        //
        // Now verify that we can move the variable declaration in front of the loop.
        //

        final Statement declarationPoint = canMoveVariableDeclarationIntoStatement(itemDeclaration, loop);

        //
        // We ignore the return value because we don't care whether we can move the variable into the loop
        // (that is possible only with non-captured variables).  We just care that we can move it in front
        // of the loop.
        //

        if (declarationPoint != loop) {
            return null;
        }

        final ForEachStatement forEach = new ForEachStatement();

        forEach.setVariableType(itemDeclaration.getType().clone());
        forEach.setVariableName(item.getIdentifier());

        forEach.putUserData(
            Keys.VARIABLE,
            itemDeclaration.getVariables().firstOrNullObject().getUserData(Keys.VARIABLE)
        );

        final BlockStatement body = new BlockStatement();
        final BlockStatement parent = (BlockStatement) loop.getParent();

        forEach.setEmbeddedStatement(body);
        parent.getStatements().insertBefore(loop, forEach);

        loop.remove();
        body.add(loop);
        loop.remove();
        body.add(loop);

        //
        // Now create the correct body for the foreach statement.
        //

        array.remove();

        forEach.setInExpression(array);

        final AstNodeCollection<Statement> bodyStatements = body.getStatements();

        bodyStatements.clear();

        for (final Statement statement : m.<Statement>get("statement")) {
            statement.remove();
            bodyStatements.add(statement);
        }

        itemDeclaration.remove();

        Statement previous = forEach.getPreviousStatement();

        while (previous instanceof LabelStatement) {
            previous = previous.getPreviousStatement();
        }

        if (previous != null) {
            final Match m2 = ARRAY_INIT_PATTERN.match(previous);

            if (m2.success()) {
                final Expression initializer = m2.<Expression>get("initializer").iterator().next();
                final IdentifierExpression array2 = m2.<IdentifierExpression>get("array").iterator().next();

                if (StringUtilities.equals(array2.getIdentifier(), array.getIdentifier())) {
                    final BlockStatement tempOuter = new BlockStatement();
                    final BlockStatement temp = new BlockStatement();

                    boolean restorePrevious = true;

                    parent.insertChildBefore(forEach, tempOuter, BlockStatement.STATEMENT_ROLE);
                    previous.remove();
                    forEach.remove();
                    temp.add(previous);
                    temp.add(forEach);
                    tempOuter.add(temp);

                    try {
                        final VariableDeclarationStatement arrayDeclaration = findVariableDeclaration(forEach, array.getIdentifier());

                        if (arrayDeclaration != null && arrayDeclaration.getParent() instanceof BlockStatement) {
                            final Statement arrayDeclarationPoint = canMoveVariableDeclarationIntoStatement(arrayDeclaration, forEach);

                            if (arrayDeclarationPoint == tempOuter) {
                                initializer.remove();
                                array.replaceWith(initializer);
                                restorePrevious = false;
                            }
                        }
                    }
                    finally {
                        previous.remove();
                        forEach.remove();

                        if (restorePrevious) {
                            parent.insertChildBefore(tempOuter, previous, BlockStatement.STATEMENT_ROLE);
                        }

                        parent.insertChildBefore(tempOuter, forEach, BlockStatement.STATEMENT_ROLE);
                        tempOuter.remove();
                    }
                }
            }
        }

        final DefiniteAssignmentAnalysis analysis = new DefiniteAssignmentAnalysis(context, body);
        final Statement firstStatement = firstOrDefault(body.getStatements());
        final Statement lastStatement = lastOrDefault(body.getStatements());

        analysis.setAnalyzedRange(firstStatement, lastStatement);
        analysis.analyze(item.getIdentifier(), DefiniteAssignmentStatus.DEFINITELY_NOT_ASSIGNED);

        final DefiniteAssignmentStatus status = analysis.getStatusAfter(lastStatement);

        if (status == DefiniteAssignmentStatus.DEFINITELY_NOT_ASSIGNED) {
            forEach.addVariableModifier(Modifier.FINAL);
        }

        return forEach;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="For Each Loop Transform (Iterables)">

    private final static ExpressionStatement GET_ITERATOR_PATTERN;
    private final static WhileStatement FOR_EACH_PATTERN;

    static {
        GET_ITERATOR_PATTERN = new ExpressionStatement(
            new AssignmentExpression(
                new NamedNode("left", new AnyNode()).toExpression(),
                new AnyNode("collection").toExpression().invoke("iterator")
            )
        );

        final WhileStatement forEachPattern = new WhileStatement();

        forEachPattern.setCondition(
            new InvocationExpression(
                new MemberReferenceExpression(
                    new NamedNode("iterator", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                    "hasNext"
                )
            )
        );

        final BlockStatement embeddedStatement = new BlockStatement();

        embeddedStatement.add(
            new NamedNode(
                "next",
                new ExpressionStatement(
                    new AssignmentExpression(
                        new NamedNode("item", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                        AssignmentOperatorType.ASSIGN,
                        new Choice(
                            new InvocationExpression(
                                new MemberReferenceExpression(
                                    new BackReference("iterator").toExpression(),
                                    "next"
                                )
                            ),
                            new CastExpression(
                                new AnyNode("castType").toType(),
                                new InvocationExpression(
                                    new MemberReferenceExpression(
                                        new BackReference("iterator").toExpression(),
                                        "next"
                                    )
                                )
                            )
                        ).toExpression()
                    )
                )
            ).toStatement()
        );

        embeddedStatement.add(
            new Repeat(
                new AnyNode("statement")
            ).toStatement()
        );

        forEachPattern.setEmbeddedStatement(embeddedStatement);

        FOR_EACH_PATTERN = forEachPattern;
    }

    public final ForEachStatement transformForEach(final ExpressionStatement node) {
        final Match m1 = GET_ITERATOR_PATTERN.match(node);

        if (!m1.success()) {
            return null;
        }

        AstNode next = node.getNextSibling();

        while (next instanceof LabelStatement) {
            next = next.getNextSibling();
        }

        final Match m2 = FOR_EACH_PATTERN.match(next);

        if (!m2.success()) {
            return null;
        }

        final IdentifierExpression iterator = m2.<IdentifierExpression>get("iterator").iterator().next();
        final IdentifierExpression item = m2.<IdentifierExpression>get("item").iterator().next();
        final WhileStatement loop = (WhileStatement) next;

        //
        // Ensure that the GET_ITERATOR_PATTERN and FOR_EACH_PATTERN reference the same iterator variable.
        //

        if (!iterator.matches(m1.get("left").iterator().next())) {
            return null;
        }

        final VariableDeclarationStatement iteratorDeclaration = findVariableDeclaration(loop, iterator.getIdentifier());

        if (iteratorDeclaration == null || !(iteratorDeclaration.getParent() instanceof BlockStatement)) {
            return null;
        }

        //
        // Find the declaration of the item variable.  Because we look only outside the loop,
        // we won't make the mistake of moving a captured variable across the loop boundary.
        //

        final VariableDeclarationStatement itemDeclaration = findVariableDeclaration(loop, item.getIdentifier());

        if (itemDeclaration == null || !(itemDeclaration.getParent() instanceof BlockStatement)) {
            return null;
        }

        //
        // Now verify that we can move the variable declaration in front of the loop.
        //

        Statement declarationPoint = canMoveVariableDeclarationIntoStatement(itemDeclaration, loop);

        //
        // We ignore the return value because we don't care whether we can move the variable into the loop
        // (that is possible only with non-captured variables).  We just care that we can move it in front
        // of the loop.
        //

        if (declarationPoint != loop) {
            return null;
        }

        final BlockStatement loopBody = (BlockStatement) loop.getEmbeddedStatement();
        final Statement secondStatement = getOrDefault(loopBody.getStatements(), 1);

        if (secondStatement != null && !secondStatement.isNull()) {
            final DefiniteAssignmentAnalysis analysis = new DefiniteAssignmentAnalysis(context, loopBody);

            analysis.setAnalyzedRange(secondStatement, loopBody);
            analysis.analyze(iterator.getIdentifier(), DefiniteAssignmentStatus.DEFINITELY_NOT_ASSIGNED);

            if (!analysis.getUnassignedVariableUses().isEmpty()) {
                //
                // We can't eliminate the iterator variable because it's used in the loop.
                //
                return null;
            }
        }

        final ForEachStatement forEach = new ForEachStatement();

        forEach.setVariableType(itemDeclaration.getType().clone());
        forEach.setVariableName(item.getIdentifier());

        forEach.putUserData(
            Keys.VARIABLE,
            itemDeclaration.getVariables().firstOrNullObject().getUserData(Keys.VARIABLE)
        );

        final BlockStatement body = new BlockStatement();

        forEach.setEmbeddedStatement(body);
        ((BlockStatement) node.getParent()).getStatements().insertBefore(loop, forEach);

        node.remove();
        body.add(node);
        loop.remove();
        body.add(loop);

        //
        // Now that we moved the whole while statement into the foreach loop, verify that we can
        // move the iterator into the foreach loop.
        //

        declarationPoint = canMoveVariableDeclarationIntoStatement(iteratorDeclaration, forEach);

        if (declarationPoint != forEach) {
            //
            // We can't move the iterator variable after all; undo our changes.
            //
            node.remove();
            ((BlockStatement) forEach.getParent()).getStatements().insertBefore(forEach, node);
            forEach.replaceWith(loop);
            return null;
        }

        //
        // Now create the correct body for the foreach statement.
        //

        final Expression collection = m1.<Expression>get("collection").iterator().next();

        collection.remove();

        if (collection instanceof SuperReferenceExpression) {
            final ThisReferenceExpression self = new ThisReferenceExpression();
            self.putUserData(Keys.TYPE_REFERENCE, collection.getUserData(Keys.TYPE_REFERENCE));
            self.putUserData(Keys.VARIABLE, collection.getUserData(Keys.VARIABLE));
            forEach.setInExpression(self);
        }
        else {
            forEach.setInExpression(collection);
        }

        final AstNodeCollection<Statement> bodyStatements = body.getStatements();

        bodyStatements.clear();

        for (final Statement statement : m2.<Statement>get("statement")) {
            statement.remove();
            bodyStatements.add(statement);
        }

//        iteratorDeclaration.remove();

        final Statement firstStatement = firstOrDefault(body.getStatements());
        final Statement lastStatement = lastOrDefault(body.getStatements());
        final DefiniteAssignmentAnalysis analysis = new DefiniteAssignmentAnalysis(context, body);

        analysis.setAnalyzedRange(firstStatement, lastStatement);
        analysis.analyze(item.getIdentifier(), DefiniteAssignmentStatus.DEFINITELY_NOT_ASSIGNED);

        final DefiniteAssignmentStatus status = analysis.getStatusAfter(lastStatement);

        if (status == DefiniteAssignmentStatus.DEFINITELY_NOT_ASSIGNED) {
            forEach.addVariableModifier(Modifier.FINAL);
        }

        return forEach;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Do While Loop Transform">

    private final static WhileStatement DO_WHILE_PATTERN;

    static {
        final WhileStatement doWhile = new WhileStatement();

        doWhile.setCondition(new PrimitiveExpression(true));

        doWhile.setEmbeddedStatement(
            new BlockStatement(
                new Repeat(new AnyNode("statement")).toStatement(),
                new IfElseStatement(
                    new AnyNode("condition").toExpression(),
                    new BlockStatement(new BreakStatement())
                )
            )
        );

        DO_WHILE_PATTERN = doWhile;
    }

    public final DoWhileStatement transformDoWhile(final WhileStatement loop) {
        final Match m = DO_WHILE_PATTERN.match(loop);

        if (!m.success() || !canConvertWhileToDoWhile(loop)) {
            return null;
        }

        final DoWhileStatement doWhile = new DoWhileStatement();

        Expression condition = m.<Expression>get("condition").iterator().next();

        condition.remove();

        if (condition instanceof UnaryOperatorExpression &&
            ((UnaryOperatorExpression) condition).getOperator() == UnaryOperatorType.NOT) {

            condition = ((UnaryOperatorExpression) condition).getExpression();
            condition.remove();
        }
        else {
            condition = new UnaryOperatorExpression(UnaryOperatorType.NOT, condition);
        }

        doWhile.setCondition(condition);

        final BlockStatement block = (BlockStatement) loop.getEmbeddedStatement();

        lastOrDefault(block.getStatements()).remove();
        block.remove();

        doWhile.setEmbeddedStatement(block);

        loop.replaceWith(doWhile);

        //
        // We may have to extract variable definitions out of the loop if they were used
        // in the condition.
        //

        for (final Statement statement : block.getStatements()) {
            if (statement instanceof VariableDeclarationStatement) {
                final VariableDeclarationStatement declaration = (VariableDeclarationStatement) statement;
                final VariableInitializer v = firstOrDefault(declaration.getVariables());

                for (final AstNode node : condition.getDescendantsAndSelf()) {
                    if (node instanceof IdentifierExpression &&
                        StringUtilities.equals(v.getName(), ((IdentifierExpression) node).getIdentifier())) {

                        final Expression initializer = v.getInitializer();

                        initializer.remove();

                        final AssignmentExpression assignment = new AssignmentExpression(
                            new IdentifierExpression(v.getName()),
                            initializer
                        );

                        assignment.putUserData(Keys.MEMBER_REFERENCE, initializer.getUserData(Keys.MEMBER_REFERENCE));
                        assignment.putUserData(Keys.VARIABLE, initializer.getUserData(Keys.VARIABLE));

                        v.putUserData(Keys.MEMBER_REFERENCE, null);
                        v.putUserData(Keys.VARIABLE, null);

                        assignment.putUserData(Keys.MEMBER_REFERENCE, declaration.getUserData(Keys.MEMBER_REFERENCE));
                        assignment.putUserData(Keys.VARIABLE, declaration.getUserData(Keys.VARIABLE));

                        declaration.replaceWith(new ExpressionStatement(assignment));

                        declaration.putUserData(Keys.MEMBER_REFERENCE, null);
                        declaration.putUserData(Keys.VARIABLE, null);

                        doWhile.getParent().insertChildBefore(doWhile, declaration, BlockStatement.STATEMENT_ROLE);
                    }
                }
            }
        }

        return doWhile;
    }

    private boolean canConvertWhileToDoWhile(final WhileStatement loop) {
        final List<ContinueStatement> continueStatements = new ArrayList<>();

        for (final AstNode node : loop.getDescendantsAndSelf()) {
            if (node instanceof ContinueStatement) {
                continueStatements.add((ContinueStatement) node);
            }
        }

        if (continueStatements.isEmpty()) {
            return true;
        }

        for (final ContinueStatement cs : continueStatements) {
            final String label = cs.getLabel();

            if (StringUtilities.isNullOrEmpty(label)) {
                return false;
            }

            final Statement previousStatement = loop.getPreviousStatement();

            if (previousStatement instanceof LabelStatement) {
                return !StringUtilities.equals(
                    ((LabelStatement) previousStatement).getLabel(),
                    label
                );
            }

            if (loop.getParent() instanceof LabeledStatement) {
                return !StringUtilities.equals(
                    ((LabeledStatement) loop.getParent()).getLabel(),
                    label
                );
            }
        }

        return true;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Continue Outer Loop Transforms">

    private final static WhileStatement CONTINUE_OUTER_PATTERN;

    static {
        final WhileStatement continueOuter = new WhileStatement();

        continueOuter.setCondition(new AnyNode().toExpression());

        continueOuter.setEmbeddedStatement(
            new BlockStatement(
                new NamedNode("label", new LabelStatement(Pattern.ANY_STRING)).toStatement(),
                new Repeat(new AnyNode("statement")).toStatement()
            )
        );

        CONTINUE_OUTER_PATTERN = continueOuter;
    }

    public final WhileStatement transformContinueOuter(final WhileStatement loop) {
        final Match m = CONTINUE_OUTER_PATTERN.match(loop);

        if (!m.success()) {
            return null;
        }

        final LabelStatement label = (LabelStatement) m.get("label").iterator().next();

        label.remove();
        loop.getParent().insertChildBefore(loop, label, BlockStatement.STATEMENT_ROLE);

        return loop;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Helper Methods">

    static VariableDeclarationStatement findVariableDeclaration(final AstNode node, final String identifier) {
        AstNode current = node;
        while (current != null) {
            while (current.getPreviousSibling() != null) {
                current = current.getPreviousSibling();
                if (current instanceof VariableDeclarationStatement) {
                    final VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement) current;
                    final Variable variable = variableDeclaration.getUserData(Keys.VARIABLE);

                    if (variable != null && StringUtilities.equals(variable.getName(), identifier)) {
                        return variableDeclaration;
                    }

                    if (variableDeclaration.getVariables().size() == 1 &&
                        StringUtilities.equals(variableDeclaration.getVariables().firstOrNullObject().getName(), identifier)) {

                        return variableDeclaration;
                    }
                }
            }
            current = current.getParent();
        }
        return null;
    }

    final Statement canMoveVariableDeclarationIntoStatement(
        final VariableDeclarationStatement declaration,
        final Statement targetStatement) {

        if (declaration == null) {
            return null;
        }

        final BlockStatement parent = (BlockStatement) declaration.getParent();

        //noinspection AssertWithSideEffects
        assert CollectionUtilities.contains(targetStatement.getAncestors(), parent);

        //
        // Find all blocks between targetStatement and declaration's parent block.
        //
        final ArrayList<BlockStatement> blocks = new ArrayList<>();

        for (final AstNode block : targetStatement.getAncestors()) {
            if (block == parent) {
                break;
            }

            if (block instanceof BlockStatement) {
                blocks.add((BlockStatement) block);
            }
        }

        //
        // Also handle the declaration's parent block itself.
        //
        blocks.add(parent);

        //
        // Go from parent blocks to child blocks.
        //
        Collections.reverse(blocks);

        final StrongBox<Statement> declarationPoint = new StrongBox<>();
        final DefiniteAssignmentAnalysis analysis = new DefiniteAssignmentAnalysis(context, blocks.get(0));

        Statement result = null;

        for (final BlockStatement block : blocks) {
            if (!DeclareVariablesTransform.findDeclarationPoint(analysis, declaration, block, declarationPoint, null)) {
                break;
            }
            result = declarationPoint.get();
        }

        return result;
    }

    // </editor-fold>
}
