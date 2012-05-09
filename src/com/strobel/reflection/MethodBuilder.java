package com.strobel.reflection;

import org.objectweb.asm.MethodVisitor;

/**
 * @author Mike Strobel
 */
public final class MethodBuilder {
    private MethodVisitor _methodWriter;

    public MethodVisitor getMethodWriter() {
        return _methodWriter;
    }
}
