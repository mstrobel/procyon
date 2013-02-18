package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.ConstantValueAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public class FieldDefinitionBuilder implements FieldVisitor {
    private final FieldDefinition _field;

    public FieldDefinitionBuilder(
        final TypeDefinition declaringType,
        final int flags,
        final String name,
        final TypeReference fieldType) {

        VerifyArgument.notNull(name, "name");
        VerifyArgument.notNull(fieldType, "fieldType");

        _field = new FieldDefinition(declaringType.getResolver());
        _field.setDeclaringType(declaringType);
        _field.setFlags(flags);
        _field.setName(name);
        _field.setFieldType(fieldType);
    }

    public FieldDefinitionBuilder(final FieldDefinition field) {
        _field = VerifyArgument.notNull(field, "field");
    }

    @Override
    public void visitAttribute(final SourceAttribute attribute) {
        switch (attribute.getName()) {
            case AttributeNames.ConstantValue: {
                final ConstantValueAttribute cva = (ConstantValueAttribute) attribute;
                _field.setConstantValue(cva.getValue());
                break;
            }
        }
    }

    @Override
    public void visitAnnotation(final CustomAnnotation annotation, final boolean visible) {
        if (visible) {
            _field.getAnnotationsInternal().add(annotation);
        }
    }

    @Override
    public void visitEnd() {
        _field.getDeclaringType().getDeclaredFieldsInternal().add(_field);
    }
}
