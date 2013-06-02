/*
 * CommandLineOptions.java
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

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

public class CommandLineOptions {
    @Parameter(description = "<class files or names>")
    private final List<String> _classNames = new ArrayList<>();

    @Parameter(names = { "-?", "--help" }, help = true, description = "Display this usage information and exit.")
    private boolean _printUsage;

    @Parameter(names = { "-ei", "--explicit-imports" }, description = "Force explicit type imports (never import '*').")
    private boolean _forceExplicitImports;

    @Parameter(names = { "-fsb", "--flatten-switch-blocks" }, description = "Remove block statements around switch sections when possible.")
    private boolean _flattenSwitchBlocks;

    @Parameter(names = { "-s", "--show-synthetic" }, description = "Show synthetic (compiler-generated) members.")
    private boolean _showSyntheticMembers;

    @Parameter(names = { "-b", "--bytecode-ast" }, description = "Output Bytecode AST instead of Java.")
    private boolean _bytecodeAst;

    @Parameter(names = { "-r", "--raw-bytecode" }, description = "Output Raw Bytecode instead of Java.")
    private boolean _rawBytecode;

    @Parameter(names = { "-u", "--unoptimized" }, description = "Show unoptimized code (only in combination with -b).")
    private boolean _unoptimized;

    @Parameter(names = { "-n", "--show-nested" }, description = "Decompile nested types along with their enclosing type.")
    private boolean _showNestedTypes;

    @Parameter(names = { "-o", "--output-directory" }, description = "Write decompiled results to specified directory instead of stdout.")
    private String _outputDirectory;

    @Parameter(names = { "-jar", "--jar-file" }, description = "Decompile all classes in the specified jar file.")
    private String _jarFile;

    public final List<String> getClassNames() {
        return _classNames;
    }

    public final boolean isBytecodeAst() {
        return _bytecodeAst;
    }

    public final boolean isRawBytecode() {
        return _rawBytecode;
    }

    public final boolean getFlattenSwitchBlocks() {
        return _flattenSwitchBlocks;
    }

    public final boolean getShowNestedTypes() {
        return _showNestedTypes;
    }

    public final void setShowNestedTypes(final boolean showNestedTypes) {
        _showNestedTypes = showNestedTypes;
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

    public final void setRawBytecode(final boolean rawBytecode) {
        _rawBytecode = rawBytecode;
    }

    public final void setBytecodeAst(final boolean bytecodeAst) {
        _bytecodeAst = bytecodeAst;
    }

    public final boolean isUnoptimized() {
        return _unoptimized;
    }

    public final void setUnoptimized(final boolean unoptimized) {
        _unoptimized = unoptimized;
    }

    public final boolean getShowSyntheticMembers() {
        return _showSyntheticMembers;
    }

    public final void setShowSyntheticMembers(final boolean showSyntheticMembers) {
        _showSyntheticMembers = showSyntheticMembers;
    }

    public final boolean getPrintUsage() {
        return _printUsage;
    }

    public final void setPrintUsage(final boolean printUsage) {
        _printUsage = printUsage;
    }

    public final String getOutputDirectory() {
        return _outputDirectory;
    }

    public final void setOutputDirectory(final String outputDirectory) {
        _outputDirectory = outputDirectory;
    }

    public final String getJarFile() {
        return _jarFile;
    }

    public final void setJarFile(final String jarFile) {
        _jarFile = jarFile;
    }
}
