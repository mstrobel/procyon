package com.strobel.reflection;

import static java.lang.String.format;

/**
 * @author Mike Strobel
 */
final class Error {
    private Error() {
    }

    public static RuntimeException notGenericParameter(final Type type) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' is not a generic parameter.",
                type.getName()
            )
        );
    }

    public static RuntimeException notGenericType(final Type type) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' is not a generic type.",
                type.getName()
            )
        );
    }

    public static RuntimeException notGenericMethod(final MethodInfo method) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' is not a generic method.",
                method.getName()
            )
        );
    }

    public static RuntimeException notGenericMethodDefinition(final MethodInfo method) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' is not a generic method definition.",
                method.getName()
            )
        );
    }

    public static RuntimeException noElementType(final Type type) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' does not have an element type.",
                type.getName()
            )
        );
    }

    public static RuntimeException notEnumType(final Type type) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' is not an enum type.",
                type.getName()
            )
        );
    }

    public static RuntimeException notArrayType(final Type type) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' is not an array type.",
                type.getName()
            )
        );
    }

    public static RuntimeException ambiguousMatch() {
        throw new RuntimeException("Ambiguous match found.");

    }

    public static RuntimeException incorrectNumberOfTypeArguments() {
        throw new UnsupportedOperationException(
            "Incorrect number of type arguments provided."
        );
    }

    public static RuntimeException incorrectNumberOfTypeArguments(final Type type) {
        throw new UnsupportedOperationException(
            format(
                "Incorrect number of type arguments provided for generic type '%s'.",
                type.getName()
            )
        );
    }

    public static RuntimeException notGenericTypeDefinition(final Type type) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' is not a generic type definition.",
                type.getName()
            )
        );
    }

    public static RuntimeException notPrimitiveType(final Class<?> type) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' is not a primitive type.",
                type.getName()
            )
        );
    }

    public static RuntimeException typeParameterNotDefined(final Type typeParameter) {
        throw new UnsupportedOperationException(
            format(
                "Generic parameter '%' is not defined on this type.",
                typeParameter.getName()
            )
        );

    }
}
