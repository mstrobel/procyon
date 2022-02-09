/*
 * AstBuilder.java
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

import com.strobel.assembler.ir.attributes.AnnotationDefaultAttribute;
import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.LineNumberTableAttribute;
import com.strobel.assembler.ir.attributes.ModuleAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.*;
import com.strobel.assembler.metadata.UnionType;
import com.strobel.assembler.metadata.annotations.*;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.Closeables;
import com.strobel.core.MutableInteger;
import com.strobel.core.Predicate;
import com.strobel.core.SafeCloseable;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.ast.TypeAnalysis;
import com.strobel.decompiler.languages.LineNumberPosition;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;
import com.strobel.decompiler.languages.java.ast.transforms.TransformationPipeline;
import com.strobel.util.ContractUtils;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;

import static com.strobel.core.CollectionUtilities.first;

public final class AstBuilder {
    private final DecompilerContext _context;
    private final CompilationUnit _compileUnit = new CompilationUnit();
    private final Map<String, Reference<TypeDeclaration>> _typeDeclarations = new LinkedHashMap<>();
    private final Map<String, String> _unqualifiedTypeNames = new LinkedHashMap<>();
    private final TextNode _packagePlaceholder;

    private boolean _decompileMethodBodies = true;
    private boolean _haveTransformationsRun;
    private int _suppressImportsDepth;

    public AstBuilder(final DecompilerContext context) {
        _context = VerifyArgument.notNull(context, "context");

        final String headerText = context.getSettings().getOutputFileHeaderText();

        if (!StringUtilities.isNullOrWhitespace(headerText)) {
            final List<String> lines = StringUtilities.split(headerText, false, '\n');

            for (final String line : lines) {
                _compileUnit.addChild(new Comment(" " + line.trim(), CommentType.SingleLine), Roles.COMMENT);
            }

            _compileUnit.addChild(new UnixNewLine(), Roles.NEW_LINE);
        }

        _packagePlaceholder = new TextNode();
        _compileUnit.addChild(_packagePlaceholder, Roles.TEXT);

        if (_context.getUserData(Keys.AST_BUILDER) == null) {
            _context.putUserData(Keys.AST_BUILDER, this);
        }
    }

    final DecompilerContext getContext() {
        return _context;
    }

    public final boolean areImportsSuppressed() {
        return _suppressImportsDepth > 0;
    }

    public final SafeCloseable suppressImports() {
        ++_suppressImportsDepth;

        return Closeables.create(
            new Runnable() {
                @Override
                public void run() {
                    --_suppressImportsDepth;
                }
            }
        );
    }

    public final boolean getDecompileMethodBodies() {
        return _decompileMethodBodies;
    }

    public final void setDecompileMethodBodies(final boolean decompileMethodBodies) {
        _decompileMethodBodies = decompileMethodBodies;
    }

    public final CompilationUnit getCompilationUnit() {
        return _compileUnit;
    }

    public final void runTransformations() {
        runTransformations(null);
    }

    public final void runTransformations(final Predicate<IAstTransform> transformAbortCondition) {
        TransformationPipeline.runTransformationsUntil(_compileUnit, transformAbortCondition, _context);
        _compileUnit.acceptVisitor(new InsertParenthesesVisitor(), null);
        _haveTransformationsRun = true;
    }

    public final void addType(final TypeDefinition type) {
        if (type.isModule()) {
            final ModuleAttribute attribute = SourceAttribute.find(AttributeNames.Module, type.getSourceAttributes());

            if (attribute != null) {
                _compileUnit.addChild(createModuleNoCache(type, attribute), CompilationUnit.MODULE_ROLE);
                return;
            }
        }

        final EntityDeclaration astType = createType(type);
        final String packageName = type.getPackageName();

        if (_compileUnit.getPackage().isNull() && !StringUtilities.isNullOrWhitespace(packageName)) {
            _compileUnit.insertChildBefore(
                _packagePlaceholder,
                new PackageDeclaration(packageName),
                Roles.PACKAGE
            );
            _packagePlaceholder.remove();
        }

        _compileUnit.addChild(astType, CompilationUnit.TYPE_ROLE);
    }

    public final TypeDeclaration createType(final TypeDefinition type) {
        VerifyArgument.notNull(type, "type");

        final Reference<TypeDeclaration> existingDeclaration = _typeDeclarations.get(type.getInternalName());
        final TypeDeclaration d;

        if (existingDeclaration != null && (d = existingDeclaration.get()) != null) {
            return d;
        }

        return createTypeNoCache(type);
    }

    protected final ModuleDeclaration createModuleNoCache(final TypeDefinition type, final ModuleAttribute attribute) {
        VerifyArgument.notNull(type, "type");

        final TypeDefinition oldCurrentType = _context.getCurrentType();

        _context.setCurrentType(type);

        try {
            final ModuleDeclaration declaration = new ModuleDeclaration();

            declaration.setName(attribute.getModuleName());
            declaration.putUserData(Keys.TYPE_DEFINITION, type);
            declaration.putUserData(Keys.MODULE_REFERENCE, new ModuleReference(attribute.getModuleName(), attribute.getVersion()));

            return declaration;

        }
        finally {
            _context.setCurrentType(oldCurrentType);
        }
    }

    protected final TypeDeclaration createTypeNoCache(final TypeDefinition type) {
        VerifyArgument.notNull(type, "type");

        final TypeDefinition oldCurrentType = _context.getCurrentType();

        _context.setCurrentType(type/*typeWithCode*/);

        try {
            return createTypeCore(type/*typeWithCode*/);
        }
        finally {
            _context.setCurrentType(oldCurrentType);
        }
    }

    public AstType convertType(final TypeReference type) {
        return convertType(type, new ConvertTypeOptions());
    }

    public AstType convertType(final TypeReference type, final ConvertTypeOptions options) {
        return convertType(type, new MutableInteger(0), options);
    }

    public final List<ParameterDeclaration> createParameters(final Iterable<ParameterDefinition> parameters) {
        final List<ParameterDeclaration> declarations = new ArrayList<>();

        for (final ParameterDefinition p : parameters) {
            final TypeReference type = p.getParameterType();
            final AstType astType = convertType(type);
            final ParameterDeclaration d = new ParameterDeclaration(p.getName(), astType);

            d.putUserData(Keys.PARAMETER_DEFINITION, p);

            for (final CustomAnnotation annotation : p.getAnnotations()) {
                d.getAnnotations().add(createAnnotation(annotation));
            }

            declarations.add(d);

            if (p.isFinal()) {
                EntityDeclaration.addModifier(d, Flags.Flag.FINAL);
            }
        }

        return Collections.unmodifiableList(declarations);
    }

    final AstType convertType(final TypeReference type, final MutableInteger typeIndex, final ConvertTypeOptions options) {
        if (type == null) {
            return AstType.NULL;
        }

        if (type instanceof ICompoundType) {
            final ICompoundType cType = (ICompoundType) type;

            if (options.getIncludeIntersectionTypes()) {
                final List<TypeReference> ifReferences = cType.getInterfaces();

                final AstType baseType = cType.getBaseType() == BuiltinTypes.Null ? AstType.NULL
                                                                                  : convertType(cType.getBaseType(), typeIndex, options);
                final AstType[] ifTypes = new AstType[ifReferences.size()];

                for (int i = 0; i < ifReferences.size(); i++) {
                    ifTypes[i] = convertType(ifReferences.get(i), typeIndex, options);
                }

                final IntersectionType isType = new IntersectionType(baseType, ifTypes);
                isType.putUserData(Keys.TYPE_REFERENCE, type);
                return isType;
            }

            return convertType(cType.getBaseType(), typeIndex, options).makeArrayType();
        }

        if (type instanceof UnionType) {
            final UnionType uType = (UnionType) type;

            if (options.getIncludeUnionTypes()) {
                final List<TypeReference> alternatives = uType.getAlternatives();
                final AstType[] astAlternatives = new AstType[alternatives.size()];

                for (int i = 0; i < alternatives.size(); i++) {
                    astAlternatives[i] = convertType(alternatives.get(i), typeIndex, options);
                }

                final com.strobel.decompiler.languages.java.ast.UnionType isType = new com.strobel.decompiler.languages.java.ast.UnionType(astAlternatives);
                isType.putUserData(Keys.TYPE_REFERENCE, type);
                return isType;
            }

            return convertType(first(uType.getAlternatives()), typeIndex, options).makeArrayType();
        }

        if (type.isArray()) {
            return convertType(type.getElementType(), typeIndex.increment(), options).makeArrayType();
        }

        if (type.isGenericParameter()) {
            final SimpleType simpleType = new SimpleType(type.getSimpleName());
            simpleType.putUserData(Keys.TYPE_REFERENCE, type);
            return simpleType;
        }

        if (type.isPrimitive()) {
            final SimpleType simpleType = new SimpleType(type.getSimpleName());
            simpleType.putUserData(Keys.TYPE_REFERENCE, type.resolve());
            return simpleType;
        }

        if (type.isWildcardType()) {
            if (!options.getAllowWildcards()) {
                if (type.hasExtendsBound()) {
                    return convertType(type.getExtendsBound(), options);
                }
                return convertType(BuiltinTypes.Object, options);
            }

            final WildcardType wildcardType = new WildcardType();

            if (type.hasExtendsBound()) {
                wildcardType.addChild(convertType(type.getExtendsBound()), Roles.EXTENDS_BOUND);
            }
            else if (type.hasSuperBound()) {
                wildcardType.addChild(convertType(type.getSuperBound()), Roles.SUPER_BOUND);
            }

            wildcardType.putUserData(Keys.TYPE_REFERENCE, type);
            return wildcardType;
        }

        final boolean includeTypeArguments = options == null || options.getIncludeTypeArguments();
        final boolean includeTypeParameterDefinitions = options == null || options.getIncludeTypeParameterDefinitions();
        final boolean allowWildcards = options == null || options.getAllowWildcards();

        if (type instanceof IGenericInstance && includeTypeArguments) {
            final AstType baseType;
            final IGenericInstance genericInstance = (IGenericInstance) type;

            if (options != null) {
                options.setIncludeTypeParameterDefinitions(false);
            }

            try {
                baseType = convertType(
                    (TypeReference) genericInstance.getGenericDefinition(),
                    typeIndex.increment(),
                    options
                );
            }
            finally {
                if (options != null) {
                    options.setIncludeTypeParameterDefinitions(includeTypeParameterDefinitions);
                }
            }

            if (options != null) {
                options.setAllowWildcards(true);
            }

            final List<AstType> typeArguments = new ArrayList<>();

            try {
                for (final TypeReference typeArgument : genericInstance.getTypeArguments()) {
                    typeArguments.add(convertType(typeArgument, typeIndex.increment(), options));
                }
            }
            finally {
                if (options != null) {
                    options.setAllowWildcards(allowWildcards);
                }
            }

            applyTypeArguments(baseType, typeArguments);
            baseType.putUserData(Keys.TYPE_REFERENCE, type);

            return baseType;
        }

        String name = null;

        final PackageDeclaration packageDeclaration = _compileUnit.getPackage();

        final TypeDefinition resolvedType = type.resolve();
        final TypeReference nameSource = resolvedType != null ? resolvedType : type;

        if (options == null || options.getIncludePackage()) {
            final String packageName = nameSource.getPackageName();
            name = StringUtilities.isNullOrEmpty(packageName) ? nameSource.getSimpleName()
                                                              : packageName + "." + nameSource.getSimpleName();
        }
//        else if (packageDeclaration != null &&
//                 StringUtilities.equals(packageDeclaration.getName(), nameSource.getPackageName())) {
//
//            String unqualifiedName = nameSource.getSimpleName();
//            TypeReference current = nameSource;
//
//            while (current.isNested()) {
//                current = current.getDeclaringType();
//
//                if (isContextWithinType(current)) {
//                    break;
//                }
//
//                unqualifiedName = current.getSimpleName() + "." + unqualifiedName;
//            }
//
//            name = unqualifiedName;
//        }
        else {
            final TypeReference typeToImport;

            StringBuilder unqualifiedName;

            if (packageDeclaration != null &&
                StringUtilities.equals(packageDeclaration.getName(), nameSource.getPackageName())) {

                unqualifiedName = new StringBuilder(nameSource.getSimpleName());
                name = unqualifiedName.toString();
            }

            if (nameSource.isNested()) {
                unqualifiedName = new StringBuilder(nameSource.getSimpleName());

                TypeReference current = nameSource;

                while (current.isNested()) {
                    current = current.getDeclaringType();

                    if (isContextWithinType(current)) {
                        break;
                    }

                    unqualifiedName.insert(0, current.getSimpleName() + ".");
                }

                name = unqualifiedName.toString();
                typeToImport = current;
            }
            else {
                typeToImport = nameSource;
                unqualifiedName = new StringBuilder(nameSource.getSimpleName());
            }

            if (options.getAddImports() && !areImportsSuppressed() && !_typeDeclarations.containsKey(typeToImport.getInternalName())) {
                String importedName = _unqualifiedTypeNames.get(typeToImport.getSimpleName());

                if (importedName == null) {
                    final SimpleType importedType = new SimpleType(typeToImport.getFullName());

                    importedType.putUserData(Keys.TYPE_REFERENCE, typeToImport);

                    if (packageDeclaration != null) {
                        _compileUnit.insertChildAfter(
                            packageDeclaration,
                            new ImportDeclaration(importedType),
                            CompilationUnit.IMPORT_ROLE
                        );
                    }
                    else {
                        _compileUnit.getImports().add(new ImportDeclaration(importedType));
                    }

                    _unqualifiedTypeNames.put(typeToImport.getSimpleName(), typeToImport.getFullName());
                    importedName = typeToImport.getFullName();
                }

                if (name == null) {
                    if (importedName.equals(typeToImport.getFullName())) {
                        name = unqualifiedName.toString();
                    }
                    else {
                        final String packageName = nameSource.getPackageName();
                        name = StringUtilities.isNullOrEmpty(packageName) ? nameSource.getSimpleName()
                                                                          : packageName + "." + nameSource.getSimpleName();
                    }
                }
            }
            else if (name != null) {
                name = nameSource.getSimpleName();
            }
        }

        if (name == null) {
            name = nameSource.getSimpleName();
        }

        final SimpleType astType = new SimpleType(name);

        astType.putUserData(Keys.TYPE_REFERENCE, type);

/*
        if (nameSource.isGenericType() && includeTypeParameterDefinitions) {
            addTypeArguments(nameSource, astType);
        }
*/

        return astType;
    }

    private boolean isContextWithinType(final TypeReference type) {
        final TypeReference scope = _context.getCurrentType();

        for (TypeReference current = scope;
             current != null;
             current = current.getDeclaringType()) {

            if (MetadataResolver.areEquivalent(current, type)) {
                return true;
            }

            final TypeDefinition resolved = current.resolve();

            if (resolved != null) {
                TypeReference baseType = resolved.getBaseType();

                while (baseType != null) {
                    if (MetadataResolver.areEquivalent(baseType, type)) {
                        return true;
                    }

                    final TypeDefinition resolvedBaseType = baseType.resolve();

                    baseType = resolvedBaseType != null ? resolvedBaseType.getBaseType()
                                                        : null;
                }

                for (final TypeReference ifType : MetadataHelper.getInterfaces(current)) {
                    if (MetadataResolver.areEquivalent(ifType, type)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private TypeDeclaration createTypeCore(final TypeDefinition type) {
        final TypeDeclaration astType = new TypeDeclaration();
        final String packageName = type.getPackageName();

        if (_compileUnit.getPackage().isNull() && !StringUtilities.isNullOrWhitespace(packageName)) {
            final PackageDeclaration packageDeclaration = new PackageDeclaration(packageName);

            packageDeclaration.putUserData(Keys.PACKAGE_REFERENCE, PackageReference.parse(packageName));

            _compileUnit.insertChildBefore(
                _packagePlaceholder,
                packageDeclaration,
                Roles.PACKAGE
            );

            _packagePlaceholder.remove();
        }

        _typeDeclarations.put(type.getInternalName(), new SoftReference<>(astType));

        long flags = type.getFlags();

        if (type.isInterface()) {
            flags &= Flags.AccessFlags | Flags.ExtendedInterfaceFlags;
        }
        else if (type.isEnum()) {
            flags &= Flags.AccessFlags;
        }
        else {
            flags &= (Flags.AccessFlags | Flags.ExtendedClassFlags | Flags.STATIC | Flags.FINAL);
        }

        EntityDeclaration.setModifiers(
            astType,
            Flags.asFlagSet(scrubAccessModifiers(flags))
        );

        astType.setName(type.getSimpleName());
        astType.putUserData(Keys.TYPE_DEFINITION, type);
        astType.putUserData(Keys.TYPE_REFERENCE, type);

        if (type.isEnum()) {
            astType.setClassType(ClassType.ENUM);
        }
        else if (type.isAnnotation()) {
            astType.setClassType(ClassType.ANNOTATION);
        }
        else if (type.isInterface()) {
            astType.setClassType(ClassType.INTERFACE);
        }
        else {
            astType.setClassType(ClassType.CLASS);
        }

        final List<TypeParameterDeclaration> typeParameters = createTypeParameters(type.getGenericParameters());

        if (!typeParameters.isEmpty()) {
            astType.getTypeParameters().addAll(typeParameters);
        }

        final TypeReference baseType = type.getBaseType();

        if (baseType != null && !type.isEnum() && !BuiltinTypes.Object.equals(baseType)) {
            astType.addChild(convertType(baseType), Roles.BASE_TYPE);
        }

        for (final TypeReference interfaceType : type.getExplicitInterfaces()) {
            if (type.isAnnotation() && CommonTypeReferences.Annotation.isEquivalentTo(interfaceType)) {
                continue;
            }
            astType.addChild(convertType(interfaceType), Roles.IMPLEMENTED_INTERFACE);
        }

        for (final TypeReference permittedSubclass : type.getPermittedSubclasses()) {
            astType.addChild(convertType(permittedSubclass), Roles.PERMITTED_SUBCLASSES);
        }

        for (final CustomAnnotation annotation : type.getAnnotations()) {
            astType.getAnnotations().add(createAnnotation(annotation));
        }

        addTypeMembers(astType, type);

        return astType;
    }

    private long scrubAccessModifiers(final long flags) {
        final long result = flags & ~Flags.AccessFlags;

        if ((flags & Flags.PRIVATE) != 0) {
            return result | Flags.PRIVATE;
        }

        if ((flags & Flags.PROTECTED) != 0) {
            return result | Flags.PROTECTED;
        }

        if ((flags & Flags.PUBLIC) != 0) {
            return result | Flags.PUBLIC;
        }

        return result;
    }

    private void addTypeMembers(final TypeDeclaration astType, final TypeDefinition type) {
        for (final FieldDefinition field : type.getDeclaredFields()) {
            astType.addChild(createField(field), Roles.TYPE_MEMBER);
        }

        for (final MethodDefinition method : type.getDeclaredMethods()) {
            if (method.isConstructor()) {
                astType.addChild(createConstructor(method), Roles.TYPE_MEMBER);
            }
            else {
                astType.addChild(createMethod(method), Roles.TYPE_MEMBER);
            }
        }

        final List<TypeDefinition> nestedTypes = new ArrayList<>();

        for (final TypeDefinition nestedType : type.getDeclaredTypes()) {
            final TypeReference declaringType = nestedType.getDeclaringType();

            if (!nestedType.isLocalClass() &&
                type.isEquivalentTo(declaringType)) {

                if (nestedType.isAnonymous()) {
                    _typeDeclarations.put(type.getInternalName(), new SoftReference<>(astType));
                }
                else {
                    nestedTypes.add(nestedType);
                }
            }
        }

        sortNestedTypes(nestedTypes);

        for (final TypeDefinition nestedType : nestedTypes) {
            astType.addChild(createTypeNoCache(nestedType), Roles.TYPE_MEMBER);
        }
    }

    private static void sortNestedTypes(final List<TypeDefinition> types) {
        final IdentityHashMap<TypeDefinition, Integer> minOffsets = new IdentityHashMap<>();

        for (final TypeDefinition type : types) {
            minOffsets.put(type, findFirstLineNumber(type));
        }

        Collections.sort(
            types,
            new Comparator<TypeDefinition>() {
                @Override
                public int compare(final TypeDefinition o1, final TypeDefinition o2) {
                    return Integer.compare(minOffsets.get(o1), minOffsets.get(o2));
                }
            }
        );
    }

    private static Integer findFirstLineNumber(final TypeDefinition type) {
        int minLineNumber = Integer.MAX_VALUE;

        for (final MethodDefinition method : type.getDeclaredMethods()) {
            final LineNumberTableAttribute attribute = SourceAttribute.find(AttributeNames.LineNumberTable, method.getSourceAttributes());

            if (attribute != null && !attribute.getEntries().isEmpty()) {
                final int firstLineNumber = attribute.getEntries().get(0).getLineNumber();

                if (firstLineNumber < minLineNumber) {
                    minLineNumber = firstLineNumber;
                }
            }
        }

        return minLineNumber;
    }

    private FieldDeclaration createField(final FieldDefinition field) {
        final FieldDeclaration astField = new FieldDeclaration();
        final VariableInitializer initializer = new VariableInitializer(field.getName());

        astField.setName(field.getName());
        astField.addChild(initializer, Roles.VARIABLE);
        astField.setReturnType(convertType(field.getFieldType()));
        astField.putUserData(Keys.FIELD_DEFINITION, field);
        astField.putUserData(Keys.MEMBER_REFERENCE, field);

        EntityDeclaration.setModifiers(
            astField,
            Flags.asFlagSet(scrubAccessModifiers(field.getFlags() & Flags.VarFlags))
        );

        if (field.hasConstantValue()) {
            initializer.setInitializer(new PrimitiveExpression(Expression.MYSTERY_OFFSET, field.getConstantValue()));
            initializer.putUserData(Keys.FIELD_DEFINITION, field);
            initializer.putUserData(Keys.MEMBER_REFERENCE, field);
        }

        for (final CustomAnnotation annotation : field.getAnnotations()) {
            astField.getAnnotations().add(createAnnotation(annotation));
        }

        return astField;
    }

    public final MethodDeclaration createMethod(final MethodDefinition method) {
        final MethodDeclaration astMethod = new MethodDeclaration();

        final Set<Flags.Flag> modifiers;

        if (method.isTypeInitializer()) {
            modifiers = Collections.singleton(Flags.Flag.STATIC);
        }
        else if (method.getDeclaringType().isInterface()) {
            modifiers = Collections.emptySet();
        }
        else {
            modifiers = Flags.asFlagSet(scrubAccessModifiers(method.getFlags() & Flags.MethodFlags));
        }

        EntityDeclaration.setModifiers(astMethod, modifiers);

        astMethod.setName(method.getName());
        astMethod.getParameters().addAll(createParameters(method.getParameters()));
        astMethod.getTypeParameters().addAll(createTypeParameters(method.getGenericParameters()));
        astMethod.setReturnType(convertType(method.getReturnType()));
        astMethod.putUserData(Keys.METHOD_DEFINITION, method);
        astMethod.putUserData(Keys.MEMBER_REFERENCE, method);

        for (final TypeDefinition declaredType : method.getDeclaredTypes()) {
            if (!declaredType.isAnonymous()) {
                astMethod.getDeclaredTypes().add(createType(declaredType));
            }
        }

        if (!method.getDeclaringType().isInterface() || method.isTypeInitializer() || method.isDefault()) {
            astMethod.setBody(createMethodBody(method, astMethod.getParameters()));
        }

        for (final TypeReference thrownType : method.getThrownTypes()) {
            astMethod.addChild(convertType(thrownType), Roles.THROWN_TYPE);
        }

        for (final CustomAnnotation annotation : method.getAnnotations()) {
            astMethod.getAnnotations().add(createAnnotation(annotation));
        }

        final AnnotationDefaultAttribute defaultAttribute = SourceAttribute.find(
            AttributeNames.AnnotationDefault,
            method.getSourceAttributes()
        );

        if (defaultAttribute != null) {
            final Expression defaultValue = createAnnotationElement(defaultAttribute.getDefaultValue());

            if (defaultValue != null && !defaultValue.isNull()) {
                astMethod.setDefaultValue(defaultValue);
            }
        }

        return astMethod;
    }

    private ConstructorDeclaration createConstructor(final MethodDefinition method) {
        final ConstructorDeclaration astMethod = new ConstructorDeclaration();

        EntityDeclaration.setModifiers(
            astMethod,
            Flags.asFlagSet(scrubAccessModifiers(method.getFlags() & Flags.ConstructorFlags))
        );

        astMethod.setName(method.getDeclaringType().getName());
        astMethod.getParameters().addAll(createParameters(method.getParameters()));
        astMethod.getTypeParameters().addAll(createTypeParameters(method.getGenericParameters()));
        astMethod.setBody(createMethodBody(method, astMethod.getParameters()));
        astMethod.putUserData(Keys.METHOD_DEFINITION, method);
        astMethod.putUserData(Keys.MEMBER_REFERENCE, method);

        for (final CustomAnnotation annotation : method.getAnnotations()) {
            astMethod.getAnnotations().add(createAnnotation(annotation));
        }

        for (final TypeReference thrownType : method.getThrownTypes()) {
            astMethod.addChild(convertType(thrownType), Roles.THROWN_TYPE);
        }

        return astMethod;
    }

    final List<TypeParameterDeclaration> createTypeParameters(final List<GenericParameter> genericParameters) {
        if (genericParameters.isEmpty()) {
            return Collections.emptyList();
        }

        final int count = genericParameters.size();
        final TypeParameterDeclaration[] typeParameters = new TypeParameterDeclaration[genericParameters.size()];

        for (int i = 0; i < count; i++) {
            final GenericParameter genericParameter = genericParameters.get(i);
            final TypeParameterDeclaration typeParameter = new TypeParameterDeclaration(genericParameter.getName());

            if (genericParameter.hasExtendsBound()) {
                typeParameter.setExtendsBound(convertType(genericParameter.getExtendsBound()));
            }

            typeParameter.putUserData(Keys.TYPE_REFERENCE, genericParameter);
            typeParameter.putUserData(Keys.TYPE_DEFINITION, genericParameter);
            typeParameters[i] = typeParameter;
        }

        return ArrayUtilities.asUnmodifiableList(typeParameters);
    }

    static void addTypeArguments(final TypeReference type, final AstType astType) {
        if (type.hasGenericParameters()) {
            final List<GenericParameter> genericParameters = type.getGenericParameters();
            final int count = genericParameters.size();
            final AstType[] typeArguments = new AstType[count];

            for (int i = 0; i < count; i++) {
                final GenericParameter genericParameter = genericParameters.get(i);
                final SimpleType typeParameter = new SimpleType(genericParameter.getName());

                typeParameter.putUserData(Keys.TYPE_REFERENCE, genericParameter);
                typeArguments[i] = typeParameter;
            }

            applyTypeArguments(astType, ArrayUtilities.asUnmodifiableList(typeArguments));
        }
    }

    static void applyTypeArguments(final AstType baseType, final List<AstType> typeArguments) {
        if (baseType instanceof SimpleType) {
            final SimpleType st = (SimpleType) baseType;
            st.getTypeArguments().addAll(typeArguments);
        }
    }

    private BlockStatement createMethodBody(
        final MethodDefinition method,
        final Iterable<ParameterDeclaration> parameters) {

        if (_decompileMethodBodies) {
            return AstMethodBodyBuilder.createMethodBody(this, method, _context, parameters);
        }

        return null;
    }

    public static Expression makePrimitive(final long val, final TypeReference type) {
        if (TypeAnalysis.isBoolean(type)) {
            if (val == 0L) {
                return new PrimitiveExpression(Expression.MYSTERY_OFFSET, Boolean.FALSE);
            }
            return new PrimitiveExpression(Expression.MYSTERY_OFFSET, Boolean.TRUE);
        }

        if (type != null) {
            return new PrimitiveExpression(Expression.MYSTERY_OFFSET, JavaPrimitiveCast.cast(type.getSimpleType(), val));
        }

        return new PrimitiveExpression(Expression.MYSTERY_OFFSET, JavaPrimitiveCast.cast(JvmType.Integer, val));
    }

    public static Expression makeDefaultValue(final TypeReference type) {
        if (type == null) {
            return new NullReferenceExpression(Expression.MYSTERY_OFFSET);
        }

        switch (type.getSimpleType()) {
            case Boolean:
                return new PrimitiveExpression(Expression.MYSTERY_OFFSET, Boolean.FALSE);

            case Byte:
                return new PrimitiveExpression(Expression.MYSTERY_OFFSET, (byte) 0);

            case Character:
                return new PrimitiveExpression(Expression.MYSTERY_OFFSET, '\0');

            case Short:
                return new PrimitiveExpression(Expression.MYSTERY_OFFSET, (short) 0);

            case Integer:
                return new PrimitiveExpression(Expression.MYSTERY_OFFSET, 0);

            case Long:
                return new PrimitiveExpression(Expression.MYSTERY_OFFSET, 0L);

            case Float:
                return new PrimitiveExpression(Expression.MYSTERY_OFFSET, 0f);

            case Double:
                return new PrimitiveExpression(Expression.MYSTERY_OFFSET, 0d);

            default:
                return new NullReferenceExpression(Expression.MYSTERY_OFFSET);
        }
    }

    public List<LineNumberPosition> generateCode(final ITextOutput output) {
        if (!_haveTransformationsRun) {
            runTransformations();
        }

        final JavaOutputVisitor visitor = new JavaOutputVisitor(output, _context.getSettings());
        _compileUnit.acceptVisitor(visitor, null);
        return visitor.getLineNumberPositions();
    }

    public static boolean isMemberHidden(final IMemberDefinition member, final DecompilerContext context) {
        final DecompilerSettings settings = context.getSettings();

        if (member.isSynthetic() && !settings.getShowSyntheticMembers()) {
            return !context.getForcedVisibleMembers().contains(member);
        }

        if (member instanceof TypeReference &&
            ((TypeReference) member).isNested() &&
            settings.getExcludeNestedTypes()) {

            final TypeDefinition resolvedType = ((TypeReference) member).resolve();

            return resolvedType == null ||
                   !resolvedType.isAnonymous() && findLocalType(resolvedType) == null;
        }

        return false;
    }

    private static TypeReference findLocalType(final TypeReference type) {
        if (type != null) {
            final TypeDefinition resolvedType = type.resolve();

            if (resolvedType != null && resolvedType.isLocalClass()) {
                return resolvedType;
            }

            final TypeReference declaringType = type.getDeclaringType();

            if (declaringType != null) {
                return findLocalType(declaringType);
            }
        }

        return null;
    }

    public Annotation createAnnotation(final CustomAnnotation annotation) {
        final Annotation a = new Annotation();
        final AstNodeCollection<Expression> arguments = a.getArguments();

        a.setType(convertType(annotation.getAnnotationType()));

        final List<AnnotationParameter> parameters = annotation.getParameters();

        for (final AnnotationParameter p : parameters) {
            final String member = p.getMember();
            final Expression value = createAnnotationElement(p.getValue());

            if (StringUtilities.isNullOrEmpty(member) ||
                parameters.size() == 1 && "value".equals(member)) {

                arguments.add(value);
            }
            else {
                arguments.add(new AssignmentExpression(new IdentifierExpression(value.getOffset(), member), value));
            }
        }

        return a;
    }

    public Expression createAnnotationElement(final AnnotationElement element) {
        switch (element.getElementType()) {
            case Constant: {
                final ConstantAnnotationElement constant = (ConstantAnnotationElement) element;
                return new PrimitiveExpression(Expression.MYSTERY_OFFSET, constant.getConstantValue());
            }

            case Enum: {
                final EnumAnnotationElement enumElement = (EnumAnnotationElement) element;
                return new TypeReferenceExpression(Expression.MYSTERY_OFFSET, convertType(enumElement.getEnumType())).member(enumElement.getEnumConstantName());
            }

            case Array: {
                final ArrayAnnotationElement arrayElement = (ArrayAnnotationElement) element;
                final ArrayInitializerExpression initializer = new ArrayInitializerExpression();
                final AstNodeCollection<Expression> elements = initializer.getElements();

                for (final AnnotationElement e : arrayElement.getElements()) {
                    elements.add(createAnnotationElement(e));
                }

                return initializer;
            }

            case Class: {
                return new ClassOfExpression(
                    Expression.MYSTERY_OFFSET,
                    convertType(((ClassAnnotationElement) element).getClassType())
                );
            }

            case Annotation: {
                return createAnnotation(((AnnotationAnnotationElement) element).getAnnotation());
            }
        }

        throw ContractUtils.unreachable();
    }
}
