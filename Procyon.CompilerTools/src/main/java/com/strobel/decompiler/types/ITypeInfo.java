package com.strobel.decompiler.types;

import com.strobel.collections.ImmutableList;

public interface ITypeInfo {
    String getName();
    String getPackageName();
    String getFullName();
    String getCanonicalName();
    String getInternalName();
    String getSignature();

    boolean isArray();
    boolean isPrimitive();
    boolean isPrimitiveOrVoid();
    boolean isVoid();
    boolean isRawType();
    boolean isGenericType();
    boolean isGenericTypeInstance();
    boolean isGenericTypeDefinition();
    boolean isGenericParameter();
    boolean isWildcard();
    boolean isUnknownType();
    boolean isBound();
    boolean isAnonymous();
    boolean isLocal();

    boolean hasConstraints();
    boolean hasSuperConstraint();
    boolean hasExtendsConstraint();

    ITypeInfo getDeclaringType();

    ITypeInfo getElementType();
    ITypeInfo getSuperConstraint();
    ITypeInfo getExtendsConstraint();
    ITypeInfo getSuperClass();

    ImmutableList<ITypeInfo> getSuperInterfaces();
    ImmutableList<ITypeInfo> getGenericParameters();
    ImmutableList<ITypeInfo> getTypeArguments();

    ITypeInfo getGenericDefinition();

    void addListener(ITypeListener listener);
    void removeListener(ITypeListener listener);
}
