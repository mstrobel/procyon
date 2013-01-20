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
}
