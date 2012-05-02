package com.strobel.expressions;

import com.strobel.util.ContractUtils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

/**
 * Represents accessing a field.
 * @author Mike Strobel
 */
public abstract class MemberExpression extends Expression {
    private final Expression _target;

    MemberExpression(final Expression target) {
        _target = target;
    }

    @Override
    public final ExpressionType getNodeType() {
        return ExpressionType.MemberAccess;
    }

    public final Expression getTarget() {
        return _target;
    }

    AccessibleObject getMember() {
        throw ContractUtils.unreachable();
    }

    @Override
    protected Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitMember(this);
    }

    public final MemberExpression update(final Expression target) {
        if (target == _target) {
            return this;
        }
        return makeMemberAccess(target, getMember());
    }
}
