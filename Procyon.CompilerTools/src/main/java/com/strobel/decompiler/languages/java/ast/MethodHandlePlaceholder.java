package com.strobel.decompiler.languages.java.ast;

import com.strobel.assembler.metadata.MethodHandle;

import static java.util.Objects.requireNonNull;

public final class MethodHandlePlaceholder extends BytecodeConstant {
    public MethodHandlePlaceholder(final MethodHandle handle) {
        super(requireNonNull(handle, "A method handle is required."));
    }

    public MethodHandle getHandle() {
        return (MethodHandle) super.getConstantValue();
    }
}
