package com.strobel.reflection;

import java.lang.reflect.Modifier;

/**
 * @author Mike Strobel
 */
public final class BindingFlags {
    private BindingFlags() {}

    public static final int Default = 0x0;
    public static final int IgnoreCase = 0x1;
    public static final int DeclaredOnly = 0x2;
    public static final int Instance = 0x4;
    public static final int Static = 0x8;
    public static final int Public = 0x10;
    public static final int NonPublic = 0x20;
    public static final int FlattenHierarchy = 0x40;
    public static final int InvokeMethod = 0x100;
    public static final int CreateInstance = 0x200;
    public static final int GetField = 0x400;
    public static final int SetField = 0x800;
    public static final int ExactBinding = 0x10000;
    public static final int SuppressChangeType = 0x20000;
    public static final int OptionalParamBinding = 0x40000;
    public static final int IgnoreReturn = 0x1000000;

    public static boolean isSet(final int bindingFlags, final int test) {
        return (bindingFlags & test) == test;
    }

    static int fromMember(final MemberInfo member) {
        if (member instanceof Type) {
            return fromTypeOrFieldModifiers(member.getModifiers());
        }
        if (member instanceof FieldInfo) {
            return fromTypeOrFieldModifiers(member.getModifiers());
        }
        return fromMethodModifiers(member.getModifiers());
    }

    private static int fromMethodModifiers(final int modifiers) {
        int bindingFlags = Default;

        if (Modifier.isPublic(modifiers)) {
            bindingFlags |= Public;
        }
        else {
            bindingFlags |= NonPublic;
        }

        if (Modifier.isStatic(modifiers)) {
            bindingFlags |= Static;
        }
        else {
            bindingFlags |= Instance;
        }

        return bindingFlags;
    }

    private static int fromTypeOrFieldModifiers(final int modifiers) {
        int bindingFlags = Default;

        if (Modifier.isPublic(modifiers)) {
            bindingFlags |= Public;
        }
        else {
            bindingFlags |= NonPublic;
        }

        if (Modifier.isStatic(modifiers)) {
            bindingFlags |= Static;
        }

        return bindingFlags;
    }
}
