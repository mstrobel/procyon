package com.strobel.decompiler;

import com.beust.jcommander.JCommander;
import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.ExceptionUtilities;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.languages.BytecodeLanguage;
import com.strobel.decompiler.languages.Languages;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import com.strobel.io.PathHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
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
        settings.setOutputDirectory(options.getOutputDirectory());
        settings.setTypeLoader(new InputTypeLoader());

        final StringWriter writer = new StringWriter();
        final ITextOutput printer = new AnsiTextOutput(writer);

        if (options.isRawBytecode()) {
            settings.setLanguage(Languages.bytecode());
            printer.setIndentToken("  ");
        }
        else if (options.isBytecodeAst()) {
            settings.setLanguage(
                options.isUnoptimized() ? Languages.bytecodeAstUnoptimized()
                                        : Languages.bytecodeAst()
            );
        }

        final MetadataSystem metadataSystem = new MetadataSystem(settings.getTypeLoader());
        final DecompilationOptions decompilationOptions = new DecompilationOptions();

        decompilationOptions.setSettings(settings);
        decompilationOptions.setFullDecompilation(true);

        if (settings.getFormattingOptions() == null) {
            settings.setFormattingOptions(JavaFormattingOptions.createDefault());
        }

        try {
            for (final String typeName : typeNames) {
                decompileType(metadataSystem, typeName, decompilationOptions);
            }
        }
        catch (Throwable t) {
            System.err.println(ExceptionUtilities.getMessage(t));
            System.exit(-1);
            return;
        }

        System.out.flush();
    }

    private static void decompileType(
        final MetadataSystem metadataSystem,
        final String typeName,
        final DecompilationOptions options) throws IOException {

        final DecompilerSettings settings = options.getSettings();

        final TypeReference type = metadataSystem.lookupType(typeName);
        final TypeDefinition resolvedType;

        if (type == null || (resolvedType = type.resolve()) == null) {
            System.err.printf("!!! ERROR: Failed to load class %s.\n", typeName);
            return;
        }

        final Writer writer = createWriter(resolvedType, settings);

        final PlainTextOutput output = writer instanceof FileWriter ? new PlainTextOutput(writer)
                                                                    : new AnsiTextOutput(writer);

        if (settings.getLanguage() instanceof BytecodeLanguage) {
            output.setIndentToken("  ");
        }

        settings.getLanguage().decompileType(resolvedType, output, options);

        writer.flush();
    }

    private static Writer createWriter(final TypeDefinition type, final DecompilerSettings settings) throws IOException {
        final String outputDirectory = settings.getOutputDirectory();

        if (StringUtilities.isNullOrWhitespace(outputDirectory)) {
            return new OutputStreamWriter(System.out);
        }

        final String outputPath;
        final String fileName = type.getName() + settings.getLanguage().getFileExtension();
        final String packageName = type.getPackageName();

        if (StringUtilities.isNullOrWhitespace(packageName)) {
            outputPath = PathHelper.combine(outputDirectory, fileName);
        }
        else {
            outputPath = PathHelper.combine(
                outputDirectory,
                packageName.replace('.', PathHelper.DirectorySeparator),
                fileName
            );
        }

        final File outputFile = new File(outputPath);

        final File parentFile = outputFile.getParentFile();

        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new IllegalStateException(
                String.format(
                    "Could not create output directory for file \"%s\".",
                    outputPath
                )
            );
        }

        if (!outputFile.exists() && !outputFile.createNewFile()) {
            throw new IllegalStateException(
                String.format(
                    "Could not create output file \"%s\".",
                    outputPath
                )
            );
        }

        return new FileWriter(outputFile);
    }
}
