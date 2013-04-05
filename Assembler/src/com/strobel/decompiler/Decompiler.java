/*
 * Decompiler.java
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

import com.sampullara.cli.Args;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.Languages;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import com.strobel.util.ContractUtils;

import java.util.List;

public final class Decompiler {
    public static void decompile(final String internalName, final ITextOutput output) {
        decompile(internalName, output, new DecompilerSettings());
    }

    public static void decompile(final String internalName, final ITextOutput output, final DecompilerSettings settings) {
        VerifyArgument.notNull(internalName, "internalName");
        VerifyArgument.notNull(settings, "settings");

        final MetadataSystem metadataSystem = MetadataSystem.instance();
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
        final PlainTextOutput printer = new AnsiTextOutput();
        final CommandLineOptions options = new CommandLineOptions();
        final List<String> typeNames;

        try {
            typeNames = Args.parse(options, args);
        }
        catch (Throwable t) {
            System.err.println(t.getMessage());
            System.exit(-1);
            throw ContractUtils.unreachable();
        }

        if (options.getPrintUsage()) {
            printUsageAndExit(options);
            return;
        }

        final DecompilerSettings settings = new DecompilerSettings();

        settings.setAlwaysGenerateExceptionVariableForCatchBlocks(options.getAlwaysGenerateExceptionVariableForCatchBlocks());
        settings.setShowSyntheticMembers(options.getShowSyntheticMembers());
        settings.setLanguage(decodeLanguage(options.getLanguage()));

        if (typeNames.isEmpty()) {
            decompile("com/strobel/decompiler/Decompiler", printer, settings);
        }
        else {
            for (final String typeName : typeNames) {
                decompile(typeName, printer, settings);
            }
        }

        System.out.print(printer.toString());
    }

    private static void printUsageAndExit(final CommandLineOptions options) {
        Args.usage(options);
        printLanguages();
    }

    private static Language decodeLanguage(final String language) {
        int languageIndex = -1;

        if (language != null) {
            try {
                languageIndex = Integer.parseInt(language) - 1;
            }
            catch (NumberFormatException e) {
                System.err.printf("Invalid language selection: %d.\n", languageIndex + 1);
                System.exit(-1);
            }
        }

        if (languageIndex >= 0) {
            if (languageIndex < Languages.all().size()) {
                return Languages.all().get(languageIndex);
            }
            else {
                System.err.printf("Invalid language selection: %d.\n", languageIndex + 1);
                System.exit(-1);
            }
        }

        return Languages.java();
    }

    private static void printLanguages() {
        System.out.printf("\nAvailable Languages:\n");

        final List<Language> all = Languages.all();

        for (int i = 0; i < all.size(); i++) {
            final Language language = all.get(i);
            System.out.printf("  %d) %s\n", i + 1, language.getName());
        }
    }
}
