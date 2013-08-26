package com.strobel.decompiler;

import com.beust.jcommander.JCommander;
import com.strobel.annotations.NotNull;
import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.*;
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
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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

        configureLogging(options);

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
        settings.setForceExplicitTypeArguments(options.getForceExplicitTypeArguments());
        settings.setRetainRedundantCasts(options.getRetainRedundantCasts());
        settings.setShowSyntheticMembers(options.getShowSyntheticMembers());
        settings.setShowNestedTypes(options.getShowNestedTypes());
        settings.setOutputDirectory(options.getOutputDirectory());
        settings.setIncludeLineNumbersInBytecode(options.getIncludeLineNumbers());
        settings.setRetainPointlessSwitches(options.getRetainPointlessSwitches());
        settings.setTypeLoader(new InputTypeLoader());

        if (options.isRawBytecode()) {
            settings.setLanguage(Languages.bytecode());
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
                decompileJar(jarFile, options, decompilationOptions);
            }
            catch (Throwable t) {
                System.err.println(ExceptionUtilities.getMessage(t));
                System.exit(-1);
            }
        }
        else {
            final MetadataSystem metadataSystem = new NoRetryMetadataSystem(settings.getTypeLoader());

            for (final String typeName : typeNames) {
                try {
                    decompileType(metadataSystem, typeName, options, decompilationOptions, true);
                }
                catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    private static void configureLogging(final CommandLineOptions options) {
        final Logger globalLogger = Logger.getGlobal();
        final Logger rootLogger = Logger.getAnonymousLogger().getParent();

        for (final Handler handler : globalLogger.getHandlers()) {
            globalLogger.removeHandler(handler);
        }

        for (final Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        final Level verboseLevel;

        switch (options.getVerboseLevel()) {
            case 0:
                verboseLevel = Level.SEVERE;
                break;
            case 1:
                verboseLevel = Level.FINE;
                break;
            case 2:
                verboseLevel = Level.FINER;
                break;
            case 3:
            default:
                verboseLevel = Level.FINEST;
                break;
        }

        globalLogger.setLevel(verboseLevel);
        rootLogger.setLevel(verboseLevel);

        final ConsoleHandler handler = new ConsoleHandler();

        handler.setLevel(verboseLevel);
        handler.setFormatter(new BriefLogFormatter());

        globalLogger.addHandler(handler);
        rootLogger.addHandler(handler);
    }

    private static void decompileJar(
        final String jarFilePath,
        final CommandLineOptions commandLineOptions,
        final DecompilationOptions decompilationOptions) throws IOException {

        final File jarFile = new File(jarFilePath);

        if (!jarFile.exists()) {
            throw new FileNotFoundException("File not found: " + jarFilePath);
        }

        final DecompilerSettings settings = decompilationOptions.getSettings();
        final JarFile jar = new JarFile(jarFile);
        final Enumeration<JarEntry> entries = jar.entries();

        settings.setShowNestedTypes(true);
        settings.setShowSyntheticMembers(false);

        settings.setTypeLoader(
            new CompositeTypeLoader(
                new JarTypeLoader(jar),
                settings.getTypeLoader()
            )
        );

        final MetadataSystem metadataSystem = new NoRetryMetadataSystem(settings.getTypeLoader());

        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            final String name = entry.getName();

            if (!name.endsWith(".class")) {
                continue;
            }

            final String internalName = StringUtilities.removeRight(name, ".class");

            try {
                decompileType(metadataSystem, internalName, commandLineOptions, decompilationOptions, false);
            }
            catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static void decompileType(
        final MetadataSystem metadataSystem,
        final String typeName,
        final CommandLineOptions commandLineOptions,
        final DecompilationOptions options,
        final boolean includeNested) throws IOException {

        final TypeReference type;
        final DecompilerSettings settings = options.getSettings();

        if (typeName.length() == 1) {
            //
            // Hack to get around classes whose descriptors clash with primitive types.
            //

            final MetadataParser parser = new MetadataParser(IMetadataResolver.EMPTY);
            final TypeReference reference = parser.parseTypeDescriptor(typeName);

            type = metadataSystem.resolve(reference);
        }
        else {
            type = metadataSystem.lookupType(typeName);
        }

        final TypeDefinition resolvedType;

        if (type == null || (resolvedType = type.resolve()) == null) {
            System.err.printf("!!! ERROR: Failed to load class %s.\n", typeName);
            return;
        }

        if (!includeNested && (resolvedType.isNested() || resolvedType.isAnonymous() || resolvedType.isSynthetic())) {
            return;
        }

        final Writer writer = createWriter(resolvedType, settings);
        final boolean writeToFile = writer instanceof FileWriter;
        final PlainTextOutput output;

        if (writeToFile) {
            output = new PlainTextOutput(writer);
        }
        else {
            output = new AnsiTextOutput(
                writer,
                commandLineOptions.getUseLightColorScheme() ? AnsiTextOutput.ColorScheme.LIGHT
                                                            : AnsiTextOutput.ColorScheme.DARK
            );
        }

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

final class BriefLogFormatter extends Formatter {
    private static final DateFormat format = new SimpleDateFormat("h:mm:ss");
    private static final String lineSep = System.getProperty("line.separator");

    /**
     * A Custom format implementation that is designed for brevity.
     */
    public String format(@NotNull final LogRecord record) {
        String loggerName = record.getLoggerName();
        if (loggerName == null) {
            loggerName = "root";
        }
        return new StringBuilder()
            .append(format.format(new Date(record.getMillis())))
            .append(" [")
            .append(record.getLevel())
            .append("] ")
            .append(loggerName)
            .append(": ")
            .append(record.getMessage()).append(' ')
            .append(lineSep).toString();
    }
}

final class NoRetryMetadataSystem extends MetadataSystem {
    private final Set<String> _failedTypes = new HashSet<>();

    NoRetryMetadataSystem() {
    }

    NoRetryMetadataSystem(final String classPath) {
        super(classPath);
    }

    NoRetryMetadataSystem(final ITypeLoader typeLoader) {
        super(typeLoader);
    }

    @Override
    protected TypeDefinition resolveType(final String descriptor, final boolean mightBePrimitive) {
        if (_failedTypes.contains(descriptor)) {
            return null;
        }

        final TypeDefinition result = super.resolveType(descriptor, mightBePrimitive);

        if (result == null) {
            _failedTypes.add(descriptor);
        }

        return result;

    }
}