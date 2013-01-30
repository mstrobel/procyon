package com.strobel.assembler.metadata;

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
public class MethodDefinitionBuilder implements MethodVisitor<MutableTypeDefinition> {
    private final MutableMethodDefinition _method = new MutableMethodDefinition();

    public MethodDefinitionBuilder(
        final MutableTypeDefinition declaringType,
        final int flags,
        final String name,
        final IMethodSignature signature,
        final TypeReference... thrownTypes) {

        VerifyArgument.notNull(signature, "signature");

        _method.setDeclaringType(declaringType);
        _method.setFlags(flags);
        _method.setName(VerifyArgument.notNull(name, "name"));
        _method.setReturnType(signature.getReturnType());

        for (final GenericParameter genericParameter : signature.getGenericParameters()) {
            _method.genericParameters.add(genericParameter);
        }

        for (final ParameterDefinition parameter : signature.getParameters()) {
            _method.parameters.add(parameter);
        }

        if (thrownTypes != null) {
            for (final TypeReference thrownType : thrownTypes) {
                _method.thrownTypes.add(thrownType);
            }
        }
    }

    @Override
    public boolean canVisitBody(final MutableTypeDefinition declaringType) {
        return false;
    }

    @Override
    public InstructionVisitor<MutableTypeDefinition> visitBody(final MutableTypeDefinition declaringType) {
        throw ContractUtils.unsupported();
    }

    @Override
    public void visitEnd(final MutableTypeDefinition declaringType) {
        _method.setDeclaringType(declaringType);
        declaringType.declaredMethods.add(_method);
    }

    @Override
    public void visitFrame(final MutableTypeDefinition declaringType, final Frame frame) {
    }

    @Override
    public void visitLineNumber(final MutableTypeDefinition declaringType, final Instruction instruction, final int lineNumber) {
    }

    @Override
    public void visitAttribute(final MutableTypeDefinition declaringType, final SourceAttribute attribute) {
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
    public void visitAnnotation(final MutableTypeDefinition declaringType, final CustomAnnotation annotation, final boolean visible) {
        if (visible) {
            _method.getAnnotations().add(annotation);
        }
    }
}
