package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public abstract class MethodBase extends MemberInfo {
    public ParameterCollection getParameters() {
        return ParameterCollection.empty();
    }

    public CallingConvention getCallingConvention() {
        return CallingConvention.fromMethodModifiers(getModifiers());
    }
}
