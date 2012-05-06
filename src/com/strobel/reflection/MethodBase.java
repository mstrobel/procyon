package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public abstract class MethodBase extends MemberInfo {
    public ParameterList getParameters() {
        return ParameterList.empty();
    }

    public CallingConvention getCallingConvention() {
        return CallingConvention.fromMethodModifiers(getModifiers());
    }

    @Override
    public String toString() {
        return getSignature();
    }

    public String getSignature() {
        return appendSignature(new StringBuilder()).toString();
    }

    public String getErasedSignature() {
        return appendErasedSignature(new StringBuilder()).toString();
    }

    public String getDescription() {
        return appendDescription(new StringBuilder()).toString();
    }

    public String getErasedDescription() {
        return appendErasedDescription(new StringBuilder()).toString();
    }

    public StringBuilder appendDescription(final StringBuilder sb) {
        final ParameterList parameters = getParameters();

        StringBuilder s = sb;
        s.append('(');

        for (int i = 0, n = parameters.size(); i < n; ++i) {
            final ParameterInfo p = parameters.get(i);
            s = p.getParameterType().appendSignature(s);
        }

        s.append(')');
        return s;
    }

    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        final ParameterList parameters = getParameters();

        StringBuilder s = sb;
        s.append('(');

        for (int i = 0, n = parameters.size(); i < n; ++i) {
            final ParameterInfo p = parameters.get(i);
            s = p.getParameterType().appendErasedSignature(s);
        }

        s.append(')');
        return s;
    }

    public StringBuilder appendSignature(final StringBuilder sb) {
        return appendDescription(sb.append(getName()));
    }

    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        return appendErasedDescription(sb.append(getName()));
    }
}
