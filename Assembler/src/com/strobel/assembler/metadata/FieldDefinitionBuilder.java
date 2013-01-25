package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.ConstantValueAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public class FieldDefinitionBuilder implements FieldVisitor<MutableTypeDefinition> {
    private final MutableFieldDefinition _field;

    public FieldDefinitionBuilder(
        final TypeReference declaringType,
        final int flags,
        final String name,
        final TypeReference fieldType) {

        VerifyArgument.notNull(name, "name");
        VerifyArgument.notNull(fieldType, "fieldType");

        _field = new MutableFieldDefinition();
        _field.setDeclaringType(declaringType);
        _field.setFlags(flags);
        _field.setName(name);
        _field.setFieldType(fieldType);
    }

    public FieldDefinitionBuilder(final MutableFieldDefinition field) {
        _field = VerifyArgument.notNull(field, "field");
    }

    @Override
    public void visitAttribute(final MutableTypeDefinition declaringType, final SourceAttribute attribute) {
        switch (attribute.getName()) {
            case AttributeNames.ConstantValue: {
                final ConstantValueAttribute cva = (ConstantValueAttribute) attribute;
                _field.setConstantValue(cva.getValue());
                break;
            }
        }
    }

    @Override
    public void visitAnnotation(final MutableTypeDefinition declaringType, final CustomAnnotation annotation, final boolean visible) {
        if (visible) {
            _field.getAnnotations().add(annotation);
        }
    }

    @Override
    public void visitEnd(final MutableTypeDefinition declaringType) {
        _field.setDeclaringType(declaringType);
        declaringType.declaredFields.add(_field);
    }
}
