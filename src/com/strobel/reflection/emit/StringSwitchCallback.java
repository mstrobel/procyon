package com.strobel.reflection.emit;

/**
 * @author Mike Strobel
 */
public interface StringSwitchCallback {
    void emitCase(final String key, final Label breakTarget) throws Exception;
    void emitDefault(final Label breakTarget) throws Exception;
}
