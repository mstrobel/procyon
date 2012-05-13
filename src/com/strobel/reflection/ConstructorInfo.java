package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.lang.reflect.Constructor;

/**
 * @author Mike Strobel
 */
public abstract class ConstructorInfo extends MethodBase {
    @Override
    public final MemberType getMemberType() {
        return MemberType.Constructor;
    }

    public abstract Constructor<?> getRawConstructor();

}

class ReflectedConstructor extends ConstructorInfo {
    private final Type _declaringType;
    private final ParameterList _parameters;
    private final Constructor _rawConstructor;
    private final TypeList _thrownTypes;

    ReflectedConstructor(final Type declaringType, final Constructor rawConstructor, final ParameterList parameters, final TypeList thrownTypes) {
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _rawConstructor = VerifyArgument.notNull(rawConstructor, "rawConstructor");
        _parameters = VerifyArgument.notNull(parameters, "parameters");
        _thrownTypes = VerifyArgument.notNull(thrownTypes, "thrownTypes");
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
    public String getName() {
        return _rawConstructor.getName();
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    public Constructor<?> getRawConstructor() {
        return _rawConstructor;
    }

    @Override
    int getModifiers() {
        return _rawConstructor.getModifiers();
    }

    @Override
    public StringBuilder appendDescription(final StringBuilder sb) {
        StringBuilder s = new StringBuilder();

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
}
