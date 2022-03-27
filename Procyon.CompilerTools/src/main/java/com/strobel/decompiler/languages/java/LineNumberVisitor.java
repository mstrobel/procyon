package com.strobel.decompiler.languages.java;

import com.strobel.decompiler.languages.java.ast.Annotation;
import com.strobel.decompiler.languages.java.ast.AnonymousObjectCreationExpression;
import com.strobel.decompiler.languages.java.ast.ArrayCreationExpression;
import com.strobel.decompiler.languages.java.ast.ArrayInitializerExpression;
import com.strobel.decompiler.languages.java.ast.ArraySpecifier;
import com.strobel.decompiler.languages.java.ast.AssertStatement;
import com.strobel.decompiler.languages.java.ast.AssignmentExpression;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.BinaryOperatorExpression;
import com.strobel.decompiler.languages.java.ast.BlockStatement;
import com.strobel.decompiler.languages.java.ast.BreakStatement;
import com.strobel.decompiler.languages.java.ast.BytecodeConstant;
import com.strobel.decompiler.languages.java.ast.CaseLabel;
import com.strobel.decompiler.languages.java.ast.CastExpression;
import com.strobel.decompiler.languages.java.ast.CatchClause;
import com.strobel.decompiler.languages.java.ast.ClassOfExpression;
import com.strobel.decompiler.languages.java.ast.Comment;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.ComposedType;
import com.strobel.decompiler.languages.java.ast.ConditionalExpression;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.ContinueStatement;
import com.strobel.decompiler.languages.java.ast.DoWhileStatement;
import com.strobel.decompiler.languages.java.ast.EmptyStatement;
import com.strobel.decompiler.languages.java.ast.EnumValueDeclaration;
import com.strobel.decompiler.languages.java.ast.Expression;
import com.strobel.decompiler.languages.java.ast.ExpressionStatement;
import com.strobel.decompiler.languages.java.ast.FieldDeclaration;
import com.strobel.decompiler.languages.java.ast.ForEachStatement;
import com.strobel.decompiler.languages.java.ast.ForStatement;
import com.strobel.decompiler.languages.java.ast.GotoStatement;
import com.strobel.decompiler.languages.java.ast.IAstVisitor;
import com.strobel.decompiler.languages.java.ast.Identifier;
import com.strobel.decompiler.languages.java.ast.IdentifierExpression;
import com.strobel.decompiler.languages.java.ast.IfElseStatement;
import com.strobel.decompiler.languages.java.ast.ImportDeclaration;
import com.strobel.decompiler.languages.java.ast.IndexerExpression;
import com.strobel.decompiler.languages.java.ast.InlinedBytecodeExpression;
import com.strobel.decompiler.languages.java.ast.InstanceInitializer;
import com.strobel.decompiler.languages.java.ast.InstanceOfExpression;
import com.strobel.decompiler.languages.java.ast.IntersectionType;
import com.strobel.decompiler.languages.java.ast.InvocationExpression;
import com.strobel.decompiler.languages.java.ast.JavaTokenNode;
import com.strobel.decompiler.languages.java.ast.LabelStatement;
import com.strobel.decompiler.languages.java.ast.LabeledStatement;
import com.strobel.decompiler.languages.java.ast.LambdaExpression;
import com.strobel.decompiler.languages.java.ast.LocalTypeDeclarationStatement;
import com.strobel.decompiler.languages.java.ast.MemberReferenceExpression;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.MethodGroupExpression;
import com.strobel.decompiler.languages.java.ast.ModuleDeclaration;
import com.strobel.decompiler.languages.java.ast.NewLineNode;
import com.strobel.decompiler.languages.java.ast.NullReferenceExpression;
import com.strobel.decompiler.languages.java.ast.ObjectCreationExpression;
import com.strobel.decompiler.languages.java.ast.PackageDeclaration;
import com.strobel.decompiler.languages.java.ast.ParameterDeclaration;
import com.strobel.decompiler.languages.java.ast.ParenthesizedExpression;
import com.strobel.decompiler.languages.java.ast.PrimitiveExpression;
import com.strobel.decompiler.languages.java.ast.ReturnStatement;
import com.strobel.decompiler.languages.java.ast.SimpleType;
import com.strobel.decompiler.languages.java.ast.SuperReferenceExpression;
import com.strobel.decompiler.languages.java.ast.SwitchExpression;
import com.strobel.decompiler.languages.java.ast.SwitchExpressionArm;
import com.strobel.decompiler.languages.java.ast.SwitchSection;
import com.strobel.decompiler.languages.java.ast.SwitchStatement;
import com.strobel.decompiler.languages.java.ast.SynchronizedStatement;
import com.strobel.decompiler.languages.java.ast.TextNode;
import com.strobel.decompiler.languages.java.ast.ThisReferenceExpression;
import com.strobel.decompiler.languages.java.ast.ThrowStatement;
import com.strobel.decompiler.languages.java.ast.TryCatchStatement;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import com.strobel.decompiler.languages.java.ast.TypeParameterDeclaration;
import com.strobel.decompiler.languages.java.ast.TypeReferenceExpression;
import com.strobel.decompiler.languages.java.ast.UnaryOperatorExpression;
import com.strobel.decompiler.languages.java.ast.UnionType;
import com.strobel.decompiler.languages.java.ast.VariableDeclarationStatement;
import com.strobel.decompiler.languages.java.ast.VariableInitializer;
import com.strobel.decompiler.languages.java.ast.WhileStatement;
import com.strobel.decompiler.languages.java.ast.WildcardType;
import com.strobel.decompiler.patterns.Pattern;

