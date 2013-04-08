/*
 * MethodDeclaration.java
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

package com.strobel.decompiler.languages.java.ast;

import com.strobel.decompiler.languages.EntityType;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;

public class MethodDeclaration extends EntityDeclaration {
    public final AstType getPrivateImplementationType() {
        return getChildByRole(PRIVATE_IMPLEMENTATION_TYPE_ROLE);
    }

    public final void setPrivateImplementationType(final AstType type) {
        setChildByRole(PRIVATE_IMPLEMENTATION_TYPE_ROLE, type);
    }

    public final AstNodeCollection<TypeDeclaration> getDeclaredTypes() {
        return getChildrenByRole(Roles.LOCAL_TYPE_DECLARATION);
    }

    public final AstNodeCollection<TypeParameterDeclaration> getTypeParameters() {
        return getChildrenByRole(Roles.TYPE_PARAMETER);
    }

    public final AstNodeCollection<ParameterDeclaration> getParameters() {
        return getChildrenByRole(Roles.PARAMETER);
    }

    public final BlockStatement getBody() {
        return getChildByRole(Roles.BODY);
    }

    public final void setBody(final BlockStatement value) {
        setChildByRole(Roles.BODY, value);
    }

    public final JavaTokenNode getLeftParenthesisToken() {
        return getChildByRole(Roles.LEFT_PARENTHESIS);
    }

    public final JavaTokenNode getRightParenthesisToken() {
        return getChildByRole(Roles.RIGHT_PARENTHESIS);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.METHOD;
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitMethodDeclaration(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof MethodDeclaration) {
            final MethodDeclaration otherDeclaration = (MethodDeclaration) other;

            return !otherDeclaration.isNull() &&
                   matchString(getName(), otherDeclaration.getName()) &&
                   matchAnnotationsAndModifiers(otherDeclaration, match) &&
                   getPrivateImplementationType().matches(otherDeclaration.getPrivateImplementationType(), match) &&
                   getTypeParameters().matches(otherDeclaration.getTypeParameters(), match) &&
                   getParameters().matches(otherDeclaration.getParameters(), match) &&
                   getBody().matches(otherDeclaration.getBody(), match);
        }

        return false;
    }
}
