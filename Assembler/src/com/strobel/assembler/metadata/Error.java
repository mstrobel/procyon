package com.strobel.assembler.metadata;

import static java.lang.String.format;

/**
 * @author Mike Strobel
 */
final class Error {
    public static RuntimeException notGenericParameter(final TypeReference type) {
        return new UnsupportedOperationException(
            format(
                "TypeReference '%s' is not a generic parameter.",
                type.getFullName()
            )
        );
    }

    public static RuntimeException notWildcard(final TypeReference type) {
        throw new UnsupportedOperationException(
            format(
                "TypeReference '%s' is not a wildcard or captured type.",
                type.getFullName()
            )
        );
    }

    public static RuntimeException notBoundedType(final TypeReference type) {
        throw new UnsupportedOperationException(
            format(
                "TypeReference '%s' is not a bounded type.",
                type.getFullName()
            )
        );
    }

    public static RuntimeException notGenericType(final TypeReference type) {
        return new UnsupportedOperationException(
            format(
                "TypeReference '%s' is not a generic type.",
                type.getFullName()
            )
        );
    }

    public static RuntimeException notGenericMethod(final MethodReference method) {
        return new UnsupportedOperationException(
            format(
                "TypeReference '%s' is not a generic method.",
                method.getName()
            )
        );
    }

    public static RuntimeException notGenericMethodDefinition(final MethodReference method) {
        return new UnsupportedOperationException(
            format(
                "TypeReference '%s' is not a generic method definition.",
                method.getName()
            )
        );
    }

    public static RuntimeException noElementType(final TypeReference type) {
        return new UnsupportedOperationException(
            format(
                "TypeReference '%s' does not have an element type.",
                type.getFullName()
            )
        );
    }

    public static RuntimeException notEnumType(final TypeReference type) {
        return new UnsupportedOperationException(
            format(
                "TypeReference '%s' is not an enum type.",
                type.getFullName()
            )
        );
    }

    public static RuntimeException notArrayType(final TypeReference type) {
        return new UnsupportedOperationException(
            format(
                "TypeReference '%s' is not an array type.",
                type.getFullName()
            )
        );
    }

    public static RuntimeException invalidSignatureTypeExpected(final String signature, final int position) {
        return new IllegalArgumentException(
            format(
                "Invalid signature: type expected at position %d (%s).",
                position,
                signature
            )
        );
    }

    public static RuntimeException invalidSignatureTopLevelGenericParameterUnexpected(final String signature, final int position) {
        return new IllegalArgumentException(
            format(
                "Invalid signature: unexpected generic parameter at position %d.  (%s)",
                position,
                signature
            )
        );
    }

    public static RuntimeException invalidSignatureNonGenericTypeTypeArguments(final TypeReference type) {
        return new IllegalArgumentException(
            format(
                "Invalid signature: unexpected type arguments specified for non-generic type '%s'.",
                type.getBriefDescription()
            )
        );
    }

    public static RuntimeException invalidSignatureUnexpectedToken(final String signature, final int position) {
        return new IllegalArgumentException(
            format(
                "Invalid signature: unexpected token at position %d.  (%s)",
                position,
                signature
            )
        );
    }

    public static RuntimeException invalidSignatureUnexpectedEnd(final String signature, final int position) {
        return new IllegalArgumentException(
            format(
                "Invalid signature: unexpected end of signature at position %d.  (%s)",
                position,
                signature
            )
        );
    }

    public static RuntimeException invalidSignatureExpectedEndOfTypeArguments(final String signature, final int position) {
        return new IllegalArgumentException(
            format(
                "Invalid signature: expected end of type argument list at position %d.  (%s)",
                position,
                signature
            )
        );
    }

    public static RuntimeException invalidSignatureExpectedTypeArgument(final String signature, final int position) {
        return new IllegalArgumentException(
            format(
                "Invalid signature: expected type argument at position %d.  (%s)",
                position,
                signature
            )
        );
    }
}
