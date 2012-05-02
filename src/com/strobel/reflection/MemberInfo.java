package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public abstract class MemberInfo implements java.lang.reflect.AnnotatedElement {
    final static int ENUM_MODIFIER = 0x00004000;
    final static int VARARGS_MODIFIER = 0x00000080;

    MemberInfo() {}

    public abstract MemberType getMemberType();
    public abstract String getName();
    public abstract Type getDeclaringType();

    abstract int getModifiers();
}
