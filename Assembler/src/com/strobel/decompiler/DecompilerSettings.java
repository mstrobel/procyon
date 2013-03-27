/*
 * DecompilerSettings.java
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

package com.strobel.decompiler;

import com.sampullara.cli.Argument;
import com.strobel.decompiler.ast.AstOptimizationStep;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

public class DecompilerSettings {
    private boolean _printUsage;
    private boolean _alwaysGenerateExceptionVariableForCatchBlocks;
    private AstOptimizationStep _abortBeforeStep = AstOptimizationStep.None;
    private JavaFormattingOptions _formattingOptions;

    public JavaFormattingOptions getFormattingOptions() {
        return _formattingOptions;
    }

    public void setFormattingOptions(final JavaFormattingOptions formattingOptions) {
        _formattingOptions = formattingOptions;
    }

    public final boolean getPrintUsage() {
        return _printUsage;
    }

    @Argument(value = "?", description = "Display this usage information and exit.")
    public final void setPrintUsage(final boolean printUsage) {
        _printUsage = printUsage;
    }

    public final AstOptimizationStep getAbortBeforeStep() {
        return _abortBeforeStep;
    }

    @Argument(value = "a", description = "Abort before this step.")
    public final void setAbortBeforeStep(final AstOptimizationStep abortBeforeStep) {
        _abortBeforeStep = abortBeforeStep != null ? abortBeforeStep : AstOptimizationStep.None;
    }

    public final boolean getAlwaysGenerateExceptionVariableForCatchBlocks() {
        return _alwaysGenerateExceptionVariableForCatchBlocks;
    }

    @Argument(value = "ev", description = "Always generate exception variables for catch blocks.")
    public final void setAlwaysGenerateExceptionVariableForCatchBlocks(final boolean value) {
        _alwaysGenerateExceptionVariableForCatchBlocks = value;
    }
}
