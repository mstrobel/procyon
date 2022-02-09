/*
 * ModuleDeclaration.java
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

public class ModuleDeclaration extends EntityDeclaration {
    public final JavaTokenNode getLeftBraceToken() {
        return getChildByRole(Roles.LEFT_BRACE);
    }

    public final AstNodeCollection<EntityDeclaration> getMembers() {
        return getChildrenByRole(Roles.TYPE_MEMBER);
    }

    public final JavaTokenNode getRightBraceToken() {
        return getChildByRole(Roles.RIGHT_BRACE);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.MODULE_DECLARATION;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MODULE_DEFINITION;
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitModuleDeclaration(this, data);
    }

    @Override
    public ModuleDeclaration clone() {
        return (ModuleDeclaration) super.clone();
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof ModuleDeclaration) {
            final ModuleDeclaration otherDeclaration = (ModuleDeclaration) other;

            return !otherDeclaration.isNull() &&
                   matchString(getName(), otherDeclaration.getName());
        }

        return false;
    }

    // <editor-fold defaultstate="collapsed" desc="Null ModuleDeclaration">

    public final static ModuleDeclaration NULL = new NullModuleDeclaration();

    private static final class NullModuleDeclaration extends ModuleDeclaration {
        @Override
        public final boolean isNull() {
            return true;
        }

        @Override
        public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
            return null;
        }

        @Override
        public boolean matches(final INode other, final Match match) {
            return other == null || other.isNull();
        }
    }

    // </editor-fold>
}
