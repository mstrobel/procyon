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
    R acceptNullReferenceExpression(NullReferenceExpression nullReferenceExpression, T data);
    R acceptThisReferenceExpression(ThisReferenceExpression thisReferenceExpression, T data);
    R acceptSuperReferenceExpression(SuperReferenceExpression superReferenceExpression, T data);
    R acceptClassOfExpression(ClassOfExpression classOfExpression, T data);
}
