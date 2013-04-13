/*
 * TypePrinter.java
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

import com.strobel.assembler.DisassemblerOptions;
import com.strobel.assembler.ir.ConstantPool;
import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.EnclosingMethodAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.ir.attributes.SourceFileAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.decompiler.ITextOutput;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * @author Mike Strobel
 */
public class TypePrinter implements TypeVisitor {
    private final ITextOutput _output;
    private final DisassemblerOptions _options;

    public TypePrinter(final ITextOutput output, final DisassemblerOptions options) {
        _output = output;
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

        _output.writeLine("Class %s", name);
        _output.indent();

        if (genericSignature != null) {
            _output.writeAttribute("Signature");
            _output.write(": %s", genericSignature);
            _output.writeLine();
        }

        _output.writeAttribute("Minor version");
        _output.write(": ");
        _output.writeLiteral(minorVersion);
        _output.writeLine();

        _output.writeAttribute("Major version");
        _output.write(": ");
        _output.writeLiteral(majorVersion);
        _output.writeLine();

        final List<String> flagStrings = new ArrayList<>();
        final EnumSet<Flags.Flag> flagsSet = Flags.asFlagSet(flags & (Flags.ClassFlags | ~Flags.StandardFlags));

        for (final Flags.Flag flag : flagsSet) {
            flagStrings.add(flag.name());
        }

        if (!flagStrings.isEmpty()) {
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
                _output.writeAttribute("SourceFile");
                _output.write(": ");
                _output.writeTextLiteral(((SourceFileAttribute) attribute).getSourceFile());
                _output.writeLine();
                break;
            }

            case AttributeNames.Deprecated: {
                _output.writeAttribute("Deprecated");
                _output.writeLine();
                break;
            }

            case AttributeNames.EnclosingMethod: {
                final TypeReference enclosingType = ((EnclosingMethodAttribute) attribute).getEnclosingType();
                final MethodReference enclosingMethod = ((EnclosingMethodAttribute) attribute).getEnclosingMethod();

                if (enclosingType != null) {
                    _output.writeAttribute("EnclosingType");
                    _output.write(": ");
                    _output.writeReference(enclosingType.getInternalName(), enclosingType);
                    _output.writeLine();
                }

                if (enclosingMethod != null) {
                    final TypeReference declaringType = enclosingMethod.getDeclaringType();

                    _output.writeAttribute("EnclosingMethod");
                    _output.write(": ");
                    _output.writeReference(declaringType.getInternalName(), declaringType);
                    _output.write(".");
                    _output.writeReference(enclosingMethod.getName(), enclosingMethod);
                    _output.write(":");
                    _output.write(enclosingMethod.getSignature());
                    _output.writeLine();
                }

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
            _output.writeLine();
            return new FieldPrinter(_output, flags, name, fieldType);
        }
        else {
            return FieldVisitor.EMPTY;
        }
    }

    @Override
    public MethodVisitor visitMethod(final long flags, final String name, final IMethodSignature signature, final TypeReference... thrownTypes) {
        _output.writeLine();
        return new MethodPrinter(_output, _options, flags, name, signature, thrownTypes);
    }

    @Override
    public ConstantPool.Visitor visitConstantPool() {
        if (_options.getPrintConstantPool()) {
            return new ConstantPoolPrinter(_output);
        }
        return ConstantPool.Visitor.EMPTY;
    }

    @Override
    public void visitEnd() {
        _output.unindent();
    }
}
