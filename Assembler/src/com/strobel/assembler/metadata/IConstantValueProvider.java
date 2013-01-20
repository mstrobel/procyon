package com.strobel.assembler.metadata;

/**
 * @author Mike Strobel
 */
public interface IConstantValueProvider {
    boolean hasConstantValue();
    Object getConstantValue();
}
