package com.strobel.reflection;

import com.strobel.core.VerifyArgument;
import com.strobel.reflection.emit.TypeBuilder;

import java.lang.reflect.Field;

/**
 * @author strobelm
 */
public final class FieldBuilder extends FieldInfo {
    private final TypeBuilder _typeBuilder;
    private final String _name;
    private final Type<?> _type;
    private final int _modifiers;

    FieldBuilder(final TypeBuilder typeBuilder, final String name, final Type<?> type, final int modifiers) {
        _typeBuilder = VerifyArgument.notNull(typeBuilder, "typeBuilder");
        _name = VerifyArgument.notNull(name, "name");
        _type = VerifyArgument.notNull(type, "type");
        _modifiers = modifiers;
    }

    FieldInfo getCreatedField() {
        if (!_typeBuilder.isCreated()) {
            throw Error.typeNotCreated();
        }

        return _typeBuilder.createType().getField(
            _name,
            BindingFlags.fromMember(this)
        );
    }

    @Override
    public Type getFieldType() {
        return _type;
    }

    @Override
    public boolean isEnumConstant() {
        return (_modifiers & Type.ENUM_MODIFIER) == Type.ENUM_MODIFIER;
    }

    @Override
    public Field getRawField() {
        final FieldInfo createdField = getCreatedField();
        return createdField.getRawField();
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public TypeBuilder getDeclaringType() {
        return _typeBuilder;
    }

    @Override
    public int getModifiers() {
        return _modifiers;
    }
}
