/*
 * FieldPrinter.java
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

package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.ConstantValueAttribute;
import com.strobel.assembler.ir.attributes.SignatureAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerHelpers;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.NameSyntax;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class FieldPrinter implements FieldVisitor {
    private final ITextOutput _output;
    private final long _flags;
    private final String _name;
    private final TypeReference _fieldType;

    public FieldPrinter(final ITextOutput output, final long flags, final String name, final TypeReference fieldType) {
        _output = VerifyArgument.notNull(output, "output");
        _flags = flags;
        _name = VerifyArgument.notNull(name, "name");
        _fieldType = VerifyArgument.notNull(fieldType, "fieldType");

        printDescription();
    }

    private void printDescription() {
        final EnumSet<Flags.Flag> flagSet = Flags.asFlagSet(_flags & Flags.VarFlags & ~Flags.ENUM);
        final List<String> flagStrings = new ArrayList<>();

        for (final Flags.Flag flag : flagSet) {
            flagStrings.add(flag.toString());
        }

        if (flagSet.size() > 0) {
            for (int i = 0; i < flagStrings.size(); i++) {
                _output.writeKeyword(flagStrings.get(i));
                _output.write(' ');
            }
        }

        DecompilerHelpers.writeType(_output, _fieldType, NameSyntax.TYPE_NAME);

        _output.write(' ');
        _output.write(_name);
        _output.write(';');
        _output.writeLine();

        flagStrings.clear();

        for (final Flags.Flag flag : Flags.asFlagSet(_flags & (Flags.VarFlags | ~Flags.StandardFlags))) {
            flagStrings.add(flag.name());
        }

        if (flagStrings.isEmpty()) {
            return;
        }

        _output.indent();

        try {
            _output.writeAttribute("Flags");
            _output.write(": ");

            for (int i = 0; i < flagStrings.size(); i++) {
                if (i != 0) {
                    _output.write(", ");
                }

                _output.writeLiteral(flagStrings.get(i));
            }

            _output.writeLine();
        }
        finally {
            _output.unindent();
        }
    }

    @Override
    public void visitAttribute(final SourceAttribute attribute) {
        switch (attribute.getName()) {
            case AttributeNames.ConstantValue: {
                final Object constantValue = ((ConstantValueAttribute) attribute).getValue();
                _output.indent();
                _output.writeAttribute("ConstantValue");
                _output.write(": ");
                DecompilerHelpers.writeOperand(_output, constantValue);
                _output.writeLine();
                _output.unindent();

                break;
            }

            case AttributeNames.Signature: {
                _output.indent();
                _output.writeAttribute("Signature");
                _output.write(": ");
                _output.writeTextLiteral(((SignatureAttribute) attribute).getSignature());
                _output.writeLine();
                break;
            }
        }
    }

    @Override
    public void visitAnnotation(final CustomAnnotation annotation, final boolean visible) {
    }

    @Override
    public void visitEnd() {
    }
}
