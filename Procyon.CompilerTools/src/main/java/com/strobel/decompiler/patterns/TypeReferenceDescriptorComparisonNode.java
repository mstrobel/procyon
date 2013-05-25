/*
 * TypeReferenceNode.java
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

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.TypeReferenceExpression;

public final class TypeReferenceDescriptorComparisonNode extends Pattern {
    private final String _descriptor;

    public TypeReferenceDescriptorComparisonNode(final String descriptor) {
        _descriptor = VerifyArgument.notNull(descriptor, "descriptor");
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof TypeReferenceExpression) {
            final TypeReferenceExpression typeReferenceExpression = (TypeReferenceExpression) other;
            final TypeReference typeReference = typeReferenceExpression.getType().getUserData(Keys.TYPE_REFERENCE);

            return typeReference != null &&
                   StringUtilities.equals(_descriptor, typeReference.getInternalName());
        }
        return false;
    }
}
