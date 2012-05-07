package com.strobel.expressions;

import com.strobel.reflection.FieldInfo;
import com.strobel.reflection.MemberInfo;
import com.strobel.reflection.MemberType;
import com.strobel.util.ContractUtils;

/**
 * Represents accessing a field.
 *
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

    MemberInfo getMember() {
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

    static MemberExpression make(final Expression expression, final MemberInfo member) {
        if (member.getMemberType() == MemberType.Field) {
            final FieldInfo fi = (FieldInfo) member;
            return new FieldExpression(expression, fi);
        }
        throw Error.memberNotField(member);
    }
}
