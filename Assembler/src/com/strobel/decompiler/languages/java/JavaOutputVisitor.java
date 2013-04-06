/*
 * JavaOutputVisitor.java
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

package com.strobel.decompiler.languages.java;

import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.*;
import com.strobel.util.ContractUtils;

import java.util.Stack;

import static com.strobel.core.CollectionUtilities.*;
import static java.lang.String.format;

public final class JavaOutputVisitor implements IAstVisitor<Void, Void> {
    final IOutputFormatter formatter;
    final JavaFormattingOptions policy;
    final Stack<AstNode> containerStack = new Stack<>();
    final Stack<AstNode> positionStack = new Stack<>();

    private LastWritten lastWritten;

    public JavaOutputVisitor(final ITextOutput output, final JavaFormattingOptions formattingPolicy) {
        this.formatter = new TextOutputFormatter(output);
        this.policy = formattingPolicy;
    }

    // <editor-fold defaultstate="collapsed" desc="Start/End Node">

    void startNode(final AstNode node) {
        // Ensure that nodes are visited in the proper nested order.
        // Jumps to different subtrees are allowed only for the child of a placeholder node.
        assert containerStack.isEmpty() ||
               node.getParent() == containerStack.peek() ||
               containerStack.peek().getNodeType() == NodeType.PATTERN;

        if (positionStack.size() > 0) {
            writeSpecialsUpToNode(node);
        }
        containerStack.push(node);
        positionStack.push(node.getFirstChild());
        formatter.startNode(node);
    }

    void endNode(final AstNode node) {
        assert node == containerStack.peek();

        final AstNode position = positionStack.pop();

        assert position == null || position.getParent() == node;

        writeSpecials(position, null);
        containerStack.pop();
        formatter.endNode(node);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Write Specials">

    private void writeSpecials(final AstNode start, final AstNode end) {
        for (AstNode current = start; current != end; current = current.getNextSibling()) {
            if (current.getRole() == Roles.COMMENT || current.getRole() == Roles.NEW_LINE) {
                current.acceptVisitor(this, null);
            }
        }
    }

    private void writeSpecialsUpToRole(final Role<?> role) {
        writeSpecialsUpToRole(role, null);
    }

    private void writeSpecialsUpToRole(final Role<?> role, final AstNode nextNode) {
        if (positionStack.isEmpty()) {
            return;
        }

        for (AstNode current = positionStack.peek();
             current != null && current != nextNode;
             current = current.getNextSibling()) {

            if (current.getRole() == role) {
                writeSpecials(positionStack.pop(), current);
                //
                // Push the next sibling because the node itself instanceof not a special,
                // and should be considered to be already handled.
                //
                positionStack.push(current.getNextSibling());

                //
                // This instanceof necessary for optionalComma() to work correctly.
                //
                break;
            }
        }
    }

    private void writeSpecialsUpToNode(final AstNode node) {
        if (positionStack.isEmpty()) {
            return;
        }

        for (AstNode current = positionStack.peek(); current != null; current = current.getNextSibling()) {
            if (current == node) {
                writeSpecials(positionStack.pop(), current);
                //
                // Push the next sibling because the node itself instanceof not a special,
                // and should be considered to be already handled.
                //
                positionStack.push(current.getNextSibling());

                //
                // This instanceof necessary for optionalComma() to work correctly.
                //
                break;
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Common Tokens">

    void leftParenthesis() {
        writeToken(Roles.LEFT_PARENTHESIS);
    }

    void rightParenthesis() {
        writeToken(Roles.RIGHT_PARENTHESIS);
    }

    void space() {
        formatter.space();
        lastWritten = LastWritten.Whitespace;
    }

    void space(final boolean addSpace) {
        if (addSpace) {
            space();
        }
    }

    void newLine() {
        formatter.newLine();
        lastWritten = LastWritten.Whitespace;
    }

    void openBrace(final BraceStyle style) {
        writeSpecialsUpToRole(Roles.LEFT_BRACE);
        formatter.openBrace(style);
        lastWritten = LastWritten.Other;
    }

    void closeBrace(final BraceStyle style) {
        writeSpecialsUpToRole(Roles.RIGHT_BRACE);
        formatter.closeBrace(style);
        lastWritten = LastWritten.Other;
    }

    void writeIdentifier(final String identifier) {
        writeIdentifier(identifier, null);
    }

    void writeIdentifier(final String identifier, final Role<Identifier> identifierRole) {
        writeSpecialsUpToRole(identifierRole != null ? identifierRole : Roles.IDENTIFIER);

        if (isKeyword(identifier, containerStack.peek())) {
            if (lastWritten == LastWritten.KeywordOrIdentifier) {
                space();
            }
            // this space instanceof not strictly required, so we call space()
//            formatter.writeToken("$");
        }
        else if (lastWritten == LastWritten.KeywordOrIdentifier) {
            formatter.space();
            // this space instanceof strictly required, so we directly call the formatter
        }

        if (identifierRole == Roles.LABEL) {
            formatter.writeLabel(identifier);
        }
        else {
            formatter.writeIdentifier(identifier);
        }

        lastWritten = LastWritten.KeywordOrIdentifier;
    }

    void writeToken(final TokenRole tokenRole) {
        writeToken(tokenRole.getToken(), tokenRole);
    }

    void writeToken(final String token, final Role role) {
        writeSpecialsUpToRole(role);

        if (lastWritten == LastWritten.Plus && token.charAt(0) == '+' ||
            lastWritten == LastWritten.Minus && token.charAt(0) == '-' ||
            lastWritten == LastWritten.Ampersand && token.charAt(0) == '&' ||
            lastWritten == LastWritten.QuestionMark && token.charAt(0) == '?' ||
            lastWritten == LastWritten.Division && token.charAt(0) == '*') {

            formatter.space();
        }

        if (role instanceof TokenRole) {
            final TokenRole tokenRole = (TokenRole) role;

            if (tokenRole.isKeyword()) {
                formatter.writeKeyword(token);
                lastWritten = LastWritten.KeywordOrIdentifier;
                return;
            }
            else if (tokenRole.isOperator()) {
                formatter.writeOperator(token);
                lastWritten = LastWritten.Operator;
                return;
            }
        }

        formatter.writeToken(token);

        switch (token) {
            case "+":
                lastWritten = LastWritten.Plus;
                break;
            case "-":
                lastWritten = LastWritten.Minus;
                break;
            case "&":
                lastWritten = LastWritten.Ampersand;
                break;
            case "?":
                lastWritten = LastWritten.QuestionMark;
                break;
            case "/":
                lastWritten = LastWritten.Division;
                break;
            default:
                lastWritten = LastWritten.Other;
                break;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Commas">

    void comma(final AstNode nextNode) {
        comma(nextNode, false);
    }

    void comma(final AstNode nextNode, final boolean noSpaceAfterComma) {
        writeSpecialsUpToRole(Roles.COMMA, nextNode);
        space(policy.SpaceBeforeBracketComma);

        formatter.writeToken(",");
        lastWritten = LastWritten.Other;
        space(!noSpaceAfterComma && policy.SpaceAfterBracketComma);
    }

    void optionalComma() {
        // Look if there's a comma after the current node, and insert it if it exists.
        AstNode position = positionStack.peek();

        while (position != null && position.getNodeType() == NodeType.WHITESPACE) {
            position = position.getNextSibling();
        }

        if (position != null && position.getRole() == Roles.COMMA) {
            comma(null, true);
        }
    }

    void semicolon() {
        final Role role = containerStack.peek().getRole();
        if (!(role == ForStatement.INITIALIZER_ROLE || role == ForStatement.ITERATOR_ROLE)) {
            writeToken(Roles.SEMICOLON);
            newLine();
        }
    }

    private void optionalSemicolon() {
        // Look if there's a Semicolon after the current node, and insert it if it exists.
        AstNode pos = positionStack.peek();
        while (pos != null && pos.getNodeType() == NodeType.WHITESPACE) {
            pos = pos.getNextSibling();
        }
        if (pos != null && pos.getRole() == Roles.SEMICOLON) {
            semicolon();
        }
    }

    private void writeCommaSeparatedList(final Iterable<? extends AstNode> list) {
        boolean isFirst = true;
        for (final AstNode node : list) {
            if (isFirst) {
                isFirst = false;
            }
            else {
                comma(node);
            }
            node.acceptVisitor(this, null);
        }
    }

    private void writePipeSeparatedList(final Iterable<? extends AstNode> list) {
        boolean isFirst = true;
        for (final AstNode node : list) {
            if (isFirst) {
                isFirst = false;
            }
            else {
                writeToken(Roles.PIPE);
            }
            node.acceptVisitor(this, null);
        }
    }

    private void writeCommaSeparatedListInParenthesis(final Iterable<? extends AstNode> list, final boolean spaceWithin) {
        leftParenthesis();
        if (any(list)) {
            space(spaceWithin);
            writeCommaSeparatedList(list);
            space(spaceWithin);
        }
        rightParenthesis();
    }

/*
    private void writeCommaSeparatedListInBrackets(final Iterable<? extends AstNode> list, final boolean spaceWithin) {
        writeToken(Roles.LEFT_BRACKET);
        if (any(list)) {
            space(spaceWithin);
            writeCommaSeparatedList(list);
            space(spaceWithin);
        }
        writeToken(Roles.RIGHT_BRACKET);
    }
*/

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Write Constructs">

    private void writeTypeArguments(final Iterable<AstType> typeArguments) {
        if (any(typeArguments)) {
            writeToken(Roles.LEFT_CHEVRON);
            writeCommaSeparatedList(typeArguments);
            writeToken(Roles.RIGHT_CHEVRON);
        }
    }

    public void writeTypeParameters(final Iterable<TypeParameterDeclaration> typeParameters) {
        if (any(typeParameters)) {
            writeToken(Roles.LEFT_CHEVRON);
            writeCommaSeparatedList(typeParameters);
            writeToken(Roles.RIGHT_CHEVRON);
        }
    }

    private void writeModifiers(final Iterable<JavaModifierToken> modifierTokens) {
        for (final JavaModifierToken modifier : modifierTokens) {
            modifier.acceptVisitor(this, null);
        }
    }

    private void writeQualifiedIdentifier(final Iterable<Identifier> identifiers) {
        boolean first = true;

        for (final Identifier identifier : identifiers) {
            if (first) {
                first = false;
                if (lastWritten == LastWritten.KeywordOrIdentifier) {
                    formatter.space();
                }
            }
            else {
                writeSpecialsUpToRole(Roles.DOT, identifier);
                formatter.writeToken(".");
                lastWritten = LastWritten.Other;
            }

            writeSpecialsUpToNode(identifier);

            formatter.writeIdentifier(identifier.getName());
            lastWritten = LastWritten.KeywordOrIdentifier;
        }
    }

    void writeEmbeddedStatement(final Statement embeddedStatement) {
        if (embeddedStatement.isNull()) {
            newLine();
            return;
        }

        if (embeddedStatement instanceof BlockStatement) {
            visitBlockStatement((BlockStatement) embeddedStatement, null);
        }
        else {
            newLine();
            formatter.indent();
            embeddedStatement.acceptVisitor(this, null);
            formatter.unindent();
        }
    }

    void writeMethodBody(final BlockStatement body) {
        if (body.isNull()) {
            semicolon();
        }
        else {
            visitBlockStatement(body, null);
        }
    }

    void writeAttributes(final Iterable<AnnotationSection> annotations) {
        for (final AnnotationSection section : annotations) {
            section.acceptVisitor(this, null);
        }
    }

    void writePrivateImplementationType(final AstType privateImplementationType) {
        if (!privateImplementationType.isNull()) {
            privateImplementationType.acceptVisitor(this, null);
            writeToken(Roles.DOT);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Write Tokens">

    void writeKeyword(final TokenRole tokenRole) {
        writeKeyword(tokenRole.getToken(), tokenRole);
    }

    void writeKeyword(final String token) {
        writeKeyword(token, null);
    }

    void writeKeyword(final String token, final Role tokenRole) {
        if (tokenRole != null) {
            writeSpecialsUpToRole(tokenRole);
        }

        if (lastWritten == LastWritten.KeywordOrIdentifier) {
            formatter.space();
        }

        formatter.writeKeyword(token);
        lastWritten = LastWritten.KeywordOrIdentifier;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Patterns">

    void visitNodeInPattern(final INode childNode) {
        if (childNode instanceof AstNode) {
            ((AstNode) childNode).acceptVisitor(this, null);
        }
        else if (childNode instanceof IdentifierExpressionBackReference) {
            visitIdentifierExpressionBackReference((IdentifierExpressionBackReference) childNode);
        }
        else if (childNode instanceof Choice) {
            visitChoice((Choice) childNode);
        }
        else if (childNode instanceof AnyNode) {
            visitAnyNode((AnyNode) childNode);
        }
        else if (childNode instanceof BackReference) {
            visitBackReference((BackReference) childNode);
        }
        else if (childNode instanceof NamedNode) {
            visitNamedNode((NamedNode) childNode);
        }
        else if (childNode instanceof OptionalNode) {
            visitOptionalNode((OptionalNode) childNode);
        }
        else if (childNode instanceof Repeat) {
            visitRepeat((Repeat) childNode);
        }
        else {
            writePrimitiveValue(childNode);
        }
    }

    private void visitIdentifierExpressionBackReference(final IdentifierExpressionBackReference node) {
        writeKeyword("identifierBackReference");
        leftParenthesis();
        writeIdentifier(node.getReferencedGroupName());
        rightParenthesis();
    }

    private void visitChoice(final Choice choice) {
        writeKeyword("choice");
        space();
        leftParenthesis();
        newLine();
        formatter.indent();

        final INode last = lastOrDefault(choice);

        for (final INode alternative : choice) {
            visitNodeInPattern(alternative);
            if (alternative != last) {
                writeToken(Roles.COMMA);
            }
            newLine();
        }

        formatter.unindent();
        rightParenthesis();
    }

    private void visitAnyNode(final AnyNode anyNode) {
        if (!StringUtilities.isNullOrEmpty(anyNode.getGroupName())) {
            writeIdentifier(anyNode.getGroupName());
            writeToken(Roles.COLON);
            writeIdentifier("*");
        }
    }

    private void visitBackReference(final BackReference backReference) {
        writeKeyword("backReference");
        leftParenthesis();
        writeIdentifier(backReference.getReferencedGroupName());
        rightParenthesis();
    }

    private void visitNamedNode(final NamedNode namedNode) {
        if (!StringUtilities.isNullOrEmpty(namedNode.getGroupName())) {
            writeIdentifier(namedNode.getGroupName());
            writeToken(Roles.COLON);
        }
        visitNodeInPattern(namedNode.getNode());
    }

    private void visitOptionalNode(final OptionalNode optionalNode) {
        writeKeyword("optional");
        leftParenthesis();
        visitNodeInPattern(optionalNode.getNode());
        rightParenthesis();
    }

    private void visitRepeat(final Repeat repeat) {
        writeKeyword("repeat");
        leftParenthesis();

        if (repeat.getMinCount() != 0 || repeat.getMaxCount() != Integer.MAX_VALUE) {
            writeIdentifier(String.valueOf(repeat.getMinCount()));
            writeToken(Roles.COMMA);
            writeIdentifier(String.valueOf(repeat.getMaxCount()));
            writeToken(Roles.COMMA);
        }

        visitNodeInPattern(repeat.getNode());
        rightParenthesis();
    }

    // </editor-fold>

    @Override
    public Void visitComment(final Comment comment, final Void _) {
        if (lastWritten == LastWritten.Division) {
            formatter.space();
        }

        formatter.startNode(comment);
        formatter.writeComment(comment.getCommentType(), comment.getContent());
        formatter.endNode(comment);
        lastWritten = LastWritten.Whitespace;

        return null;
    }

    @Override
    public Void visitPatternPlaceholder(final AstNode node, final Pattern pattern, final Void _) {
        startNode(node);
        visitNodeInPattern(pattern);
        endNode(node);
        return null;
    }

    @Override
    public Void visitInvocationExpression(final InvocationExpression node, final Void _) {
        startNode(node);
        node.getTarget().acceptVisitor(this, null);
        space(policy.SpaceBeforeMethodCallParentheses);
        writeCommaSeparatedListInParenthesis(node.getArguments(), policy.SpaceWithinMethodCallParentheses);
        endNode(node);
        return null;
    }

    @Override
    public Void visitTypeReference(final TypeReferenceExpression node, final Void _) {
        startNode(node);
        node.getType().acceptVisitor(this, null);
        endNode(node);
        return null;
    }

    @Override
    public Void visitJavaTokenNode(final JavaTokenNode node, final Void _) {
        if (node instanceof JavaModifierToken) {
            final JavaModifierToken modifierToken = (JavaModifierToken) node;
            startNode(modifierToken);
            writeKeyword(JavaModifierToken.getModifierName(modifierToken.getModifier()));
            endNode(modifierToken);
        }
        else {
            throw ContractUtils.unsupported();
        }
        return null;
    }

    @Override
    public Void visitMemberReferenceExpression(final MemberReferenceExpression node, final Void _) {
        startNode(node);

        final Expression target = node.getTarget();

        if (!target.isNull()) {
            target.acceptVisitor(this, null);
            writeToken(Roles.DOT);
        }

        writeTypeArguments(node.getTypeArguments());
        writeIdentifier(node.getMemberName());
        endNode(node);
        return null;
    }

    @Override
    public Void visitIdentifier(final Identifier node, final Void _) {
        startNode(node);
        writeIdentifier(node.getName());
        endNode(node);
        return null;
    }

    @Override
    public Void visitNullReferenceExpression(final NullReferenceExpression node, final Void _) {
        startNode(node);
        writeKeyword("null", node.getRole());
        endNode(node);
        return null;
    }

    @Override
    public Void visitThisReferenceExpression(final ThisReferenceExpression node, final Void _) {
        startNode(node);
        writeKeyword("this", node.getRole());
        endNode(node);
        return null;
    }

    @Override
    public Void visitSuperReferenceExpression(final SuperReferenceExpression node, final Void _) {
        startNode(node);
        writeKeyword("super", node.getRole());
        endNode(node);
        return null;
    }

    @Override
    public Void visitClassOfExpression(final ClassOfExpression node, final Void _) {
        startNode(node);
        node.getType().acceptVisitor(this, _);
        writeToken(Roles.DOT);
        writeKeyword("class", node.getRole());
        endNode(node);
        return null;
    }

    @Override
    public Void visitBlockStatement(final BlockStatement node, final Void _) {
        startNode(node);

        final BraceStyle style;
        final BraceEnforcement braceEnforcement;
        final AstNode parent = node.getParent();

        if (parent instanceof ConstructorDeclaration) {
            style = policy.ConstructorBraceStyle;
            braceEnforcement = BraceEnforcement.AddBraces;
        }
        else if (parent instanceof MethodDeclaration) {
            style = policy.MethodBraceStyle;
            braceEnforcement = BraceEnforcement.AddBraces;
        }
        else {
            style = policy.StatementBraceStyle;

            if (parent instanceof IfElseStatement) {
                braceEnforcement = policy.IfElseBraceEnforcement;
            }
            else if (parent instanceof WhileStatement) {
                braceEnforcement = policy.WhileBraceEnforcement;
            }
            else {
                braceEnforcement = BraceEnforcement.AddBraces;
            }
        }

        final boolean addBraces;
        final AstNodeCollection<Statement> statements = node.getStatements();

        switch (braceEnforcement) {
            case RemoveBraces:
                addBraces = false;
                break;
            default:
                addBraces = true;
                break;
        }

        if (addBraces) {
            openBrace(style);
        }

        for (final AstNode statement : statements) {
            statement.acceptVisitor(this, null);
        }

        if (addBraces) {
            closeBrace(style);
        }

        if (!(parent instanceof Expression)) {
            newLine();
        }

        endNode(node);

        return null;
    }

    @Override
    public Void visitExpressionStatement(final ExpressionStatement node, final Void _) {
        startNode(node);
        node.getExpression().acceptVisitor(this, null);
        semicolon();
        endNode(node);
        return null;
    }

    @Override
    public Void visitBreakStatement(final BreakStatement node, final Void _) {
        startNode(node);
        writeKeyword("break");

        final String label = node.getLabel();

        if (!StringUtilities.isNullOrEmpty(label)) {
            writeIdentifier(label);
        }

        semicolon();
        endNode(node);
        return null;
    }

    @Override
    public Void visitContinueStatement(final ContinueStatement node, final Void _) {
        startNode(node);
        writeKeyword("continue");

        final String label = node.getLabel();

        if (!StringUtilities.isNullOrEmpty(label)) {
            writeIdentifier(label);
        }

        semicolon();
        endNode(node);
        return null;
    }

    @Override
    public Void visitDoWhileStatement(final DoWhileStatement node, final Void _) {
        startNode(node);
        writeKeyword(DoWhileStatement.DO_KEYWORD_ROLE);
        writeEmbeddedStatement(node.getEmbeddedStatement());
        writeKeyword(DoWhileStatement.WHILE_KEYWORD_ROLE);
        space(policy.SpaceBeforeWhileParentheses);
        leftParenthesis();
        space(policy.SpacesWithinWhileParentheses);
        node.getCondition().acceptVisitor(this, null);
        space(policy.SpacesWithinWhileParentheses);
        rightParenthesis();
        semicolon();
        endNode(node);
        return null;
    }

    @Override
    public Void visitEmptyStatement(final EmptyStatement node, final Void _) {
        startNode(node);
        semicolon();
        endNode(node);
        return null;
    }

    @Override
    public Void visitIfElseStatement(final IfElseStatement node, final Void _) {
        startNode(node);
        writeKeyword(IfElseStatement.IF_KEYWORD_ROLE);
        space(policy.SpaceBeforeIfParentheses);
        leftParenthesis();
        space(policy.SpacesWithinIfParentheses);
        node.getCondition().acceptVisitor(this, null);
        space(policy.SpacesWithinIfParentheses);
        rightParenthesis();
        writeEmbeddedStatement(node.getTrueStatement());

        if (!node.getFalseStatement().isNull()) {
            writeKeyword(IfElseStatement.ELSE_KEYWORD_ROLE);
            writeEmbeddedStatement(node.getFalseStatement());
        }

        endNode(node);
        return null;
    }

    @Override
    public Void visitLabelStatement(final LabelStatement node, final Void _) {
        startNode(node);
        writeIdentifier(node.getLabel(), Roles.LABEL);
        writeToken(Roles.COLON);

        boolean foundLabelledStatement = false;

        for (AstNode sibling = node.getNextSibling(); sibling != null; sibling = sibling.getNextSibling()) {
            if (sibling.getRole() == node.getRole()) {
                foundLabelledStatement = true;
            }
        }

        if (!foundLabelledStatement) {
            // introduce an EmptyStatement so that the output becomes syntactically valid
            writeToken(Roles.SEMICOLON);
        }

        newLine();
        endNode(node);
        return null;
    }

    @Override
    public Void visitReturnStatement(final ReturnStatement node, final Void _) {
        startNode(node);
        writeKeyword(ReturnStatement.RETURN_KEYWORD_ROLE);

        if (!node.getExpression().isNull()) {
            space();
            node.getExpression().acceptVisitor(this, null);
        }

        semicolon();
        endNode(node);
        return null;
    }

    @Override
    public Void visitSwitchStatement(final SwitchStatement node, final Void _) {
        startNode(node);
        writeKeyword(SwitchStatement.SWITCH_KEYWORD_ROLE);
        space(policy.SpaceBeforeSwitchParentheses);
        leftParenthesis();
        space(policy.SpacesWithinSwitchParentheses);
        node.getExpression().acceptVisitor(this, _);
        space(policy.SpacesWithinSwitchParentheses);
        rightParenthesis();
        openBrace(policy.StatementBraceStyle);

        if (policy.IndentSwitchBody) {
            formatter.indent();
        }

        for (final SwitchSection section : node.getSwitchSections()) {
            section.acceptVisitor(this, _);
        }

        if (policy.IndentSwitchBody) {
            formatter.unindent();
        }

        closeBrace(policy.StatementBraceStyle);
        newLine();
        endNode(node);

        return null;
    }

    @Override
    public Void visitSwitchSection(final SwitchSection node, final Void _) {
        startNode(node);

        boolean first = true;

        for (final CaseLabel label : node.getCaseLabels()) {
            if (!first) {
                newLine();
            }
            label.acceptVisitor(this, _);
            first = false;
        }

        final boolean isBlock = node.getStatements().size() == 1 &&
                                firstOrDefault(node.getStatements()) instanceof BlockStatement;

        if (policy.IndentCaseBody && !isBlock) {
            formatter.indent();
        }

        if (!isBlock) {
            newLine();
        }

        for (final Statement statement : node.getStatements()) {
            statement.acceptVisitor(this, _);
        }

        if (policy.IndentCaseBody && !isBlock) {
            formatter.unindent();
        }

        endNode(node);
        return null;
    }

    @Override
    public Void visitCaseLabel(final CaseLabel node, final Void _) {
        startNode(node);

        if (node.getExpression().isNull()) {
            writeKeyword(CaseLabel.DEFAULT_KEYWORD_ROLE);
        }
        else {
            writeKeyword(CaseLabel.CASE_KEYWORD_ROLE);
            space();
            node.getExpression().acceptVisitor(this, _);
        }

        writeToken(Roles.COLON);
        endNode(node);
        return null;
    }

    @Override
    public Void visitThrowStatement(final ThrowStatement node, final Void _) {
        startNode(node);
        writeKeyword(ThrowStatement.THROW_KEYWORD_ROLE);

        if (!node.getExpression().isNull()) {
            space();
            node.getExpression().acceptVisitor(this, _);
        }

        semicolon();
        endNode(node);
        return null;
    }

    @Override
    public Void visitCatchClause(final CatchClause node, final Void _) {
        startNode(node);
        writeKeyword(CatchClause.CATCH_KEYWORD_ROLE);

        if (!node.getExceptionTypes().isEmpty()) {
            space(policy.SpaceBeforeCatchParentheses);
            leftParenthesis();
            space(policy.SpacesWithinCatchParentheses);
            writePipeSeparatedList(node.getExceptionTypes());

            if (!StringUtilities.isNullOrEmpty(node.getVariableName())) {
                space();
                node.getVariableNameToken().acceptVisitor(this, _);
            }

            space(policy.SpacesWithinCatchParentheses);
            rightParenthesis();
        }

        node.getBody().acceptVisitor(this, _);
        endNode(node);
        return null;
    }

    @Override
    public Void visitAnnotationSection(final AnnotationSection node, final Void _) {
        return null;
    }

    @Override
    public Void visitAnnotation(final Annotation node, final Void _) {
        return null;
    }

    @Override
    public Void visitNewLine(final NewLineNode node, final Void _) {
        formatter.startNode(node);
        formatter.newLine();
        formatter.endNode(node);
        return null;
    }

    @Override
    public Void visitVariableDeclaration(final VariableDeclarationStatement node, final Void _) {
        startNode(node);
        writeModifiers(node.getChildrenByRole(VariableDeclarationStatement.MODIFIER_ROLE));
        node.getType().acceptVisitor(this, _);
        space();
        writeCommaSeparatedList(node.getVariables());
        semicolon();
        endNode(node);
        return null;
    }

    @Override
    public Void visitVariableInitializer(final VariableInitializer node, final Void _) {
        startNode(node);
        node.getNameToken().acceptVisitor(this, _);

        if (!node.getInitializer().isNull()) {
            space(policy.SpaceAroundAssignment);
            writeToken(Roles.ASSIGN);
            space(policy.SpaceAroundAssignment);
            node.getInitializer().acceptVisitor(this, _);
        }

        endNode(node);
        return null;
    }

    @Override
    public Void visitText(final TextNode node, final Void _) {
        return null;
    }

    @Override
    public Void visitImportDeclaration(final ImportDeclaration node, final Void _) {
        startNode(node);
        writeKeyword(ImportDeclaration.IMPORT_KEYWORD_RULE);
        node.getImport().acceptVisitor(this, _);
        semicolon();
        endNode(node);
        return null;
    }

    @Override
    public Void visitSimpleType(final SimpleType node, final Void _) {
        startNode(node);

        final TypeDefinition typeDefinition = node.getUserData(Keys.TYPE_DEFINITION);

        if (typeDefinition != null && typeDefinition.isPrimitive()) {
            writeKeyword(typeDefinition.getSimpleName());
        }
        else {
            writeIdentifier(node.getIdentifier());
        }

        writeTypeArguments(node.getTypeArguments());
        endNode(node);
        return null;
    }

    @Override
    public Void visitMethodDeclaration(final MethodDeclaration node, final Void _) {
        startNode(node);
        writeAttributes(node.getAnnotations());
        writeModifiers(node.getModifiers());

        final MethodDefinition definition = node.getUserData(Keys.METHOD_DEFINITION);

        if (definition == null || !definition.isTypeInitializer()) {
            final AstNodeCollection<TypeParameterDeclaration> typeParameters = node.getTypeParameters();

            if (any(typeParameters)) {
                space();
                writeTypeParameters(typeParameters);
                space();
            }

            node.getReturnType().acceptVisitor(this, _);
            space();
            writePrivateImplementationType(node.getPrivateImplementationType());
            node.getNameToken().acceptVisitor(this, _);
            space(policy.SpaceBeforeMethodDeclarationParentheses);
            writeCommaSeparatedListInParenthesis(node.getParameters(), policy.SpaceWithinMethodDeclarationParentheses);
        }

//        for (final Constraint constraint : node.getConstraints()) {
//            constraint.acceptVisitor(this, _);
//        }

        writeMethodBody(node.getBody());
        endNode(node);
        return null;
    }

    @Override
    public Void visitConstructorDeclaration(final ConstructorDeclaration node, final Void _) {
        startNode(node);
        writeAttributes(node.getAnnotations());
        writeModifiers(node.getModifiers());

        final AstNode parent = node.getParent();
        final TypeDeclaration type = parent instanceof TypeDeclaration ? (TypeDeclaration) parent : null;

        startNode(node.getNameToken());
        writeIdentifier(type != null ? type.getName() : node.getName());
        endNode(node.getNameToken());
        space(policy.SpaceBeforeConstructorDeclarationParentheses);
        writeCommaSeparatedListInParenthesis(node.getParameters(), policy.SpaceWithinMethodDeclarationParentheses);
        writeMethodBody(node.getBody());
        endNode(node);
        return null;
    }

    @Override
    public Void visitTypeParameterDeclaration(final TypeParameterDeclaration node, final Void _) {
        startNode(node);
        writeAttributes(node.getAnnotations());
        node.getNameToken().acceptVisitor(this, _);

        final AstType extendsBound = node.getExtendsBound();

        if (extendsBound != null && !extendsBound.isNull()) {
            writeKeyword(Roles.EXTENDS_KEYWORD);
            extendsBound.acceptVisitor(this, _);
        }

        endNode(node);
        return null;
    }

    @Override
    public Void visitParameterDeclaration(final ParameterDeclaration node, final Void _) {
        startNode(node);
        writeAttributes(node.getAnnotations());
        node.getType().acceptVisitor(this, _);

        if (!node.getType().isNull() && !StringUtilities.isNullOrEmpty(node.getName())) {
            space();
        }

        if (!StringUtilities.isNullOrEmpty(node.getName())) {
            node.getNameToken().acceptVisitor(this, _);
        }

        endNode(node);
        return null;
    }

    @Override
    public Void visitFieldDeclaration(final FieldDeclaration node, final Void _) {
        startNode(node);
        writeAttributes(node.getAnnotations());
        writeModifiers(node.getModifiers());
        node.getReturnType().acceptVisitor(this, _);
        space();
        writeCommaSeparatedList(node.getVariables());
        semicolon();
        endNode(node);
        return null;
    }

    @Override
    public Void visitTypeDeclaration(final TypeDeclaration node, final Void _) {
        startNode(node);

        final TypeDefinition type = node.getUserData(Keys.TYPE_DEFINITION);

        final boolean isTrulyAnonymous = type != null &&
                                         type.isAnonymous() &&
                                         node.getParent() instanceof AnonymousObjectCreationExpression;

        if (!isTrulyAnonymous) {
            writeAttributes(node.getAnnotations());
            writeModifiers(node.getModifiers());

            switch (node.getClassType()) {
                case ENUM:
                    writeKeyword(Roles.ENUM_KEYWORD);
                    break;
                case INTERFACE:
                    writeKeyword(Roles.INTERFACE_KEYWORD);
                    break;
                case ANNOTATION:
                    writeKeyword(Roles.ANNOTATION_KEYWORD);
                    break;
                default:
                    writeKeyword(Roles.CLASS_KEYWORD);
                    break;
            }

            node.getNameToken().acceptVisitor(this, _);
            writeTypeParameters(node.getTypeParameters());

            if (!node.getBaseType().isNull()) {
                space();
                writeKeyword(Roles.EXTENDS_KEYWORD);
                space();
                node.getBaseType().acceptVisitor(this, _);
            }

            if (any(node.getInterfaces())) {
                space();
                writeKeyword(Roles.IMPLEMENTS_KEYWORD);
                space();
                writeCommaSeparatedList(node.getInterfaces());
            }
        }

        final BraceStyle braceStyle;

        switch (node.getClassType()) {
            case ENUM:
                braceStyle = policy.EnumBraceStyle;
                break;
            case INTERFACE:
                braceStyle = policy.InterfaceBraceStyle;
                break;
            case ANNOTATION:
                braceStyle = policy.AnnotationBraceStyle;
                break;
            default:
                if (type != null && type.isAnonymous()) {
                    braceStyle = policy.AnonymousClassBraceStyle;
                }
                else {
                    braceStyle = policy.ClassBraceStyle;
                }
                break;
        }

//        for (final Constraint constraint : node.getConstraints()) {
//            constraint.acceptVisitor(this, _);
//        }

        openBrace(braceStyle);

//        if (node.getClassType() == ClassType.ENUM) {
//            boolean first = true;
//            for (final EntityDeclaration member : node.getMembers()) {
//                if (first) {
//                    first = false;
//                }
//                else {
//                    comma(member, true);
//                    newLine();
//                }
//                member.acceptVisitor(this, _);
//            }
//            optionalComma();
//            newLine();
//        }
//        else {
        boolean first = true;

        for (final EntityDeclaration member : node.getMembers()) {
            if (first) {
                first = false;
            }
            else {
                for (int i = 0; i < policy.BlankLinesBetweenMembers; i++) {
                    formatter.newLine();
                }
            }
            member.acceptVisitor(this, _);
        }
//        }

        closeBrace(braceStyle);

        if (type == null || !type.isAnonymous()) {
            optionalSemicolon();
            newLine();
        }

        endNode(node);
        return null;
    }

    @Override
    public Void visitCompilationUnit(final CompilationUnit node, final Void _) {
        for (final AstNode child : node.getChildren()) {
            child.acceptVisitor(this, _);
        }
        return null;
    }

    @Override
    public Void visitPackageDeclaration(final PackageDeclaration node, final Void _) {
        startNode(node);
        writeKeyword(Roles.PACKAGE_KEYWORD);
        writeQualifiedIdentifier(node.getIdentifiers());
        semicolon();
        newLine();
        endNode(node);
        return null;
    }

    @Override
    public Void visitArraySpecifier(final ArraySpecifier node, final Void _) {
        startNode(node);
        writeToken(Roles.LEFT_BRACKET);

        for (final JavaTokenNode comma : node.getChildrenByRole(Roles.COMMA)) {
            writeSpecialsUpToNode(comma);
            formatter.writeToken(",");
            lastWritten = LastWritten.Other;
        }

        writeToken(Roles.RIGHT_BRACKET);
        endNode(node);
        return null;
    }

    @Override
    public Void visitComposedType(final ComposedType node, final Void _) {
        startNode(node);
        node.getBaseType().acceptVisitor(this, _);

        for (final ArraySpecifier specifier : node.getArraySpecifiers()) {
            specifier.acceptVisitor(this, _);
        }

        endNode(node);
        return null;
    }

    @Override
    public Void visitWhileStatement(final WhileStatement node, final Void _) {
        startNode(node);
        writeKeyword(WhileStatement.WHILE_KEYWORD_ROLE);
        space(policy.SpaceBeforeWhileParentheses);
        leftParenthesis();
        space(policy.SpacesWithinWhileParentheses);
        node.getCondition().acceptVisitor(this, _);
        space(policy.SpacesWithinWhileParentheses);
        rightParenthesis();
        writeEmbeddedStatement(node.getEmbeddedStatement());
        endNode(node);
        return null;
    }

    @Override
    public Void visitPrimitiveExpression(final PrimitiveExpression node, final Void _) {
        startNode(node);
        if (!StringUtilities.isNullOrEmpty(node.getLiteralValue())) {
            formatter.writeLiteral(node.getLiteralValue());
        }
        else {
            writePrimitiveValue(node.getValue());
        }
        endNode(node);
        return null;
    }

    void writePrimitiveValue(final Object val) {
        if (val == null) {
            writeKeyword("null");
            return;
        }

        if (val instanceof Boolean) {
            if ((Boolean) val) {
                writeKeyword("true");
            }
            else {
                writeKeyword("false");
            }
            return;
        }

        if (val instanceof String) {
            formatter.writeTextLiteral("\"" + convertString(val.toString()) + "\"");
            lastWritten = LastWritten.Other;
        }
        else if (val instanceof Character) {
            formatter.writeTextLiteral("'" + convertCharacter((Character) val) + "'");
            lastWritten = LastWritten.Other;
        }
        else if (val instanceof Float) {
            final float f = (Float) val;
            if (Float.isInfinite(f) || Float.isNaN(f)) {
                writeKeyword("Float");
                writeToken(Roles.DOT);
                if (f == Float.POSITIVE_INFINITY) {
                    writeIdentifier("POSITIVE_INFINITY");
                }
                else if (f == Float.NEGATIVE_INFINITY) {
                    writeIdentifier("NEGATIVE_INFINITY");
                }
                else {
                    writeIdentifier("NaN");
                }
                return;
            }
            formatter.writeLiteral(Float.toString(f) + "f");
            lastWritten = LastWritten.Other;
        }
        else if (val instanceof Double) {
            final double d = (Double) val;
            if (Double.isInfinite(d) || Double.isNaN(d)) {
                writeKeyword("Double");
                writeToken(Roles.DOT);
                if (d == Double.POSITIVE_INFINITY) {
                    writeIdentifier("POSITIVE_INFINITY");
                }
                else if (d == Double.NEGATIVE_INFINITY) {
                    writeIdentifier("NEGATIVE_INFINITY");
                }
                else {
                    writeIdentifier("NaN");
                }
                return;
            }

            String number = Double.toString(d);

            if (number.indexOf('.') < 0 && number.indexOf('E') < 0) {
                number += ".0";
            }

            formatter.writeLiteral(number);
            lastWritten = LastWritten.KeywordOrIdentifier;
        }
        else if (val instanceof Long) {
            formatter.writeLiteral(String.valueOf(val) + "L");
            lastWritten = LastWritten.Other;
        }
        else {
            formatter.writeLiteral(String.valueOf(val));
            lastWritten = LastWritten.Other;
        }
    }

    @Override
    public Void visitCastExpression(final CastExpression node, final Void _) {
        startNode(node);
        leftParenthesis();
        space(policy.SpacesWithinCastParentheses);
        node.getType().acceptVisitor(this, _);
        space(policy.SpacesWithinCastParentheses);
        rightParenthesis();
        space(policy.SpaceAfterTypecast);
        node.getExpression().acceptVisitor(this, _);
        endNode(node);
        return null;
    }

    @Override
    public Void visitBinaryOperatorExpression(final BinaryOperatorExpression node, final Void _) {
        startNode(node);
        node.getLeft().acceptVisitor(this, _);

        final boolean spacePolicy;

        switch (node.getOperator()) {
            case BITWISE_AND:
            case BITWISE_OR:
            case EXCLUSIVE_OR:
                spacePolicy = policy.SpaceAroundBitwiseOperator;
                break;
            case LOGICAL_AND:
            case LOGICAL_OR:
                spacePolicy = policy.SpaceAroundLogicalOperator;
                break;
            case GREATER_THAN:
            case GREATER_THAN_OR_EQUAL:
            case LESS_THAN_OR_EQUAL:
            case LESS_THAN:
                spacePolicy = policy.SpaceAroundRelationalOperator;
                break;
            case EQUALITY:
            case INEQUALITY:
                spacePolicy = policy.SpaceAroundEqualityOperator;
                break;
            case ADD:
            case SUBTRACT:
                spacePolicy = policy.SpaceAroundAdditiveOperator;
                break;
            case MULTIPLY:
            case DIVIDE:
            case MODULUS:
                spacePolicy = policy.SpaceAroundMultiplicativeOperator;
                break;
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
            case UNSIGNED_SHIFT_RIGHT:
                spacePolicy = policy.SpaceAroundShiftOperator;
                break;
            default:
                spacePolicy = true;
        }

        space(spacePolicy);
        writeToken(BinaryOperatorExpression.getOperatorRole(node.getOperator()));
        space(spacePolicy);
        node.getRight().acceptVisitor(this, _);
        endNode(node);
        return null;
    }

    @Override
    public Void visitInstanceOfExpression(final InstanceOfExpression node, final Void _) {
        startNode(node);
        node.getExpression().acceptVisitor(this, _);
        space();
        writeKeyword(InstanceOfExpression.INSTANCE_OF_KEYWORD_ROLE);
        node.getType().acceptVisitor(this, _);
        endNode(node);
        return null;
    }

    @Override
    public Void visitIndexerExpression(final IndexerExpression node, final Void _) {
        startNode(node);
        node.getTarget().acceptVisitor(this, _);
        space(policy.SpaceBeforeMethodCallParentheses);
        writeToken(Roles.LEFT_BRACKET);
        node.getArgument().acceptVisitor(this, _);
        writeToken(Roles.RIGHT_BRACKET);
        endNode(node);
        return null;
    }

    @Override
    public Void visitIdentifierExpression(final IdentifierExpression node, final Void _) {
        startNode(node);
        writeIdentifier(node.getIdentifier());
        writeTypeArguments(node.getTypeArguments());
        endNode(node);
        return null;
    }

    @Override
    public Void visitUnaryOperatorExpression(final UnaryOperatorExpression node, final Void _) {
        startNode(node);

        final UnaryOperatorType operator = node.getOperator();
        final TokenRole symbol = UnaryOperatorExpression.getOperatorRole(operator);

        if (operator != UnaryOperatorType.POST_INCREMENT && operator != UnaryOperatorType.POST_DECREMENT) {
            writeToken(symbol);
        }

        node.getExpression().acceptVisitor(this, _);

        if (operator == UnaryOperatorType.POST_INCREMENT || operator == UnaryOperatorType.POST_DECREMENT) {
            writeToken(symbol);
        }

        endNode(node);
        return null;
    }

    @Override
    public Void visitConditionalExpression(final ConditionalExpression node, final Void _) {
        startNode(node);
        node.getCondition().acceptVisitor(this, _);

        space(policy.SpaceBeforeConditionalOperatorCondition);
        writeToken(ConditionalExpression.QUESTION_MARK_ROLE);
        space(policy.SpaceAfterConditionalOperatorCondition);

        node.getTrueExpression().acceptVisitor(this, _);

        space(policy.SpaceBeforeConditionalOperatorSeparator);
        writeToken(ConditionalExpression.COLON_ROLE);
        space(policy.SpaceAfterConditionalOperatorSeparator);

        node.getFalseExpression().acceptVisitor(this, _);

        endNode(node);
        return null;
    }

    @Override
    public Void visitArrayInitializerExpression(final ArrayInitializerExpression node, final Void _) {
        startNode(node);
        writeInitializerElements(node.getElements());
        endNode(node);
        return null;
    }

    private void writeInitializerElements(final AstNodeCollection<Expression> elements) {
        final BraceStyle style;

        if (policy.ArrayInitializerWrapping == Wrapping.WrapAlways) {
            style = BraceStyle.NextLine;
        }
        else {
            style = BraceStyle.EndOfLine;
        }

        openBrace(style);

        boolean isFirst = true;

        for (final AstNode node : elements) {
            if (isFirst) {
                isFirst = false;
            }
            else {
                comma(node, true);
                newLine();
            }
            node.acceptVisitor(this, null);
        }

        optionalComma();
        newLine();
        closeBrace(style);
    }

    @Override
    public Void visitObjectCreationExpression(final ObjectCreationExpression node, final Void _) {
        startNode(node);
        writeKeyword(ObjectCreationExpression.NEW_KEYWORD_ROLE);
        node.getType().acceptVisitor(this, _);
        space(policy.SpaceBeforeMethodCallParentheses);
        writeCommaSeparatedListInParenthesis(node.getArguments(), policy.SpaceWithinMethodCallParentheses);
        endNode(node);
        return null;
    }

    @Override
    public Void visitAnonymousObjectCreationExpression(final AnonymousObjectCreationExpression node, final Void _) {
        startNode(node);
        writeKeyword(ObjectCreationExpression.NEW_KEYWORD_ROLE);
        node.getType().acceptVisitor(this, _);
        space(policy.SpaceBeforeMethodCallParentheses);
        writeCommaSeparatedListInParenthesis(node.getArguments(), policy.SpaceWithinMethodCallParentheses);
        visitTypeDeclaration(node.getTypeDeclaration(), _);
        endNode(node);
        return null;
    }

    @Override
    public Void visitWildcardType(final WildcardType node, final Void _) {
        startNode(node);
        writeToken(WildcardType.WILDCARD_TOKEN_ROLE);

        final AstNodeCollection<AstType> extendsBounds = node.getExtendsBounds();

        if (!extendsBounds.isEmpty()) {
            space();
            writeKeyword(WildcardType.EXTENDS_KEYWORD_ROLE);
            writePipeSeparatedList(extendsBounds);
        }
        else {
            final AstNodeCollection<AstType> superBounds = node.getSuperBounds();

            if (!superBounds.isEmpty()) {
                space();
                writeKeyword(WildcardType.SUPER_KEYWORD_ROLE);
                writePipeSeparatedList(superBounds);
            }
        }

        endNode(node);
        return null;
    }

    @Override
    public Void visitArrayCreationExpression(final ArrayCreationExpression node, final Void _) {
        startNode(node);
        writeKeyword(ArrayCreationExpression.NEW_KEYWORD_ROLE);
        node.getType().acceptVisitor(this, _);

        if (!node.getDimension().isNull()) {
            writeToken(Roles.LEFT_BRACKET);
            node.getDimension().acceptVisitor(this, _);
            writeToken(Roles.RIGHT_BRACKET);
        }

        for (final ArraySpecifier specifier : node.getAdditionalArraySpecifiers()) {
            specifier.acceptVisitor(this, _);
        }

        node.getInitializer().acceptVisitor(this, _);
        endNode(node);
        return null;
    }

    @Override
    public Void visitAssignmentExpression(final AssignmentExpression node, final Void _) {
        startNode(node);
        node.getLeft().acceptVisitor(this, _);
        space(policy.SpaceAroundAssignment);
        writeToken(AssignmentExpression.getOperatorRole(node.getOperator()));
        space(policy.SpaceAroundAssignment);
        node.getRight().acceptVisitor(this, _);
        endNode(node);
        return null;
    }

    @Override
    public Void visitForStatement(final ForStatement node, final Void _) {
        startNode(node);
        writeKeyword(ForStatement.FOR_KEYWORD_ROLE);
        space(policy.SpaceBeforeForParentheses);
        leftParenthesis();
        space(policy.SpacesWithinForParentheses);

        writeCommaSeparatedList(node.getInitializers());
        space(policy.SpaceBeforeForSemicolon);
        writeToken(Roles.SEMICOLON);
        space(policy.SpaceAfterForSemicolon);

        node.getCondition().acceptVisitor(this, _);
        space(policy.SpaceBeforeForSemicolon);
        writeToken(Roles.SEMICOLON);

        if (any(node.getIterators())) {
            space(policy.SpaceAfterForSemicolon);
            writeCommaSeparatedList(node.getIterators());
        }

        space(policy.SpacesWithinForParentheses);
        rightParenthesis();
        writeEmbeddedStatement(node.getEmbeddedStatement());
        endNode(node);
        return null;
    }

    @Override
    public Void visitForEachStatement(final ForEachStatement node, final Void _) {
        startNode(node);
        writeKeyword(ForEachStatement.FOR_KEYWORD_ROLE);
        space(policy.SpaceBeforeForeachParentheses);
        leftParenthesis();
        space(policy.SpacesWithinForeachParentheses);
        writeModifiers(node.getChildrenByRole(EntityDeclaration.MODIFIER_ROLE));
        node.getVariableType().acceptVisitor(this, _);
        space();
        node.getVariableNameToken().acceptVisitor(this, _);
        space();
        writeToken(ForEachStatement.COLON_ROLE);
        space();
        node.getInExpression().acceptVisitor(this, _);
        space(policy.SpacesWithinForeachParentheses);
        rightParenthesis();
        writeEmbeddedStatement(node.getEmbeddedStatement());
        endNode(node);
        return null;
    }

    @Override
    public Void visitTryCatchStatement(final TryCatchStatement node, final Void _) {
        startNode(node);
        writeKeyword(TryCatchStatement.TRY_KEYWORD_ROLE);
        node.getTryBlock().acceptVisitor(this, _);

        for (final CatchClause catchClause : node.getCatchClauses()) {
            catchClause.acceptVisitor(this, _);
        }

        if (!node.getFinallyBlock().isNull()) {
            writeKeyword(TryCatchStatement.FINALLY_KEYWORD_ROLE);
            node.getFinallyBlock().acceptVisitor(this, _);
        }

        endNode(node);
        return null;
    }

    @Override
    public Void visitGotoStatement(final GotoStatement node, final Void _) {
        startNode(node);
        writeKeyword(GotoStatement.GOTO_KEYWORD_ROLE);
        writeIdentifier(node.getLabel());
        semicolon();
        endNode(node);
        return null;
    }

    @Override
    public Void visitParenthesizedExpression(final ParenthesizedExpression node, final Void _) {
        startNode(node);
        leftParenthesis();
        space(policy.SpacesWithinParentheses);
        node.getExpression().acceptVisitor(this, _);
        space(policy.SpacesWithinParentheses);
        rightParenthesis();
        endNode(node);
        return null;
    }

    @Override
    public Void visitSynchronizedStatement(final SynchronizedStatement node, final Void _) {
        startNode(node);
        writeKeyword(SynchronizedStatement.SYNCHRONIZED_KEYWORD_ROLE);
        space(policy.SpaceBeforeSynchronizedParentheses);
        leftParenthesis();
        space(policy.SpacesWithinSynchronizedParentheses);
        node.getExpression().acceptVisitor(this, _);
        space(policy.SpacesWithinSynchronizedParentheses);
        rightParenthesis();
        writeEmbeddedStatement(node.getEmbeddedStatement());
        endNode(node);
        return null;
    }

    // <editor-fold defaultstate="collapsed" desc="Utility Methods">

    public static String convertCharacter(final char ch) {
        switch (ch) {
            case '\\':
                return "\\\\";
            case '\0':
                return "\\0";
            case '\b':
                return "\\b";
            case '\f':
                return "\\f";
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\t':
                return "\\t";

            default:
                if (Character.isISOControl(ch) ||
                    Character.isSurrogate(ch) ||
                    Character.isWhitespace(ch) && ch != ' ') {

                    return format("\\u%1$04x", (int) ch);
                }
                else {
                    return String.valueOf(ch);
                }
        }
    }

    public static String convertString(final String s) {
        return convertString(s, false);
    }

    public static String convertString(final String s, final boolean quote) {
        final StringBuilder sb = new StringBuilder(Math.min(16, s.length()));

        if (quote) {
            sb.append('"');
        }

        for (int i = 0, n = s.length(); i < n; i++) {
            sb.append(convertCharacter(s.charAt(i)));
        }

        if (quote) {
            sb.append('"');
        }

        return sb.toString();
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="LastWritten Enum">

    enum LastWritten {
        Whitespace,
        Other,
        KeywordOrIdentifier,
        Plus,
        Minus,
        Ampersand,
        QuestionMark,
        Division,
        Operator
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Keywords">

    private final static String[] KEYWORDS = {
        "abstract",
        "assert",
        "boolean",
        "break",
        "byte",
        "case",
        "catch",
        "char",
        "class",
        "const",
        "continue",
        "default",
        "do",
        "double",
        "else",
        "enum",
        "extends",
        "final",
        "finally",
        "float",
        "for",
        "goto",
        "if",
        "implements",
        "import",
        "instanceof",
        "int",
        "interface",
        "long",
        "native",
        "new",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "short",
        "static",
        "strictfp",
        "super",
        "switch",
        "synchronized",
        "this",
        "throw",
        "throws",
        "transient",
        "try",
        "void",
        "volatile",
        "while"
    };

    public static boolean isKeyword(final String identifier) {
        return ArrayUtilities.contains(KEYWORDS, identifier);
    }

    @SuppressWarnings("UnusedParameters")
    public static boolean isKeyword(final String identifier, final AstNode context) {
        return ArrayUtilities.contains(KEYWORDS, identifier);
    }

    // </editor-fold>
}
