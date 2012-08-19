package com.strobel.reflection.emit;

/**
 * @author Mike Strobel
 */
public interface SwitchCallback {
    void emitCase(final int key, final Label breakTarget) throws Exception;
    void emitDefault(final Label breakTarget) throws Exception;
}
