package com.strobel.assembler;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:29 PM
 */
public abstract class MethodReference extends MemberReference implements IMethodSignature,
                                                                         IGenericParameterProvider {
    final static String CONSTRUCTOR_NAME = "<init>";
    final static String CLASS_CONSTRUCTOR_NAME = "<clinit>";

    // <editor-fold defaultstate="collapsed" desc="Signature">

    public abstract TypeReference getReturnType();

    public boolean hasParameters() {
        return !getParameters().isEmpty();
    }

    public abstract List<ParameterDefinition> getParameters();
    
    public List<TypeReference> getThrownTypes() {
        return Collections.emptyList();
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Method Attributes">

    @Override
    public boolean isSpecialName() {
        return CONSTRUCTOR_NAME.equals(getName()) ||
               CLASS_CONSTRUCTOR_NAME.equals(getName());
    }

    public boolean isConstructor() {
        return MethodDefinition.CONSTRUCTOR_NAME.equals(getName());
    }

    public boolean isTypeInitializer() {
        return MethodDefinition.CLASS_CONSTRUCTOR_NAME.equals(getName());
    }

    public final boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }

    public final boolean isBridgeMethod() {
        return Modifier.isAbstract(getModifiers());
    }

    public final boolean isVarArgs() {
        return (getModifiers() & MODIFIER_VARARGS) != 0;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Generics">

    public boolean isGenericMethod() {
        return hasGenericParameters();
    }

    @Override
    public boolean hasGenericParameters() {
        return !getGenericParameters().isEmpty();
    }

    @Override
    public boolean isGenericDefinition() {
        return hasGenericParameters() &&
               isDefinition();
    }

    public List<GenericParameter> getGenericParameters() {
        return Collections.emptyList();
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Member Resolution">

    public abstract MethodDefinition resolve();

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Name and Signature Formatting">

    @Override
    public StringBuilder appendDescription(final StringBuilder sb) {
        StringBuilder s = new StringBuilder();

        for (final javax.lang.model.element.Modifier modifier : Flags.asModifierSet(getModifiers())) {
            s.append(modifier.toString());
            s.append(' ');
        }

        final List<? extends TypeReference> typeArguments;

        if (this instanceof IGenericInstance) {
            typeArguments = ((IGenericInstance) this).getTypeArguments();
        }
        else if (hasGenericParameters()) {
            typeArguments = getGenericParameters();
        }
        else {
            typeArguments = Collections.emptyList();
        }

        if (!typeArguments.isEmpty()) {
            final int count = typeArguments.size();

            s.append('<');

            for (int i = 0; i < count; i++) {
                if (i != 0) {
                    s.append(", ");
                }
                s = typeArguments.get(i).appendSimpleDescription(s);
            }

            s.append('>');
            s.append(' ');
        }

        TypeReference returnType = getReturnType();

        while (returnType.isWildcardType()) {
            returnType = returnType.getExtendsBound();
        }

        if (returnType.isGenericParameter()) {
            s.append(returnType.getName());
        }
        else {
            s = returnType.appendSimpleDescription(s);
        }

        s.append(' ');
        s.append(getName());
        s.append('(');

        final List<ParameterDefinition> parameters = getParameters();

        for (int i = 0, n = parameters.size(); i < n; ++i) {
            final ParameterDefinition p = parameters.get(i);
            
            if (i != 0) {
                s.append(", ");
            }

            TypeReference parameterType = p.getParameterType();

            while (parameterType.isWildcardType()) {
                parameterType = parameterType.getExtendsBound();
            }

            if (parameterType.isGenericParameter()) {
                s.append(parameterType.getName());
            }
            else {
                s = parameterType.appendSimpleDescription(s);
            }
        }

        s.append(')');

        final List<TypeReference> thrownTypes = getThrownTypes();

        if (!thrownTypes.isEmpty()) {
            s.append(" throws ");

            for (int i = 0, n = thrownTypes.size(); i < n; ++i) {
                final TypeReference t = thrownTypes.get(i);
                if (i != 0) {
                    s.append(", ");
                }
                s = t.appendBriefDescription(s);
            }
        }

        return s;
    }

    @Override
    public StringBuilder appendSimpleDescription(final StringBuilder sb) {
        StringBuilder s = new StringBuilder();

        for (final javax.lang.model.element.Modifier modifier : Flags.asModifierSet(getModifiers())) {
            s.append(modifier.toString());
            s.append(' ');
        }

        final List<? extends TypeReference> typeArguments;

        if (this instanceof IGenericInstance) {
            typeArguments = ((IGenericInstance) this).getTypeArguments();
        }
        else if (hasGenericParameters()) {
            typeArguments = getGenericParameters();
        }
        else {
            typeArguments = Collections.emptyList();
        }

        if (!typeArguments.isEmpty()) {
            s.append('<');
            for (int i = 0, n = typeArguments.size(); i < n; i++) {
                if (i != 0) {
                    s.append(", ");
                }
                s = typeArguments.get(i).appendSimpleDescription(s);
            }
            s.append('>');
            s.append(' ');
        }

        TypeReference returnType = getReturnType();

        while (returnType.isWildcardType()) {
            returnType = returnType.getExtendsBound();
        }

        if (returnType.isGenericParameter()) {
            s.append(returnType.getName());
        }
        else {
            s = returnType.appendSimpleDescription(s);
        }

        s.append(' ');
        s.append(getName());
        s.append('(');

        final List<ParameterDefinition> parameters = getParameters();

        for (int i = 0, n = parameters.size(); i < n; ++i) {
            final ParameterDefinition p = parameters.get(i);
            
            if (i != 0) {
                s.append(", ");
            }

            TypeReference parameterType = p.getParameterType();

            while (parameterType.isWildcardType()) {
                parameterType = parameterType.getExtendsBound();
            }

            if (parameterType.isGenericParameter()) {
                s.append(parameterType.getName());
            }
            else {
                s = parameterType.appendSimpleDescription(s);
            }
        }

        s.append(')');

        final List<TypeReference> thrownTypes = getThrownTypes();

        if (!thrownTypes.isEmpty()) {
            s.append(" throws ");

            for (int i = 0, n = thrownTypes.size(); i < n; ++i) {
                final TypeReference t = thrownTypes.get(i);
                if (i != 0) {
                    s.append(", ");
                }
                s = t.appendSimpleDescription(s);
            }
        }

        return s;
    }

    @Override
    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        StringBuilder s = new StringBuilder();

        TypeReference returnType = getReturnType();

        while (returnType.isWildcardType()) {
            returnType = returnType.getExtendsBound();
        }

        if (returnType.isGenericParameter()) {
            s.append(returnType.getName());
        }
        else {
            s = returnType.appendBriefDescription(s);
        }

        s.append(' ');
        s.append(getName());
        s.append('(');

        final List<ParameterDefinition> parameters = getParameters();

        for (int i = 0, n = parameters.size(); i < n; ++i) {
            final ParameterDefinition p = parameters.get(i);

            if (i != 0) {
                s.append(", ");
            }

            TypeReference parameterType = p.getParameterType();

            while (parameterType.isWildcardType()) {
                parameterType = parameterType.getExtendsBound();
            }

            if (parameterType.isGenericParameter()) {
                s.append(parameterType.getName());
            }
            else {
                s = parameterType.appendBriefDescription(s);
            }
        }

        s.append(')');

        return s;
    }

    @Override
    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        if (hasGenericParameters() && !isGenericDefinition()) {
            final MethodDefinition definition = resolve();
            if (definition != null) {
                return definition.appendErasedDescription(sb);
            }
        }

        for (final javax.lang.model.element.Modifier modifier : Flags.asModifierSet(getModifiers())) {
            sb.append(modifier.toString());
            sb.append(' ');
        }

        final List<ParameterDefinition> parameterTypes = getParameters();

        StringBuilder s = getReturnType().appendErasedDescription(sb);

        s.append(' ');
        s.append(getName());
        s.append('(');

        for (int i = 0, n = parameterTypes.size(); i < n; ++i) {
            if (i != 0) {
                s.append(", ");
            }
            s = parameterTypes.get(i).getParameterType().appendErasedDescription(s);
        }

        s.append(')');
        return s;
    }

    @Override
    public StringBuilder appendSignature(final StringBuilder sb) {
        final List<ParameterDefinition> parameters = getParameters();

        StringBuilder s = sb;
        s.append('(');

        for (int i = 0, n = parameters.size(); i < n; ++i) {
            final ParameterDefinition p = parameters.get(i);
            s = p.getParameterType().appendSignature(s);
        }

        s.append(')');
        s = getReturnType().appendSignature(s);

        return s;
    }

    @Override
    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        StringBuilder s = sb;
        s.append('(');

        final List<ParameterDefinition> parameterTypes = getParameters();

        for (int i = 0, n = parameterTypes.size(); i < n; ++i) {
            s = parameterTypes.get(i).getParameterType().appendErasedSignature(s);
        }

        s.append(')');
        s = getReturnType().appendErasedSignature(s);

        return s;
    }

    // </editor-fold>
}

