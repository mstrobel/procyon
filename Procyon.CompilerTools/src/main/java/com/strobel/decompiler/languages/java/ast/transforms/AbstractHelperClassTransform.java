package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.annotations.NotNull;
import com.strobel.annotations.Nullable;
import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.TextLocation;
import com.strobel.decompiler.languages.java.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("unused")
public abstract class AbstractHelperClassTransform extends ContextTrackingVisitor<Void> {
    protected final int BOOTSTRAP_ARG_OFFSET = 3;

    protected final static String T_DESC_STRING = "java/lang/String";
    protected final static String T_DESC_CLASS = "java/lang/Class";
    protected final static String T_DESC_METHOD_TYPE = "java/lang/invoke/MethodType";
    protected final static String T_DESC_T_DESCRIPTOR = "java/lang/invoke/TypeDescriptor";
    protected final static String T_DESC_T_DESCRIPTOR_INNER_PREFIX = "java/lang/invoke/TypeDescriptor$";
    protected final static String T_DESC_METHOD_HANDLE = "java/lang/invoke/MethodHandle";
    protected final static String T_DESC_METHOD_HANDLES = "java/lang/invoke/MethodHandles";
    protected final static String T_DESC_LOOKUP = "java/lang/invoke/MethodHandles$Lookup";
    protected final static String M_SIGNATURE_LOOKUP = "()L" + T_DESC_LOOKUP + ';';
    protected final static String M_SIGNATURE_PRIVATE_LOOKUP = "(Ljava/lang/Class;Ljava/lang/invoke/MethodHandles$Lookup;)Ljava/lang/invoke/MethodHandles$Lookup;";
    protected final static String M_DESC_METHOD_TYPE = "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;";
    protected final static String T_DESC_THROWABLE = "java/lang/Throwable";
    protected final static String T_DESC_THROWABLE_WRAPPER = "java/lang/reflect/UndeclaredThrowableException";
    protected final static String T_DESC_REFLECTION_EXCEPTION = "java/lang/ReflectiveOperationException";
    protected final static String T_DESC_CALL_SITE = "java/lang/invoke/CallSite";
    protected final static String M_DESC_INVOKE_EXACT = "([Ljava/lang/Object;)Ljava/lang/Object;";
    protected final static String M_DESC_THROW_EXCEPTION = "(Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;";
    protected final static String M_DESC_INSERT_ARGUMENTS = "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;";
    protected final static String M_DESC_PERMUTE_ARGUMENTS = "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;[I)Ljava/lang/invoke/MethodHandle;";
    protected final static String M_DESC_AS_TYPE = "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;";
    protected final static String M_DESC_RETURN_TYPE = "()Ljava/lang/Class;";
    protected final static String M_DESC_GET_CLASS = "()Ljava/lang/Class;";
    protected final static String M_DESC_DYNAMIC_INVOKER = "()L" + T_DESC_METHOD_HANDLE + ";";

    protected final MetadataParser parser = new MetadataParser();

    protected TypeDeclaration currentType;
    protected IMetadataResolver resolver;

    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    protected static int nextUniqueId() {
        return NEXT_ID.incrementAndGet();
    }

    public AbstractHelperClassTransform(final DecompilerContext context) {
        super(context);
    }

    @Override
    protected Void visitTypeDeclarationOverride(final TypeDeclaration typeDeclaration, final Void p) {
        final TypeDeclaration oldType = this.currentType;
        final IMetadataResolver oldResolver = this.resolver;

        try {
            this.resolver = context.getCurrentType().getResolver();

            if (JavaNameResolver.isStaticContext(typeDeclaration, true, false)) {
                this.currentType = typeDeclaration;
            }

            super.visitTypeDeclarationOverride(typeDeclaration, p);
            return null;
        }
        finally {
            this.resolver = oldResolver;
            this.currentType = oldType;
        }
    }

    protected static IMetadataResolver resolver(final TypeReference parentType) {
        final TypeDefinition r = parentType.resolve();
        return r != null ? r.getResolver() : MetadataSystem.instance();
    }

    protected TypeReference translateArgumentType(final Object o) {
        if (o == null) {
            return BuiltinTypes.Null;
        }
        if (o instanceof MethodHandle) {
            return resolver.lookupType(T_DESC_METHOD_HANDLE);
        }
        return resolver.lookupType(o.getClass().getName().replace('.', '/'));
    }

