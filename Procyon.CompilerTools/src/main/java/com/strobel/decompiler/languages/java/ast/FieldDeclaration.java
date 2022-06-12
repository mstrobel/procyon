/*
 * FieldDeclaration.java
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

public class FieldDeclaration extends EntityDeclaration {

    private int lineNumber;

    public final AstNodeCollection<VariableInitializer> getVariables() {
        return getChildrenByRole(Roles.VARIABLE);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.FIELD;
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitFieldDeclaration(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof FieldDeclaration) {
            final FieldDeclaration otherDeclaration = (FieldDeclaration) other;

            return !otherDeclaration.isNull() &&
                   matchString(getName(), otherDeclaration.getName()) &&
                   matchAnnotationsAndModifiers(otherDeclaration, match) &&
                   getReturnType().matches(otherDeclaration.getReturnType(), match);
        }

        return false;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public int getFirstKnownLineNumber() {
        return lineNumber;
    }
}
