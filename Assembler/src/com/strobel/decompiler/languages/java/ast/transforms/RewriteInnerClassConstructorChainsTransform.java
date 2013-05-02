/*
 * InnerClassConstructorChainTransform.java
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

import com.strobel.assembler.metadata.MetadataResolver;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.Expression;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.ObjectCreationExpression;

public class RewriteInnerClassConstructorChainsTransform extends ContextTrackingVisitor<Void> {
    public RewriteInnerClassConstructorChainsTransform(final DecompilerContext context) {
        super(context);
    }

    @Override
    public Void visitObjectCreationExpression(final ObjectCreationExpression node, final Void data) {
        super.visitObjectCreationExpression(node, data);

        if (!node.getArguments().isEmpty()) {
            final Expression firstArgument = node.getArguments().firstOrNullObject();

            if (firstArgument instanceof ObjectCreationExpression) {
                final ObjectCreationExpression innerCreation = (ObjectCreationExpression) firstArgument;
                final TypeReference outerType = node.getType().getUserData(Keys.TYPE_REFERENCE);
                final TypeReference innerType = innerCreation.getType().getUserData(Keys.TYPE_REFERENCE);

                if (outerType != null && innerType != null) {
                    final TypeDefinition outerResolved = outerType.resolve();

                    if (outerResolved != null &&
                        outerResolved.isInnerClass() &&
                        !outerResolved.isStatic() &&
                        MetadataResolver.areEquivalent(outerResolved.getDeclaringType(), innerType)) {

                        innerCreation.remove();
                        node.setTarget(innerCreation);
                    }
                }
            }
        }

        return null;
    }
}
