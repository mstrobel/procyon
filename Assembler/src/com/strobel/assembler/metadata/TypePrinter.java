/*
 * TypePrinter.java
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
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.StringUtilities;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * @author Mike Strobel
 */
public class TypePrinter implements TypeVisitor {
    private final CodePrinter _printer;

    public TypePrinter(final CodePrinter printer) {
        _printer = printer;
    }

    @Override
    public void visit(
        final int majorVersion,
        final int minorVersion,
        final long flags,
        final String name,
        final String genericSignature,
        final String baseTypeName,
        final String[] interfaceNames) {

        _printer.printf("Class %s\n", name);
        _printer.increaseIndent();

        try {
            if (genericSignature != null) {
                _printer.printf("Signature: %s\n", genericSignature);
            }

            final EnumSet<Flags.Flag> flagSet = Flags.asFlagSet(flags & Flags.ClassFlags);
            final List<String> flagStrings = new ArrayList<>();

            if (Flags.testAll(flags, Flags.ACC_SUPER)) {
                flagStrings.add("ACC_SUPER");
            }

            for (final Flags.Flag flag : flagSet) {
                flagStrings.add(flag.name());
            }

            _printer.printf("Flags: %s\n", StringUtilities.join(", ", flagStrings));
        }
        finally {
            _printer.decreaseIndent();
        }
    }

    @Override
    public void visitParser(final MetadataParser parser) {
    }

    @Override
    public void visitOuterType(final TypeReference type) {
    }

    @Override
    public void visitInnerType(final TypeDefinition type) {
    }

    @Override
    public void visitAttribute(final SourceAttribute attribute) {
    }

    @Override
    public void visitAnnotation(final CustomAnnotation annotation, final boolean visible) {
    }

    @Override
    public FieldPrinter visitField(final long flags, final String name, final TypeReference fieldType) {
        _printer.println();
        return new FieldPrinter(_printer, flags, name, fieldType);
    }

    @Override
    public MethodPrinter visitMethod(final long flags, final String name, final IMethodSignature signature, final TypeReference... thrownTypes) {
        _printer.println();
        return new MethodPrinter(_printer, flags, name, signature, thrownTypes);
    }

    @Override
    public void visitEnd() {
    }
}