    protected  MethodDeclaration newMethod(final MethodDefinition definition) {
        final MethodDeclaration declaration = new MethodDeclaration();
        final AstNodeCollection<JavaModifierToken> modifiers = declaration.getModifiers();
        final AstNodeCollection<ParameterDeclaration> parameters = declaration.getParameters();

        modifiers.clear();

        for (final Flags.Flag modifier : Flags.asFlagSet((Flags.AccessFlags | Flags.MethodFlags) & definition.getFlags())) {
            modifiers.add(new JavaModifierToken(TextLocation.EMPTY, modifier));
        }

        for (final ParameterDefinition pd : definition.getParameters()) {
            final ParameterDeclaration p = new ParameterDeclaration(pd.getName(), makeType(pd.getParameterType()));
            final Variable v = new Variable();

            v.setOriginalParameter(pd);
            v.setName(pd.getName());
            v.setGenerated(false);
            v.setType(pd.getParameterType());

            p.putUserData(Keys.PARAMETER_DEFINITION, pd);
            p.putUserData(Keys.VARIABLE, v);

            parameters.add(p);
        }

        declaration.setName(definition.getName());
        declaration.setReturnType(makeType(definition.getReturnType()));
        declaration.putUserData(Keys.MEMBER_REFERENCE, definition);
        declaration.putUserData(Keys.METHOD_DEFINITION, definition);

        return declaration;
    }

    protected MemberReferenceExpression makeReference(final MemberReference reference) {
        final AstType type = makeType(reference.getDeclaringType());
        final MemberReferenceExpression member = type.member(reference.getName());

        member.putUserData(Keys.MEMBER_REFERENCE, reference);

        if (reference instanceof FieldDefinition) {
            member.putUserData(Keys.FIELD_DEFINITION, (FieldDefinition) reference);
        }
        else if (reference instanceof MethodDefinition) {
            member.putUserData(Keys.METHOD_DEFINITION, (MethodDefinition) reference);
        }

        return member;
    }

    protected IdentifierExpression varReference(final AstNode declaration) {
        final Variable variable = declaration.getUserData(Keys.VARIABLE);
        final IdentifierExpression id = new IdentifierExpression(Expression.MYSTERY_OFFSET, variable.getName());

        id.putUserData(Keys.VARIABLE, variable);

        return id;
    }

    protected IdentifierExpression varReference(final Variable variable) {
        final IdentifierExpression id = new IdentifierExpression(Expression.MYSTERY_OFFSET, variable.getName());

        id.putUserData(Keys.VARIABLE, variable);

        return id;
    }

    protected FieldDeclaration declareField(final @NotNull FieldDefinition fd, final @NotNull Expression initializer, final int extraFlags) {
        final FieldDeclaration field = new FieldDeclaration();
        final AstNodeCollection<JavaModifierToken> modifiers = field.getModifiers();

        field.setReturnType(makeType(fd.getFieldType()));
        field.setName(fd.getName());
        field.putUserData(Keys.MEMBER_REFERENCE, fd);
        field.putUserData(Keys.FIELD_DEFINITION, fd);
        field.getVariables().add(new VariableInitializer(fd.getName(), initializer));

        for (final Flags.Flag modifier : Flags.asFlagSet(((Flags.AccessFlags | Flags.VarFlags) & (fd.getModifiers() | extraFlags)))) {
            modifiers.add(new JavaModifierToken(TextLocation.EMPTY, modifier));
        }

        return field;
    }

    protected InvocationExpression makeMethodType(final IMethodSignature methodSignature) {
        final TypeReference mt = parser.parseTypeDescriptor(T_DESC_METHOD_TYPE);
        final MethodReference methodType = parser.parseMethod(mt, "methodType", M_DESC_METHOD_TYPE);
        final List<ParameterDefinition> parameters = methodSignature.getParameters();
        final InvocationExpression invocation = makeType(mt).invoke(methodType);
        final AstNodeCollection<Expression> arguments = invocation.getArguments();

        arguments.add(makeType(methodSignature.getReturnType()).classOf());

        for (final ParameterDefinition parameter : parameters) {
            arguments.add(makeType(parameter.getParameterType()).classOf());
        }

        invocation.putUserData(Keys.MEMBER_REFERENCE, methodType);
        return invocation;
    }

    protected InvocationExpression makeMethodHandle(final Expression lookup, final MethodHandle handle) {
        return makeMethodHandle(lookup, handle, null, null);
    }

    protected InvocationExpression makeMethodHandle(final Expression lookup, final MethodHandle handle, final @Nullable Expression methodType) {
        return makeMethodHandle(lookup, handle, methodType, null);
    }

