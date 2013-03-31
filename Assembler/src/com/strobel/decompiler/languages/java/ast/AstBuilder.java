/*
 * AstBuilder.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast;

import com.strobel.assembler.metadata.*;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AstBuilder {
    private final DecompilerContext _context;
    private final CompilationUnit _compileUnit = new CompilationUnit();

    private boolean _decompileMethodBodies = true;
    private boolean _haveTransformationsRun;

    public AstBuilder(final DecompilerContext context) {
        _context = VerifyArgument.notNull(context, "context");
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

        if (!StringUtilities.isNullOrWhitespace(packageName)) {
            astType.setPackage(new PackageDeclaration(type.getPackageName()));
        }

        EntityDeclaration.setModifiers(astType, Flags.asModifierSet(type.getFlags() & Flags.ClassFlags));

        _compileUnit.addChild(astType, CompilationUnit.MEMBER_ROLE);
    }

    public final TypeDeclaration createType(final TypeDefinition type) {
        VerifyArgument.notNull(type, "type");

        final TypeDefinition oldCurrentType = _context.getCurrentType();

        _context.setCurrentType(type);

        try {
            return createTypeCore(type);
        }
        finally {
            _context.setCurrentType(oldCurrentType);
        }
    }

    public static AstType convertType(final TypeReference type) {
        return convertType(type, new ConvertTypeOptions());
    }

    public static AstType convertType(final TypeReference type, final ConvertTypeOptions options) {
        return convertType(type, new MutableInteger(0), options);
    }

    public static List<ParameterDeclaration> createParameters(final Iterable<ParameterDefinition> parameters) {
        final List<ParameterDeclaration> declarations = new ArrayList<>();

        for (final ParameterDefinition p : parameters) {
            declarations.add(new ParameterDeclaration(p.getName(), convertType(p.getParameterType())));
        }

        return Collections.unmodifiableList(declarations);
    }

    static AstType convertType(final TypeReference type, final MutableInteger typeIndex, final ConvertTypeOptions options) {
        if (type == null) {
            return AstType.NULL;
        }

        if (type.isArray()) {
            return convertType(type.getElementType(), typeIndex.increment(), options).makeArrayType();
        }

        if (type.isGenericParameter()) {
            return new SimpleType(type.getName());
        }

        if (type.isPrimitive()) {
            final SimpleType simpleType = new SimpleType(type.getSimpleName());
            simpleType.putUserData(Keys.TYPE_DEFINITION, type.resolve());
            return simpleType;
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

        final String name = options == null || options.getIncludePackage() ? type.getFullName()
                                                                           : type.getName();

        final SimpleType astType = new SimpleType(name);

        if (type.isGenericType() && includeTypeParameterDefinitions) {
            addTypeArguments(type, astType);
        }

        return astType;
    }

    private TypeDeclaration createTypeCore(final TypeDefinition type) {
        final TypeDeclaration astType = new TypeDeclaration();
        final String packageName = type.getPackageName();

        if (!StringUtilities.isNullOrEmpty(packageName)) {
            astType.setPackage(new PackageDeclaration(packageName));
        }

        astType.setName(type.getSimpleName());
        astType.putUserData(Keys.TYPE_DEFINITION, type);

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
            astType.addChild(convertType(interfaceType), Roles.BASE_TYPE);
        }

        addTypeMembers(astType, type);

        return astType;
    }

    private void addTypeMembers(final TypeDeclaration astType, final TypeDefinition type) {
        for (final TypeDefinition nestedType : type.getDeclaredTypes()) {
            astType.addChild(createType(nestedType), Roles.TYPE_MEMBER);
        }

        for (final FieldDefinition field : type.getDeclaredFields()) {
            astType.addChild(createField(field), Roles.TYPE_MEMBER);
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
    }

    private FieldDeclaration createField(final FieldDefinition field) {
        final FieldDeclaration astField = new FieldDeclaration();
        final VariableInitializer initializer = new VariableInitializer(field.getName());

        astField.addChild(initializer, Roles.VARIABLE);
        astField.setReturnType(convertType(field.getFieldType()));
        astField.putUserData(Keys.FIELD_DEFINITION, field);

        EntityDeclaration.setModifiers(astField, Flags.asModifierSet(field.getFlags() & Flags.VarFlags));

        if (field.hasConstantValue()) {
            initializer.setInitializer(new PrimitiveExpression(field.getConstantValue()));
        }

        return astField;
    }

    private MethodDeclaration createMethod(final MethodDefinition method) {
        final MethodDeclaration astMethod = new MethodDeclaration();

        EntityDeclaration.setModifiers(astMethod, Flags.asModifierSet(method.getFlags() & Flags.ConstructorFlags));

        astMethod.setName(method.getName());
        astMethod.getParameters().addAll(createParameters(method.getParameters()));
        astMethod.getTypeParameters().addAll(createTypeParameters(method.getGenericParameters()));
        astMethod.setReturnType(convertType(method.getReturnType()));
        astMethod.putUserData(Keys.METHOD_DEFINITION, method);

        if (!method.getDeclaringType().isInterface()) {
            astMethod.setBody(createMethodBody(method, astMethod.getParameters()));
        }

        return astMethod;
    }

    private ConstructorDeclaration createConstructor(final MethodDefinition method) {
        final ConstructorDeclaration astMethod = new ConstructorDeclaration();

        EntityDeclaration.setModifiers(astMethod, Flags.asModifierSet(method.getFlags() & Flags.MethodFlags));

        astMethod.setName(method.getDeclaringType().getName());
        astMethod.getParameters().addAll(createParameters(method.getParameters()));
        astMethod.setBody(createMethodBody(method, astMethod.getParameters()));
        astMethod.putUserData(Keys.METHOD_DEFINITION, method);

        return astMethod;
    }

    static List<TypeParameterDeclaration> createTypeParameters(final List<GenericParameter> genericParameters) {
        if (genericParameters.isEmpty()) {
            return Collections.emptyList();
        }

        final int count = genericParameters.size();
        final TypeParameterDeclaration[] typeParameters = new TypeParameterDeclaration[genericParameters.size()];

        for (int i = 0; i < count; i++) {
            typeParameters[i] = new TypeParameterDeclaration(genericParameters.get(i).getName());
        }

        return ArrayUtilities.asUnmodifiableList(typeParameters);
    }

    static void addTypeArguments(final TypeReference type, final AstType astType) {
        if (type.hasGenericParameters()) {
            final List<GenericParameter> genericParameters = type.getGenericParameters();
            final int count = genericParameters.size();
            final AstType[] typeArguments = new AstType[count];

            for (int i = 0; i < count; i++) {
                typeArguments[i] = new SimpleType(genericParameters.get(i).getName());
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
            return AstMethodBodyBuilder.createMethodBody(method, _context, parameters);
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

    public static boolean isMemberHidden(final MemberReference member, final DecompilerSettings settings) {
        if (member instanceof MethodDefinition) {
            if (settings.getShowSyntheticMembers()) {
                return false;
            }

            final MethodDefinition method = (MethodDefinition) member;

            if (Flags.testAny(method.getFlags(), Flags.ACC_BRIDGE | Flags.ACC_SYNTHETIC)) {
                return true;
            }
        }

        return false;
    }
}

