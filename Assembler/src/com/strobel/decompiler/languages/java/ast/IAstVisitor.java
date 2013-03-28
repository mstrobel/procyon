/*
 * IAstVisitor.java
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

package com.strobel.decompiler.languages.java.ast;

import com.strobel.decompiler.patterns.Pattern;

public interface IAstVisitor<T, R> {
    R visitComment(Comment comment, T data);
    R visitPatternPlaceholder(AstNode patternPlaceholder, Pattern pattern, T data);
    R visitInvocationExpression(InvocationExpression invocationExpression, T data);
    R visitTypeReference(TypeReferenceExpression typeReferenceExpression, T data);
    R visitJavaTokenNode(JavaTokenNode javaTokenNode, T data);
    R visitMemberReferenceExpression(MemberReferenceExpression memberReferenceExpression, T data);
    R visitIdentifier(Identifier identifier, T data);
    R visitNullReferenceExpression(NullReferenceExpression nullReferenceExpression, T data);
    R visitThisReferenceExpression(ThisReferenceExpression thisReferenceExpression, T data);
    R visitSuperReferenceExpression(SuperReferenceExpression superReferenceExpression, T data);
    R visitClassOfExpression(ClassOfExpression classOfExpression, T data);
    R visitBlockStatement(BlockStatement statements, T data);
    R visitExpressionStatement(ExpressionStatement expressionStatement, T data);
    R visitBreakStatement(BreakStatement breakStatement, T data);
    R visitContinueStatement(ContinueStatement continueStatement, T data);
    R visitDoWhileStatement(DoWhileStatement doWhileStatement, T data);
    R visitEmptyStatement(EmptyStatement emptyStatement, T data);
    R visitIfElseStatement(IfElseStatement ifElseStatement, T data);
    R visitLabel(LabelStatement labelStatement, T data);
    R visitReturnStatement(ReturnStatement returnStatement, T data);
    R visitSwitchStatement(SwitchStatement switchStatement, T data);
    R visitSwitchSection(SwitchSection switchSection, T data);
    R visitCaseLabel(CaseLabel caseLabel, T data);
    R visitThrowStatement(ThrowStatement throwStatement, T data);
    R visitCatchClause(CatchClause catchClause, T data);
    R visitAnnotationSection(AnnotationSection annotationSection, T data);
    R visitAnnotation(Annotation annotation, T data);
    R visitNewLine(NewLineNode newLineNode, T data);
    R visitVariableDeclaration(VariableDeclarationStatement variableDeclarationStatement, T data);
    R visitVariableInitializer(VariableInitializer variableInitializer, T data);
    R visitText(TextNode textNode, T data);
    R visitImportDeclaration(ImportDeclaration importDeclaration, T data);
    R visitSimpleType(SimpleType simpleType, T data);
    R visitMethodDeclaration(MethodDeclaration methodDeclaration, T data);
    R visitConstructorDeclaration(ConstructorDeclaration constructorDeclaration, T data);
    R visitTypeParameterDeclaration(TypeParameterDeclaration typeParameterDeclaration, T data);
    R visitParameterDeclaration(ParameterDeclaration parameterDeclaration, T data);
    R visitFieldDeclaration(FieldDeclaration fieldDeclaration, T data);
    R visitTypeDeclaration(TypeDeclaration typeDeclaration, T data);
    R visitCompilationUnit(CompilationUnit compilationUnit, T data);
    R visitPackageDeclaration(PackageDeclaration packageDeclaration, T data);
    R visitArraySpecifier(ArraySpecifier arraySpecifier, T data);
    R visitComposedType(ComposedType composedType, T data);
    R visitWhileStatement(WhileStatement whileStatement, T data);
    R visitPrimitiveExpression(PrimitiveExpression primitiveExpression, T data);
    R visitCastExpression(CastExpression castExpression, T data);
    R visitBinaryOperatorExpression(BinaryOperatorExpression binaryOperatorExpression, T data);
    R visitInstanceOfExpression(InstanceOfExpression instanceOfExpression, T data);
    R visitIndexerExpression(IndexerExpression indexerExpression, T data);
    R visitIdentifierExpression(IdentifierExpression identifierExpression, T data);
    R visitUnaryOperatorExpression(UnaryOperatorExpression unaryOperatorExpression, T data);
    R visitConditionalExpression(ConditionalExpression conditionalExpression, T data);
    R visitArrayInitializerExpression(ArrayInitializerExpression arrayInitializerExpression, T data);
    R visitObjectCreationExpression(ObjectCreationExpression objectCreationExpression, T data);
    R visitArrayCreationExpression(ArrayCreationExpression arrayCreationExpression, T data);
    R visitAssignmentExpression(AssignmentExpression assignmentExpression, T data);
    R visitForStatement(ForStatement forStatement, T data);
    R visitForEachStatement(ForEachStatement forEachStatement, T data);
}
