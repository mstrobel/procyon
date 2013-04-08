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

import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
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

    public TResult visitTypeDeclaration(final TypeDeclaration typeDeclaration, final Void _) {
        final TypeDefinition oldType = context.getCurrentType();
        final MethodDefinition oldMethod = context.getCurrentMethod();

        try {
            context.setCurrentType(typeDeclaration.getUserData(Keys.TYPE_DEFINITION));
            context.setCurrentMethod(null);
            return super.visitTypeDeclaration(typeDeclaration, _);
        }
        finally {
            context.setCurrentType(oldType);
            context.setCurrentMethod(oldMethod);
        }
    }

    public TResult visitMethodDeclaration(final MethodDeclaration methodDeclaration, final Void _) {
        assert context.getCurrentMethod() == null;
        try {
            context.setCurrentMethod(methodDeclaration.getUserData(Keys.METHOD_DEFINITION));
            return super.visitMethodDeclaration(methodDeclaration, _);
        }
        finally {
            context.setCurrentMethod(null);
        }
    }

    public TResult visitConstructorDeclaration(final ConstructorDeclaration constructorDeclaration, final Void _) {
        assert (context.getCurrentMethod() == null);
        try {
            context.setCurrentMethod(constructorDeclaration.getUserData(Keys.METHOD_DEFINITION));
            return super.visitConstructorDeclaration(constructorDeclaration, _);
        }
        finally {
            context.setCurrentMethod(null);
        }
    }

    @Override
    public void run(final AstNode compilationUnit) {
        compilationUnit.acceptVisitor(this, null);
    }
}
