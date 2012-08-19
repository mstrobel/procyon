package com.strobel.reflection.emit;

/**
 * @author Mike Strobel
 */
public interface EnumSwitchCallback<E extends Enum<E>> {
    void emitCase(final E key, final Label breakTarget) throws Exception;
    void emitDefault(final Label breakTarget) throws Exception;
}
