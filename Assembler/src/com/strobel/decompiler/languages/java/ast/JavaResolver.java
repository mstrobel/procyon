/*
 * JavaResolver.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast;

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.semantics.ResolveResult;
import com.strobel.functions.Function;

public class JavaResolver implements Function<AstNode, ResolveResult> {
    @Override
    public ResolveResult apply(final AstNode input) {
        if (input instanceof PrimitiveExpression) {
            final PrimitiveExpression primitive = (PrimitiveExpression) input;
            final String literalValue = primitive.getLiteralValue();
            final Object value = primitive.getValue();

            final TypeReference primitiveType;

            if (literalValue != null || value instanceof String) {
                primitiveType = MetadataSystem.instance().lookupType("java/lang/String");
            }
            else if (value instanceof Number) {
                if (value instanceof Byte) {
                    primitiveType = BuiltinTypes.Byte;
                }
                else if (value instanceof Short) {
                    primitiveType = BuiltinTypes.Short;
                }
                else if (value instanceof Integer) {
                    primitiveType = BuiltinTypes.Integer;
                }
                else if (value instanceof Long) {
                    primitiveType = BuiltinTypes.Long;
                }
                else if (value instanceof Float) {
                    primitiveType = BuiltinTypes.Float;
                }
                else if (value instanceof Double) {
                    primitiveType = BuiltinTypes.Double;
                }
                else {
                    primitiveType = null;
                }
            }
            else if (value instanceof Character) {
                primitiveType = BuiltinTypes.Character;
            }
            else if (value instanceof Boolean) {
                primitiveType = BuiltinTypes.Boolean;
            }
            else {
                primitiveType = null;
            }

            if (primitiveType == null) {
                return null;
            }

            return new PrimitiveResolveResult(
                primitiveType,
                literalValue != null ? literalValue : value
            );
        }

        return null;
    }

    private final static class PrimitiveResolveResult extends ResolveResult {
        private final Object _value;

        private PrimitiveResolveResult(final TypeReference type, final Object value) {
            super(type);
            _value = value;
        }

        @Override
        public boolean isCompileTimeConstant() {
            return true;
        }

        @Override
        public Object getConstantValue() {
            return _value;
        }
    }
}
