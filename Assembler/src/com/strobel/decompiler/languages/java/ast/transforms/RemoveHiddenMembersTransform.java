/*
 * RemoveHiddenMembersTransform.java
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

package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.FieldDeclaration;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;

public class RemoveHiddenMembersTransform extends ContextTrackingVisitor<Void> {
    public RemoveHiddenMembersTransform(final DecompilerContext context) {
        super(context);
    }

    @Override
    public Void visitTypeDeclaration(final TypeDeclaration node, final Void _) {
        final TypeDefinition type = node.getUserData(Keys.TYPE_DEFINITION);

        if (type != null && AstBuilder.isMemberHidden(type, context.getSettings())) {
            node.remove();
            return null;
        }

        return super.visitTypeDeclaration(node, _);
    }

    @Override
    public Void visitFieldDeclaration(final FieldDeclaration node, final Void data) {
        final FieldDefinition field = node.getUserData(Keys.FIELD_DEFINITION);

        if (field != null && AstBuilder.isMemberHidden(field, context.getSettings())) {
            node.remove();
            return null;
        }

        return super.visitFieldDeclaration(node, data);
    }

    @Override
    public Void visitMethodDeclaration(final MethodDeclaration node, final Void _) {
        final MethodDefinition method = node.getUserData(Keys.METHOD_DEFINITION);

        if (method != null) {
            if (AstBuilder.isMemberHidden(method, context.getSettings())) {
                node.remove();
                return null;
            }

            if (method.isTypeInitializer()) {
                if (node.getBody().getStatements().isEmpty()) {
                    node.remove();
                    return null;
                }
            }
        }

        return super.visitMethodDeclaration(node, _);
    }

    @Override
    public Void visitConstructorDeclaration(final ConstructorDeclaration node, final Void _) {
        final MethodDefinition method = node.getUserData(Keys.METHOD_DEFINITION);

        if (method != null && AstBuilder.isMemberHidden(method, context.getSettings())) {
            node.remove();
            return null;
        }

        return super.visitConstructorDeclaration(node, _);
    }
}
