package com.strobel.assembler.metadata;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 5:37 PM
 */
public interface IMemberDefinition {
    String getName();
    String getFullName();
    boolean isSpecialName();
    TypeReference getDeclaringType();
    long getFlags();
    int getModifiers();
    boolean isFinal();
    boolean isNonPublic();
    boolean isPrivate();
    boolean isProtected();
    boolean isPublic();
    boolean isStatic();
    boolean isSynthetic();
    boolean isDeprecated();
    boolean isPackagePrivate();
    String getBriefDescription();
    String getDescription();
    String getErasedDescription();
    String getSimpleDescription();
}
