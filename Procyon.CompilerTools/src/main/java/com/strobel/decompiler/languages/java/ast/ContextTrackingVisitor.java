/*
 * ContextTrackingVisitor.java
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

import com.strobel.assembler.metadata.IMetadataResolver;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;

/// <summary>
/// Base class for AST visitors that need the current type/method context info.
/// </summary>
public abstract class ContextTrackingVisitor<TResult> extends DepthFirstAstVisitor<Void, TResult> implements IAstTransform {
    @SuppressWarnings("ProtectedField")
    protected final DecompilerContext context;

    protected ContextTrackingVisitor(final DecompilerContext context) {
        this.context = VerifyArgument.notNull(context, "context");
    }

    protected final boolean inConstructor() {
        final MethodDefinition currentMethod = context.getCurrentMethod();
        return currentMethod != null && currentMethod.isConstructor();
    }

    protected final boolean inStaticInitializer() {
        final MethodDefinition currentMethod = context.getCurrentMethod();
        return currentMethod != null && currentMethod.isTypeInitializer();
    }

    protected final boolean inMethod() {
        return context.getCurrentMethod() != null;
    }

    public final TResult visitTypeDeclaration(final TypeDeclaration typeDeclaration, final Void p) {
        final TypeDefinition oldType = context.getCurrentType();
        final MethodDefinition oldMethod = context.getCurrentMethod();

        try {
            context.setCurrentType(typeDeclaration.getUserData(Keys.TYPE_DEFINITION));
            context.setCurrentMethod(null);
            return visitTypeDeclarationOverride(typeDeclaration, p);
        }
        finally {
            context.setCurrentType(oldType);
            context.setCurrentMethod(oldMethod);
        }
    }

    protected TResult visitTypeDeclarationOverride(final TypeDeclaration typeDeclaration, final Void p) {
        return super.visitTypeDeclaration(typeDeclaration, p);
    }

    public TResult visitMethodDeclaration(final MethodDeclaration node, final Void p) {
        assert context.getCurrentMethod() == null;
        try {
            context.setCurrentMethod(node.getUserData(Keys.METHOD_DEFINITION));
            return visitMethodDeclarationOverride(node, p);
        }
        finally {
            context.setCurrentMethod(null);
        }
    }

    protected TResult visitMethodDeclarationOverride(final MethodDeclaration node, final Void p) {
        return super.visitMethodDeclaration(node, p);
    }

    public TResult visitConstructorDeclaration(final ConstructorDeclaration node, final Void p) {
        assert (context.getCurrentMethod() == null);
        try {
            context.setCurrentMethod(node.getUserData(Keys.METHOD_DEFINITION));
            return super.visitConstructorDeclaration(node, p);
        }
        finally {
            context.setCurrentMethod(null);
        }
    }

    @Override
    public void run(final AstNode compilationUnit) {
        compilationUnit.acceptVisitor(this, null);
    }

    protected IMetadataResolver resolver() {
        final TypeDefinition currentType = context.getCurrentType();
        return currentType != null ? currentType.getResolver() : MetadataSystem.instance();
    }

    protected AstType makeType(final TypeReference reference) {
        VerifyArgument.notNull(reference, "reference");

        final AstBuilder builder = context.getUserData(Keys.AST_BUILDER);

        if (builder != null) {
            return builder.convertType(reference);
        }

        final SimpleType type = new SimpleType(reference.getName());
        final TypeDefinition resolvedType = reference.resolve();

        type.putUserData(Keys.TYPE_REFERENCE, reference);

        if (resolvedType != null) {
            type.putUserData(Keys.TYPE_DEFINITION, resolvedType);
        }

        return type;
    }

    @SuppressWarnings("SameParameterValue")
    protected AstType makeType(final String descriptor) {
        final AstType reference = makeType(resolver().lookupType(descriptor));

        if (reference == null) {
            return new SimpleType(descriptor.replace('/', '.'));
        }

        return reference;
    }
}
