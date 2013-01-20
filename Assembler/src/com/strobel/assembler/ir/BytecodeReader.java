package com.strobel.assembler.ir;

import com.strobel.assembler.metadata.TypeDefinition;

import java.io.InputStream;

/**
 * @author Mike Strobel
 */
public interface BytecodeReader {
    public TypeDefinition read(final InputStream in);
}
