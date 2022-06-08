/*
 * RewriteInitForLineStretchTransform.java
 *
 * Copyright (c) 2013-2022 Mike Strobel and other contributors
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

import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.LineNumberTableAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.LineNumberTableConverter;
import com.strobel.decompiler.languages.java.MinMaxLineNumberVisitor;
import com.strobel.decompiler.languages.java.ast.AssignmentExpression;
import com.strobel.decompiler.languages.java.ast.AssignmentOperatorType;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.BlockStatement;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.EntityDeclaration;
import com.strobel.decompiler.languages.java.ast.Expression;
import com.strobel.decompiler.languages.java.ast.ExpressionStatement;
import com.strobel.decompiler.languages.java.ast.FieldDeclaration;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MemberReferenceExpression;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.Roles;
import com.strobel.decompiler.languages.java.ast.VariableInitializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewriteInitForLineStretchTransform extends ContextTrackingVisitor<Void> {

    private Map<String, FieldInit> fieldDeclarations = new HashMap<>();
    private int constructorCount;

    public RewriteInitForLineStretchTransform(DecompilerContext context) {
        super(context);
    }

    @Override
    public Void visitBlockStatement(BlockStatement node, Void data) {
        if (node.getParent() instanceof MethodDeclaration) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) node.getParent();
            MethodDefinition methodDefinition = methodDeclaration.getUserData(Keys.METHOD_DEFINITION);
            if (methodDefinition != null && methodDefinition.isTypeInitializer()) {
                final LineNumberTableAttribute lineNumberTable = SourceAttribute.find(AttributeNames.LineNumberTable, methodDefinition.getSourceAttributes());
                if (lineNumberTable != null) {
                    LineNumberTableConverter lineNumberTableConverter = new LineNumberTableConverter(lineNumberTable);
                    int previousLineNumber = 0;
                    int pivotLineNumber = 0;
                    MethodDeclaration newMethodDeclaration = null;
                    for (AstNode child : node.getChildren()) {
                        MinMaxLineNumberVisitor minMaxLineNumberVisitor = new MinMaxLineNumberVisitor(lineNumberTableConverter);
                        child.acceptVisitor(minMaxLineNumberVisitor, null);
                        int currentLineNumber = minMaxLineNumberVisitor.getMinLineNumber();
                        if (!methodDeclaration.isFirstLineNumberKnown()) {
                            methodDeclaration.setFirstKnownLineNumber(currentLineNumber);
                        }
                        if (previousLineNumber > 0 && currentLineNumber > previousLineNumber + 3) {
                            newMethodDeclaration = (MethodDeclaration) methodDeclaration.clone();
                            newMethodDeclaration.setFirstKnownLineNumber(currentLineNumber);
                            methodDeclaration.getParent().insertChildAfter(methodDeclaration, newMethodDeclaration, Roles.TYPE_MEMBER);
                            pivotLineNumber = currentLineNumber;
                            break;
                        }
                        previousLineNumber = minMaxLineNumberVisitor.getMaxLineNumber();
                    }
                    if (pivotLineNumber > 0) {
                        for (AstNode child : node.getChildren()) {
                            MinMaxLineNumberVisitor minMaxLineNumberVisitor = new MinMaxLineNumberVisitor(lineNumberTableConverter);
                            child.acceptVisitor(minMaxLineNumberVisitor, null);
                            int currentLineNumber = minMaxLineNumberVisitor.getMinLineNumber();
                            if (currentLineNumber >= pivotLineNumber) {
                                child.remove();
                            }
                        }
                    }
                    if (newMethodDeclaration != null) {
                        BlockStatement body = newMethodDeclaration.getBody();
                        if (body != null) {
                            for (AstNode child : body.getChildren()) {
                                MinMaxLineNumberVisitor minMaxLineNumberVisitor = new MinMaxLineNumberVisitor(lineNumberTableConverter);
                                child.acceptVisitor(minMaxLineNumberVisitor, null);
                                int currentLineNumber = minMaxLineNumberVisitor.getMinLineNumber();
                                if (currentLineNumber < pivotLineNumber) {
                                    child.remove();
                                }
                            }
                            visitBlockStatement(body, data);
                        }
                    }
                }
            }
        }
        return super.visitBlockStatement(node, data);
    }

    @Override
    public Void visitAssignmentExpression(AssignmentExpression node, Void data) {
        AstNode parent = node.getParent();
        if (parent instanceof ExpressionStatement 
                && parent.getParent() instanceof BlockStatement 
                && parent.getParent().getParent() instanceof EntityDeclaration
                && node.getLeft() instanceof MemberReferenceExpression 
                && node.getOperator() == AssignmentOperatorType.ASSIGN) {
            EntityDeclaration entityDeclaration = (EntityDeclaration) parent.getParent().getParent();
            MethodDefinition methodDefinition = entityDeclaration.getUserData(Keys.METHOD_DEFINITION);
            if (methodDefinition != null && (methodDefinition.isConstructor() || methodDefinition.isTypeInitializer())) {
                LineNumberTableAttribute lineNumberTable = SourceAttribute.find(AttributeNames.LineNumberTable, methodDefinition.getSourceAttributes());
                LineNumberTableConverter lineNumberTableConverter = new LineNumberTableConverter(lineNumberTable);
                MemberReferenceExpression memberReferenceExpression = (MemberReferenceExpression) node.getFirstChild();
                MemberReference memberReference = memberReferenceExpression.getUserData(Keys.MEMBER_REFERENCE);
                FieldInit fieldInit = fieldDeclarations.get(memberReference.getFullName());
                if (fieldInit != null && !methodDefinition.hasParameter(memberReference.getName())) {
                    Expression initializer = node.getRight();
                    int fieldInitLineNo = lineNumberTableConverter.getLineForOffset(initializer.getOffset());
                    if (fieldInitLineNo > 0) {
                        FieldDeclaration fieldDeclaration = fieldInit.declaration;
                        int fieldDeclLineNo = fieldDeclaration.getLineNumber();
                        if (fieldInitLineNo == fieldDeclLineNo || (fieldInit.initializers.isEmpty() && fieldDeclLineNo == 0)) {
                            fieldDeclaration.setLineNumber(fieldInitLineNo);
                            fieldInit.initializers.add(initializer);
                            fieldInit.statements.add((ExpressionStatement) node.getParent());
                            fieldInit.initMethod = methodDefinition;
                        }
                    }
                }
            }
        }
        return super.visitAssignmentExpression(node, data);
    }

    @Override
    public Void visitFieldDeclaration(FieldDeclaration node, Void data) {
        FieldDefinition fieldDefinition = node.getUserData(Keys.FIELD_DEFINITION);
        if (fieldDefinition != null) {
            fieldDeclarations.put(fieldDefinition.getFullName(), new FieldInit(node));
        }
        return super.visitFieldDeclaration(node, data);
    }

    @Override
    public Void visitConstructorDeclaration(ConstructorDeclaration node, Void p) {
        constructorCount++;
        return super.visitConstructorDeclaration(node, p);
    }

    @Override
    public void run(AstNode compilationUnit) {
        super.run(compilationUnit);
        for (FieldInit fieldInit : fieldDeclarations.values()) {
            if (fieldInit.isInAllConstructors() || fieldInit.isInStaticBlock()) {
                fieldInit.removeInitializers();
                fieldInit.removeStatements();
                fieldInit.declaration.getVariables().clear();
                fieldInit.declaration.getVariables().add(new VariableInitializer(fieldInit.declaration.getName(), fieldInit.initializers.get(0)));
            }
        }
    }

    private class FieldInit {
        private final FieldDeclaration declaration;
        private final List<Expression> initializers = new ArrayList<>();
        private final List<ExpressionStatement> statements = new ArrayList<>();
        private MethodDefinition initMethod;

        public FieldInit(FieldDeclaration declaration) {
            this.declaration = declaration;
        }

        public void removeStatements() {
            AstNode parent = null;
            for (ExpressionStatement expressionStatement : statements) {
                parent = expressionStatement.getParent();
                expressionStatement.remove();
            }
            if (isInStaticBlock() && parent != null && !parent.hasChildren() && parent.getParent() instanceof MethodDeclaration) {
                parent.getParent().remove();
            }
        }

        public void removeInitializers() {
            for (Expression initializer : initializers) {
                initializer.remove();
            }
        }
        
        public boolean isInAllConstructors() {
            return initMethod != null && initMethod.isConstructor() && constructorCount > 0 && initializers.size() == constructorCount;
        }

        public boolean isInStaticBlock() {
            return initMethod != null && initMethod.isTypeInitializer();
        }
    }
}
