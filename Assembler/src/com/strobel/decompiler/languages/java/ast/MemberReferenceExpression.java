/*
 * MemberReferenceExpression.java
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

public class MemberReferenceExpression extends Expression {
    public MemberReferenceExpression() {
    }

    public MemberReferenceExpression(final Expression target, final String memberName, final Iterable<AstType> typeArguments) {
        addChild(target, Roles.TargetExpression);

        setMemberName(memberName);

        if (typeArguments != null) {
            for (final AstType argument : typeArguments) {
                addChild(argument, Roles.TypeArgument);
            }
        }
    }

    public MemberReferenceExpression(final Expression target, final String memberName, final AstType... typeArguments) {
        addChild(target, Roles.TargetExpression);

        setMemberName(memberName);

        if (typeArguments != null) {
            for (final AstType argument : typeArguments) {
                addChild(argument, Roles.TypeArgument);
            }
        }
    }

    public final String getMemberName() {
        return getChildByRole(Roles.Identifier).getName();
    }

    public final void setMemberName(final String name) {
        setChildByRole(Roles.Identifier, Identifier.create(name));
    }

    public final Identifier getMemberNameToken() {
        return getChildByRole(Roles.Identifier);
    }

    public final void setMemberNameToken(final Identifier token) {
        setChildByRole(Roles.Identifier, token);
    }

    public final Expression getTarget() {
        return getChildByRole(Roles.TargetExpression);
    }

    public final void setTarget(final Expression value) {
        setChildByRole(Roles.TargetExpression, value);
    }

    public final AstNodeCollection<AstType> getTypeArguments() {
        return getChildrenByRole(Roles.TypeArgument);
    }

    public final JavaTokenNode getDotToken() {
        return getChildByRole(Roles.Dot);
    }
    
    public final JavaTokenNode getLeftChevronToken() {
        return getChildByRole(Roles.LeftChevron);
    }

    public final JavaTokenNode getRightChevronToken() {
        return getChildByRole(Roles.RightChevron);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitMemberReferenceExpression(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof MemberReferenceExpression) {
            final MemberReferenceExpression otherExpression = (MemberReferenceExpression) other;

            return getTarget().matches(otherExpression.getTarget(), match) &&
                   matchString(getMemberName(), otherExpression.getMemberName()) &&
                   getTypeArguments().matches(otherExpression.getTypeArguments(), match);
        }

        return false;
    }
}