    @SuppressWarnings("SameParameterValue")
    protected InvocationExpression makeMethodHandle(final Expression lookup,
                                                    final MethodHandle handle,
                                                    final @Nullable Expression methodType,
                                                    final @Nullable TypeReference optionalSpecialCaller) {

        final TypeReference lookupType = parser.parseTypeDescriptor(T_DESC_LOOKUP);
        final MethodHandleType handleType = handle.getHandleType();
        final MethodReference find = parser.parseMethod(lookupType, handleType.lookupMethodName(), handleType.lookupDescriptor());
        final MethodReference method = handle.getMethod();
        final InvocationExpression invocation = lookup.invoke(find);
        final AstNodeCollection<Expression> arguments = invocation.getArguments();

        arguments.add(makeType(method.getDeclaringType()).classOf());

        if (handleType != MethodHandleType.NewInvokeSpecial) {
            arguments.add(new PrimitiveExpression(Expression.MYSTERY_OFFSET, method.getName()));
        }

        if (handleType.isField()) {
            arguments.add(makeType(method.getReturnType()).classOf());
        }
        else {
            arguments.add(methodType != null ? methodType : makeMethodType(method));
        }

        if (handleType == MethodHandleType.InvokeSpecial) {
            if (optionalSpecialCaller != null) {
                arguments.add(makeType(optionalSpecialCaller).classOf());
            }
            else {
                arguments.add(makeType(method.getDeclaringType()).classOf());
            }
        }

        invocation.putUserData(Keys.MEMBER_REFERENCE, find);
        return invocation;
    }

    protected MethodReference resolveLookupMethod(final TypeReference lookupType, final String methodName, final String returnType) {
        return parser.parseMethod(
            lookupType,
            methodName,
            format("(L%s;L%s;L%s;)L%s;", T_DESC_CLASS, T_DESC_STRING, returnType, T_DESC_METHOD_HANDLE)
        );
    }

    @SuppressWarnings("SameParameterValue")
    protected Variable makeCatchVariable(final @NotNull String name, final @NotNull TypeReference type) {
        final Variable v = new Variable();

        v.setGenerated(false);
        v.setName(requireNonNull(name, "A name is required."));
        v.setType(requireNonNull(type, "A type is required."));

        return v;
    }

    protected InvocationExpression makeBootstrapCall(final DynamicCallSite callSite, final Variable lookupVariable) {
        final AstType methodHandles = makeType(T_DESC_METHOD_HANDLES);
        final MethodReference bootstrapMethod = callSite.getBootstrapMethod();
        final MethodDefinition resolvedBootstrapMethod = bootstrapMethod.resolve();
        final List<ParameterDefinition> parameters = bootstrapMethod.getParameters();
        final List<Object> argumentValues = callSite.getBootstrapArguments();
        final List<Expression> arguments = new ArrayList<>();

        boolean isVarArgs = resolvedBootstrapMethod != null && resolvedBootstrapMethod.isVarArgs();

        final int n = Math.min(parameters.size(), argumentValues.size() + BOOTSTRAP_ARG_OFFSET);

        for (int i = 0; i < n; i++) {
            final ParameterDefinition p = parameters.get(i);

            if (i == n - 1 &&
                resolvedBootstrapMethod == null &&
                p.getParameterType().isArray() &&
                argumentValues.size() + BOOTSTRAP_ARG_OFFSET >= parameters.size()) {

                final TypeReference argumentType = translateArgumentType(argumentValues.get(i - BOOTSTRAP_ARG_OFFSET));

                if (argumentType != null &&
                    argumentType != BuiltinTypes.Null &&
                    MetadataHelper.isAssignableFrom(p.getParameterType().getElementType(), argumentType)) {

                    isVarArgs = true;
                }
            }

            final Expression convertedArgument = tryConvertArgument(lookupVariable, methodHandles, p, callSite, i, isVarArgs);

            if (convertedArgument != null) {
                arguments.add(convertedArgument);
            }
            else {
                return null;
            }
        }

        if (isVarArgs) {
            final ParameterDefinition varArgsParameter = parameters.get(parameters.size() - 1);

            for (int i = n, m = argumentValues.size() + BOOTSTRAP_ARG_OFFSET; i < m; i++) {
                final Expression expression = tryConvertArgument(lookupVariable, methodHandles, varArgsParameter, callSite, i, true);

                if (expression != null) {
                    arguments.add(expression);
                }
                else {
                    return null;
                }
            }
        }

        final AstType bootstrapType = makeType(bootstrapMethod.getDeclaringType());

        @SuppressWarnings("UnnecessaryLocalVariable")
        final InvocationExpression bootstrapCall = bootstrapType.invoke(bootstrapMethod, arguments);

        return bootstrapCall;
    }

