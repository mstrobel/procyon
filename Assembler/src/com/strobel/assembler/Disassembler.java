/*
 * Disassembler.java
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

package com.strobel.assembler;

import com.sampullara.cli.Args;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ClassFileReader;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.MetadataParser;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypePrinter;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.AnsiTextOutput;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.PlainTextOutput;

import java.util.List;

public final class Disassembler {
    public static void disassemble(final String internalName, final ITextOutput output) {
        disassemble(internalName, output, new DisassemblerOptions());
    }

    public static void disassemble(final String internalName, final ITextOutput output, final DisassemblerOptions options) {
        VerifyArgument.notNull(internalName, "internalName");
        VerifyArgument.notNull(options, "options");

        final MetadataSystem metadataSystem = MetadataSystem.instance();
        final TypeReference type = metadataSystem.lookupType(internalName);
        final TypeDefinition resolvedType;

        if (type == null || (resolvedType = type.resolve()) == null) {
            System.err.printf("ERROR: Could not resolve class '%s'.\n", internalName);
            return;
        }

        final ClasspathTypeLoader loader = new ClasspathTypeLoader();
        final Buffer buffer = new Buffer(0);

        if (!loader.tryLoadType(type.getInternalName(), buffer)) {
            System.err.printf("ERROR: Could not resolve class '%s'.\n", internalName);
            return;
        }

        final ClassFileReader reader = ClassFileReader.readClass(
            ClassFileReader.OPTION_PROCESS_CODE |
            ClassFileReader.OPTION_PROCESS_ANNOTATIONS,
            resolvedType.getResolver(),
            buffer
        );

        final MetadataParser parser = reader.getParser();

        parser.pushGenericContext(resolvedType);

        try {
            final TypePrinter typePrinter = new TypePrinter(output, options);
            reader.accept(typePrinter);
        }
        finally {
            parser.popGenericContext();
        }
        System.out.print(output.toString());
    }

    public static void main(final String[] args) {
        final DisassemblerOptions options = new DisassemblerOptions();
        final List<String> typeNames = Args.parse(options, args);

        if (options.getPrintUsage()) {
            Args.usage(options);
            return;
        }

        final PlainTextOutput output = new AnsiTextOutput();

        output.setIndentToken("  ");

        if (typeNames.isEmpty()) {
            disassemble("com/strobel/assembler/Disassembler", output, options);
        }
        else {
            for (final String typeName : typeNames) {
                disassemble(typeName, output, options);
            }
        }
    }
}

