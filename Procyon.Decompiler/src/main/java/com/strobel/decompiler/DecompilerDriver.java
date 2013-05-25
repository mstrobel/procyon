package com.strobel.decompiler;

import com.beust.jcommander.JCommander;
import com.strobel.assembler.InputTypeLoader;
import com.strobel.decompiler.languages.Languages;

import java.io.StringWriter;
import java.util.List;

public class DecompilerDriver {
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
            Decompiler.decompile(typeName, printer, settings);
            System.out.print(printer.toString());
            writer.getBuffer().setLength(0);
        }
    }
}
