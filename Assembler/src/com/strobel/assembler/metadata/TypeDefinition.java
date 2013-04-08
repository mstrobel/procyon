/*
 * TypeDefinition.java
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

import com.strobel.assembler.Collection;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.reflection.SimpleType;
import com.strobel.reflection.Types;

import java.util.Collections;
import java.util.List;

public class TypeDefinition extends TypeReference implements IMemberDefinition {
    private final GenericParameterCollection _genericParameters;
    private final Collection<TypeDefinition> _declaredTypes;
    private final Collection<FieldDefinition> _declaredFields;
    private final Collection<MethodDefinition> _declaredMethods;
    private final Collection<TypeReference> _explicitInterfaces;
    private final Collection<CustomAnnotation> _customAnnotations;
    private final List<GenericParameter> _genericParametersView;
    private final List<TypeDefinition> _declaredTypesView;
    private final List<FieldDefinition> _declaredFieldsView;
    private final List<MethodDefinition> _declaredMethodsView;
    private final List<TypeReference> _explicitInterfacesView;
    private final List<CustomAnnotation> _customAnnotationsView;

    private IMetadataResolver _resolver;
    private String _simpleName;
    private String _packageName;
    private String _internalName;
    private String _fullName;
    private TypeReference _baseType;
    private long _flags;
    private List<Enum> _enumConstants;
    private TypeReference _rawType;
    private MethodReference _declaringMethod;

    public TypeDefinition() {
        _genericParameters = new GenericParameterCollection(this);
        _declaredTypes = new Collection<>();
        _declaredFields = new Collection<>();
        _declaredMethods = new Collection<>();
        _explicitInterfaces = new Collection<>();
        _customAnnotations = new Collection<>();
        _genericParametersView = Collections.unmodifiableList(_genericParameters);
        _declaredTypesView = Collections.unmodifiableList(_declaredTypes);
        _declaredFieldsView = Collections.unmodifiableList(_declaredFields);
        _declaredMethodsView = Collections.unmodifiableList(_declaredMethods);
        _explicitInterfacesView = Collections.unmodifiableList(_explicitInterfaces);
        _customAnnotationsView = Collections.unmodifiableList(_customAnnotations);
    }

    public TypeDefinition(final IMetadataResolver resolver) {
        this();
        _resolver = VerifyArgument.notNull(resolver, "resolver");
    }

    public final IMetadataResolver getResolver() {
        return _resolver;
    }

    protected final void setResolver(final IMetadataResolver resolver) {
        _resolver = resolver;
    }

    public String getPackageName() {
        final TypeReference declaringType = getDeclaringType();

        if (declaringType != null) {
            return declaringType.getPackageName();
        }

        return _packageName != null ? _packageName
                                    : StringUtilities.EMPTY;
    }

    @Override
    public String getSimpleName() {
        return _simpleName != null ? _simpleName
                                   : getName();
    }

    protected final void setSimpleName(final String simpleName) {
        _simpleName = simpleName;
    }

    protected void setPackageName(final String packageName) {
        _packageName = packageName;
        _fullName = null;
        _internalName = null;
    }

    public String getFullName() {
        if (_fullName == null) {
            final StringBuilder name = new StringBuilder();
            appendName(name, true, true);
            _fullName = name.toString();
        }
        return _fullName;
    }

    public String getInternalName() {
        if (_internalName == null) {
            final StringBuilder name = new StringBuilder();
            appendName(name, true, false);
            _internalName = name.toString();
        }
        return _internalName;
    }

    public final MethodReference getDeclaringMethod() {
        return _declaringMethod;
    }

    protected final void setDeclaringMethod(final MethodReference declaringMethod) {
        _declaringMethod = declaringMethod;
    }

    public final TypeReference getBaseType() {
        return _baseType;
    }

    protected final void setBaseType(final TypeReference baseType) {
        _baseType = baseType;
    }

    public final List<Enum> getEnumConstants() {
        if (isEnum()) {
            return _enumConstants != null ? _enumConstants
                                          : Collections.<Enum>emptyList();
        }
        throw Error.notEnumType(this);
    }

    protected final void setEnumConstants(final Enum... values) {
        VerifyArgument.notNull(values, "values");

        _enumConstants = values.length == 0 ? null
                                            : ArrayUtilities.asUnmodifiableList(values);
    }

    public final List<TypeReference> getExplicitInterfaces() {
        return _explicitInterfacesView;
    }

    @Override
    public final List<CustomAnnotation> getAnnotations() {
        return _customAnnotationsView;
    }

    @Override
    public final List<GenericParameter> getGenericParameters() {
        return _genericParametersView;
    }

    @Override
    public TypeReference getRawType() {
        if (isGenericType()) {
            if (_rawType == null) {
                synchronized (this) {
                    if (_rawType == null) {
                        _rawType = new RawType(this);
                    }
                }
            }
        }
        return this;
    }

    protected final GenericParameterCollection getGenericParametersInternal() {
        return _genericParameters;
    }

    protected final Collection<TypeDefinition> getDeclaredTypesInternal() {
        return _declaredTypes;
    }

    protected final Collection<FieldDefinition> getDeclaredFieldsInternal() {
        return _declaredFields;
    }

    protected final Collection<MethodDefinition> getDeclaredMethodsInternal() {
        return _declaredMethods;
    }

    protected final Collection<TypeReference> getExplicitInterfacesInternal() {
        return _explicitInterfaces;
    }

    protected final Collection<CustomAnnotation> getAnnotationsInternal() {
        return _customAnnotations;
    }

    @Override
    public TypeDefinition resolve() {
        return this;
    }

    // <editor-fold defaultstate="collapsed" desc="Type Attributes">

    public final long getFlags() {
        return _flags;
    }

    protected final void setFlags(final long flags) {
        _flags = flags;
    }

    public final int getModifiers() {
        return Flags.toModifiers(getFlags());
    }

    public final boolean isFinal() {
        return Flags.testAny(getFlags(), Flags.FINAL);
    }

    public final boolean isNonPublic() {
        return !Flags.testAny(getFlags(), Flags.PUBLIC);
    }

    public final boolean isPrivate() {
        return Flags.testAny(getFlags(), Flags.PRIVATE);
    }

    public final boolean isProtected() {
        return Flags.testAny(getFlags(), Flags.PROTECTED);
    }

    public final boolean isPublic() {
        return Flags.testAny(getFlags(), Flags.PUBLIC);
    }

    public final boolean isStatic() {
        return Flags.testAny(getFlags(), Flags.STATIC);
    }

    public final boolean isSynthetic() {
        return Flags.testAny(getFlags(), Flags.SYNTHETIC);
    }

    public final boolean isDeprecated() {
        return Flags.testAny(getFlags(), Flags.DEPRECATED);
    }

    public final boolean isPackagePrivate() {
        return !Flags.testAny(getFlags(), Flags.PUBLIC | Flags.PROTECTED | Flags.PRIVATE);
    }

    public SimpleType getSimpleType() {
        return SimpleType.Object;
    }

    public final boolean isAnnotation() {
        return isInterface() &&
               Flags.testAny(getFlags(), Flags.ANNOTATION);
    }

    public final boolean isClass() {
        return !isPrimitive() && !isInterface() && !isEnum();
    }

    public final boolean isInterface() {
        return Flags.testAny(getFlags(), Flags.INTERFACE);
    }

    public final boolean isEnum() {
        return Flags.testAny(getFlags(), Flags.ENUM);
    }

    public final boolean isAnonymous() {
        return Flags.testAny(getFlags(), Flags.ANONYMOUS);
    }

    public final boolean isInnerClass() {
        return getDeclaringType() != null;
    }

    public final boolean isLocalClass() {
        return getDeclaringMethod() != null;
    }

    public boolean isNested() {
        return getDeclaringType() != null;
    }

    public boolean isArray() {
        return getSimpleType() == SimpleType.Array;
    }

    public boolean isPrimitive() {
        return false;
    }

    @Override
    public final boolean isDefinition() {
        return true;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Members">

    public final List<FieldDefinition> getDeclaredFields() {
        return _declaredFieldsView;
    }

    public final List<MethodDefinition> getDeclaredMethods() {
        return _declaredMethodsView;
    }

    public final List<TypeDefinition> getDeclaredTypes() {
        return _declaredTypesView;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Name and Signature Formatting">

    @Override
    public boolean isCompoundType() {
        return Flags.testAny(getFlags(), Flags.COMPOUND);
    }

    @Override
    protected StringBuilder appendDescription(final StringBuilder sb) {
        for (final javax.lang.model.element.Modifier modifier : Flags.asModifierSet(getModifiers() & ~Flags.ACC_VARARGS)) {
            sb.append(modifier.toString());
            sb.append(' ');
        }

        if (isEnum()) {
            sb.append("enum ");
        }
        else if (isInterface()) {
            sb.append("interface ");

            if (isAnnotation()) {
                sb.append('@');
            }
        }
        else {
            sb.append("class ");
        }

        StringBuilder s = super.appendDescription(sb);

        final TypeReference baseType = getBaseType();

        if (baseType != null) {
            s.append(" extends ");
            s = baseType.appendBriefDescription(s);
        }

        final List<TypeReference> interfaces = getExplicitInterfaces();
        final int interfaceCount = interfaces.size();

        if (interfaceCount > 0) {
            s.append(" implements ");
            for (int i = 0; i < interfaceCount; ++i) {
                if (i != 0) {
                    s.append(",");
                }
                s = interfaces.get(i).appendBriefDescription(s);
            }
        }

        return s;
    }

    @Override
    protected StringBuilder appendGenericSignature(final StringBuilder sb) {
        StringBuilder s = super.appendGenericSignature(sb);

        final TypeReference baseType = getBaseType();
        final List<TypeReference> interfaces = getExplicitInterfaces();

        if (baseType == null) {
            if (interfaces.isEmpty()) {
                s = Types.Object.appendSignature(s);
            }
        }
        else {
            s = baseType.appendSignature(s);
        }

        for (final TypeReference interfaceType : interfaces) {
            s = interfaceType.appendSignature(s);
        }

        return s;
    }

    // </editor-fold>
}
