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
import com.strobel.decompiler.languages.java.ast.InvocationExpression;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MemberReferenceExpression;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.Roles;
import com.strobel.decompiler.languages.java.ast.ThisReferenceExpression;
import com.strobel.decompiler.languages.java.ast.VariableInitializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RewriteInitForLineStretchTransform extends ContextTrackingVisitor<Void> {

    private ConcurrentHashMap<String, FieldDeclaration> fieldDeclarations = new ConcurrentHashMap<>();
    private ConcurrentHashMap<FieldLocation, FieldInit> fieldInitLocations = new ConcurrentHashMap<>();
    private ConcurrentHashMap<MethodDefinition, ConstructorDeclaration> constructionDefinitionToDeclaration = new ConcurrentHashMap<>();
    
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
                FieldDeclaration fieldDeclaration = fieldDeclarations.get(memberReference.getFullName());
                Expression initializer = node.getRight();
                int offset = initializer.getOffset();
                int lineNumber = lineNumberTableConverter.getLineForOffset(offset);
                if (lineNumber > 0 && fieldDeclaration != null && !methodDefinition.hasParameter(memberReference.getName())) {
                    fieldDeclaration.setLineNumber(lineNumber);
                    FieldLocation fieldLocation = new FieldLocation(memberReference.getFullName(), offset);
                    fieldInitLocations.putIfAbsent(fieldLocation, new FieldInit(fieldDeclaration));
                    fieldInitLocations.get(fieldLocation).init(initializer, (ExpressionStatement) node.getParent(), methodDefinition);
                }
            }
        }
        return super.visitAssignmentExpression(node, data);
    }

    @Override
    public Void visitFieldDeclaration(FieldDeclaration node, Void data) {
        FieldDefinition fieldDefinition = node.getUserData(Keys.FIELD_DEFINITION);
        if (fieldDefinition != null) {
            fieldDeclarations.put(fieldDefinition.getFullName(), node);
        }
        return super.visitFieldDeclaration(node, data);
    }

    @Override
    public Void visitThisReferenceExpression(ThisReferenceExpression node, Void data) {
        if (node.getParent() instanceof InvocationExpression) {
            MemberReference memberReference = node.getParent().getUserData(Keys.MEMBER_REFERENCE);
            if (memberReference instanceof MethodDefinition) {
                MethodDefinition methodDefinition = (MethodDefinition) memberReference;
                ConstructorDeclaration constructorDeclaration = constructionDefinitionToDeclaration.get(methodDefinition);
                if (constructorDeclaration != null) {
                    MethodDefinition currentMethodDefinition = context.getCurrentMethod();
                    try {
                        context.setCurrentMethod(null);
                        visitConstructorDeclaration(constructorDeclaration, data);
                    }
                    finally {
                        context.setCurrentMethod(currentMethodDefinition);
                    }
                }
            }
        }
        return super.visitThisReferenceExpression(node, data);
    }
    
    @Override
    public void run(AstNode compilationUnit) {
        new ConstructorGatherer(context).run(compilationUnit);
        super.run(compilationUnit);
        for (FieldInit fieldInit : fieldInitLocations.values()) {
            if (fieldInit.isInAllConstructors() || fieldInit.isInTypeInitializer()) {
                fieldInit.removeFieldInitStatements();
                fieldInit.createVariableInitializer();
            }
        }
    }

    private class FieldInit {
        private final FieldDeclaration declaration;
        private final List<FieldInitStatement> fieldInitStatements = new ArrayList<>();
        private boolean inConstructor = true;
        private boolean inTypeInitializer = true;

        public FieldInit(FieldDeclaration declaration) {
            this.declaration = declaration;
        }

        public void init(Expression initializer, ExpressionStatement expressionStatement, MethodDefinition initMethod) {
            inConstructor &= initMethod != null && initMethod.isConstructor();
            inTypeInitializer &= initMethod != null && initMethod.isTypeInitializer();
            fieldInitStatements.add(new FieldInitStatement(initializer, expressionStatement));
        }

        public void removeFieldInitStatements() {
            AstNode parent = null;
            for (FieldInitStatement fieldInitStatement : fieldInitStatements) {
                parent = fieldInitStatement.statement.getParent();
                fieldInitStatement.remove();
            }
            if (isInTypeInitializer() && parent != null && !parent.hasChildren() && parent.getParent() instanceof MethodDeclaration) {
                parent.getParent().remove();
            }
        }

        public boolean isInAllConstructors() {
            return inConstructor && !constructionDefinitionToDeclaration.isEmpty() && fieldInitStatements.size() == constructionDefinitionToDeclaration.size();
        }

        public boolean isInTypeInitializer() {
            return inTypeInitializer;
        }

        public void createVariableInitializer() {
            declaration.getVariables().clear();
            declaration.getVariables().add(new VariableInitializer(declaration.getName(), fieldInitStatements.get(0).initializer));
        }
    }

    private static class FieldInitStatement {
        private final Expression initializer;
        private final ExpressionStatement statement;

        public FieldInitStatement(Expression initializer, ExpressionStatement statement) {
            this.initializer = initializer;
            this.statement = statement;
        }

        public void remove() {
            initializer.remove();
            statement.remove();
        }
    }

    private static class FieldLocation {
        private final String fieldName;
        private final int fieldOffset;

        public FieldLocation(String fieldName, int fieldOffset) {
            this.fieldName = fieldName;
            this.fieldOffset = fieldOffset;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fieldName, fieldOffset);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            FieldLocation other = (FieldLocation) obj;
            return Objects.equals(fieldName, other.fieldName) && fieldOffset == other.fieldOffset;
        }
    }

    private class ConstructorGatherer extends ContextTrackingVisitor<Void> {

        protected ConstructorGatherer(DecompilerContext context) {
            super(context);
        }
        
        @Override
        public Void visitConstructorDeclaration(ConstructorDeclaration node, Void p) {
            MethodDefinition methodDefinition = node.getUserData(Keys.METHOD_DEFINITION);
            if (methodDefinition != null) {
                constructionDefinitionToDeclaration.put(methodDefinition, node);
            }
            return super.visitConstructorDeclaration(node, p);
        }
    }
}
