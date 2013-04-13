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
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.*;
import com.strobel.assembler.metadata.annotations.AnnotationAnnotationElement;
import com.strobel.assembler.metadata.annotations.AnnotationElement;
import com.strobel.assembler.metadata.annotations.AnnotationParameter;
import com.strobel.assembler.metadata.annotations.ArrayAnnotationElement;
import com.strobel.assembler.metadata.annotations.ClassAnnotationElement;
import com.strobel.assembler.metadata.annotations.ConstantAnnotationElement;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.assembler.metadata.annotations.EnumAnnotationElement;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.MutableInteger;
import com.strobel.core.Predicate;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.ast.TypeAnalysis;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;
import com.strobel.decompiler.languages.java.ast.transforms.TransformationPipeline;
import com.strobel.util.ContractUtils;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AstBuilder {
    private final DecompilerContext _context;
    private final CompilationUnit _compileUnit = new CompilationUnit();
    private final Map<String, TypeDeclaration> _typeDeclarations = new LinkedHashMap<>();
    private final Map<String, String> _unqualifiedTypeNames = new LinkedHashMap<>();

    private TextNode _packagePlaceholder;
    private boolean _decompileMethodBodies = true;
    private boolean _haveTransformationsRun;

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
        _haveTransformationsRun = true;
    }

    public final void addType(final TypeDefinition type) {
        final TypeDeclaration astType = createType(type);
        final String packageName = type.getPackageName();

        if (_compileUnit.getPackage().isNull() && !StringUtilities.isNullOrWhitespace(packageName)) {
            _compileUnit.insertChildBefore(
                _packagePlaceholder,
                new PackageDeclaration(packageName),
                Roles.PACKAGE
            );
            _packagePlaceholder.remove();
        }

        _compileUnit.addChild(astType, CompilationUnit.MEMBER_ROLE);
    }

    public final TypeDeclaration createType(final TypeDefinition type) {
        VerifyArgument.notNull(type, "type");

        final TypeDeclaration existingDeclaration = _typeDeclarations.get(type.getInternalName());

        if (existingDeclaration != null) {
            return existingDeclaration;
        }

        ITypeLoader loader = _context.getSettings().getTypeLoader();

        if (loader == null) {
            loader = new ClasspathTypeLoader();
        }

        final Buffer buffer = new Buffer(0);

        if (!loader.tryLoadType(type.getInternalName(), buffer)) {
            throw new IllegalStateException(String.format("Failed to load class %s.", type.getInternalName()));
        }

        final ClassFileReader reader = ClassFileReader.readClass(
            ClassFileReader.OPTION_PROCESS_CODE |
            ClassFileReader.OPTION_PROCESS_ANNOTATIONS,
            type.getResolver(),
            buffer
        );

        final TypeDefinitionBuilder typeBuilder = new TypeDefinitionBuilder();

        reader.accept(typeBuilder);

        final TypeDefinition typeWithCode = typeBuilder.getTypeDefinition();
        final TypeDefinition oldCurrentType = _context.getCurrentType();

        _context.setCurrentType(typeWithCode);

        try {
            return createTypeCore(typeWithCode);
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
            final ParameterDeclaration d = new ParameterDeclaration(p.getName(), convertType(p.getParameterType()));

            d.putUserData(Keys.PARAMETER_DEFINITION, p);

            for (final CustomAnnotation annotation : p.getAnnotations()) {
                d.getAnnotations().add(createAnnotation(annotation));
            }

            declarations.add(d);
        }

        return Collections.unmodifiableList(declarations);
    }

    final AstType convertType(final TypeReference type, final MutableInteger typeIndex, final ConvertTypeOptions options) {
        if (type == null) {
            return AstType.NULL;
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
            simpleType.putUserData(Keys.TYPE_DEFINITION, type.resolve());
            return simpleType;
        }

        if (type.isWildcardType()) {
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

        final boolean includeTypeParameterDefinitions = options == null ||
                                                        options.getIncludeTypeParameterDefinitions();

        if (type instanceof IGenericInstance) {
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

            final List<AstType> typeArguments = new ArrayList<>();

            for (final TypeReference typeArgument : genericInstance.getTypeArguments()) {
                typeArguments.add(convertType(typeArgument, typeIndex.increment(), options));
            }

            applyTypeArguments(baseType, typeArguments);

            return baseType;
        }

        String name;

        final TypeDefinition resolvedType = type.resolve();
        final TypeReference nameSource = resolvedType != null ? resolvedType : type;
        final PackageDeclaration packageDeclaration = _compileUnit.getPackage();

        if (options == null || options.getIncludePackage()) {
            final String packageName = nameSource.getPackageName();
            name = StringUtilities.isNullOrEmpty(packageName) ? nameSource.getSimpleName()
                                                              : packageName + "." + nameSource.getSimpleName();
        }
        else if (packageDeclaration != null &&
                 StringUtilities.equals(packageDeclaration.getName(), nameSource.getPackageName())) {

            name = nameSource.getSimpleName();
        }
        else {
            final TypeReference typeToImport;

            String unqualifiedName;

            if (nameSource.isNested()) {
                unqualifiedName = nameSource.getSimpleName();
                TypeReference current = nameSource;

                while (current.isNested()) {
                    current = current.getDeclaringType();
                    unqualifiedName = current.getSimpleName() + "." + unqualifiedName;
                }

                typeToImport = current;
            }
            else {
                typeToImport = nameSource;
                unqualifiedName = nameSource.getSimpleName();
            }

            if (!_typeDeclarations.containsKey(typeToImport.getInternalName())) {
                String importedName = _unqualifiedTypeNames.get(typeToImport.getSimpleName());

                if (importedName == null) {
                    final SimpleType importedType = new SimpleType(typeToImport.getFullName());

                    importedType.putUserData(Keys.TYPE_REFERENCE, typeToImport);

                    if (!StringUtilities.startsWith(typeToImport.getFullName(), "java.lang.")) {
                        _compileUnit.getImports().add(new ImportDeclaration(importedType));
                    }

                    _unqualifiedTypeNames.put(typeToImport.getSimpleName(), typeToImport.getFullName());
                    importedName = typeToImport.getFullName();
                }

                if (importedName.equals(typeToImport.getFullName())) {
                    name = unqualifiedName;
                }
                else {
                    final String packageName = nameSource.getPackageName();
                    name = StringUtilities.isNullOrEmpty(packageName) ? nameSource.getSimpleName()
                                                                      : packageName + "." + nameSource.getSimpleName();
                }
            }
            else {
                name = nameSource.getSimpleName();
            }
        }

        final SimpleType astType = new SimpleType(name);

        astType.putUserData(Keys.TYPE_REFERENCE, type);

        if (type.isGenericType() && includeTypeParameterDefinitions) {
            addTypeArguments(type, astType);
        }

        return astType;
    }

    private TypeDeclaration createTypeCore(final TypeDefinition type) {
        final TypeDeclaration astType = new TypeDeclaration();
        final String packageName = type.getPackageName();

        if (_compileUnit.getPackage().isNull() && !StringUtilities.isNullOrWhitespace(packageName)) {
            _compileUnit.insertChildBefore(
                _packagePlaceholder,
                new PackageDeclaration(packageName),
                Roles.PACKAGE
            );
            _packagePlaceholder.remove();
        }

        _typeDeclarations.put(type.getInternalName(), astType);

        long flags = type.getFlags() & (Flags.ClassFlags | Flags.STATIC);

        if (type.isInterface()) {
            flags &= ~Flags.ABSTRACT;
        }

        EntityDeclaration.setModifiers(
            astType,
            Flags.asModifierSet(flags)
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

        if (baseType != null && !BuiltinTypes.Object.equals(baseType)) {
            astType.addChild(convertType(baseType), Roles.BASE_TYPE);
        }

        for (final TypeReference interfaceType : type.getExplicitInterfaces()) {
            astType.addChild(convertType(interfaceType), Roles.IMPLEMENTED_INTERFACE);
        }

        for (final CustomAnnotation annotation : type.getAnnotations()) {
            astType.getAnnotations().add(createAnnotation(annotation));
        }

        addTypeMembers(astType, type);

        return astType;
    }

    private void addTypeMembers(final TypeDeclaration astType, final TypeDefinition type) {
        for (final FieldDefinition field : type.getDeclaredFields()) {
            if (!isMemberHidden(field, _context.getSettings())) {
                astType.addChild(createField(field), Roles.TYPE_MEMBER);
            }
        }

        for (final MethodDefinition method : type.getDeclaredMethods()) {
            if (!isMemberHidden(method, _context.getSettings())) {
                if (method.isConstructor()) {
                    astType.addChild(createConstructor(method), Roles.TYPE_MEMBER);
                }
                else {
                    astType.addChild(createMethod(method), Roles.TYPE_MEMBER);
                }
            }
        }

        for (final TypeDefinition nestedType : type.getDeclaredTypes()) {
            astType.addChild(createType(nestedType), Roles.TYPE_MEMBER);
        }
    }

    private FieldDeclaration createField(final FieldDefinition field) {
        final FieldDeclaration astField = new FieldDeclaration();
        final VariableInitializer initializer = new VariableInitializer(field.getName());

        astField.addChild(initializer, Roles.VARIABLE);
        astField.setReturnType(convertType(field.getFieldType()));
        astField.putUserData(Keys.FIELD_DEFINITION, field);
        astField.putUserData(Keys.MEMBER_REFERENCE, field);

        EntityDeclaration.setModifiers(astField, Flags.asModifierSet(field.getFlags() & Flags.VarFlags));

        if (field.hasConstantValue()) {
            initializer.setInitializer(new PrimitiveExpression(field.getConstantValue()));
            initializer.putUserData(Keys.FIELD_DEFINITION, field);
            initializer.putUserData(Keys.MEMBER_REFERENCE, field);
        }

        return astField;
    }

    private MethodDeclaration createMethod(final MethodDefinition method) {
        final MethodDeclaration astMethod = new MethodDeclaration();

        final Set<Modifier> modifiers;

        if (method.getDeclaringType().isInterface()) {
            modifiers = Collections.emptySet();
        }
        else {
            modifiers = Flags.asModifierSet(method.getFlags() & Flags.MethodFlags);
        }

        EntityDeclaration.setModifiers(astMethod, modifiers);

        astMethod.setName(method.getName());
        astMethod.getParameters().addAll(createParameters(method.getParameters()));
        astMethod.getTypeParameters().addAll(createTypeParameters(method.getGenericParameters()));
        astMethod.setReturnType(convertType(method.getReturnType()));
        astMethod.putUserData(Keys.METHOD_DEFINITION, method);
        astMethod.putUserData(Keys.MEMBER_REFERENCE, method);

        if (!method.getDeclaringType().isInterface()) {
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

        EntityDeclaration.setModifiers(astMethod, Flags.asModifierSet(method.getFlags() & Flags.ConstructorFlags));

        astMethod.setName(method.getDeclaringType().getName());
        astMethod.getParameters().addAll(createParameters(method.getParameters()));
        astMethod.setBody(createMethodBody(method, astMethod.getParameters()));
        astMethod.putUserData(Keys.METHOD_DEFINITION, method);
        astMethod.putUserData(Keys.MEMBER_REFERENCE, method);

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
                return new PrimitiveExpression(Boolean.FALSE);
            }
            return new PrimitiveExpression(Boolean.TRUE);
        }

        if (type != null) {
            return new PrimitiveExpression(JavaPrimitiveCast.cast(type.getSimpleType(), val));
        }

        return new PrimitiveExpression(JavaPrimitiveCast.cast(com.strobel.reflection.SimpleType.Integer, val));
    }

    public static Expression makeDefaultValue(final TypeReference type) {
        if (type == null) {
            return new NullReferenceExpression();
        }

        switch (type.getSimpleType()) {
            case Boolean:
                return new PrimitiveExpression(Boolean.FALSE);

            case Byte:
                return new PrimitiveExpression((byte) 0);

            case Character:
                return new PrimitiveExpression('\0');

            case Short:
                return new PrimitiveExpression((short) 0);

            case Integer:
                return new PrimitiveExpression(0);

            case Long:
                return new PrimitiveExpression(0L);

            case Float:
                return new PrimitiveExpression(0f);

            case Double:
                return new PrimitiveExpression(0d);

            default:
                return new NullReferenceExpression();
        }
    }

    public void generateCode(final ITextOutput output) {
        if (!_haveTransformationsRun) {
            runTransformations();
        }

        _compileUnit.acceptVisitor(new InsertParenthesesVisitor(), null);
        _compileUnit.acceptVisitor(new JavaOutputVisitor(output, _context.getSettings().getFormattingOptions()), null);
    }

    public static boolean isMemberHidden(final IMemberDefinition member, final DecompilerSettings settings) {
        return member.isSynthetic() && !settings.getShowSyntheticMembers();
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
                arguments.add(new AssignmentExpression(new IdentifierExpression(member), value));
            }
        }

        return a;
    }

    public Expression createAnnotationElement(final AnnotationElement element) {
        switch (element.getElementType()) {
            case Constant: {
                final ConstantAnnotationElement constant = (ConstantAnnotationElement) element;
                return new PrimitiveExpression(constant.getConstantValue());
            }

            case Enum: {
                final EnumAnnotationElement enumElement = (EnumAnnotationElement) element;
                return new TypeReferenceExpression(convertType(enumElement.getEnumType())).member(enumElement.getEnumConstantName());
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
                return new ClassOfExpression(convertType(((ClassAnnotationElement)element).getClassType()));
            }

            case Annotation: {
                return createAnnotation(((AnnotationAnnotationElement)element).getAnnotation());
            }
        }

        throw ContractUtils.unreachable();
    }
}

