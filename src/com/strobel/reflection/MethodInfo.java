package com.strobel.reflection;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;

import com.sun.tools.javac.code.Flags;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;

/**
 * @author Mike Strobel
 */
public abstract class MethodInfo extends MethodBase {
    public final boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }

    public abstract Type getReturnType();

    @Override
    public final MemberType getMemberType() {
        return MemberType.Method;
    }

    public abstract Method getRawMethod();

    @Override
    public String getName() {
        return getRawMethod().getName();
    }

    @Override
    public StringBuilder appendDescription(final StringBuilder sb) {
        StringBuilder s = new StringBuilder();

        for (final javax.lang.model.element.Modifier modifier : Flags.asModifierSet(getModifiers())) {
            s.append(modifier.toString());
            s.append(' ');
        }

        s = getReturnType().appendBriefDescription(s);
        s.append(' ');
        s.append(getName());
        s.append('(');

        final ParameterList parameters = getParameters();

        for (int i = 0, n = parameters.size(); i < n; ++i) {
            final ParameterInfo p = parameters.get(i);
            if (i != 0) {
                s.append(", ");
            }
            s = p.getParameterType().appendBriefDescription(s);
        }

        s.append(')');

        final TypeList thrownTypes = getThrownTypes();

        if (!thrownTypes.isEmpty()) {
            s.append(" throws ");

            for (int i = 0, n = thrownTypes.size(); i < n; ++i) {
                final Type t = thrownTypes.get(i);
                if (i != 0) {
                    s.append(", ");
                }
                s = t.appendBriefDescription(s);
            }
        }

        return s;
    }

    @Override
    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        if (isGenericMethod() && !isGenericMethodDefinition()) {
            return getGenericMethodDefinition().appendErasedDescription(sb);
        }

        for (final javax.lang.model.element.Modifier modifier : Flags.asModifierSet(getModifiers())) {
            sb.append(modifier.toString());
            sb.append(' ');
        }

        return getReturnType().appendErasedSignature(super.appendDescription(sb));
    }

    public boolean isGenericMethod() {
        return !getGenericMethodParameters().isEmpty();
    }

    public boolean isGenericMethodDefinition() {
        if (!isGenericMethod()) {
            return false;
        }

        final TypeBindings typeArguments = getTypeBindings();

        return !typeArguments.isEmpty() &&
               !typeArguments.hasBoundParameters();
    }

    protected TypeBindings getTypeBindings() {
        return TypeBindings.empty();
    }

    public TypeList getTypeArguments() {
        return getTypeBindings().getBoundTypes();
    }

    public TypeList getGenericMethodParameters() {
        return getTypeBindings().getGenericParameters();
    }

    public MethodInfo getGenericMethodDefinition() {
        if (isGenericMethod()) {
            if (isGenericMethodDefinition()) {
                return this;
            }
            throw ContractUtils.unreachable();
        }
        throw Error.notGenericMethod(this);
    }

    public boolean containsGenericParameters() {
        if (getReturnType().containsGenericParameters()) {
            return true;
        }

        final ParameterList parameters = getParameters();

        for (int i = 0, n = parameters.size(); i < n; i++) {
            final ParameterInfo parameter = parameters.get(i);
            if (parameter.getParameterType().containsGenericParameters()) {
                return true;
            }
        }

        return false;
    }

    public MethodInfo makeGenericMethod(final TypeList typeArguments) {
        if (!isGenericMethodDefinition()) {
            throw Error.notGenericMethodDefinition(this);
        }

        final TypeBindings bindings = TypeBindings.create(getGenericMethodParameters(), typeArguments);

        if (!bindings.hasBoundParameters()) {
            throw new IllegalArgumentException("At least one generic parameter must be bound.");
        }

        return new GenericMethod(bindings, this);
    }
}

class ReflectedMethod extends MethodInfo {
    private final Type _declaringType;
    private final Method _rawMethod;
    private final ParameterList _parameters;
    private final Type _returnType;
    private final TypeBindings _bindings;
    private final TypeList _thrownTypes;
    private final Type _reflectedType;

    ReflectedMethod(
        final Type declaringType,
        final Method rawMethod,
        final ParameterList parameters,
        final Type returnType,
        final TypeList thrownTypes,
        final TypeBindings bindings) {

        this(
            declaringType,
            declaringType,
            rawMethod,
            parameters,
            returnType,
            thrownTypes,
            bindings
        );
    }

