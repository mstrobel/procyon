package com.strobel.util;

import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.PrimitiveTypes;
import com.strobel.reflection.Type;

/**
 * @author Mike Strobel
 */
public final class TypeUtils {
    private TypeUtils() {}

    public static boolean isAutoUnboxed(final Type type) {
        return type == Type.of(Integer.class) ||
               type == Type.of(Long.class) ||
               type == Type.of(Double.class) ||
               type == Type.of(Float.class) ||
               type == Type.of(Short.class) ||
               type == Type.of(Byte.class) ||
               type == Type.of(Boolean.class) ||
               type == Type.of(Character.class);
    }

    public static Type getUnderlyingPrimitive(final Type type) {
        if (type == Type.of(Integer.class)) {
            return PrimitiveTypes.Integer;
        }
        if (type == Type.of(Long.class)) {
            return PrimitiveTypes.Long;
        }
        if (type == Type.of(Double.class)) {
            return PrimitiveTypes.Double;
        }
        if (type == Type.of(Float.class)) {
            return PrimitiveTypes.Float;
        }
        if (type == Type.of(Short.class)) {
            return PrimitiveTypes.Short;
        }
        if (type == Type.of(Byte.class)) {
            return PrimitiveTypes.Byte;
        }
        if (type == Type.of(Boolean.class)) {
            return PrimitiveTypes.Boolean;
        }
        if (type == Type.of(Character.class)) {
            return PrimitiveTypes.Character;
        }
        return null;
    }

    public static Type getBoxedTypeOrSelf(final Type type) {
        final Type boxedType = getBoxedType(type);
        return boxedType != null ? boxedType : type;
    }

    public static Type getUnderlyingPrimitiveOrSelf(final Type type) {
        if (isAutoUnboxed(type)) {
            return getUnderlyingPrimitive(type);
        }
        return type;
    }

