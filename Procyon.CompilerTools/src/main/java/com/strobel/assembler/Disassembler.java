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

/*
package com.strobel.assembler;

import com.sampullara.cli.Args;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.AnsiTextOutput;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Languages;

import java.io.StringWriter;
import java.util.List;

public final class Disassembler {
    public static void disassemble(final String internalName, final ITextOutput output) {
        disassemble(internalName, output, new DisassemblerOptions());
    }

    public static void disassemble(final String typeNameOrPath, final ITextOutput output, final DisassemblerOptions options) {
        VerifyArgument.notNull(typeNameOrPath, "typeNameOrPath");
        VerifyArgument.notNull(options, "options");


        Decompiler.decompile(typeNameOrPath, output);
    }

    public static void main(final String[] args) {
        final DisassemblerOptions options = new DisassemblerOptions();
        final List<String> typeNames = Args.parse(options, args);

        if (options.getPrintUsage()) {
            Args.usage(options);
            return;
        }

        final StringWriter writer = new StringWriter();
        final PlainTextOutput output = new AnsiTextOutput(writer);
        final DecompilerSettings settings = new DecompilerSettings();

        output.setIndentToken("  ");

        settings.setLanguage(Languages.bytecode());
        settings.setTypeLoader(new InputTypeLoader());
        settings.setShowNestedTypes(options.getPrintNestedTypes());

        if (typeNames.isEmpty()) {
            Decompiler.decompile("com/strobel/assembler/Disassembler", output, settings);
            System.out.println(writer.toString());
        }
        else {
            for (final String typeName : typeNames) {
                Decompiler.decompile(typeName, output, settings);
                System.out.println(writer.toString());
                writer.getBuffer().setLength(0);
            }
        }
    }
}
*/
