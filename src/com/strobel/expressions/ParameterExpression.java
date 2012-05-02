package com.strobel.expressions;

import java.util.Date;

/**
 * Represents a named parameter expression.
 *
 * @author Mike Strobel
 */
public class ParameterExpression extends Expression {
    private final String _name;

    public ParameterExpression(final String name) {
        _name = name;
    }

    public final String getName() {
        return _name;
    }

    @Override
    public Class getType() {
        return Object.class;
    }

    @Override
    public final ExpressionType getNodeType() {
        return ExpressionType.Parameter;
    }

    @Override
    protected Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitParameter(this);
    }

    static ParameterExpression make(final Class type, final String name) {
        if (!type.isEnum()) {
            if (type.isPrimitive()) {
                if (type == Boolean.TYPE) {
                    return new PrimitiveParameterExpression(Boolean.TYPE, name);
                }
                if (type == Byte.TYPE) {
                    return new PrimitiveParameterExpression(Byte.TYPE, name);
                }
                if (type == Character.TYPE) {
                    return new PrimitiveParameterExpression(Character.TYPE, name);
                }
                if (type == Float.TYPE) {
                    return new PrimitiveParameterExpression(Float.TYPE, name);
                }
                if (type == Double.TYPE) {
                    return new PrimitiveParameterExpression(Double.TYPE, name);
                }
                if (type == Short.TYPE) {
                    return new PrimitiveParameterExpression(Short.TYPE, name);
                }
                if (type == Integer.TYPE) {
                    return new PrimitiveParameterExpression(Integer.TYPE, name);
                }
                if (type == Long.TYPE) {
                    return new PrimitiveParameterExpression(Long.TYPE, name);
                }
                if (type == Float.TYPE) {
                    return new PrimitiveParameterExpression(Float.TYPE, name);
                }
            }
            else if (type == String.class) {
                return new PrimitiveParameterExpression(String.class, name);
            }
            else if (type == Date.class) {
                return new PrimitiveParameterExpression(Date.class, name);
            }
            else if (type == Object.class) {
                return new PrimitiveParameterExpression(Object.class, name);
            }
            else if (type.isArray() && type.getComponentType() == Object.class) {
                return new PrimitiveParameterExpression(type, name);
            }
            else if (Throwable.class.isAssignableFrom(type)) {
                return new PrimitiveParameterExpression(type, name);
            }
        }

        return new TypedParameterExpression(type, name);
    }
}

final class TypedParameterExpression extends ParameterExpression {
    private final Class _type;

    TypedParameterExpression(final Class type, final String name) {
        super(name);
        _type = type;
    }

    @Override
    public final Class getType() {
        return _type;
    }
}

final class PrimitiveParameterExpression extends ParameterExpression {
    private final Class _type;

    PrimitiveParameterExpression(final Class type, final String name) {
        super(name);
        _type = type;
    }

    @Override
    public final Class getType() {
        return _type;
    }
}