    ReflectedMethod(
        final Type declaringType,
        final Type reflectedType,
        final Method rawMethod,
        final ParameterList parameters,
        final Type returnType,
        final TypeList thrownTypes,
        final TypeBindings bindings) {

        Type[] genericParameters = null;

        for (int i = 0, n = bindings.size(); i < n; i++) {
            final Type p = bindings.getGenericParameter(i);

            if (p instanceof GenericParameter<?>) {
                final GenericParameter<?> gp = (GenericParameter<?>)p;
                final TypeVariable<?> typeVariable = gp.getRawTypeVariable();

                if (typeVariable.getGenericDeclaration() == rawMethod) {
                    gp.setDeclaringMethod(this);

                    if (genericParameters == null) {
                        genericParameters = new Type[]{gp};
                    }
                    else {
                        genericParameters = ArrayUtilities.append(genericParameters, gp);
                    }

                    if (bindings.hasBoundParameter(gp)) {
                        throw new IllegalArgumentException(
                            "ReflectedMethod cannot be used with bound generic method parameters.  " +
                            "Use GenericMethod instead."
                        );
                    }
                }
            }
        }

        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _reflectedType = VerifyArgument.notNull(reflectedType, "reflectedType");
        _rawMethod = VerifyArgument.notNull(rawMethod, "rawMethod");
        _parameters = VerifyArgument.notNull(parameters, "parameters");
        _returnType = VerifyArgument.notNull(returnType, "returnType");
        _thrownTypes = VerifyArgument.notNull(thrownTypes, "thrownTypes");

        if (genericParameters == null) {
            _bindings = TypeBindings.empty();
        }
        else {
            _bindings = TypeBindings.createUnbound(new TypeList(genericParameters));
        }
    }

    @Override
    public Type getReturnType() {
        return _returnType;
    }

    @Override
    public Method getRawMethod() {
        return _rawMethod;
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    public Type getReflectedType() {
        return _reflectedType;
    }

    @Override
    int getModifiers() {
        return _rawMethod.getModifiers();
    }

    @Override
    public ParameterList getParameters() {
        return _parameters;
    }

    @Override
    public TypeList getThrownTypes() {
        return _thrownTypes;
    }

    @Override
    public CallingConvention getCallingConvention() {
        return _rawMethod.isVarArgs() ? CallingConvention.VarArgs : CallingConvention.Standard;
    }

    @Override
    protected TypeBindings getTypeBindings() {
        return _bindings;
    }
}

final class GenericMethod extends MethodInfo {

    private final MethodInfo _genericMethodDefinition;
    private final TypeBindings _typeBindings;
    private final ParameterList _parameters;
    private final Type _returnType;

    GenericMethod(final TypeBindings typeBindings, final MethodInfo genericMethodDefinition) {
        _typeBindings = VerifyArgument.notNull(typeBindings, "typeBindings");
        _genericMethodDefinition = VerifyArgument.notNull(genericMethodDefinition, "genericMethodDefinition");

        final ParameterList definitionParameters = _genericMethodDefinition.getParameters();

        if (definitionParameters.isEmpty()) {
            _parameters = definitionParameters;
        }
        else {
            ParameterInfo[] parameters = null;

            for (int i = 0, n = definitionParameters.size(); i < n; i++) {
                final ParameterInfo parameter = definitionParameters.get(i);
                final Type parameterType = parameter.getParameterType();

                final Type resolvedParameterType = resolveBindings(parameterType);

                if (resolvedParameterType != parameterType) {
                    if (parameters == null) {
                        parameters = definitionParameters.toArray();
                    }

                    parameters[i] = new ParameterInfo(
                        parameter.getName(),
                        resolveBindings(parameterType)
                    );
                }
            }

            if (parameters != null) {
                _parameters = new ParameterList(parameters);
            }
            else {
                _parameters = definitionParameters;
            }
        }

        _returnType = resolveBindings(genericMethodDefinition.getReturnType());
    }

    @Override
    protected TypeBindings getTypeBindings() {
        return _typeBindings;
    }

    @Override
    public MethodInfo getGenericMethodDefinition() {
        return _genericMethodDefinition;
    }

    @Override
    public Type getReturnType() {
        return _returnType;
    }

    @Override
    public Method getRawMethod() {
        return _genericMethodDefinition.getRawMethod();
    }

    private Type resolveBindings(final Type type) {
        return GenericType.GenericBinder.visit(type, _typeBindings);
    }

    @Override
    public String getName() {
        return _genericMethodDefinition.getName();
    }

    @Override
    public Type getDeclaringType() {
        return _genericMethodDefinition.getDeclaringType();
    }

    @Override
    public ParameterList getParameters() {
        return _parameters;
    }

    @Override
    public CallingConvention getCallingConvention() {
        return _genericMethodDefinition.getCallingConvention();
    }

    @Override
    int getModifiers() {
        return _genericMethodDefinition.getModifiers();
    }
}