package com.strobel.expressions;

import com.strobel.reflection.FieldInfo;
import com.strobel.reflection.Type;

/**
 * Represents accessing a field.
 * @author Mike Strobel
 */
class FieldExpression extends MemberExpression {

    private final FieldInfo _field;

    FieldExpression(final Expression target, final FieldInfo field) {
        super(target);
        _field = field;
    }

    @Override
    FieldInfo getMember() {
        return _field;
    }

    @Override
    public final Type getType() {
        return _field.getFieldType();
    }
}
