/*
 * DecompilerSettings.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler;

import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.Languages;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

public class DecompilerSettings {
    private boolean _showSyntheticMembers;
    private boolean _alwaysGenerateExceptionVariableForCatchBlocks;
    private JavaFormattingOptions _formattingOptions;
    private Language _language;

    public DecompilerSettings() {
    }

    public final Language getLanguage() {
        return _language != null ? _language : Languages.java();
    }

    public final void setLanguage(final Language language) {
        _language = language;
    }

    public final boolean getShowSyntheticMembers() {
        return _showSyntheticMembers;
    }

    public final void setShowSyntheticMembers(final boolean showSyntheticMembers) {
        _showSyntheticMembers = showSyntheticMembers;
    }

    public final JavaFormattingOptions getFormattingOptions() {
        return _formattingOptions;
    }

    public final void setFormattingOptions(final JavaFormattingOptions formattingOptions) {
        _formattingOptions = formattingOptions;
    }

    public final boolean getAlwaysGenerateExceptionVariableForCatchBlocks() {
        return _alwaysGenerateExceptionVariableForCatchBlocks;
    }

    public final void setAlwaysGenerateExceptionVariableForCatchBlocks(final boolean value) {
        _alwaysGenerateExceptionVariableForCatchBlocks = value;
    }
}
