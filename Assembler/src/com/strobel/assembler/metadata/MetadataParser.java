/*
 * MetadataParser.java
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

import com.strobel.compilerservices.RuntimeHelpers;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.MutableInteger;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mike Strobel
 */
public final class MetadataParser {
    private final Stack<IGenericContext> _genericContexts;
    private final IMetadataResolver _resolver;
    private final AtomicInteger _suppressResolveDepth = new AtomicInteger();

    public MetadataParser(final IMetadataResolver resolver) {
        _resolver = VerifyArgument.notNull(resolver, "resolver");
        _genericContexts = new Stack<>();
    }

    public final IMetadataResolver getResolver() {
        return _resolver;
    }

    public AutoCloseable suppressTypeResolution() {
        _suppressResolveDepth.incrementAndGet();

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                _suppressResolveDepth.decrementAndGet();
            }
        };
    }

    public void pushGenericContext(final IGenericParameterProvider provider) {
        VerifyArgument.notNull(provider, "provider");

        pushGenericContext(
            new IGenericContext() {
                @Override
                public TypeReference findTypeVariable(final String name) {
                    VerifyArgument.notNull(name, "name");

                    for (final GenericParameter p : provider.getGenericParameters()) {
                        if (name.equals(p.getName())) {
                            return p;
                        }
                    }

                    return null;
                }
            }
        );
    }

    public void pushGenericContext(final IGenericContext context) {
        _genericContexts.push(VerifyArgument.notNull(context, "context"));
    }

    public void popGenericContext() {
        _genericContexts.pop();
    }

    public TypeReference parseTypeDescriptor(final String descriptor) {
        VerifyArgument.notNull(descriptor, "descriptor");

        int arrayDepth = 0;

        while (descriptor.charAt(arrayDepth) == '[') {
            ++arrayDepth;
        }

        if (arrayDepth == 0) {
            return parseTypeSignature("L" + descriptor + ";");
        }

        final TypeReference elementType = parseTypeSignature(descriptor.substring(arrayDepth));

        if (elementType == null) {
            return null;
        }

        TypeReference result = elementType.makeArrayType();

        while (--arrayDepth > 0) {
            result = result.makeArrayType();
        }

        return result;
    }

    public TypeReference parseTypeSignature(final String signature) {
        VerifyArgument.notNull(signature, "signature");

        return parseTopLevelSignature(signature, new MutableInteger(0));
    }

    public TypeReference parseTypeSignature(final String signature, final MutableInteger position) {
        VerifyArgument.notNull(signature, "signature");
        VerifyArgument.notNull(position, "position");

        return parseTopLevelSignature(signature, position);
    }

    public FieldReference parseField(final TypeReference declaringType, final String name, final String signature) {
        VerifyArgument.notNull(declaringType, "declaringType");
        VerifyArgument.notNull(name, "name");
        VerifyArgument.notNull(signature, "signature");

        final FieldReference reference = new UnresolvedField(
            declaringType,
            name,
            parseTypeSignature(signature)
        );

        if (_suppressResolveDepth.get() > 0) {
            return reference;
        }

        final FieldReference resolved = _resolver.resolve(reference);

        return resolved != null ? resolved : reference;
    }

    public MethodReference parseMethod(final TypeReference declaringType, final String name, final String descriptor) {
        final boolean needGenericContext = declaringType.containsGenericParameters();

        if (needGenericContext) {
            pushGenericContext(declaringType);
        }

        try {
            final IMethodSignature signature = parseMethodSignature(descriptor);
            return lookupMethod(declaringType, name, signature);
        }
        finally {
            if (needGenericContext) {
                popGenericContext();
            }
        }
    }

    public TypeReference lookupType(final String packageName, final String typeName) {
        final TypeReference reference = new UnresolvedType(packageName, typeName);

        if (_suppressResolveDepth.get() > 0) {
            return reference;
        }

        final TypeReference resolved = _resolver.resolve(reference);

        return resolved != null ? resolved : reference;
    }

    public TypeReference lookupType(final TypeReference declaringType, final String typeName) {
        final TypeReference reference = new UnresolvedType(declaringType, typeName);

        if (_suppressResolveDepth.get() > 0) {
            return reference;
        }

        final TypeReference resolved = _resolver.resolve(reference);

        return resolved != null ? resolved : reference;
    }

    protected TypeReference lookupTypeVariable(final String name) {
        for (int i = 0, n = _genericContexts.size(); i < n; i++) {
            final IGenericContext context = _genericContexts.get(i);
            final TypeReference typeVariable = context.findTypeVariable(name);

            if (typeVariable != null) {
                return typeVariable;
            }
        }

        if (_resolver instanceof IGenericContext) {
            return ((IGenericContext) _resolver).findTypeVariable(name);
        }

        return null;
    }

    public IMethodSignature parseMethodSignature(final String signature) {
        VerifyArgument.notNullOrWhitespace(signature, "signature");

        final List<GenericParameter> genericParameters;
        final MutableInteger position = new MutableInteger(0);

        char ch = signature.charAt(0);

        if (ch == '<') {
            genericParameters = new ArrayList<>();
            parseGenericParameters(genericParameters, signature, position);
            ch = signature.charAt(position.getValue());
        }
        else {
            genericParameters = Collections.emptyList();
        }

        if (ch != '(' && ch != '<') {
            throw Error.invalidSignatureExpectedParameterList(signature, position.getValue());
        }

        position.increment();

        final int length = signature.length();
        final TypeReference returnType;
        final boolean hasGenericParameters = !genericParameters.isEmpty();

        if (hasGenericParameters) {
            pushGenericContext(
                new IGenericContext() {
                    @Override
                    public TypeReference findTypeVariable(final String name) {
                        VerifyArgument.notNull(name, "name");

                        for (final GenericParameter p : genericParameters) {
                            if (name.equals(p.getName())) {
                                return p;
                            }
                        }

                        return null;
                    }
                }
            );
        }

        try {
            ArrayList<TypeReference> parameterTypes = null;

            while (position.getValue() < length) {
                switch (signature.charAt(position.getValue())) {
                    case ')':
                        position.increment();
                        returnType = parseTopLevelSignature(signature, position);

                        return new MethodSignature(
                            parameterTypes != null ? parameterTypes
                                                   : Collections.<TypeReference>emptyList(),
                            returnType,
                            genericParameters
                        );

                    default:
                        if (parameterTypes == null) {
                            parameterTypes = new ArrayList<>();
                        }

                        parameterTypes.add(parseTopLevelSignature(signature, position));
                }
            }
        }
        finally {
            if (hasGenericParameters) {
                popGenericContext();
            }
        }

        throw Error.invalidSignatureExpectedReturnType(signature, position.getValue());
    }

    public void parseGenericParameters(
        final List<GenericParameter> genericParameters,
        final String signature,
        final MutableInteger position) {

        VerifyArgument.notNull(genericParameters, "genericParameters");
        VerifyArgument.notNull(signature, "signature");
        VerifyArgument.notNull(position, "position");
        VerifyArgument.inRange(0, signature.length() - 1, position.getValue(), "position");

        if (signature.charAt(position.getValue()) != '<') {
            throw Error.invalidSignatureExpectedEndOfTypeVariables(signature, 0);
        }

        pushGenericContext(
            new IGenericContext() {
                @Override
                public TypeReference findTypeVariable(final String name) {
                    VerifyArgument.notNull(name, "name");

                    for (final GenericParameter genericParameter : genericParameters) {
                        if (name.equals(genericParameter.getName())) {
                            return genericParameter;
                        }
                    }
                    return null;
                }
            }
        );

        try {
            position.increment();

            int typeVariableStart = position.getValue();

            for (int i = typeVariableStart, n = signature.length() - 1; i < n; position.setValue(++i)) {
                final char ch = signature.charAt(i);

                switch (ch) {
                    case ':': {
                        if (i == typeVariableStart) {
                            throw Error.invalidSignatureExpectedTypeVariable(signature, i);
                        }

                        position.increment();

                        final String typeVariableName = signature.substring(typeVariableStart, i);
                        final GenericParameter typeVariable = new GenericParameter(typeVariableName);

                        typeVariable.setPosition(genericParameters.size());
                        genericParameters.add(typeVariable);

                        final TypeReference extendsBound = parseCompoundType(signature, position);
                        final TypeReference resolvedExtendsBound;

                        if (_suppressResolveDepth.get() > 0) {
                            resolvedExtendsBound = extendsBound;
                        }
                        else {
                            resolvedExtendsBound = _resolver.resolve(extendsBound);
                        }

                        i = position.getValue();

                        typeVariable.setExtendsBound(
                            resolvedExtendsBound != null ? resolvedExtendsBound
                                                         : extendsBound
                        );

                        typeVariableStart = i--;
                        break;
                    }

                    case '>': {
                        position.increment();
                        return;
                    }
                }
            }
        }
        finally {
            popGenericContext();
        }

        throw Error.invalidSignatureExpectedEndOfTypeVariables(signature, position.getValue());
    }

    private TypeReference parseCompoundType(final String signature, final MutableInteger position) {
        final TypeReference baseType;

        boolean hasExplicitBaseType = true;
        List<TypeReference> interfaceTypes;

        if (signature.charAt(position.getValue()) == ':') {
            baseType = BuiltinTypes.Object;
            position.increment();
            interfaceTypes = new ArrayList<>();
            interfaceTypes.add(parseTopLevelSignature(signature, position));
            hasExplicitBaseType = false;
        }
        else {
            baseType = parseTopLevelSignature(signature, position);
            interfaceTypes = null;
        }

        while (position.getValue() < signature.length() &&
               signature.charAt(position.getValue()) == ':') {

            position.increment();

            if (interfaceTypes == null) {
                interfaceTypes = new ArrayList<>();
            }

            interfaceTypes.add(parseTopLevelSignature(signature, position));
        }

        if (interfaceTypes != null) {
            if (!hasExplicitBaseType && BuiltinTypes.Object.equals(baseType) && interfaceTypes.size() == 1) {
                return interfaceTypes.get(0);
            }
            return new CompoundTypeReference(hasExplicitBaseType ? baseType : null, interfaceTypes);
        }

        return baseType;
    }

    private TypeReference parseTopLevelSignature(final String s, final MutableInteger position) {
        final int i = position.getValue();

        if (i >= s.length()) {
            throw Error.invalidSignatureTypeExpected(s, i);
        }

        switch (s.charAt(i)) {
            case '*':
                position.increment();
                return Wildcard.unbounded();
            case '+':
                return Wildcard.makeExtends(parseTopLevelSignature(s, position.increment()));
            case '-':
                return Wildcard.makeSuper(parseTopLevelSignature(s, position.increment()));
            case '[':
                return parseTopLevelSignature(s, position.increment()).makeArrayType();
            case 'B':
                position.increment();
                return BuiltinTypes.Byte;
            case 'C':
                position.increment();
                return BuiltinTypes.Character;
            case 'D':
                position.increment();
                return BuiltinTypes.Double;
            case 'F':
                position.increment();
                return BuiltinTypes.Float;
            case 'I':
                position.increment();
                return BuiltinTypes.Integer;
            case 'J':
                position.increment();
                return BuiltinTypes.Long;
            case 'L':
                return finishTopLevelType(s, position);
            case 'S':
                position.increment();
                return BuiltinTypes.Short;
            case 'T':
                return parseTypeArgument(s, position);
            case 'V':
                position.increment();
                return BuiltinTypes.Void;
            case 'Z':
                position.increment();
                return BuiltinTypes.Boolean;
            default:
                throw Error.invalidSignatureUnexpectedToken(s, i);
        }
    }

    private TypeReference finishTopLevelType(final String s, final MutableInteger position) {
        int i = position.getValue();

        assert s.charAt(i) == 'L';

        final TypeReference resolvedType;
        final List<TypeReference> typeArguments;

        final StringBuilder packageBuilder = new StringBuilder();
        final StringBuilder nameBuilder = new StringBuilder();

        while (++i < s.length()) {
            final char c = s.charAt(i);

            switch (c) {
                case '/': {
                    if (packageBuilder.length() > 0) {
                        packageBuilder.append('.');
                    }

                    packageBuilder.append(nameBuilder);
                    nameBuilder.setLength(0);

                    continue;
                }

                case ';': {
                    position.setValue(i + 1);
                    resolvedType = lookupType(packageBuilder.toString(), nameBuilder.toString());

                    if (resolvedType.isGenericDefinition()) {
                        return resolvedType.getRawType();
                    }

                    return resolvedType;
                }

                case '<': {
                    resolvedType = lookupType(packageBuilder.toString(), nameBuilder.toString());

                    if (!resolvedType.isGenericType() && !(resolvedType instanceof UnresolvedType)) {
                        throw Error.invalidSignatureNonGenericTypeTypeArguments(resolvedType);
                    }

                    pushGenericContext(resolvedType);

                    try {
                        position.setValue(i);
                        typeArguments = parseTypeParameters(s, position);
                    }
                    finally {
                        popGenericContext();
                    }

                    i = position.getValue();

                    if (s.charAt(i) != ';' && s.charAt(i) != '.') {
                        throw Error.invalidSignatureUnexpectedToken(s, i);
                    }

                    position.increment();

                    boolean hasBoundTypes = false;

                    for (final TypeReference typeArgument : typeArguments) {
                        if (!typeArgument.isGenericParameter() || !resolvedType.equals(((GenericParameter) typeArgument).getOwner())) {
                            hasBoundTypes = true;
                            break;
                        }
                    }

                    TypeReference result;

                    if (hasBoundTypes) {
                        result = resolvedType.makeGenericType(typeArguments);
                    }
                    else {
                        result = resolvedType;
                    }

                    if (s.charAt(i) == '.') {
                        result = parseInnerType(result, s, position);
                    }

                    return result;
                }

                default: {
                    nameBuilder.append(c);
                }
            }
        }

        throw Error.invalidSignatureUnexpectedEnd(s, i);
    }

    private TypeReference parseInnerType(final TypeReference declaringType, final String s, final MutableInteger position) {
        final boolean isGenericDefinition = declaringType.isGenericDefinition();

        if (isGenericDefinition) {
            pushGenericContext(declaringType);
        }

        try {
            final StringBuilder name = new StringBuilder();

            for (int i = position.getValue(); i < s.length(); i = position.increment().getValue()) {
                final char ch = s.charAt(i);

                switch (ch) {
                    case '<':
                        final TypeReference type = lookupType(declaringType, name.toString());
                        final List<TypeReference> typeArgs = parseTypeParameters(s, position);

                        final TypeReference genericType = type.makeGenericType(typeArgs);

                        if (i < s.length() - 1 && s.charAt(i + 1) == '.') {
                            return parseInnerType(genericType, s, position.increment().increment());
                        }

                        if (s.charAt(position.getValue()) != ';') {
                            throw Error.invalidSignatureUnexpectedToken(s, position.getValue());
                        }

                        position.increment();
                        return genericType;

                    case ';':
                        position.increment();
                        return lookupType(declaringType, name.toString());

                    default:
                        name.append(ch);
                        break;
                }
            }

            throw Error.invalidSignatureUnexpectedEnd(s, position.getValue());
        }
        finally {
            if (isGenericDefinition) {
                popGenericContext();
            }
        }
    }

    private List<TypeReference> parseTypeParameters(final String s, final MutableInteger position) {

        assert s.charAt(position.getValue()) == '<';

        position.increment();

        List<TypeReference> typeArguments = null;

        while (s.charAt(position.getValue()) != '>') {
            if (typeArguments == null) {
                typeArguments = new ArrayList<>();
            }
            typeArguments.add(parseTypeArgument(s, position));
        }

        if (position.getValue() >= s.length() || s.charAt(position.getValue()) != '>') {
            throw Error.invalidSignatureExpectedEndOfTypeArguments(s, position.getValue());
        }

        position.increment();

        return typeArguments != null ? typeArguments
                                     : Collections.<TypeReference>emptyList();
    }

    private TypeReference parseTypeArgument(final String s, final MutableInteger position) {

        int i = position.getValue();

        if (i >= s.length()) {
            throw Error.invalidSignatureExpectedTypeArgument(s, i);
        }

        switch (s.charAt(i)) {
            case '*':
                position.increment();
                return Wildcard.unbounded();
            case '+':
                return Wildcard.makeExtends(parseTypeArgument(s, position.increment()));
            case '-':
                return Wildcard.makeSuper(parseTypeArgument(s, position.increment()));
            case '[':
                return parseTopLevelSignature(s, position);
            case 'L':
                return finishTopLevelType(s, position);
            case 'T':
                final int typeVariableStart = i + 1;
                while (++i < s.length()) {
                    if (s.charAt(i) == ';') {
                        position.setValue(i + 1);
                        final String name = s.substring(typeVariableStart, i);
                        final TypeReference typeVariable = lookupTypeVariable(name);
                        if (typeVariable != null) {
                            return typeVariable;
                        }
                        throw Error.invalidSignatureUnresolvedTypeVariable(s, name, position.getValue());
                    }
                }
                throw Error.invalidSignatureExpectedTypeArgument(s, position.getValue());
            default:
                throw Error.invalidSignatureUnexpectedToken(s, i);
        }
    }

    protected MethodReference lookupMethod(final TypeReference declaringType, final String name, final IMethodSignature signature) {
        final MethodReference reference = new UnresolvedMethod(
            declaringType,
            name,
            signature
        );

        final MethodReference resolved = _resolver.resolve(reference);

        return resolved != null ? resolved : reference;
    }

    // <editor-fold defaultstate="collapsed" desc="Primitive Lookup">

    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private final static TypeReference[] PRIMITIVE_TYPES = new TypeReference[16];

    static {
        RuntimeHelpers.ensureClassInitialized(MetadataSystem.class);

        final TypeReference[] allPrimitives = {
            BuiltinTypes.Boolean,
            BuiltinTypes.Byte,
            BuiltinTypes.Character,
            BuiltinTypes.Short,
            BuiltinTypes.Integer,
            BuiltinTypes.Long,
            BuiltinTypes.Float,
            BuiltinTypes.Double,
            BuiltinTypes.Void
        };

        for (final TypeReference t : allPrimitives) {
            PRIMITIVE_TYPES[hashPrimitiveName(t.getName())] = t;
        }
    }

    private static int hashPrimitiveName(final String name) {
        if (name.length() < 3) {
            return 0;
        }
        return (name.charAt(0) + name.charAt(2)) % 16;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="MethodSignature Class">

    private final static class MethodSignature implements IMethodSignature {
        private final List<ParameterDefinition> _parameters;
        private final TypeReference _returnType;
        private final List<GenericParameter> _genericParameters;

        MethodSignature(final List<TypeReference> parameterTypes, final TypeReference returnType) {
            this(parameterTypes, returnType, Collections.<GenericParameter>emptyList());
        }

        MethodSignature(
            final List<TypeReference> parameterTypes,
            final TypeReference returnType,
            final List<GenericParameter> genericParameters) {

            VerifyArgument.notNull(parameterTypes, "parameterTypes");
            VerifyArgument.notNull(returnType, "returnType");
            VerifyArgument.notNull(genericParameters, "genericParameters");

            final ParameterDefinition[] parameters = new ParameterDefinition[parameterTypes.size()];

            for (int i = 0, slot = 0, n = parameters.length; i < n; i++, slot++) {
                final TypeReference parameterType = parameterTypes.get(i);

                parameters[i] = new ParameterDefinition(slot, parameterType);

                if (parameterType.getSimpleType().isDoubleWord()) {
                    slot++;
                }
            }

            _parameters = ArrayUtilities.asUnmodifiableList(parameters);
            _returnType = returnType;
            _genericParameters = genericParameters;
        }

        @Override
        public boolean hasParameters() {
            return !_parameters.isEmpty();
        }

        @Override
        public List<ParameterDefinition> getParameters() {
            return _parameters;
        }

        @Override
        public TypeReference getReturnType() {
            return _returnType;
        }

        @Override
        public boolean hasGenericParameters() {
            return !_genericParameters.isEmpty();
        }

        @Override
        public boolean isGenericDefinition() {
            return !_genericParameters.isEmpty();
        }

        @Override
        public List<GenericParameter> getGenericParameters() {
            return _genericParameters;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="UnresolvedGenericType Class">

    private final class UnresolvedGenericType extends TypeReference implements IGenericInstance {
        private final TypeReference _genericDefinition;
        private final List<TypeReference> _typeParameters;

        UnresolvedGenericType(final TypeReference genericDefinition, final List<TypeReference> typeParameters) {
            _genericDefinition = genericDefinition;
            _typeParameters = typeParameters;
        }

        @Override
        public TypeReference getElementType() {
            return null;
        }

        @Override
        public String getName() {
            return _genericDefinition.getName();
        }

        @Override
        public String getPackageName() {
            return _genericDefinition.getPackageName();
        }

        @Override
        public TypeReference getDeclaringType() {
            return _genericDefinition.getDeclaringType();
        }

        @Override
        public String getSimpleName() {
            return _genericDefinition.getSimpleName();
        }

        @Override
        public String getFullName() {
            return _genericDefinition.getFullName();
        }

        @Override
        public String getInternalName() {
            return _genericDefinition.getInternalName();
        }

        @Override
        public boolean isGenericDefinition() {
            return false;
        }

        @Override
        public List<GenericParameter> getGenericParameters() {
            return _genericDefinition.getGenericParameters();
        }

        @Override
        public boolean hasTypeArguments() {
            return true;
        }

        @Override
        public List<TypeReference> getTypeArguments() {
            return _typeParameters;
        }

        @Override
        public IGenericParameterProvider getGenericDefinition() {
            return _genericDefinition;
        }

        @Override
        public TypeReference getUnderlyingType() {
            return _genericDefinition;
        }

        @Override
        public TypeDefinition resolve() {
            return _resolver.resolve(this);
        }

        @Override
        public FieldDefinition resolve(final FieldReference field) {
            return _resolver.resolve(field);
        }

        @Override
        public MethodDefinition resolve(final MethodReference method) {
            return _resolver.resolve(method);
        }

        @Override
        public TypeDefinition resolve(final TypeReference type) {
            return _resolver.resolve(type);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="UnresolvedType Class">

    private final class UnresolvedType extends TypeReference {
        private final String _name;
        private final String _packageName;
        private final TypeReference _declaringType;
        private final GenericParameterCollection _genericParameters;

        private String _fullName;
        private String _internalName;

        UnresolvedType(final TypeReference declaringType, final String name) {
            _name = VerifyArgument.notNull(name, "name");
            _packageName = StringUtilities.EMPTY;
            _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
            _genericParameters = new GenericParameterCollection(this);
        }

        UnresolvedType(final String packageName, final String name) {
            _packageName = VerifyArgument.notNull(packageName, "packageName");
            _name = VerifyArgument.notNull(name, "name");
            _declaringType = null;
            _genericParameters = new GenericParameterCollection(this);
        }

        UnresolvedType(final TypeReference declaringType, final String name, final List<GenericParameter> genericParameters) {
            _name = VerifyArgument.notNull(name, "name");
            _packageName = StringUtilities.EMPTY;
            _declaringType = VerifyArgument.notNull(declaringType, "declaringType");

            _genericParameters = new GenericParameterCollection(this);

            for (final GenericParameter genericParameter : genericParameters) {
                _genericParameters.add(genericParameter);
            }
        }

        UnresolvedType(final String packageName, final String name, final List<GenericParameter> genericParameters) {
            _packageName = VerifyArgument.notNull(packageName, "packageName");
            _name = VerifyArgument.notNull(name, "name");
            _declaringType = null;

            _genericParameters = new GenericParameterCollection(this);

            for (final GenericParameter genericParameter : genericParameters) {
                _genericParameters.add(genericParameter);
            }
        }

        @Override
        public String getName() {
            return _name;
        }

        @Override
        public String getPackageName() {
            return _packageName;
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

        @Override
        public TypeReference getDeclaringType() {
            return _declaringType;
        }

        @Override
        public String getSimpleName() {
            return _name;
        }

        @Override
        public boolean isGenericDefinition() {
            return hasGenericParameters();
        }

        @Override
        public List<GenericParameter> getGenericParameters() {
            return _genericParameters;
        }

        @Override
        public TypeReference makeGenericType(final List<TypeReference> typeArguments) {
            VerifyArgument.notEmpty(typeArguments, "typeArguments");
            VerifyArgument.noNullElements(typeArguments, "typeArguments");

            final TypeDefinition resolved = this.resolve();

            return new UnresolvedGenericType(
                resolved != null ? resolved : this,
                ArrayUtilities.asUnmodifiableList(typeArguments.toArray(new TypeReference[typeArguments.size()]))
            );
        }

        @Override
        public TypeReference makeGenericType(final TypeReference... typeArguments) {
            VerifyArgument.notEmpty(typeArguments, "typeArguments");
            VerifyArgument.noNullElements(typeArguments, "typeArguments");

            final TypeDefinition resolved = this.resolve();

            return new UnresolvedGenericType(
                resolved != null ? resolved : this,
                ArrayUtilities.asUnmodifiableList(typeArguments.clone())
            );
        }

        @Override
        public TypeDefinition resolve() {
            return _resolver.resolve(this);
        }

        @Override
        public FieldDefinition resolve(final FieldReference field) {
            return _resolver.resolve(field);
        }

        @Override
        public MethodDefinition resolve(final MethodReference method) {
            return _resolver.resolve(method);
        }

        @Override
        public TypeDefinition resolve(final TypeReference type) {
            return _resolver.resolve(type);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="UnresolvedMethod Class">

    private final class UnresolvedMethod extends MethodReference {
        private final TypeReference _declaringType;
        private final String _name;
        private final IMethodSignature _signature;
        private final List<GenericParameter> _genericParameters;

        UnresolvedMethod(final TypeReference declaringType, final String name, final IMethodSignature signature) {
            _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
            _name = VerifyArgument.notNull(name, "name");
            _signature = VerifyArgument.notNull(signature, "signature");

            if (_signature.hasGenericParameters()) {
                final GenericParameterCollection genericParameters = new GenericParameterCollection(this);

                for (final GenericParameter genericParameter : _signature.getGenericParameters()) {
                    genericParameters.add(genericParameter);
                }

                genericParameters.freeze(false);

                _genericParameters = genericParameters;
            }
            else {
                _genericParameters = Collections.emptyList();
            }
        }

        @Override
        public String getName() {
            return _name;
        }

        @Override
        public TypeReference getReturnType() {
            return _signature.getReturnType();
        }

        @Override
        public List<ParameterDefinition> getParameters() {
            return _signature.getParameters();
        }

        @Override
        public TypeReference getDeclaringType() {
            return _declaringType;
        }

        @Override
        public List<GenericParameter> getGenericParameters() {
            return _genericParameters;
        }

        @Override
        protected StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
            return sb.append(_name);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="UnresolvedField Class">

    private final class UnresolvedField extends FieldReference {
        private final TypeReference _declaringType;
        private final String _name;
        private final TypeReference _fieldType;

        UnresolvedField(final TypeReference declaringType, final String name, final TypeReference fieldType) {
            _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
            _name = VerifyArgument.notNull(name, "name");
            _fieldType = VerifyArgument.notNull(fieldType, "fieldType");
        }

        @Override
        public String getName() {
            return _name;
        }

        @Override
        public TypeReference getDeclaringType() {
            return _declaringType;
        }

        @Override
        public TypeReference getFieldType() {
            return _fieldType;
        }

        @Override
        protected StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
            return sb.append(_name);
        }
    }

    // </editor-fold>
}
