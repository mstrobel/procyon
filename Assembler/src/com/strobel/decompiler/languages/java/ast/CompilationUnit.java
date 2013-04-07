/*
 * CompilationUnit.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast;

import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.Role;
import com.strobel.util.ContractUtils;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

public class CompilationUnit extends AstNode {
    public final static Role<AstNode> MEMBER_ROLE = new Role<>("Member", AstNode.class, AstNode.NULL);

    private AstNode _topExpression;
    private String _fileName;

    public final String getFileName() {
        return _fileName;
    }

    public final void setFileName(final String fileName) {
        verifyNotFrozen();
        _fileName = fileName;
    }

    public final AstNode getTopExpression() {
        return _topExpression;
    }

    final void setTopExpression(final AstNode topExpression) {
        _topExpression = topExpression;
    }

    public final AstNodeCollection<AstNode> getMembers() {
        return getChildrenByRole(MEMBER_ROLE);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.UNKNOWN;
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitCompilationUnit(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        return other instanceof CompilationUnit &&
               !other.isNull() &&
               getMembers().matches(((CompilationUnit) other).getMembers(), match);
    }

    public Iterable<TypeDeclaration> getTypes() {
        return getTypes(false);
    }

    public Iterable<TypeDeclaration> getTypes(final boolean includeInnerTypes) {
        return new Iterable<TypeDeclaration>() {
            @Override
            public final Iterator<TypeDeclaration> iterator() {
                return new Iterator<TypeDeclaration>() {
                    final Stack<AstNode> nodeStack = new Stack<>();

                    TypeDeclaration next = null;

                    {
                        nodeStack.push(CompilationUnit.this);
                    }

                    private TypeDeclaration selectNext() {
                        if (next != null) {
                            return next;
                        }

                        while (!nodeStack.isEmpty()) {
                            final AstNode current = nodeStack.pop();

                            if (current instanceof TypeDeclaration) {
                                next = (TypeDeclaration) current;
                                break;
                            }

                            for (final AstNode child : current.getChildren()) {
                                if (!(child instanceof Statement || child instanceof Expression) &&
                                    (child.getRole() != Roles.TYPE_MEMBER || (child instanceof TypeDeclaration && includeInnerTypes))) {

                                    nodeStack.push(child);
                                }
                            }
                        }

                        return null;
                    }

                    @Override
                    public final boolean hasNext() {
                        return selectNext() != null;
                    }

                    @Override
                    public final TypeDeclaration next() {
                        final TypeDeclaration next = selectNext();

                        if (next == null) {
                            throw new NoSuchElementException();
                        }

                        this.next = null;
                        return next;
                    }

                    @Override
                    public final void remove() {
                        throw ContractUtils.unsupported();
                    }
                };
            }
        };
    }
}
