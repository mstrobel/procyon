/*
 * CompoundTypeReference.java
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

package com.strobel.assembler.metadata;

import com.strobel.core.Accumulator;
import com.strobel.core.VerifyArgument;

import java.util.ArrayList;
import java.util.List;

import static com.strobel.core.Comparer.coalesce;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("DuplicatedCode")
public final class CompoundTypeReference extends TypeReference implements ICompoundType {
    private final TypeReference _baseType;
    private final List<TypeReference> _interfaces;
    private final IMetadataResolver _resolver;

    public CompoundTypeReference(final TypeReference baseType, final List<TypeReference> interfaces) {
        this(baseType, interfaces, IMetadataResolver.EMPTY);
    }

    public CompoundTypeReference(final TypeReference baseType, final List<TypeReference> interfaces, final IMetadataResolver resolver) {
        VerifyArgument.noNullElementsAndNotEmpty(interfaces, "interfaces");
        _baseType = baseType != BuiltinTypes.Null ? baseType : null;
        _interfaces = interfaces;
        _resolver = coalesce(resolver, IMetadataResolver.EMPTY);
    }

    private TypeReference underlyingType0() {
        return _baseType != null ? _baseType : _interfaces.get(0);
    }

    @Override
    public final TypeReference getBaseType() {
        return _baseType;
    }

    @Override
    public final List<TypeReference> getInterfaces() {
        return _interfaces;
    }

    @Override
    public IMetadataResolver getResolver() {
        return coalesce(_resolver, IMetadataResolver.EMPTY);
    }

    @Override
    public TypeReference getDeclaringType() {
        return null;
    }

    @Override
    public boolean isCompoundType() {
        return true;
    }

    @Override
    public String getSimpleName() {
        return underlyingType0().getSimpleName();
    }

    @Override
    public boolean containsGenericParameters() {
        final TypeReference baseType = getBaseType();

        if (baseType != null && baseType.containsGenericParameters()) {
            return true;
        }

        for (final TypeReference t : _interfaces) {
            if (t.containsGenericParameters()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return underlyingType0().getName();
    }

    @Override
    public String getFullName() {
        return underlyingType0().getFullName();
    }

    @Override
    public String getInternalName() {
        return underlyingType0().getInternalName();
    }

    @Override
    public TypeReference getUnderlyingType() {
        return underlyingType0();
    }

    @Override
    public final <R, P> R accept(final TypeMetadataVisitor<P, R> visitor, final P parameter) {
        return visitor.visitCompoundType(this, parameter);
    }

    @Override
    protected StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        return underlyingType0().appendName(sb, fullName, dottedName);
    }

    static StringBuilder append0(final ICompoundType t, final StringBuilder sb, final String delimiter, final Accumulator<TypeReference, StringBuilder> appender) {
        final TypeReference baseType = t.getBaseType();
        final List<TypeReference> interfaces = t.getInterfaces();

        StringBuilder s = sb;

        if (baseType != null) {
            s = appender.accumulate(s, baseType);
            if (!interfaces.isEmpty()) {
                s.append(delimiter);
            }
        }

        for (int i = 0, n = interfaces.size(); i < n; i++) {
            if (i != 0) {
                s.append(delimiter);
            }
            s = appender.accumulate(s, interfaces.get(i));
        }

        return s;
    }

    @Override
    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        return append0(this, sb, " & ", TypeFunctions.APPEND_BRIEF_DESCRIPTION);
    }

    @Override
    public StringBuilder appendSimpleDescription(final StringBuilder sb) {
        return append0(this, sb, " & ", TypeFunctions.APPEND_SIMPLE_DESCRIPTION);
    }

    @Override
    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        return append0(this, sb, " & ", TypeFunctions.APPEND_ERASED_DESCRIPTION);
    }

    @Override
    public StringBuilder appendDescription(final StringBuilder sb) {
        return append0(this, sb, " & ", TypeFunctions.APPEND_DESCRIPTION);
    }

    @Override
    public StringBuilder appendSignature(final StringBuilder sb) {
        final int position = sb.length();
        final StringBuilder s = append0(this, sb, ":", TypeFunctions.APPEND_SIGNATURE);

        if (_baseType == null) {
            s.insert(position, ':');
        }

        return s;
    }

    @Override
    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        return underlyingType0().appendErasedSignature(sb);
    }

    @Override
    protected StringBuilder appendErasedClassSignature(final StringBuilder sb) {
        return underlyingType0().appendErasedClassSignature(sb);
    }

    @Override
    public TypeDefinition resolve() {
        final IMetadataResolver resolver = _resolver;
        final boolean hasResolver = resolver != IMetadataResolver.EMPTY;

        final TypeDefinition baseResolved = _baseType != null ? hasResolver ? resolver.resolve(_baseType) : _baseType.resolve()
                                                              : null;

        final List<TypeReference> ifsResolved = new ArrayList<>();

        for (final TypeReference ifType : _interfaces) {
            final TypeDefinition ifTypeResolved = ifType != null ? hasResolver ? resolver.resolve(ifType) : ifType.resolve()
                                                                 : null;

            ifsResolved.add(coalesce(ifTypeResolved, ifType));
        }

        return new CompoundTypeDefinition(coalesce(baseResolved, _baseType), ifsResolved, resolver);
//        return null;
    }
}

