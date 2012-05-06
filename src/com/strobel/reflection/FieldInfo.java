package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public abstract class FieldInfo extends MemberInfo {
    public abstract Type getFieldType();
    public abstract boolean isEnumConstant();
}