    @Nullable
    protected Expression tryConvertArgument(
        final Variable lookupVariable,
        final AstType methodHandles,
        final ParameterDefinition p,
        final DynamicCallSite callSite,
        final int argumentIndex,
        final boolean isVarArgs) {

        final Object o;
        final List<Object> argumentValues = callSite.getBootstrapArguments();

        switch (argumentIndex) {
            // @formatter:off
            case 0:  return varReference(lookupVariable);
            case 1:  o = callSite.getMethodName();                                 break;
            case 2:  o = callSite.getMethodType();                                 break;
            default: o = argumentValues.get(argumentIndex - BOOTSTRAP_ARG_OFFSET); break;
            // @formatter:on
        }

        if (o instanceof TypeReference) {
            return new ClassOfExpression(Expression.MYSTERY_OFFSET, makeType((TypeReference) o));
        }

        TypeReference pType = p.getParameterType();

        if (isVarArgs && pType.isArray()) {
            pType = pType.getElementType();
        }

        MetadataHelper.getUnderlyingPrimitiveTypeOrSelf(p.getParameterType());

        if (pType.isPrimitive() || T_DESC_STRING.equals(pType.getInternalName())) {
            return new PrimitiveExpression(Expression.MYSTERY_OFFSET, o);
        }

        final String typeName = pType.getInternalName();

        if (T_DESC_METHOD_HANDLE.equals(typeName)) {
            if (o instanceof MethodHandle) {
                final MethodHandle mh = (MethodHandle) o;
                final MethodHandleType hType = mh.getHandleType();
                final PrimitiveExpression methodName = new PrimitiveExpression(Expression.MYSTERY_OFFSET, mh.getMethod().getName());
                final AstType refType = makeType(mh.getMethod().getDeclaringType());
                final AstType returnType = makeType(mh.getMethod().getReturnType());
                final IdentifierExpression lookup = varReference(lookupVariable);
                final TypeReference lookupType = lookupVariable.getType();

                final MethodReference findMethod;
                final InvocationExpression invocation;

                switch (hType) {
                    case GetField: {
                        findMethod = resolveLookupMethod(lookupType, "findGetter", T_DESC_CLASS);
                        invocation = lookup.invoke(findMethod, refType.classOf(), methodName, returnType.classOf());
                        break;
                    }
                    case GetStatic: {
                        findMethod = resolveLookupMethod(lookupType, "findStaticGetter", T_DESC_CLASS);
                        invocation = lookup.invoke(findMethod, refType.classOf(), methodName, returnType.classOf());
                        break;
                    }
                    case PutField: {
                        findMethod = resolveLookupMethod(lookupType, "findSetter", T_DESC_CLASS);
                        invocation = lookup.invoke(findMethod, refType.classOf(), methodName, returnType.classOf());
                        break;
                    }
                    case PutStatic: {
                        findMethod = resolveLookupMethod(lookupType, "findStaticSetter", T_DESC_CLASS);
                        invocation = lookup.invoke(findMethod, refType.classOf(), methodName, returnType.classOf());
                        break;
                    }
                    case InvokeVirtual:
                    case InvokeInterface: {
                        findMethod = resolveLookupMethod(lookupType, "findVirtual", T_DESC_METHOD_TYPE);
                        invocation = lookup.invoke(findMethod, refType.classOf(), methodName, makeMethodType(mh.getMethod()));
                        break;
                    }
                    case InvokeStatic: {
                        findMethod = resolveLookupMethod(lookupType, "findStatic", T_DESC_METHOD_TYPE);
                        invocation = lookup.invoke(findMethod, refType.classOf(), methodName, makeMethodType(mh.getMethod()));
                        break;
                    }
                    case InvokeSpecial: {
                        findMethod = resolveLookupMethod(lookupType, "findSpecial", T_DESC_METHOD_TYPE);
                        invocation = lookup.invoke(findMethod, refType.classOf(), methodName, makeMethodType(mh.getMethod()));
                        break;
                    }
                    case NewInvokeSpecial: {
                        findMethod = parser.parseMethod(
                            lookupType,
                            "findConstructor",
                            format("(L%s;L%s;)L%s;", T_DESC_CLASS, T_DESC_METHOD_TYPE, T_DESC_METHOD_HANDLE)
                        );
                        invocation = lookup.invoke(findMethod, refType.classOf(), methodName, makeMethodType(mh.getMethod()));
                        break;
                    }
                    default: {
                        return null;
                    }
                }

                return invocation;
            }
        }
        else if (T_DESC_METHOD_TYPE.equals(typeName) ||
                 T_DESC_T_DESCRIPTOR.equals(typeName) ||
                 typeName.startsWith(T_DESC_T_DESCRIPTOR_INNER_PREFIX)) {

            if (o instanceof IMethodSignature) {
                return makeMethodType((IMethodSignature) o);
            }
        }

        return null;
    }
}
