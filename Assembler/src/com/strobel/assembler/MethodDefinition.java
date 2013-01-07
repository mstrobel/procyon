package com.strobel.assembler;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 4:50 PM
 */
public abstract class MethodDefinition extends MethodReference implements IMemberDefinition {
    final static String CONSTRUCTOR_NAME = "<init>";
    final static String CLASS_CONSTRUCTOR_NAME = "<clinit>";

    @Override
    public boolean isSpecialName() {
        return CONSTRUCTOR_NAME.equals(getName()) ||
               CLASS_CONSTRUCTOR_NAME.equals(getName());
    }
}