    public static Type getBoxedType(final Type type) {
        if (isAutoUnboxed(type)) {
            return type;
        }
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == PrimitiveTypes.Integer) {
            return Type.of(Integer.class);
        }
        if (type == PrimitiveTypes.Long) {
            return Type.of(Long.class);
        }
        if (type == PrimitiveTypes.Double) {
            return Type.of(Double.class);
        }
        if (type == PrimitiveTypes.Float) {
            return Type.of(Float.class);
        }
        if (type == PrimitiveTypes.Short) {
            return Type.of(Short.class);
        }
        if (type == PrimitiveTypes.Byte) {
            return Type.of(Byte.class);
        }
        if (type == PrimitiveTypes.Boolean) {
            return Type.of(Boolean.class);
        }
        if (type == PrimitiveTypes.Character) {
            return Type.of(Character.class);
        }
        return null;
    }

    public static boolean isArithmetic(final Type type) {
        final Type underlyingPrimitive = getUnderlyingPrimitive(type);
        final Type actualType = underlyingPrimitive != null ? underlyingPrimitive : type;

        return actualType == PrimitiveTypes.Integer ||
               actualType == PrimitiveTypes.Long ||
               actualType == PrimitiveTypes.Double ||
               actualType == PrimitiveTypes.Float ||
               actualType == PrimitiveTypes.Short ||
               actualType == PrimitiveTypes.Byte ||
               actualType == PrimitiveTypes.Character;
    }

    public static boolean isIntegralOrBoolean(final Type type) {
        final Type underlyingPrimitive = getUnderlyingPrimitive(type);
        final Type actualType = underlyingPrimitive != null ? underlyingPrimitive : type;

        return actualType == PrimitiveTypes.Integer ||
               actualType == PrimitiveTypes.Long ||
               actualType == PrimitiveTypes.Short ||
               actualType == PrimitiveTypes.Byte ||
               actualType == PrimitiveTypes.Character ||
               actualType == PrimitiveTypes.Boolean;
    }

    public static boolean isIntegral(final Type type) {
        final Type underlyingPrimitive = getUnderlyingPrimitive(type);
        final Type actualType = underlyingPrimitive != null ? underlyingPrimitive : type;

        return actualType == PrimitiveTypes.Integer ||
               actualType == PrimitiveTypes.Long ||
               actualType == PrimitiveTypes.Short ||
               actualType == PrimitiveTypes.Byte ||
               actualType == PrimitiveTypes.Character;
    }

    public static boolean isBoolean(final Type type) {
        return type == PrimitiveTypes.Boolean || type == Type.of(Boolean.class);
    }

    public static boolean areEquivalent(final Type class1, final Type class2) {
        return class1 == class2;
    }

    public static boolean hasIdentityPrimitiveOrBoxingConversion(final Type source, final Type destination) {
        assert source != null && destination != null;

        // Identity conversion
        if (source == destination || areEquivalent(source, destination)) {
            return true;
        }

        // Boxing conversions
        return isAutoUnboxed(source) && areEquivalent(destination, getUnderlyingPrimitive(source)) ||
               isAutoUnboxed(destination) && areEquivalent(source, getUnderlyingPrimitive(destination));
    }

    public static boolean hasReferenceConversion(final Type source, final Type destination) {
        assert source != null && destination != null;

        // void -> void conversion is handled elsewhere (it's an identity conversion) 
        // All other void conversions are disallowed.
        if (source == PrimitiveTypes.Void || destination == PrimitiveTypes.Void) {
            return false;
        }

        final Type unboxedSourceType = isAutoUnboxed(source) ? getUnderlyingPrimitive(source) : source;
        final Type unboxedDestinationType = isAutoUnboxed(destination) ? getUnderlyingPrimitive(destination) : destination;

        // Down conversion 
        if (unboxedSourceType.isAssignableFrom(unboxedDestinationType)) {
            return true;
        }

        // Up conversion 
        if (unboxedDestinationType.isAssignableFrom(unboxedSourceType)) {
            return true;
        }

        // Interface conversion
        if (source.isInterface() || destination.isInterface()) {
            return true;
        }

        // Object conversion 
        return source == Type.Object || destination == Type.Object;
    }

    public static MethodInfo getCoercionMethod(final Type source, final Type destination) {
        // NOTE: If destination type is an autoboxing type, we will need an implicit box later.
        final Type unboxedDestinationType = isAutoUnboxed(destination) ? getUnderlyingPrimitive(destination) : destination;

        if (!destination.isPrimitive()) {
            return null;
        }

        final MethodInfo method;

        if (destination == PrimitiveTypes.Integer) {
            method = source.getMethod("booleanValue");
        }
        else if (destination == PrimitiveTypes.Long) {
            method = source.getMethod("longValue");
        }
        else if (destination == PrimitiveTypes.Double) {
            method = source.getMethod("doubleValue");
        }
        else if (destination == PrimitiveTypes.Float) {
            method = source.getMethod("floatValue");
        }
        else if (destination == PrimitiveTypes.Short) {
            method = source.getMethod("shortValue");
        }
        else if (destination == PrimitiveTypes.Byte) {
            method = source.getMethod("byteValue");
        }
        else if (destination == PrimitiveTypes.Boolean) {
            method = source.getMethod("booleanValue");
        }
        else if (destination == PrimitiveTypes.Character) {
            method = source.getMethod("charValue");
        }
        else {
            return null;
        }

        if (method.getReturnType() == unboxedDestinationType) {
            return method;
        }

        return null;
    }

    public static boolean areReferenceAssignable(final Type destination, final Type source) {
        if (destination == Type.Object) {
            return true;
        }
        // WARNING: This actually checks "is this identity assignable and/or reference assignable?"
        return hasIdentityPrimitiveOrBoxingConversion(source, destination) ||
               !destination.isPrimitive() && !source.isPrimitive() && destination.isAssignableFrom(source);
    }

    public static boolean hasReferenceEquality(final Type left, final Type right) {
        if (left.isPrimitive() || right.isPrimitive()) {
            return false;
        }

        // If we have an interface and a reference type then we can do
        // reference equality.

        // If we have two reference types and one is assignable to the
        // other then we can do reference equality.

        return left.isInterface() || right.isInterface() ||
               areReferenceAssignable(left, right) ||
               areReferenceAssignable(right, left);
    }

    public static boolean hasBuiltInEqualityOperator(final Type left, final Type right) {
        // If we have an interface and a reference type, then we can do reference equality.
        if (left.isInterface() && !right.isPrimitive()) {
            return true;
        }

        if (right.isInterface() && !left.isPrimitive()) {
            return true;
        }

        // If we have two reference types, and one is assignable to the other, then we can do reference equality.
        if (!left.isPrimitive() && !right.isPrimitive()) {
            if (areReferenceAssignable(left, right) || areReferenceAssignable(right, left)) {
                return true;
            }
        }

        // Otherwise, if the types are not the same then we definitely do not have a built-in equality operator.
        if (!areEquivalent(left, right)) {
            return false;
        }

        // We have two identical value types, modulo boxed state.  (If they were both the
        // same reference type then we would have returned true earlier.)
        assert left.isPrimitive() || right.isPrimitive();

        return true;
    }

    public static boolean isValidInvocationTargetType(final MethodInfo method, final Type targetType) {
        return areReferenceAssignable(method.getDeclaringType(), targetType);
    }
}
