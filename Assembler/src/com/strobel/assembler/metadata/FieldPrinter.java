/*
 * FieldPrinter.java
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

package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.ConstantValueAttribute;
import com.strobel.assembler.ir.attributes.SignatureAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;

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
            _output.write(StringUtilities.join(" ", flagStrings));
            _output.write(' ');
        }

        _output.write(_fieldType.getBriefDescription());
        _output.write(' ');
        _output.write(_name);
        _output.write(';');
        _output.writeLine();

        flagStrings.clear();

        for (final Flags.Flag flag : Flags.asFlagSet(_flags & (Flags.VarFlags | ~Flags.StandardFlags))) {
            flagStrings.add(flag.name());
        }

        if (!flagStrings.isEmpty()) {
            _output.write("  Flags: %s\n", StringUtilities.join(", ", flagStrings));
        }
    }

    @Override
    public void visitAttribute(final SourceAttribute attribute) {
        switch (attribute.getName()) {
            case AttributeNames.ConstantValue: {
                Object constantValue = ((ConstantValueAttribute) attribute).getValue();

                if (constantValue instanceof String) {
                    constantValue = JavaOutputVisitor.convertString((String) constantValue, true);
                }

                _output.write("  ConstantValue: %s", constantValue);
                _output.writeLine();

                break;
            }

            case AttributeNames.Signature: {
                _output.write("  Signature: %s", ((SignatureAttribute) attribute).getSignature());
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
