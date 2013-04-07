/*
 * DecompilerVisitor.java
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

package com.strobel.decompiler;

import com.strobel.assembler.ir.ConstantPool;
import com.strobel.assembler.ir.Frame;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.InstructionVisitor;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.*;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.languages.Language;

final class DecompilerVisitor implements TypeVisitor {
    private final ITextOutput _output;
    private final Language _language;
    private final DecompilationOptions _options;

    private TypeDefinition _type;

    DecompilerVisitor(final Language language, final ITextOutput output, final DecompilationOptions options) {
        _language = VerifyArgument.notNull(language, "language");
        _output = VerifyArgument.notNull(output, "output");
        _options = VerifyArgument.notNull(options, "options");
    }

    @Override
    public void visitParser(final MetadataParser parser) {
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

        final TypeReference type = MetadataSystem.instance().lookupType(name);

        if (type != null) {
            _type = type.resolve();
        }
    }

    @Override
    public void visitDeclaringMethod(final MethodReference method) {
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
    public FieldVisitor visitField(final long flags, final String name, final TypeReference fieldType) {
        return new FieldVisitor() {
            @Override
            public void visitAttribute(final SourceAttribute attribute) {
            }

            @Override
            public void visitAnnotation(final CustomAnnotation annotation, final boolean visible) {
            }

            @Override
            public void visitEnd() {
                if (_type != null) {
                    final MetadataParser parser = new MetadataParser(_type.getResolver());
                    final FieldReference fieldReference = parser.parseField(_type, name, fieldType.getSignature());
                    final FieldDefinition fieldDefinition = fieldReference != null ? fieldReference.resolve() : null;
                    
                    if (fieldDefinition != null) {
                        _language.decompileField(fieldDefinition, _output, _options);
                        _output.writeLine();
                    }
                }
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(
        final long flags,
        final String name,
        final IMethodSignature signature,
        final TypeReference... thrownTypes) {

        return new MethodVisitor() {
            @Override
            public boolean canVisitBody() {
                return true;
            }

            @Override
            public InstructionVisitor visitBody(final MethodBody body) {
                final MethodDefinition methodDefinition = body.getMethod().resolve();

                _output.writeLine();
                _output.writeLine("%s {", methodDefinition.isTypeInitializer() ? "static" : methodDefinition.getDescription());
                _output.indent();

                _language.decompileMethod(methodDefinition, _output, _options);

                _output.unindent();
                _output.writeLine("}");

                return InstructionVisitor.EMPTY;
            }

            @Override
            public void visitEnd() {
            }

            @Override
            public void visitFrame(final Frame frame) {
            }

            @Override
            public void visitLineNumber(final Instruction instruction, final int lineNumber) {
            }

            @Override
            public void visitAttribute(final SourceAttribute attribute) {
            }

            @Override
            public void visitAnnotation(final CustomAnnotation annotation, final boolean visible) {
            }
        };
    }

    @Override
    public ConstantPool.Visitor visitConstantPool() {
        return ConstantPool.Visitor.EMPTY;
    }

    @Override
    public void visitEnd() {
    }
}
