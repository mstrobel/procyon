/*
 * IdentifierExpressionBackReference.java
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

package com.strobel.decompiler.patterns;

import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.Identifier;
import com.strobel.decompiler.languages.java.ast.Roles;

import static com.strobel.core.CollectionUtilities.*;

public final class IdentifierBackReference extends Pattern {
    private final String _referencedGroupName;

    public IdentifierBackReference(final String referencedGroupName) {
        _referencedGroupName = VerifyArgument.notNull(referencedGroupName, "referencedGroupName");
    }

    public final String getReferencedGroupName() {
        return _referencedGroupName;
    }

    @Override
    public final boolean matches(final INode other, final Match match) {
        final Identifier identifier;

        if (other instanceof AstNode) {
            if ((identifier = ((AstNode) other).getChildByRole(Roles.IDENTIFIER)) != null &&
                !identifier.isNull() &&
                !any(((AstNode) other).getChildrenByRole(Roles.TYPE_ARGUMENT))) {

                final INode referenced = lastOrDefault(match.get(_referencedGroupName));

                return referenced instanceof AstNode &&
                       (Pattern.matchString(identifier.getName(), ((AstNode) referenced).getChildByRole(Roles.IDENTIFIER).getName()) ||
                        Pattern.matchString(((AstNode) referenced).getChildByRole(Roles.IDENTIFIER).getName(), identifier.getName()));
            }
        }

        return false;
    }
}
