package com.strobel.expressions;

import java.lang.reflect.Field;

/**
 * Represents accessing a field.
 * @author Mike Strobel
 */
class FieldExpression extends MemberExpression {

    private final Field _field;

    FieldExpression(final Expression target, final Field field) {
        super(target);
        _field = field;
    }

    @Override
    Field getMember() {
        return _field;
    }

    @Override
    public final Class getType() {
        return _field.getType();
    }
}