public class LineNumberVisitor implements IAstVisitor<Void, Void> {

    private final LineNumberTableConverter lineNumberTableConverter;
    private int minLineNumber = Integer.MAX_VALUE;
    private int maxLineNumber = Integer.MIN_VALUE;

    public LineNumberVisitor(final LineNumberTableConverter lineNumberTableConverter) {
        this.lineNumberTableConverter = lineNumberTableConverter;
    }

    private Void updateMinMaxLineNumbers(int offset, AstNode node) {
        if (offset != Expression.MYSTERY_OFFSET) {
            int lineNumber = lineNumberTableConverter.getLineForOffset(offset);
            minLineNumber = Math.min(minLineNumber, lineNumber);
            maxLineNumber = Math.max(maxLineNumber, lineNumber);
        }
        visitChildren(node);
        return null;
    }

    private void visitChildren(AstNode node) {
        if (node.hasChildren()) {
            for (AstNode child : node.getChildren()) {
                child.acceptVisitor(this, null);
            }
        }
    }
    
    @Override
    public Void visitComment(Comment node, Void data) {
        return null;
    }

    @Override
    public Void visitPatternPlaceholder(AstNode node, Pattern pattern, Void data) {
        return null;
    }

    @Override
    public Void visitInvocationExpression(InvocationExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitTypeReference(TypeReferenceExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitJavaTokenNode(JavaTokenNode node, Void data) {
        return null;
    }

    @Override
    public Void visitMemberReferenceExpression(MemberReferenceExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitIdentifier(Identifier node, Void data) {
        return null;
    }

    @Override
    public Void visitNullReferenceExpression(NullReferenceExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitThisReferenceExpression(ThisReferenceExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitSuperReferenceExpression(SuperReferenceExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitClassOfExpression(ClassOfExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitBlockStatement(BlockStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitBreakStatement(BreakStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitContinueStatement(ContinueStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitDoWhileStatement(DoWhileStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitEmptyStatement(EmptyStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitIfElseStatement(IfElseStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitLabelStatement(LabelStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitLabeledStatement(LabeledStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitReturnStatement(ReturnStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitSwitchStatement(SwitchStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitSwitchSection(SwitchSection node, Void data) {
        return null;
    }

    @Override
    public Void visitSwitchExpression(SwitchExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitSwitchExpressionArm(SwitchExpressionArm node, Void data) {
        return null;
    }

    @Override
    public Void visitCaseLabel(CaseLabel node, Void data) {
        return null;
    }

    @Override
    public Void visitThrowStatement(ThrowStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitCatchClause(CatchClause node, Void data) {
        return null;
    }

    @Override
    public Void visitAnnotation(Annotation node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitNewLine(NewLineNode node, Void data) {
        return null;
    }

    @Override
    public Void visitVariableDeclaration(VariableDeclarationStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitVariableInitializer(VariableInitializer node, Void data) {
        visitChildren(node);
        return null;
    }

    @Override
    public Void visitText(TextNode node, Void data) {
        return null;
    }

    @Override
    public Void visitImportDeclaration(ImportDeclaration node, Void data) {
        return null;
    }

    @Override
    public Void visitSimpleType(SimpleType node, Void data) {
        return null;
    }

    @Override
    public Void visitMethodDeclaration(MethodDeclaration node, Void data) {
        return null;
    }

    @Override
    public Void visitInitializerBlock(InstanceInitializer node, Void data) {
        return null;
    }

    @Override
    public Void visitConstructorDeclaration(ConstructorDeclaration node, Void data) {
        return null;
    }

    @Override
    public Void visitTypeParameterDeclaration(TypeParameterDeclaration node, Void data) {
        return null;
    }

    @Override
    public Void visitParameterDeclaration(ParameterDeclaration node, Void data) {
        return null;
    }

    @Override
    public Void visitFieldDeclaration(FieldDeclaration node, Void data) {
        return null;
    }

    @Override
    public Void visitTypeDeclaration(TypeDeclaration node, Void data) {
        return null;
    }

    @Override
    public Void visitModuleDeclaration(ModuleDeclaration node, Void data) {
        return null;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnit node, Void data) {
        return null;
    }

    @Override
    public Void visitPackageDeclaration(PackageDeclaration node, Void data) {
        return null;
    }

    @Override
    public Void visitArraySpecifier(ArraySpecifier node, Void data) {
        return null;
    }

    @Override
    public Void visitComposedType(ComposedType node, Void data) {
        return null;
    }

    @Override
    public Void visitIntersectionType(IntersectionType node, Void data) {
        return null;
    }

    @Override
    public Void visitUnionType(UnionType node, Void data) {
        return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitPrimitiveExpression(PrimitiveExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitCastExpression(CastExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitBinaryOperatorExpression(BinaryOperatorExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitInstanceOfExpression(InstanceOfExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitIndexerExpression(IndexerExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitIdentifierExpression(IdentifierExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitUnaryOperatorExpression(UnaryOperatorExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitArrayInitializerExpression(ArrayInitializerExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitObjectCreationExpression(ObjectCreationExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitArrayCreationExpression(ArrayCreationExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitAssignmentExpression(AssignmentExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitForStatement(ForStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitForEachStatement(ForEachStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitTryCatchStatement(TryCatchStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitGotoStatement(GotoStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitParenthesizedExpression(ParenthesizedExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitSynchronizedStatement(SynchronizedStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitWildcardType(WildcardType node, Void data) {
        return null;
    }

    @Override
    public Void visitMethodGroupExpression(MethodGroupExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitEnumValueDeclaration(EnumValueDeclaration node, Void data) {
        return null;
    }

    @Override
    public Void visitAssertStatement(AssertStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitLambdaExpression(LambdaExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitLocalTypeDeclarationStatement(LocalTypeDeclarationStatement node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitInlinedBytecode(InlinedBytecodeExpression node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    @Override
    public Void visitBytecodeConstant(BytecodeConstant node, Void data) {
        return updateMinMaxLineNumbers(node.getOffset(), node);
    }

    public int getMinLineNumber() {
        return minLineNumber;
    }

    public int getMaxLineNumber() {
        return maxLineNumber;
    }
}
