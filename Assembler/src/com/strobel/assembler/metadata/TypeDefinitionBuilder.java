/*
 * TypeDefinitionBuilder.java
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
import com.strobel.assembler.ir.ConstantPool;
import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.assembler.ir.attributes.InnerClassEntry;
import com.strobel.assembler.ir.attributes.InnerClassesAttribute;
import com.strobel.core.Comparer;
import com.strobel.core.MutableInteger;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;

import java.util.HashMap;

/**
 * @author Mike Strobel
 */
public class TypeDefinitionBuilder implements TypeVisitor {
    private ResolverFrame _resolverFrame;
    private TypeDefinition _typeDefinition;
    private MetadataParser _parser;
    private int _genericContextCount = 0;

    public TypeDefinitionBuilder() {
        _resolverFrame = new ResolverFrame();
    }

    TypeDefinitionBuilder(final TypeDefinition typeDefinition) {
        this();
        _typeDefinition = VerifyArgument.notNull(typeDefinition, "typeDefinition");
    }

    public TypeDefinition getTypeDefinition() {
        return _typeDefinition;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void visit(
        final int majorVersion,
        final int minorVersion,
        final long flags,
        final String name,
        final String genericSignature,
        final String baseTypeName,
        final String[] interfaceNames) {

        VerifyArgument.notNull(name, "name");

        if (_typeDefinition == null) {
            _typeDefinition = new TypeDefinition(MetadataSystem.instance());
        }

        _typeDefinition.setFlags(flags);
        _typeDefinition.setCompilerVersion(majorVersion, minorVersion);

        final int delimiterIndex = name.lastIndexOf('/');

        if (delimiterIndex < 0) {
            _typeDefinition.setName(name);
        }
        else {
            _typeDefinition.setPackageName(name.substring(0, delimiterIndex).replace('/', '.'));
            _typeDefinition.setName(name.substring(delimiterIndex + 1));
        }

        _resolverFrame.addType(_typeDefinition);

        final TypeReference baseType;
        final Collection<TypeReference> explicitInterfaces = _typeDefinition.getExplicitInterfacesInternal();

        if (genericSignature != null) {
            final MutableInteger position = new MutableInteger(0);

            if (genericSignature.startsWith("<")) {
                _parser.parseGenericParameters(_typeDefinition.getGenericParametersInternal(), genericSignature, position);

                for (final GenericParameter genericParameter : _typeDefinition.getGenericParametersInternal()) {
                    genericParameter.setDeclaringType(_typeDefinition);
                    _resolverFrame.addTypeVariable(genericParameter);
                }
            }

            _genericContextCount = pushGenericContexts();

            if (position.getValue() < genericSignature.length()) {
                baseType = _parser.parseTypeSignature(genericSignature, position);

                if (position.getValue() < genericSignature.length()) {
                    while (position.getValue() < genericSignature.length()) {
                        explicitInterfaces.add(_parser.parseTypeSignature(genericSignature, position));
                    }
                }
            }
            else {
                baseType = baseTypeName != null ? _parser.parseTypeDescriptor(baseTypeName) : null;

                for (final String interfaceName : interfaceNames) {
                    explicitInterfaces.add(_parser.parseTypeDescriptor(interfaceName));
                }
            }
        }
        else {
            baseType = baseTypeName != null ? _parser.parseTypeDescriptor(baseTypeName) : null;

            for (final String interfaceName : interfaceNames) {
                explicitInterfaces.add(_parser.parseTypeDescriptor(interfaceName));
            }
        }

        _typeDefinition.setBaseType(baseType);
    }

    @Override
    public void visitDeclaringMethod(final MethodReference method) {
        if (method == null) {
            return;
        }

        final MethodDefinition resolvedMethod = method instanceof MethodDefinition
                                                ? (MethodDefinition) method
                                                : method.resolve();

        if (resolvedMethod != null) {
            if (!resolvedMethod.getDeclaredTypesInternal().contains(_typeDefinition)) {
                resolvedMethod.getDeclaredTypesInternal().add(_typeDefinition);
            }
        }
        else {
            _typeDefinition.setDeclaringMethod(method);
        }

        if (method.hasGenericParameters()) {
            _parser.pushGenericContext(method);
            ++_genericContextCount;
        }
    }

    private int pushGenericContexts() {
        int genericContextCount = 0;
        TypeReference currentType = _typeDefinition;

        while (currentType != null) {
            if (currentType.isGenericDefinition()) {
                _parser.pushGenericContext(currentType);
                ++genericContextCount;
            }

            final TypeDefinition currentTypeDefinition = currentType.resolve();

            if (currentTypeDefinition != null &&
                currentTypeDefinition.isStatic()) {

                break;
            }

            currentType = currentType.getDeclaringType();
        }

        return genericContextCount;
    }

    private void popGenericContexts() {
        while (_genericContextCount-- > 0) {
            _parser.popGenericContext();
        }
    }

    @Override
    public void visitParser(final MetadataParser parser) {
        if (_typeDefinition == null) {
            _typeDefinition = new TypeDefinition(parser.getResolver());
        }
        _parser = VerifyArgument.notNull(parser, "parser");
        _parser.getResolver().pushFrame(_resolverFrame);
    }

    @Override
    public void visitOuterType(final TypeReference type) {
        popGenericContexts();
        _typeDefinition.setDeclaringType(type);
        pushGenericContexts();
    }

    @Override
    public void visitInnerType(final TypeDefinition type) {
        final Collection<TypeDefinition> declaredTypes = _typeDefinition.getDeclaredTypesInternal();

        if (!declaredTypes.contains(type)) {
            declaredTypes.add(type);
        }
    }

    @Override
    public void visitAttribute(final SourceAttribute attribute) {
        _typeDefinition.getSourceAttributesInternal().add(attribute);

        switch (attribute.getName()) {
            case AttributeNames.InnerClasses: {
                final InnerClassesAttribute innerClasses = (InnerClassesAttribute) attribute;

                for (final InnerClassEntry entry : innerClasses.getEntries()) {
//                    final String outerClassName = entry.getOuterClassName();

//                    if (outerClassName != null) {
//                        continue;
//                    }

                    final String innerClassName = entry.getInnerClassName();

                    if (Comparer.equals(innerClassName, _typeDefinition.getInternalName())) {
                        final String simpleName = entry.getShortName();

                        if (!StringUtilities.isNullOrEmpty(simpleName)) {
                            _typeDefinition.setSimpleName(simpleName);
                        }
                        else {
                            _typeDefinition.setFlags(_typeDefinition.getFlags() | Flags.ANONYMOUS);
                        }
                        return;
                    }
                }

                break;
            }
        }
    }

    @Override
    public void visitAnnotation(final CustomAnnotation annotation, final boolean visible) {
        _typeDefinition.getAnnotationsInternal().add(annotation);
    }

    @Override
    public FieldVisitor visitField(
        final long flags,
        final String name,
        final TypeReference fieldType) {

        return new FieldDefinitionBuilder(
            _typeDefinition,
            flags,
            name,
            fieldType
        );
    }

    @Override
    public MethodVisitor visitMethod(
        final long flags,
        final String name,
        final IMethodSignature signature,
        final TypeReference... thrownTypes) {

        final MethodDefinitionBuilder builder = new MethodDefinitionBuilder(
            _typeDefinition,
            flags,
            name,
            signature,
            thrownTypes
        );

        _typeDefinition.getDeclaredMethodsInternal().add(builder.getMethod());

        return builder;
    }

    @Override
    public ConstantPool.Visitor visitConstantPool() {
        return ConstantPool.Visitor.EMPTY;
    }

    @Override
    public void visitEnd() {
        try {
            if (_resolverFrame != null) {
                _resolverFrame = null;
                _parser.getResolver().popFrame();
            }
        }
        finally {
            popGenericContexts();
        }
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
            final GenericParameter typeVariable = typeVariables.get(name);

            if (typeVariable != null) {
                return typeVariable;
            }

            for (final String typeName : types.keySet()) {
                final TypeReference t = types.get(typeName);

                if (t.containsGenericParameters()) {
                    for (final GenericParameter p : t.getGenericParameters()) {
                        if (StringUtilities.equals(p.getName(), name)) {
                            return p;
                        }
                    }
                }
            }

            return null;
        }
    }

    // </editor-fold>
}
