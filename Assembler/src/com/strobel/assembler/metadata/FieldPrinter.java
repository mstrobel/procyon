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

import com.strobel.assembler.CodePrinter;
import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.ConstantValueAttribute;
import com.strobel.assembler.ir.attributes.SignatureAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.java.JavaOutputVisitor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class FieldPrinter implements FieldVisitor {
    private final CodePrinter _printer;
    private final long _flags;
    private final String _name;
    private final TypeReference _fieldType;

    public FieldPrinter(final CodePrinter printer, final long flags, final String name, final TypeReference fieldType) {
        _printer = VerifyArgument.notNull(printer, "printer");
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
            _printer.printf(StringUtilities.join(" ", flagStrings));
            _printer.print(' ');
        }

        _printer.printf(_fieldType.getBriefDescription());
        _printer.print(' ');
        _printer.printf(_name);
        _printer.print(';');
        _printer.println();

        flagStrings.clear();

        for (final Flags.Flag flag : Flags.asFlagSet(_flags & (Flags.VarFlags | ~Flags.StandardFlags))) {
            flagStrings.add(flag.name());
        }

        if (!flagStrings.isEmpty()) {
            _printer.printf("  Flags: %s\n", StringUtilities.join(", ", flagStrings));
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

                _printer.printf("  ConstantValue: %s", constantValue);
                _printer.println();

                break;
            }

            case AttributeNames.Signature: {
                _printer.printf("  Signature: %s", ((SignatureAttribute)attribute).getSignature());
                _printer.println();
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
