package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public abstract class MethodBase extends MemberInfo {
    public ParameterList getParameters() {
        return ParameterList.empty();
    }

    public TypeList getThrownTypes() {
        return TypeList.empty();
    }

    public CallingConvention getCallingConvention() {
        return CallingConvention.fromMethodModifiers(getModifiers());
    }

    @Override
    public String toString() {
        return getSimpleDescription();
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

    public String getSimpleDescription() {
        return appendSimpleDescription(new StringBuilder()).toString();
    }

    public String getErasedDescription() {
        return appendErasedDescription(new StringBuilder()).toString();
    }

    public abstract StringBuilder appendDescription(final StringBuilder sb);
    public abstract StringBuilder appendSimpleDescription(final StringBuilder sb);
    public abstract StringBuilder appendErasedDescription(final StringBuilder sb);
    public abstract StringBuilder appendSignature(final StringBuilder sb);
    public abstract StringBuilder appendErasedSignature(final StringBuilder sb);
}
