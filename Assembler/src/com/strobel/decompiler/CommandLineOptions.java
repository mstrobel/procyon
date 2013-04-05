/*
 * CommandLineOptions.java
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

public class CommandLineOptions {
    private boolean _printUsage;
    private boolean _showSyntheticMembers;
    private boolean _alwaysGenerateExceptionVariableForCatchBlocks;
    private String _language;

    public final String getLanguage() {
        return _language;
    }

    @Argument(value = "l", description = "Specify output language.")
    public final void setLanguage(final String language) {
        _language = language;
    }

    public final boolean getShowSyntheticMembers() {
        return _showSyntheticMembers;
    }

    @Argument(value = "s", description = "Show synthetic (compiler-generated) members.")
    public final void setShowSyntheticMembers(final boolean showSyntheticMembers) {
        _showSyntheticMembers = showSyntheticMembers;
    }

    public final boolean getPrintUsage() {
        return _printUsage;
    }

    @Argument(value = "?", description = "Display this usage information and exit.")
    public final void setPrintUsage(final boolean printUsage) {
        _printUsage = printUsage;
    }

    public final boolean getAlwaysGenerateExceptionVariableForCatchBlocks() {
        return _alwaysGenerateExceptionVariableForCatchBlocks;
    }

    @Argument(value = "ev", description = "Always generate exception variables for catch blocks.")
    public final void setAlwaysGenerateExceptionVariableForCatchBlocks(final boolean value) {
        _alwaysGenerateExceptionVariableForCatchBlocks = value;
    }
}
