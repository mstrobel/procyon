package com.strobel.assembler;

/**
 * @author Mike Strobel
 */
public interface IConstantValueProvider {
    boolean hasConstantValue();
    Object getConstantValue();
}
