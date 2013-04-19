/*
 * MethodDefinitionBuilder.java
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

import com.strobel.assembler.Collection;
import com.strobel.assembler.ir.Frame;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.InstructionVisitor;
import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.LocalVariableTableAttribute;
import com.strobel.assembler.ir.attributes.LocalVariableTableEntry;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.VerifyArgument;

import java.util.List;

/**
 * @author Mike Strobel
 */
public class MethodDefinitionBuilder implements MethodVisitor {
    private final MethodDefinition _method = new MethodDefinition();

    public MethodDefinitionBuilder(
        final TypeDefinition declaringType,
        final long flags,
        final String name,
        final IMethodSignature signature,
        final TypeReference... thrownTypes) {

        VerifyArgument.notNull(signature, "signature");
        VerifyArgument.notNull(declaringType, "declaringType");

        _method.setDeclaringType(declaringType);
        _method.setFlags(flags);
        _method.setName(VerifyArgument.notNull(name, "name"));
        _method.setReturnType(signature.getReturnType());

        final GenericParameterCollection genericParameters = _method.getGenericParametersInternal();
        final ParameterDefinitionCollection parameters = _method.getParametersInternal();

        for (final GenericParameter genericParameter : signature.getGenericParameters()) {
            genericParameters.add(genericParameter);
            genericParameter.setDeclaringMethod(_method);
        }

        int slot = 0;

        if (!Flags.testAny(flags, Flags.STATIC)) {
            ++slot;
        }

        for (final ParameterDefinition parameter : signature.getParameters()) {
            parameter.setSlot(slot);
            parameters.add(parameter);
            slot += parameter.getSize();
        }

        if (thrownTypes != null) {
            final Collection<TypeReference> thrownTypesInternal = _method.getThrownTypesInternal();

            for (final TypeReference thrownType : thrownTypes) {
                thrownTypesInternal.add(thrownType);
            }
        }
    }

    public final MethodDefinition getMethod() {
        return _method;
    }

    @Override
    public boolean canVisitBody() {
        return true;
    }

    @Override
    public InstructionVisitor visitBody(final MethodBody body) {
        _method.setBody(body);
        return InstructionVisitor.EMPTY;
    }

    @Override
    public void visitEnd() {
//        _method.getDeclaringType().getDeclaredMethodsInternal().add(_method);
    }

    @Override
    public void visitFrame(final Frame frame) {
    }

    @Override
    public void visitLineNumber(final Instruction instruction, final int lineNumber) {
    }

    @Override
    public void visitAttribute(final SourceAttribute attribute) {
        _method.getSourceAttributesInternal().add(attribute);

        switch (attribute.getName()) {
            case AttributeNames.Synthetic: {
                _method.setFlags(_method.getFlags() | Flags.SYNTHETIC);
                break;
            }

            case AttributeNames.Deprecated: {
                _method.setFlags(_method.getFlags() | Flags.DEPRECATED);
                break;
            }

            case AttributeNames.LocalVariableTable:
            case AttributeNames.LocalVariableTypeTable: {
                final LocalVariableTableAttribute lvt = (LocalVariableTableAttribute) attribute;
                final List<LocalVariableTableEntry> entries = lvt.getEntries();
                final List<ParameterDefinition> parameters = _method.getParameters();

                for (int i = 0; i < entries.size(); i++) {
                    final LocalVariableTableEntry entry = entries.get(i);

                    if (entry.getScopeOffset() != 0) {
                        continue;
                    }

                    final int slot = _method.isStatic() ? entry.getIndex() : entry.getIndex() - 1;

                    ParameterDefinition parameter = null;

                    for (int j = 0; j < parameters.size(); j++) {

                        if (parameters.get(j).getSlot() == slot) {
                            parameter = parameters.get(j);
                            break;
                        }
                    }

                    if (parameter != null && !parameter.hasName()) {
                        parameter.setName(entry.getName());
                    }
                }

                break;
            }
        }
    }

    @Override
    public void visitAnnotation(final CustomAnnotation annotation, final boolean visible) {
        _method.getAnnotationsInternal().add(annotation);
    }

    @Override
    public void visitParameterAnnotation(final int parameter, final CustomAnnotation annotation, final boolean visible) {
        _method.getParameters().get(parameter).getAnnotationsInternal().add(annotation);
    }
}
