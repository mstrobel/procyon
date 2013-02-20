/*
 * DisassemblerOptions.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler;

import com.sampullara.cli.Argument;

public final class DisassemblerOptions {
    private boolean _printConstantPool;
    private boolean _printUsage;
    private boolean _printLineNumbers;
    private boolean _printFields;

    public boolean getPrintLineNumbers() {
        return _printLineNumbers;
    }

    @Argument(value = "l", description = "Include instruction line numbers (if available).")
    public void setPrintLineNumbers(final boolean printLineNumbers) {
        _printLineNumbers = printLineNumbers;
    }

    public boolean getPrintFields() {
        return _printFields;
    }

    @Argument(value = "f", description = "Print field descriptions and attributes.")
    public void setPrintFields(final boolean printFields) {
        _printFields = printFields;
    }

    public boolean getPrintUsage() {
        return _printUsage;
    }

    @Argument(value = "?", description = "Display this usage information and exit.")
    public void setPrintUsage(final boolean printUsage) {
        _printUsage = printUsage;
    }

    @Argument(value = "v", description = "Enable verbose output (equivalent to -c -f -l).")
    public void setVerbose(final boolean verbose) {
        if (verbose) {
            setPrintConstantPool(true);
            setPrintFields(true);
            setPrintLineNumbers(true);
        }
    }

    public boolean getPrintConstantPool() {
        return _printConstantPool;
    }

    @Argument(value = "c", description = "Print the contents of the constant pool.")
    public void setPrintConstantPool(final boolean printConstantPool) {
        _printConstantPool = printConstantPool;
    }
}
