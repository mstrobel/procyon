/*
 * JavaResolver.java
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

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.IMetadataResolver;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.Comparer;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.semantics.ResolveResult;
import com.strobel.functions.Function;

public class JavaResolver implements Function<AstNode, ResolveResult> {
    private final DecompilerContext _context;

    public JavaResolver(final DecompilerContext context) {
        _context = VerifyArgument.notNull(context, "context");
    }

    @Override
    public ResolveResult apply(final AstNode input) {
        return input.acceptVisitor(new ResolveVisitor(_context), null);
    }

    private final static class ResolveVisitor extends ContextTrackingVisitor<ResolveResult> {
        protected ResolveVisitor(final DecompilerContext context) {
            super(context);
        }

        @Override
        public ResolveResult visitObjectCreationExpression(final ObjectCreationExpression node, final Void _) {
            final ResolveResult result = resolveTypeFromMember(node.getUserData(Keys.MEMBER_REFERENCE));

            if (result != null) {
                return result;
            }

            return node.getType().acceptVisitor(this, _);
        }

        @Override
        public ResolveResult visitAnonymousObjectCreationExpression(final AnonymousObjectCreationExpression node, final Void _) {
            final ResolveResult result = resolveTypeFromMember(node.getUserData(Keys.MEMBER_REFERENCE));

            if (result != null) {
                return result;
            }

            return node.getType().acceptVisitor(this, _);
        }

        @Override
        public ResolveResult visitComposedType(final ComposedType node, final Void _) {
            return resolveType(node.toTypeReference());
        }

        @Override
        public ResolveResult visitSimpleType(final SimpleType node, final Void _) {
            return resolveType(node.toTypeReference());
        }

        @Override
        public ResolveResult visitThisReferenceExpression(final ThisReferenceExpression node, final Void data) {
            return resolveType(node.getUserData(Keys.TYPE_REFERENCE));
        }

        @Override
        public ResolveResult visitSuperReferenceExpression(final SuperReferenceExpression node, final Void data) {
            return super.visitSuperReferenceExpression(node, data);
        }

        @Override
        public ResolveResult visitTypeReference(final TypeReferenceExpression node, final Void _) {
            return resolveType(node.getType().getUserData(Keys.TYPE_REFERENCE));
        }

        @Override
        public ResolveResult visitWildcardType(final WildcardType node, final Void _) {
            return resolveType(node.toTypeReference());
        }

        @Override
        public ResolveResult visitIdentifier(final Identifier node, final Void _) {
            final ResolveResult result = resolveTypeFromMember(node.getUserData(Keys.MEMBER_REFERENCE));

            if (result != null) {
                return result;
            }

            return resolveTypeFromVariable(node.getUserData(Keys.VARIABLE));
        }

        @Override
        public ResolveResult visitIdentifierExpression(final IdentifierExpression node, final Void data) {
            final ResolveResult result = resolveTypeFromVariable(node.getUserData(Keys.VARIABLE));

            if (result != null) {
                return result;
            }

            return super.visitIdentifierExpression(node, data);
        }

        @Override
        public ResolveResult visitMemberReferenceExpression(final MemberReferenceExpression node, final Void _) {
            return resolveTypeFromMember(node.getUserData(Keys.MEMBER_REFERENCE));
        }

        @Override
        public ResolveResult visitInvocationExpression(final InvocationExpression node, final Void _) {
            final ResolveResult result = resolveTypeFromMember(node.getUserData(Keys.MEMBER_REFERENCE));
            
            if (result != null) {
                return result;
            }
            
            return node.getTarget().acceptVisitor(this, _);
        }

        @Override
        protected ResolveResult visitChildren(final AstNode node, final Void _) {
            ResolveResult result = null;

            AstNode next;

            for (AstNode child = node.getFirstChild(); child != null; child = next) {
                //
                // Store next to allow the loop to continue if the visitor removes/replaces child.
                //
                next = child.getNextSibling();

                if (child instanceof JavaTokenNode) {
                    continue;
                }

                final ResolveResult childResult = child.acceptVisitor(this, _);

                if (childResult == null) {
                    return null;
                }
                else if (result == null) {
                    result = childResult;
                }
                else if (result.isCompileTimeConstant() &&
                         childResult.isCompileTimeConstant() &&
                         Comparer.equals(result.getConstantValue(), childResult.getConstantValue())) {

                    continue;
                }
                else {
                    final TypeReference commonSuperType = MetadataHelper.findCommonSuperType(result.getType(), childResult.getType());

                    if (commonSuperType != null) {
                        result = new ResolveResult(commonSuperType);
                    }
                    else {
                        return null;
                    }
                }
            }

            return null;
        }

        @Override
        public ResolveResult visitPrimitiveExpression(final PrimitiveExpression node, final Void _) {
            final String literalValue = node.getLiteralValue();
            final Object value = node.getValue();

            final TypeReference primitiveType;

            if (literalValue != null || value instanceof String) {
                final TypeDefinition currentType = context.getCurrentType();
                final IMetadataResolver resolver = currentType != null ? currentType.getResolver() : MetadataSystem.instance();

                primitiveType = resolver.lookupType("java/lang/String");
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
    }

    private final static ResolveResult resolveTypeFromVariable(final Variable variable) {
        if (variable == null) {
            return null;
        }

        final TypeReference type = variable.getType();

        if (type != null) {
            return new ResolveResult(type);
        }

        return null;
    }
    
    private final static ResolveResult resolveType(final TypeReference type) {
        return type == null ? null : new ResolveResult(type);
    }
    
    private final static ResolveResult resolveTypeFromMember(final MemberReference member) {
        if (member == null) {
            return null;
        }

        if (member instanceof FieldReference) {
            return new ResolveResult(((FieldReference) member).getFieldType());
        }

        if (member instanceof MethodReference) {
            final MethodReference method = (MethodReference) member;

            if (method.isConstructor()) {
                return new ResolveResult(method.getDeclaringType());
            }

            return new ResolveResult(method.getReturnType());
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
