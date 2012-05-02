package com.strobel.reflection;

import static java.lang.String.format;

/**
 * @author Mike Strobel
 */
final class Error {
    private Error() {}

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
}
