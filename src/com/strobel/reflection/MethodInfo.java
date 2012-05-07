package com.strobel.reflection;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;

/**
 * @author Mike Strobel
 */
public abstract class MethodInfo extends MethodBase {
    public abstract boolean isStatic();

    public abstract Type getReturnType();

    @Override
    public final MemberType getMemberType() {
        return MemberType.Method;
    }

    @Override
    public StringBuilder appendDescription(final StringBuilder sb) {
        return getReturnType().appendSignature(super.appendDescription(sb));
    }

    @Override
    public StringBuilder appendErasedDescription(final StringBuilder sb) {
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
        if (getReturnType().isGenericParameter()) {
            return true;
        }

        final ParameterList parameters = getParameters();
        
        for (int i = 0, n = parameters.size(); i < n; i++) {
            final ParameterInfo parameter = parameters.get(i);
            if (parameter.getParameterType().isGenericParameter()) {
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

    ReflectedMethod(
        final Type declaringType,
        final Method rawMethod,
        final ParameterList parameters,
        final Type returnType,
        final TypeBindings bindings) {
        
        Type[] genericParameters = null;
        
        for (int i = 0, n = bindings.size(); i < n; i++) {
            final GenericParameterType p = (GenericParameterType) bindings.getGenericParameter(i);
            final TypeVariable<?> typeVariable = p.getRawTypeVariable();
            
            if (typeVariable.getGenericDeclaration() == rawMethod) {
                p.setDeclaringMethod(this);

                if (genericParameters == null) {
                    genericParameters = new Type[] { p };
                }
                else {
                    genericParameters = ArrayUtilities.append(genericParameters, p);
                }
                if (bindings.hasBoundParameter(p)) {
                    throw new IllegalArgumentException(
                        "ReflectedMethod cannot be used with bound generic method parameters.  " +
                        "Use GenericMethod instead.");
                }
            }
        }

        _declaringType = declaringType;
        _rawMethod = rawMethod;
        _parameters = parameters;
        _returnType = returnType;

        if (genericParameters == null) {
            _bindings = TypeBindings.empty();
        }
        else {
            _bindings = TypeBindings.createUnbound(new TypeList(genericParameters));
        }
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    @Override
    public Type getReturnType() {
        return _returnType;
    }

    @Override
    public String getName() {
        return _rawMethod.getName();
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
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
                        resolveBindings(parameterType));
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
    public boolean isStatic() {
        return _genericMethodDefinition.isStatic();
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

    private Type resolveBindings(final Type type) {
        if (type.isGenericType() || type.isGenericParameter()) {
            final TypeBindings bindings = getDeclaringType().getTypeBindings().withAdditionalBindings(_typeBindings);
            if (type.isGenericParameter() && bindings.hasBoundParameter(type)) {
                return bindings.getBoundType(type);
            }
            return Type.resolve(
                type.getErasedClass(),
                bindings);
        }
        return type;
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