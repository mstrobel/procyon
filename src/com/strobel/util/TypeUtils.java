package com.strobel.util;

import java.lang.reflect.Method;

/**
 * @author Mike Strobel
 */
public final class TypeUtils {
    private TypeUtils() {}

    public static boolean isAutoUnboxed(final Class type) {
        return type == Integer.class ||
               type == Long.class ||
               type == Double.class ||
               type == Float.class ||
               type == Short.class ||
               type == Byte.class ||
               type == Boolean.class ||
               type == Character.class;
    }

    public static Class getUnderlyingPrimitive(final Class type) {
        if (type == Integer.class) {
            return Integer.TYPE;
        }
        if (type == Long.class) {
            return Long.TYPE;
        }
        if (type == Double.class) {
            return Double.TYPE;
        }
        if (type == Float.class) {
            return Float.TYPE;
        }
        if (type == Short.class) {
            return Short.TYPE;
        }
        if (type == Byte.class) {
            return Byte.TYPE;
        }
        if (type == Boolean.class) {
            return Boolean.TYPE;
        }
        if (type == Character.class) {
            return Character.TYPE;
        }
        return null;
    }

    public static Class getBoxedTypeOrSelf(final Class type) {
        final Class boxedType = getBoxedType(type);
        return boxedType != null ? boxedType : type;
    }

    public static Class getUnderlyingPrimitiveOrSelf(final Class type) {
        if (isAutoUnboxed(type)) {
            return getUnderlyingPrimitive(type);
        }
        return type;
    }

    public static Class getBoxedType(final Class type) {
        if (isAutoUnboxed(type)) {
            return type;
        }
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == Integer.TYPE) {
            return Integer.class;
        }
        if (type == Long.TYPE) {
            return Long.class;
        }
        if (type == Double.TYPE) {
            return Double.class;
        }
        if (type == Float.TYPE) {
            return Float.class;
        }
        if (type == Short.TYPE) {
            return Short.class;
        }
        if (type == Byte.TYPE) {
            return Byte.class;
        }
        if (type == Boolean.TYPE) {
            return Boolean.class;
        }
        if (type == Character.TYPE) {
            return Character.class;
        }
        return null;
    }

    public static boolean isArithmetic(final Class type) {
        final Class underlyingPrimitive = getUnderlyingPrimitive(type);
        final Class actualType = underlyingPrimitive != null ? underlyingPrimitive : type;

        return actualType == Integer.TYPE ||
               actualType == Long.TYPE ||
               actualType == Double.TYPE ||
               actualType == Float.TYPE ||
               actualType == Short.TYPE ||
               actualType == Byte.TYPE ||
               actualType == Character.TYPE;
    }

    public static boolean isIntegralOrBoolean(final Class type) {
        final Class underlyingPrimitive = getUnderlyingPrimitive(type);
        final Class actualType = underlyingPrimitive != null ? underlyingPrimitive : type;

        return actualType == Integer.TYPE ||
               actualType == Long.TYPE ||
               actualType == Short.TYPE ||
               actualType == Byte.TYPE ||
               actualType == Character.TYPE ||
               actualType == Boolean.TYPE;
    }

    public static boolean isIntegral(final Class type) {
        final Class underlyingPrimitive = getUnderlyingPrimitive(type);
        final Class actualType = underlyingPrimitive != null ? underlyingPrimitive : type;

        return actualType == Integer.TYPE ||
               actualType == Long.TYPE ||
               actualType == Short.TYPE ||
               actualType == Byte.TYPE ||
               actualType == Character.TYPE;
    }

    public static boolean isBoolean(final Class type) {
        return type == Boolean.TYPE || type == Boolean.class;
    }

    public static boolean areEquivalent(final Class class1, final Class class2) {
        return class1 == class2;
    }

    public static boolean hasIdentityPrimitiveOrBoxingConversion(final Class source, final Class destination) {
        assert source != null && destination != null;

        // Identity conversion
        if (areEquivalent(source, destination)) {
            return true;
        }

        // Boxing conversions
        return isAutoUnboxed(source) && areEquivalent(destination, getUnderlyingPrimitive(source)) ||
               isAutoUnboxed(destination) && areEquivalent(source, getUnderlyingPrimitive(destination));
    }

    public static boolean hasReferenceConversion(final Class source, final Class destination) {
        assert source != null && destination != null;

        // void -> void conversion is handled elsewhere (it's an identity conversion) 
        // All other void conversions are disallowed.
        if (source == Void.TYPE || destination == Void.TYPE) {
            return false;
        }

        final Class unboxedSourceType = isAutoUnboxed(source) ? getUnderlyingPrimitive(source) : source;
        final Class unboxedDestinationType = isAutoUnboxed(destination) ? getUnderlyingPrimitive(destination) : destination;

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
        return source == Object.class || destination == Object.class;
    }

    public static Method getCoercionMethod(final Class source, final Class destination) {
        // NOTE: If destination type is an autoboxing type, we will need an implicit box later.
        final Class unboxedDestinationType = isAutoUnboxed(destination) ? getUnderlyingPrimitive(destination) : destination;

        if (!destination.isPrimitive()) {
            return null;
        }

        try {
            final Method method;

            if (destination == Integer.TYPE) {
                method = source.getMethod("booleanValue");
            }
            else if (destination == Long.TYPE) {
                method = source.getMethod("longValue");
            }
            else if (destination == Double.TYPE) {
                method = source.getMethod("doubleValue");
            }
            else if (destination == Float.TYPE) {
                method = source.getMethod("floatValue");
            }
            else if (destination == Short.TYPE) {
                method = source.getMethod("shortValue");
            }
            else if (destination == Byte.TYPE) {
                method = source.getMethod("byteValue");
            }
            else if (destination == Boolean.TYPE) {
                method = source.getMethod("booleanValue");
            }
            else if (destination == Character.TYPE) {
                method = source.getMethod("charValue");
            }
            else {
                return null;
            }

            if (method.getReturnType() == unboxedDestinationType) {
                return method;
            }
        }
        catch (NoSuchMethodException ignored) {
        }

        return null;
    }

    public static boolean areReferenceAssignable(final Class destination, final Class source) {
        if (destination == Object.class) {
            return true;
        }
        // WARNING: This actually checks "is this identity assignable and/or reference assignable?"
        return areEquivalent(destination, source) ||
               !destination.isPrimitive() && !source.isPrimitive() && destination.isAssignableFrom(source);
    }

    public static boolean hasReferenceEquality(final Class left, final Class right) {
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

    public static boolean hasBuiltInEqualityOperator(final Class left, final Class right) {
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

    public static boolean isValidInvocationTargetType(final Method method, final Class targetType) {
        return areReferenceAssignable(method.getDeclaringClass(), targetType);
    }
}
