/*
 * PatternStatementTransform.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.core.CollectionUtilities;
import com.strobel.core.StringUtilities;
import com.strobel.core.StrongBox;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.AnyNode;
import com.strobel.decompiler.patterns.BackReference;
import com.strobel.decompiler.patterns.Choice;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.NamedNode;
import com.strobel.decompiler.patterns.Pattern;
import com.strobel.decompiler.patterns.Repeat;

import java.util.ArrayList;
import java.util.Collections;

public final class PatternStatementTransform extends ContextTrackingVisitor<AstNode> {
    private final static AstNode VARIABLE_ASSIGN_PATTERN = new ExpressionStatement(
        new AssignmentExpression(
            new NamedNode("variable", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
            new AnyNode("initializer").toExpression()
        )
    );

    public PatternStatementTransform(final DecompilerContext context) {
        super(context);
    }

    // <editor-fold defaultstate="collapsed" desc="Visitor Overrides">

    @Override
    public AstNode visitExpressionStatement(final ExpressionStatement node, final Void data) {
        AstNode result = transformFor(node);

        if (result != null) {
            final AstNode forEachInArray = transformForEachInArray(result.getPreviousNode());

            if (forEachInArray != null) {
                return forEachInArray;
            }

            return result;
        }

        result = transformForEach(node);

        if (result != null) {
            return result;
        }

        return super.visitExpressionStatement(node, data);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="For Loop Transform">

    private final static WhileStatement FOR_PATTERN;

    static {
        final WhileStatement forPattern = new WhileStatement();

        forPattern.setCondition(
            new BinaryOperatorExpression(
                new NamedNode("identifier", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                BinaryOperatorType.ANY,
                new AnyNode("endExpression").toExpression()
            )
        );

        final BlockStatement embeddedStatement = new BlockStatement();

        embeddedStatement.add(
            new Repeat(
                new AnyNode("statement")
            ).toStatement()
        );

        embeddedStatement.add(
            new NamedNode(
                "increment",
                new Choice(
                    new ExpressionStatement(
                        new AssignmentExpression(
                            new BackReference("identifier").toExpression(),
                            AssignmentOperatorType.ANY,
                            new AnyNode().toExpression()
                        )
                    ),
                    new ExpressionStatement(
                        new NamedNode(
                            "unaryUpdate",
                            new UnaryOperatorExpression(
                                UnaryOperatorType.ANY,
                                new BackReference("identifier").toExpression()
                            )
                        ).toExpression()
                    )
                )
            ).toStatement()
        );

        forPattern.setEmbeddedStatement(embeddedStatement);

        FOR_PATTERN = forPattern;
    }

    public final ForStatement transformFor(final ExpressionStatement node) {
        final Match m1 = VARIABLE_ASSIGN_PATTERN.match(node);

        if (!m1.success()) {
            return null;
        }

        final AstNode next = node.getNextSibling();
        final Match m2 = FOR_PATTERN.match(next);

        if (!m2.success()) {
            return null;
        }

        //
        // Ensure that the variable in the 'for' pattern is the same as in the declaration.
        //

        final boolean variablesMatch = StringUtilities.equals(
            m1.<IdentifierExpression>get("variable").iterator().next().getIdentifier(),
            m2.<IdentifierExpression>get("identifier").iterator().next().getIdentifier()
        );

        if (!variablesMatch) {
            return null;
        }

        final WhileStatement loop = (WhileStatement) next;

        node.remove();

        final BlockStatement newBody = new BlockStatement();

        for (final Statement statement : m2.<Statement>get("statement")) {
            statement.remove();
            newBody.add(statement);
        }

        final ForStatement forStatement = new ForStatement();
        final Expression condition = loop.getCondition();

        if (m2.has("unaryUpdate")) {
            switch (m2.<UnaryOperatorExpression>get("unaryUpdate").iterator().next().getOperator()) {
                case INCREMENT:
                case DECREMENT:
                case POST_INCREMENT:
                case POST_DECREMENT:
                    break;

                default:
                    return null;
            }
        }

        final Iterable<Statement> increment = m2.get("increment");

        condition.remove();

        forStatement.getInitializers().add(node);
        forStatement.setCondition(condition);

        for (final Statement incrementStatement : increment) {
            incrementStatement.remove();
            forStatement.getIterators().add(incrementStatement);
        }

        forStatement.setEmbeddedStatement(newBody);
        loop.replaceWith(forStatement);

        return forStatement;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="For Each Loop Transform (Arrays)">

    private final static ExpressionStatement GET_ARRAY_LENGTH_PATTERN;
    private final static ForStatement FOR_ARRAY_PATTERN;

    static {
        GET_ARRAY_LENGTH_PATTERN = new ExpressionStatement(
            new AssignmentExpression(
                new NamedNode("left", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                new AnyNode("array").toExpression().member("length")
            )
        );

        final ForStatement forArrayPattern = new ForStatement();

        forArrayPattern.getInitializers().add(
            new ExpressionStatement(
                new AssignmentExpression(
                    new NamedNode("index", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                    new PrimitiveExpression(0)
                )
            )
        );

        forArrayPattern.setCondition(
            new BinaryOperatorExpression(
                new BackReference("index").toExpression(),
                BinaryOperatorType.LESS_THAN,
                new NamedNode("length", new IdentifierExpression(Pattern.ANY_STRING)).toExpression()
            )
        );

        forArrayPattern.getIterators().add(
            new ExpressionStatement(
                new UnaryOperatorExpression(
                    UnaryOperatorType.INCREMENT,
                    new BackReference("index").toExpression()
                )
            )
        );

        final BlockStatement embeddedStatement = new BlockStatement();

        embeddedStatement.add(
            new ExpressionStatement(
                new AssignmentExpression(
                    new NamedNode("item", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                    AssignmentOperatorType.ASSIGN,
                    new IndexerExpression(
                        new AnyNode("array").toExpression(),
                        new BackReference("index").toExpression()
                    )
                )
            )
        );

        embeddedStatement.add(
            new Repeat(
                new AnyNode("statement")
            ).toStatement()
        );

        forArrayPattern.setEmbeddedStatement(embeddedStatement);

        FOR_ARRAY_PATTERN = forArrayPattern;
    }

    public final ForEachStatement transformForEachInArray(final AstNode node) {
        final Match m1 = GET_ARRAY_LENGTH_PATTERN.match(node);

        if (!m1.success()) {
            return null;
        }

        final AstNode next = node.getNextSibling();
        final Match m2 = FOR_ARRAY_PATTERN.match(next);

        if (!m2.success()) {
            return null;
        }

        final IdentifierExpression array = m2.<IdentifierExpression>get("array").iterator().next();
        final IdentifierExpression index = m2.<IdentifierExpression>get("index").iterator().next();
        final IdentifierExpression length = m2.<IdentifierExpression>get("length").iterator().next();
        final IdentifierExpression item = m2.<IdentifierExpression>get("item").iterator().next();

        final ForStatement loop = (ForStatement) next;

        //
        // Ensure that the GET_ARRAY_LENGTH_PATTERN and FOR_ARRAY_PATTERN reference the same length variable.
        //

        if (!length.matches(m1.get("left").iterator().next()) ||
            !array.matches(m1.get("array").iterator().next())) {

            return null;
        }

        final VariableDeclarationStatement indexDeclaration = findVariableDeclaration(loop, index.getIdentifier());
        final VariableDeclarationStatement lengthDeclaration = findVariableDeclaration(loop, length.getIdentifier());

        if (lengthDeclaration == null || !(lengthDeclaration.getParent() instanceof BlockStatement) ||
            indexDeclaration == null || !(indexDeclaration.getParent() instanceof BlockStatement)) {

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

        final ForEachStatement forEach = new ForEachStatement();

        forEach.setVariableType(itemDeclaration.getType().clone());
        forEach.setVariableName(item.getIdentifier());

        forEach.putUserData(
            Keys.VARIABLE,
            itemDeclaration.getVariables().firstOrNullObject().getUserData(Keys.VARIABLE)
        );

        final BlockStatement body = new BlockStatement();

        forEach.setEmbeddedStatement(body);
        ((BlockStatement) node.getParent()).getStatements().insertBefore((Statement) node, forEach);

        node.remove();
        body.add((Statement) node);
        loop.remove();
        body.add(loop);

        //
        // Now that we moved the whole while statement into the foreach loop, verify that we can
        // move the length into the foreach loop.
        //

        declarationPoint = canMoveVariableDeclarationIntoStatement(lengthDeclaration, forEach);

        if (declarationPoint != forEach) {
            //
            // We can't move the length variable after all; undo our changes.
            //
            node.remove();
            ((BlockStatement) forEach.getParent()).getStatements().insertBefore(forEach, (Statement) node);
            forEach.replaceWith(loop);
            return null;
        }

        //
        // Now create the correct body for the foreach statement.
        //

        final Expression collection = m1.<Expression>get("array").iterator().next();

        collection.remove();
        forEach.setInExpression(collection);

        final AstNodeCollection<Statement> bodyStatements = body.getStatements();

        bodyStatements.clear();

        for (final Statement statement : m2.<Statement>get("statement")) {
            statement.remove();
            bodyStatements.add(statement);
        }

        itemDeclaration.remove();
        indexDeclaration.remove();
        lengthDeclaration.remove();

        return forEach;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="For Each Loop Transform (Iterables)">

    private final static ExpressionStatement GET_ITERATOR_PATTERN;
    private final static WhileStatement FOR_EACH_PATTERN;

    static {
        GET_ITERATOR_PATTERN = new ExpressionStatement(
            new AssignmentExpression(
                new NamedNode("left", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
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

        final AstNode next = node.getNextSibling();
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

        final ForEachStatement forEach = new ForEachStatement();

        forEach.setVariableType(itemDeclaration.getType().clone());
        forEach.setVariableName(item.getIdentifier());

        forEach.putUserData(
            Keys.VARIABLE,
            itemDeclaration.getVariables().firstOrNullObject().getUserData(Keys.VARIABLE)
        );

        final BlockStatement body = new BlockStatement();

        forEach.setEmbeddedStatement(body);
        ((BlockStatement) node.getParent()).getStatements().insertBefore(node, forEach);

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

        iteratorDeclaration.remove();

        return forEach;
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
        final DefiniteAssignmentAnalysis analysis = new DefiniteAssignmentAnalysis(blocks.get(0));

        for (final BlockStatement block : blocks) {
            if (!DeclareVariablesTransform.findDeclarationPoint(analysis, declaration, block, declarationPoint)) {
                return null;
            }
        }

        return declarationPoint.get();
    }

    // </editor-fold>
}
