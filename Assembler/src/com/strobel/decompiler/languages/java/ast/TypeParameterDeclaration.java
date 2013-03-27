/*
 * TypeParameterDeclaration.java
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

import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.Role;

public class TypeParameterDeclaration extends AstNode {
    public final static Role<AnnotationSection> ANNOTATION_ROLE = EntityDeclaration.ANNOTATION_ROLE;

    public TypeParameterDeclaration() {
    }

    public TypeParameterDeclaration(final String name) {
        setName(name);
    }

    public final AstNodeCollection<AnnotationSection> getAnnotations() {
        return getChildrenByRole(ANNOTATION_ROLE);
    }

    public final String getName() {
        return getChildByRole(Roles.IDENTIFIER).getName();
    }

    public final void setName(final String value) {
        setChildByRole(Roles.IDENTIFIER, Identifier.create(value));
    }

    public final Identifier getNameToken() {
        return getChildByRole(Roles.IDENTIFIER);
    }

    public final void setNameToken(final Identifier value) {
        setChildByRole(Roles.IDENTIFIER, value);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.UNKNOWN;
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitTypeParameterDeclaration(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof TypeParameterDeclaration) {
            final TypeParameterDeclaration otherDeclaration = (TypeParameterDeclaration) other;

            return !otherDeclaration.isNull() &&
                   matchString(getName(), otherDeclaration.getName()) &&
                   getAnnotations().matches(otherDeclaration.getAnnotations(), match);
        }

        return false;
    }
}
