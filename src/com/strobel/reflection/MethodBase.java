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
}
