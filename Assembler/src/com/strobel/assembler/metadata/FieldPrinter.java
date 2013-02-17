package com.strobel.assembler.metadata;

import com.strobel.assembler.CodePrinter;
import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.SignatureAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class FieldPrinter implements FieldVisitor<MutableTypeDefinition> {
    private final CodePrinter _printer;
    private final int _flags;
    private final String _name;
    private final TypeReference _fieldType;

    public FieldPrinter(final CodePrinter printer, final int flags, final String name, final TypeReference fieldType) {
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
    }

    @Override
    public void visitAttribute(final MutableTypeDefinition _, final SourceAttribute attribute) {
        switch (attribute.getName()) {
            case AttributeNames.Signature: {
                _printer.printf("  Signature: %s", ((SignatureAttribute)attribute).getSignature());
                _printer.println();
            }
        }
    }

    @Override
    public void visitAnnotation(final MutableTypeDefinition _, final CustomAnnotation annotation, final boolean visible) {
    }

    @Override
    public void visitEnd(final MutableTypeDefinition _) {
    }
}
