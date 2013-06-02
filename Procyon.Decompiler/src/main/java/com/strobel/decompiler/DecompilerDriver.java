package com.strobel.decompiler;

import com.beust.jcommander.JCommander;
import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.CompositeTypeLoader;
import com.strobel.assembler.metadata.JarTypeLoader;
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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
            System.err.println(ExceptionUtilities.getMessage(t));
            System.exit(-1);
            return;
        }

        final String jarFile = options.getJarFile();
        final boolean decompileJar = !StringUtilities.isNullOrWhitespace(jarFile);

        if (options.getPrintUsage() ||
            typeNames.isEmpty() && !decompileJar) {

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

        final DecompilationOptions decompilationOptions = new DecompilationOptions();

        decompilationOptions.setSettings(settings);
        decompilationOptions.setFullDecompilation(true);

        if (settings.getFormattingOptions() == null) {
            settings.setFormattingOptions(JavaFormattingOptions.createDefault());
        }

        if (decompileJar) {
            try {
                decompileJar(jarFile, decompilationOptions);
            }
            catch (Throwable t) {
                System.err.println(ExceptionUtilities.getMessage(t));
                System.exit(-1);
            }
        }
        else {
            final MetadataSystem metadataSystem = new MetadataSystem(settings.getTypeLoader());

            for (final String typeName : typeNames) {
                try {
                    decompileType(metadataSystem, typeName, decompilationOptions, true);
                }
                catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    private static void decompileJar(
        final String jarFilePath,
        final DecompilationOptions decompilationOptions) throws IOException {

        final File jarFile = new File(jarFilePath);

        if (!jarFile.exists()) {
            throw new FileNotFoundException("File not found: " + jarFilePath);
        }

        final DecompilerSettings settings = decompilationOptions.getSettings();
        final JarFile jar = new JarFile(jarFile);
        final Enumeration<JarEntry> entries = jar.entries();

        settings.setTypeLoader(
            new CompositeTypeLoader(
                new JarTypeLoader(jar),
                settings.getTypeLoader()
            )
        );

        final MetadataSystem metadataSystem = new MetadataSystem(settings.getTypeLoader());

        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            final String name = entry.getName();

            if (!name.endsWith(".class")) {
                continue;
            }

            final String internalName = StringUtilities.removeRight(name, ".class");

            try {
                decompileType(metadataSystem, internalName, decompilationOptions, !settings.getShowNestedTypes());
            }
            catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static void decompileType(
        final MetadataSystem metadataSystem,
        final String typeName,
        final DecompilationOptions options,
        final boolean includeNested) throws IOException {

        final DecompilerSettings settings = options.getSettings();
        final TypeReference type = metadataSystem.lookupType(typeName);
        final TypeDefinition resolvedType;

        if (type == null || (resolvedType = type.resolve()) == null) {
            System.err.printf("!!! ERROR: Failed to load class %s.\n", typeName);
            return;
        }

        if (type.isNested() && !includeNested) {
            return;
        }

        final Writer writer = createWriter(resolvedType, settings);

        final boolean writeToFile = writer instanceof FileWriter;

        final PlainTextOutput output = writeToFile ? new PlainTextOutput(writer)
                                                   : new AnsiTextOutput(writer);

        if (settings.getLanguage() instanceof BytecodeLanguage) {
            output.setIndentToken("  ");
        }

        if (writeToFile) {
            System.out.printf("Decompiling %s...\n", typeName);
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
