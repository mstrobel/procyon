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
import com.strobel.assembler.DisassemblerOptions;
import com.strobel.assembler.ir.ConstantPool;
import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.EnclosingMethodAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.ir.attributes.SourceFileAttribute;
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
    private final DisassemblerOptions _options;

    public TypePrinter(final CodePrinter printer, final DisassemblerOptions options) {
        _printer = printer;
        _options = options;
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
                _printer.printf("  Signature: %s\n", genericSignature);
            }

            _printer.printf("  Minor version: %d\n", minorVersion);
            _printer.printf("  Major version: %d\n", majorVersion);

            final List<String> flagStrings = new ArrayList<>();
            final EnumSet<Flags.Flag> flagsSet = Flags.asFlagSet(flags & (Flags.ClassFlags | ~Flags.StandardFlags));

            for (final Flags.Flag flag : flagsSet) {
                flagStrings.add(flag.name());
            }

            if (!flagStrings.isEmpty()) {
                _printer.printf("  Flags: %s\n", StringUtilities.join(", ", flagStrings));
            }
        }
        finally {
            _printer.decreaseIndent();
        }
    }

    @Override
    public void visitDeclaringMethod(final MethodReference method) {
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
        switch (attribute.getName()) {
            case AttributeNames.SourceFile: {
                _printer.printf("  SourceFile: %s", ((SourceFileAttribute) attribute).getSourceFile());
                _printer.println();
                break;
            }
            case AttributeNames.Deprecated: {
                _printer.print("  Deprecated");
                _printer.println();
                break;
            }
            case AttributeNames.EnclosingMethod: {
                final MethodReference enclosingMethod = ((EnclosingMethodAttribute) attribute).getEnclosingMethod();
                _printer.printf("  EnclosingMethod: %s:%s", enclosingMethod.getFullName(), enclosingMethod.getSignature());
                _printer.println();
                break;
            }
        }
    }

    @Override
    public void visitAnnotation(final CustomAnnotation annotation, final boolean visible) {
    }

    @Override
    public FieldVisitor visitField(final long flags, final String name, final TypeReference fieldType) {
        if (_options.getPrintFields()) {
            _printer.println();
            return new FieldPrinter(_printer, flags, name, fieldType);
        }
        else {
            return FieldVisitor.EMPTY;
        }
    }

    @Override
    public MethodVisitor visitMethod(final long flags, final String name, final IMethodSignature signature, final TypeReference... thrownTypes) {
        _printer.println();
        return new MethodPrinter(_printer, _options, flags, name, signature, thrownTypes);
    }

    @Override
    public ConstantPool.Visitor visitConstantPool() {
        return new ConstantPoolPrinter(_printer);
    }

    @Override
    public void visitEnd() {
    }
}
