/*
 * ReOrderMembersForLineStretchTransform.java
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

import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.AstNodeCollection;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.EntityDeclaration;
import com.strobel.decompiler.languages.java.ast.Roles;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReOrderMembersForLineStretchTransform extends ContextTrackingVisitor<Void> {

    public ReOrderMembersForLineStretchTransform(DecompilerContext context) {
        super(context);
    }

    @Override
    protected Void visitTypeDeclarationOverride(TypeDeclaration typeDeclaration, Void p) {
        AstNodeCollection<EntityDeclaration> members = typeDeclaration.getChildrenByRole(Roles.TYPE_MEMBER);
        List<EntityDeclaration> sortedMembers = new ArrayList<>(members);
        Collections.sort(sortedMembers, new MemberComparator());
        for (EntityDeclaration member : members) {
            member.remove();
        }
        for (EntityDeclaration member : sortedMembers) {
            typeDeclaration.addChild(member, Roles.TYPE_MEMBER);
        }
        return p;
    }

    private static class MemberComparator implements Comparator<EntityDeclaration>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public int compare(EntityDeclaration e1, EntityDeclaration e2) {
            return Integer.compare(e1.getFirstKnownLineNumber(), e2.getFirstKnownLineNumber());
        }
    }
}
