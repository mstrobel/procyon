package com.strobel.assembler.metadata;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.MutableInteger;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * @author Mike Strobel
 */
public final class MetadataParser {
    private final Stack<IGenericContext> _genericContexts;
    private final IMetadataResolver _resolver;

    public MetadataParser(final IMetadataResolver resolver) {
        _resolver = VerifyArgument.notNull(resolver, "resolver");
        _genericContexts = new Stack<>();
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
        return _resolver.lookupType(descriptor);
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

        final FieldReference resolved = _resolver.resolve(reference);

        return resolved != null ? resolved : reference;
    }

    public MethodReference parseMethod(final TypeReference declaringType, final String name, final String descriptor) {
        final boolean pushGenericContext = declaringType.containsGenericParameters();

        if (pushGenericContext) {
            pushGenericContext(declaringType);
        }

        try {
            final IMethodSignature signature = parseMethodSignature(descriptor);
            return lookupMethod(declaringType, name, signature);
        }
        finally {
            if (pushGenericContext) {
                popGenericContext();
            }
        }
    }

    public TypeReference lookupType(final String packageName, final String typeName) {
        final TypeReference reference = new UnresolvedType(packageName, typeName);
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

//            if (signature.charAt(position.getValue()) != '>') {
//                throw Error.invalidSignatureExpectedEndOfTypeVariables(signature, 0);
//            }

//            position.increment();
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

                        genericParameters.add(typeVariable);

                        final TypeReference extendsBound = parseCompoundType(signature, position);
                        final TypeReference resolvedExtendsBound = _resolver.resolve(extendsBound);

                        i = position.getValue();

                        if (i < n && signature.charAt(i) == ':') {

                        }
                        else {
                            typeVariable.setExtendsBound(
                                resolvedExtendsBound != null ? resolvedExtendsBound
                                                             : extendsBound
                            );
                        }

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

        List<TypeReference> interfaceTypes;

        if (signature.charAt(position.getValue()) == ':') {
            baseType = BuiltinTypes.Object;
            position.increment();
            interfaceTypes = new ArrayList<>();
            interfaceTypes.add(parseTopLevelSignature(signature, position));
        }
        else {
            final TypeReference t = parseTopLevelSignature(signature, position);
            final TypeReference r = _resolver.resolve(t);

            baseType = r != null ? r : t;
            interfaceTypes = null;
        }

        while (position.getValue() < signature.length() &&
               signature.charAt(position.getValue()) == ':') {

            position.increment();

            final TypeReference t = parseTopLevelSignature(signature, position);
            final TypeReference r = _resolver.resolve(t);

            if (interfaceTypes == null) {
                interfaceTypes = new ArrayList<>();
            }

            interfaceTypes.add(r != null ? r : t);
        }

        return new CompoundTypeReference(
            baseType,
            interfaceTypes != null ? interfaceTypes
                                   : Collections.<TypeReference>emptyList()
        );
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
        final TypeReference[] typeArguments;

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

                    if (!resolvedType.isGenericType()) {
                        throw Error.invalidSignatureNonGenericTypeTypeArguments(resolvedType);
                    }

                    pushGenericContext(resolvedType);

                    try {
                        typeArguments = new TypeReference[resolvedType.getGenericParameters().size()];
                        position.setValue(i);
                        parseTypeParameters(s, position, typeArguments);
                    }
                    finally {
                        popGenericContext();
                    }

                    i = position.getValue();

                    if (s.charAt(i) != ';') {
                        throw Error.invalidSignatureUnexpectedToken(s, i);
                    }

                    position.increment();

                    boolean hasBoundTypes = false;

                    for (final TypeReference typeArgument : typeArguments) {
                        if (!typeArgument.isGenericParameter()) {
                            hasBoundTypes = true;
                            break;
                        }
                    }

                    if (hasBoundTypes) {
                        return resolvedType.makeGenericType(typeArguments);
                    }

                    return resolvedType;
                }

                default: {
                    nameBuilder.append(c);
                }
            }
        }

        throw Error.invalidSignatureUnexpectedEnd(s, i);
    }

    private void parseTypeParameters(
        final String s,
        final MutableInteger position,
        final TypeReference[] typeArguments) {

        int i = position.getValue();

        assert s.charAt(i) == '<';

        position.increment();

        for (int j = 0; j < typeArguments.length; j++) {
            typeArguments[j] = parseTypeArgument(s, position);
        }

        i = position.getValue();

        if (s.charAt(i) != '>') {
            throw Error.invalidSignatureExpectedEndOfTypeArguments(s, i);
        }

        position.increment();
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
                return parseTypeArgument(s, position.increment()).makeArrayType();
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

    private final static TypeReference[] PRIMITIVE_TYPES = new TypeReference[16];

    static {
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

            for (int i = 0, n = parameters.length; i < n; i++) {
                parameters[i] = new ParameterDefinition(parameterTypes.get(i));
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

    private final class UnresolvedGenericType extends TypeSpecification implements IGenericInstance {
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
        public long getFlags() {
            return _genericDefinition.getFlags();
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
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="UnresolvedType Class">

    private final class UnresolvedType extends TypeReference {
        private final String _name;
        private final String _packageName;
        private final TypeReference _declaringType;
        private final GenericParameterCollection _genericParameters;

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

        @Override
        public TypeReference getDeclaringType() {
            return _declaringType;
        }

        @Override
        public long getFlags() {
            return 0;
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
        public TypeSpecification makeGenericType(final TypeReference... typeArguments) {
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

                genericParameters.freeze();

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
        public long getFlags() {
            return 0;
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
        public long getFlags() {
            return 0;
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
