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
import com.strobel.core.Predicate;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.INode;

import static com.strobel.core.CollectionUtilities.any;

public class RemoveHiddenMembersTransform extends ContextTrackingVisitor<Void> {
    public RemoveHiddenMembersTransform(final DecompilerContext context) {
        super(context);
    }

    @Override
    public Void visitTypeDeclaration(final TypeDeclaration node, final Void _) {
        if (!(node.getParent() instanceof CompilationUnit)) {
            final TypeDefinition type = node.getUserData(Keys.TYPE_DEFINITION);

            if (type != null && AstBuilder.isMemberHidden(type, context)) {
                node.remove();
                return null;
            }
        }

        return super.visitTypeDeclaration(node, _);
    }

    @Override
    public Void visitFieldDeclaration(final FieldDeclaration node, final Void data) {
        final FieldDefinition field = node.getUserData(Keys.FIELD_DEFINITION);

        if (field != null && AstBuilder.isMemberHidden(field, context)) {
            node.remove();
            return null;
        }

        return super.visitFieldDeclaration(node, data);
    }

    @Override
    public Void visitMethodDeclaration(final MethodDeclaration node, final Void _) {
        final MethodDefinition method = node.getUserData(Keys.METHOD_DEFINITION);

        if (method != null) {
            if (AstBuilder.isMemberHidden(method, context)) {
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

    private final static INode DEFAULT_CONSTRUCTOR_BODY;

    static {
        DEFAULT_CONSTRUCTOR_BODY = new BlockStatement(
            new ExpressionStatement(
                new InvocationExpression(
                    Expression.MYSTERY_OFFSET,
                    new SuperReferenceExpression( Expression.MYSTERY_OFFSET)
                )
            )
        );
    }

    @Override
    public Void visitConstructorDeclaration(final ConstructorDeclaration node, final Void _) {
        final MethodDefinition method = node.getUserData(Keys.METHOD_DEFINITION);

        if (method != null) {
            if (AstBuilder.isMemberHidden(method, context)) {
                if (method.getDeclaringType().isEnum() &&
                    method.getDeclaringType().isAnonymous() &&
                    !node.getBody().getStatements().isEmpty()) {

                    //
                    // Keep initializer blocks in anonymous enum value bodies.
                    //
                    return super.visitConstructorDeclaration(node, _);
                }

                node.remove();
                return null;
            }

            if (!context.getSettings().getShowSyntheticMembers() &&
                node.getParameters().isEmpty() &&
                DEFAULT_CONSTRUCTOR_BODY.matches(node.getBody())) {

                //
                // Remove redundant default constructors.
                //

                final TypeDefinition declaringType = method.getDeclaringType();

                if (declaringType != null) {
                    final boolean hasOtherConstructors = any(
                        declaringType.getDeclaredMethods(),
                        new Predicate<MethodDefinition>() {
                            @Override
                            public boolean test(final MethodDefinition m) {
                                return m.isConstructor() &&
                                       !m.isSynthetic() &&
                                       !StringUtilities.equals(m.getErasedSignature(), method.getErasedSignature());
                            }
                        }
                    );

                    if (!hasOtherConstructors) {
                        node.remove();
                        return null;
                    }
                }
            }
        }

        return super.visitConstructorDeclaration(node, _);
    }
}
