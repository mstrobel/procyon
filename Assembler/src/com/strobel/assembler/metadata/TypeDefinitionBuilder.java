package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.MutableInteger;
import com.strobel.core.VerifyArgument;

import java.util.HashMap;

/**
 * @author Mike Strobel
 */
public class TypeDefinitionBuilder implements ClassVisitor<MutableTypeDefinition> {
    private final MetadataParser _parser;
    private final ResolverFrame _resolverFrame;

    public TypeDefinitionBuilder(final IMetadataResolver resolver) {
        _resolverFrame = new ResolverFrame();
        _parser = new MetadataParser(resolver);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void visit(
        final MutableTypeDefinition type,
        final int majorVersion,
        final int minorVersion,
        final int flags,
        final String name,
        final String genericSignature,
        final String baseTypeName,
        final String[] interfaceNames) {

        VerifyArgument.notNull(name, "name");

        type.setFlags(flags);

        final int delimiterIndex = name.lastIndexOf('/');

        if (delimiterIndex < 0) {
            type.setName(name);
        }
        else {
            type.setPackageName(name.substring(0, delimiterIndex).replace('/', '.'));
            type.setName(name.substring(delimiterIndex + 1));
        }

        _resolverFrame.addType(type);

        try {
            final TypeReference baseType;

            if (genericSignature != null) {
                final MutableInteger position = new MutableInteger(0);

                if (genericSignature.startsWith("<")) {
                    _parser.parseGenericParameters(type.genericParameters, genericSignature, position);
                }

                int genericContextCount = 0;

                TypeReference currentType = type;

                while (currentType != null) {
                    if (currentType.isGenericDefinition()) {
                        _parser.pushGenericContext(currentType);
                        ++genericContextCount;
                    }

                    if (currentType.isStatic()) {
                        break;
                    }

                    currentType = currentType.getDeclaringType();
                }

                try {
                    if (position.getValue() < genericSignature.length()) {
                        baseType = _parser.parseTypeSignature(genericSignature, position);

                        if (position.getValue() < genericSignature.length()) {
                            while (position.getValue() < genericSignature.length()) {
                                type.explicitInterfaces.add(_parser.parseTypeSignature(genericSignature, position));
                            }
                        }
                    }
                    else {
                        baseType = baseTypeName != null ? _parser.parseTypeDescriptor(baseTypeName) : null;

                        for (final String interfaceName : interfaceNames) {
                            type.explicitInterfaces.add(_parser.parseTypeDescriptor(interfaceName));
                        }
                    }
                }
                finally {
                    while (genericContextCount-- > 0) {
                        _parser.popGenericContext();
                    }
                }
            }
            else {
                baseType = baseTypeName != null ? _parser.parseTypeDescriptor(baseTypeName) : null;

                for (final String interfaceName : interfaceNames) {
                    type.explicitInterfaces.add(_parser.parseTypeDescriptor(interfaceName));
                }
            }

            type.setBaseType(baseType);
        }
        finally {
            _resolverFrame.removeType(type);
        }
    }

    @Override
    public void visitAttribute(final MutableTypeDefinition type, final SourceAttribute attribute) {
    }

    @Override
    public void visitAnnotation(final MutableTypeDefinition type, final CustomAnnotation annotation, final boolean visible) {
    }

    @Override
    public FieldVisitor<MutableTypeDefinition> visitField(
        final MutableTypeDefinition type,
        final int flags,
        final String name,
        final TypeReference fieldType) {

        return new FieldDefinitionBuilder(
            type,
            flags,
            name,
            fieldType
        );
    }

    @Override
    public MethodVisitor<MutableTypeDefinition> visitMethod(
        final MutableTypeDefinition type,
        final int flags,
        final String name,
        final IMethodSignature signature,
        final TypeReference... thrownTypes) {

        return new MethodDefinitionBuilder(
            type,
            flags,
            name,
            signature,
            thrownTypes
        );
    }

    @Override
    public void visitEnd(final MutableTypeDefinition type) {
        type.freezeIfUnfrozen();
    }

    // <editor-fold defaultstate="collapsed" desc="ResolverFrame Class">

    private final class ResolverFrame implements IResolverFrame {
        final HashMap<String, TypeReference> types = new HashMap<>();
        final HashMap<String, GenericParameter> typeVariables = new HashMap<>();

        public void addType(final TypeReference type) {
            VerifyArgument.notNull(type, "type");
            types.put(type.getInternalName(), type);
        }

        public void addTypeVariable(final GenericParameter type) {
            VerifyArgument.notNull(type, "type");
            typeVariables.put(type.getName(), type);
        }

        public void removeType(final TypeReference type) {
            VerifyArgument.notNull(type, "type");
            types.remove(type.getInternalName());
        }

        public void removeTypeVariable(final GenericParameter type) {
            VerifyArgument.notNull(type, "type");
            typeVariables.remove(type.getName());
        }

        @Override
        public TypeReference findType(final String descriptor) {
            final TypeReference type = types.get(descriptor);

            if (type != null) {
                return type;
            }

            return null;
        }

        @Override
        public TypeReference findTypeVariable(final String name) {
            final GenericParameter type = typeVariables.get(name);

            if (type != null) {
                return type;
            }

            return null;
        }
    }

    // </editor-fold>
}
