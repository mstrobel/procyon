package com.strobel.assembler.metadata;

import com.strobel.core.MutableInteger;
import com.strobel.core.VerifyArgument;

import java.util.Stack;

/**
 * @author Mike Strobel
 */
public abstract class MetadataScope implements IMetadataScope {
    private final Stack<IGenericContext> _genericContexts;

    protected MetadataScope() {
        _genericContexts = new Stack<>();
    }

    protected void pushGenericContext(final IGenericParameterProvider provider) {
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

    protected void pushGenericContext(final IGenericContext context) {
        _genericContexts.push(VerifyArgument.notNull(context, "context"));
    }

    protected void popGenericContext() {
        _genericContexts.pop();
    }

    @Override
    public abstract TypeReference lookupType(final int token);

    @Override
    public abstract FieldReference lookupField(final int typeToken, final int fieldToken);

    @Override
    public abstract MethodReference lookupMethod(final int typeToken, final int methodToken);

    @Override
    public abstract <T> T lookupConstant(final int token);

    @Override
    public TypeReference lookupType(final String descriptor) {
        return parseSignature(descriptor);
    }

    @Override
    public abstract TypeReference lookupType(final String packageName, final String typeName);

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

    protected TypeReference parseSignature(final String signature) {
        VerifyArgument.notNullOrWhitespace(signature, "signature");
        return parseTopLevelSignature(signature, new MutableInteger());
    }

    private TypeReference parseTopLevelSignature(final String s, final MutableInteger position) {
        final int i = position.getValue();

        if (i >= s.length()) {
            throw Error.invalidSignatureTypeExpected(s, i);
        }

        switch (s.charAt(i)) {
            case '*':
                return Wildcard.unbounded();
            case '+':
                return Wildcard.makeExtends(parseTopLevelSignature(s, position.increment()));
            case '-':
                return Wildcard.makeSuper(parseTopLevelSignature(s, position.increment()));
            case '[':
                return parseTopLevelSignature(s, position.increment()).makeArrayType();
            case 'B':
                return BuiltinTypes.Byte;
            case 'C':
                return BuiltinTypes.Character;
            case 'D':
                return BuiltinTypes.Double;
            case 'F':
                return BuiltinTypes.Float;
            case 'I':
                return BuiltinTypes.Integer;
            case 'J':
                return BuiltinTypes.Long;
            case 'L':
                return finishTopLevelType(s, position);
            case 'S':
                return BuiltinTypes.Short;
            case 'T':
                throw Error.invalidSignatureTopLevelGenericParameterUnexpected(s, position.getValue());
            case 'V':
                return BuiltinTypes.Void;
            case 'Z':
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
                        parseTypeParameters(s, position, resolvedType, typeArguments);
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
        final TypeReference resolvedType,
        final TypeReference[] typeArguments) {

        int i = position.getValue();

        assert s.charAt(i) == '<';

        position.increment();

        for (int j = 0; j < typeArguments.length; j++) {
            typeArguments[j] = parseTypeArgument(s, position, resolvedType, j);
        }

        i = position.getValue();

        if (s.charAt(i) != '>') {
            throw Error.invalidSignatureExpectedEndOfTypeArguments(s, i);
        }

        position.increment();
    }

    private TypeReference parseTypeArgument(
        final String s,
        final MutableInteger position,
        final TypeReference genericType,
        final int typeArgumentIndex) {

        int i = position.getValue();

        if (i >= s.length()) {
            throw Error.invalidSignatureExpectedTypeArgument(s, i);
        }

        switch (s.charAt(i)) {
            case '*':
                return Wildcard.unbounded();
            case '+':
                return Wildcard.makeExtends(parseTypeArgument(s, position.increment(), genericType, typeArgumentIndex));
            case '-':
                return Wildcard.makeSuper(parseTypeArgument(s, position.increment(), genericType, typeArgumentIndex));
            case '[':
                return parseTypeArgument(s, position.increment(), genericType, typeArgumentIndex).makeArrayType();
            case 'L':
                return finishTopLevelType(s, position);
            case 'T':
                while (++i < s.length()) {
                    if (s.charAt(i) == ';') {
                        position.setValue(i + 1);
                        return genericType.getGenericParameters().get(typeArgumentIndex);
                    }
                }
                throw Error.invalidSignatureExpectedTypeArgument(s, position.getValue());
            default:
                throw Error.invalidSignatureUnexpectedToken(s, i);
        }
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
}
