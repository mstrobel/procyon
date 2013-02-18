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
import com.strobel.util.ContractUtils;

import java.util.List;

/**
 * @author Mike Strobel
 */
public class MethodDefinitionBuilder implements MethodVisitor {
    private final MethodDefinition _method = new MethodDefinition();

    public MethodDefinitionBuilder(
        final TypeDefinition declaringType,
        final int flags,
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
        }

        for (final ParameterDefinition parameter : signature.getParameters()) {
            parameters.add(parameter);
        }

        if (thrownTypes != null) {
            final Collection<TypeReference> thrownTypesInternal = _method.getThrownTypesInternal();

            for (final TypeReference thrownType : thrownTypes) {
                thrownTypesInternal.add(thrownType);
            }
        }
    }

    @Override
    public boolean canVisitBody() {
        return false;
    }

    @Override
    public InstructionVisitor visitBody(final int maxStack, final int maxLocals) {
        throw ContractUtils.unsupported();
    }

    @Override
    public void visitEnd() {
        _method.getDeclaringType().getDeclaredMethodsInternal().add(_method);
    }

    @Override
    public void visitFrame(final Frame frame) {
    }

    @Override
    public void visitLineNumber(final Instruction instruction, final int lineNumber) {
    }

    @Override
    public void visitAttribute(final SourceAttribute attribute) {
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
                    final int parameterIndex = _method.isStatic() ? entry.getIndex() : entry.getIndex() - 1;

                    if (parameterIndex >= 0 && parameterIndex < parameters.size()) {
                        final ParameterDefinition parameter = parameters.get(parameterIndex);

                        if (!parameter.hasName()) {
                            parameter.setName(entry.getName());
                        }
                    }
                }

                break;
            }
        }
    }

    @Override
    public void visitAnnotation(final CustomAnnotation annotation, final boolean visible) {
        if (visible) {
            _method.getAnnotationsInternal().add(annotation);
        }
    }
}
