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

import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.Languages;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

public class DecompilerSettings {
    private ITypeLoader _typeLoader;
    private boolean _showSyntheticMembers;
    private boolean _alwaysGenerateExceptionVariableForCatchBlocks;
    private boolean _forceExplicitImports;
    private boolean _flattenSwitchBlocks;
    private boolean _showNestedTypes;
    private JavaFormattingOptions _formattingOptions;
    private Language _language;
    private String _outputFileHeaderText;

    public DecompilerSettings() {
    }

    public final boolean getShowNestedTypes() {
        return _showNestedTypes;
    }

    public final void setShowNestedTypes(final boolean showNestedTypes) {
        _showNestedTypes = showNestedTypes;
    }

    public final boolean getFlattenSwitchBlocks() {
        return _flattenSwitchBlocks;
    }

    public final void setFlattenSwitchBlocks(final boolean flattenSwitchBlocks) {
        _flattenSwitchBlocks = flattenSwitchBlocks;
    }

    public final boolean getForceExplicitImports() {
        return _forceExplicitImports;
    }

    public final void setForceExplicitImports(final boolean forceExplicitImports) {
        _forceExplicitImports = forceExplicitImports;
    }

    public final String getOutputFileHeaderText() {
        return _outputFileHeaderText;
    }

    public final void setOutputFileHeaderText(final String outputFileHeaderText) {
        _outputFileHeaderText = outputFileHeaderText;
    }

    public final ITypeLoader getTypeLoader() {
        return _typeLoader;
    }

    public final void setTypeLoader(final ITypeLoader typeLoader) {
        _typeLoader = typeLoader;
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
