/*
 * DecompilerVisitor.java
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

package com.strobel.decompiler;

import com.strobel.assembler.ir.ConstantPool;
import com.strobel.assembler.ir.Frame;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.InstructionVisitor;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.*;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.ast.AstBuilder;
import com.strobel.decompiler.ast.AstOptimizer;
import com.strobel.decompiler.ast.Block;
import com.strobel.decompiler.ast.Node;

import java.util.List;

/**
 * @author strobelm
 */
final class DecompilerVisitor implements TypeVisitor {
    private final ITextOutput _output;
    private final DecompilerSettings _options;

    DecompilerVisitor(final ITextOutput output, final DecompilerSettings options) {
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
        return FieldVisitor.EMPTY;
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
                final DecompilerContext context = new DecompilerContext();
                final MethodDefinition methodDefinition = body.getMethod().resolve();

                context.setCurrentType(body.getMethod().getDeclaringType().resolve());
                context.setCurrentMethod(methodDefinition);
                context.setSettings(_options);

                final List<Node> ast = AstBuilder.build(body, true, context);
                final Block methodBlock = new Block(ast);

                AstOptimizer.optimize(context, methodBlock);

                _output.writeLine();
                _output.writeLine("%s {", methodDefinition.getDescription());
                _output.indent();

                methodBlock.writeTo(_output);

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
