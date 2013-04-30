/*
 * Decompiler.java
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

import com.beust.jcommander.JCommander;
import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.languages.Languages;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

import java.io.StringWriter;
import java.util.List;

public final class Decompiler {
    public static void decompile(final String internalName, final ITextOutput output) {
        decompile(internalName, output, new DecompilerSettings());
    }

    public static void decompile(final String internalName, final ITextOutput output, final DecompilerSettings settings) {
        VerifyArgument.notNull(internalName, "internalName");
        VerifyArgument.notNull(settings, "settings");

        final ITypeLoader typeLoader = settings.getTypeLoader() != null ? settings.getTypeLoader() : new ClasspathTypeLoader();
        final MetadataSystem metadataSystem = new MetadataSystem(typeLoader);
        final TypeReference type = metadataSystem.lookupType(internalName);
        final TypeDefinition resolvedType;

        if (type == null || (resolvedType = type.resolve()) == null) {
            output.writeLine("!!! ERROR: Failed to load class %s.", internalName);
            return;
        }

        final DecompilationOptions options = new DecompilationOptions();

        options.setSettings(settings);
        options.setFullDecompilation(true);

        if (settings.getFormattingOptions() == null) {
            settings.setFormattingOptions(JavaFormattingOptions.createDefault());
        }

        settings.getLanguage().decompileType(resolvedType, output, options);
    }

    public static void main(final String[] args) {
        final CommandLineOptions options = new CommandLineOptions();
        final JCommander jCommander;
        final List<String> typeNames;

        try {
            jCommander = new JCommander(options);
            jCommander.setAllowAbbreviatedOptions(true);
            jCommander.parse(args);
            typeNames = options.getClassNames();
        }
        catch (Throwable t) {
            System.err.println(t.getMessage());
            System.exit(-1);
            return;
        }

        if (options.getPrintUsage() || typeNames.isEmpty()) {
            jCommander.usage();
            return;
        }

        final DecompilerSettings settings = new DecompilerSettings();

        settings.setFlattenSwitchBlocks(options.getFlattenSwitchBlocks());
        settings.setForceExplicitImports(options.getForceExplicitImports());
        settings.setShowSyntheticMembers(options.getShowSyntheticMembers());
        settings.setShowNestedTypes(options.getShowNestedTypes());
        settings.setTypeLoader(new InputTypeLoader());

        final StringWriter writer = new StringWriter();
        final PlainTextOutput printer = new AnsiTextOutput(writer);

        if (options.isRawBytecode()) {
            settings.setLanguage(Languages.bytecode());
            printer.setIndentToken("  ");
        }
        else if (options.isBytecodeAst()) {
            settings.setLanguage(options.isUnoptimized() ? Languages.bytecodeAstUnoptimized()
                                                         : Languages.bytecodeAst());
        }

        for (final String typeName : typeNames) {
            decompile(typeName, printer, settings);
            System.out.print(printer.toString());
            writer.getBuffer().setLength(0);
        }
    }
}
