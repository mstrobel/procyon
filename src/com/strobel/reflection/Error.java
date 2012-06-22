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
                type.getFullName()
            )
        );
    }

    public static RuntimeException notGenericType(final Type type) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' is not a generic type.",
                type.getFullName()
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
                type.getFullName()
            )
        );
    }

    public static RuntimeException notEnumType(final Type type) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' is not an enum type.",
                type.getFullName()
            )
        );
    }

    public static RuntimeException notArrayType(final Type type) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' is not an array type.",
                type.getFullName()
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
                type.getFullName()
            )
        );
    }

    public static RuntimeException notGenericTypeDefinition(final Type type) {
        throw new UnsupportedOperationException(
            format(
                "Type '%s' is not a generic type definition.",
                type.getFullName()
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
                "Generic parameter '%s' is not defined on this type.",
                typeParameter.getFullName()
            )
        );
    }

    public static RuntimeException couldNotResolveMethod(final Object signature) {
        throw new RuntimeException(
            format(
                "Could not resolve method '%s'.",
                signature
            )
        );
    }

    public static RuntimeException couldNotResolveMember(final MemberInfo member) {
        throw new MemberResolutionException(member);
    }

    public static RuntimeException couldNotResolveType(final Object signature) {
        throw new RuntimeException(
            format(
                "Could not resolve type '%s'.",
                signature
            )
        );
    }

    public static RuntimeException couldNotResolveParameterType(final Object signature) {
        throw new RuntimeException(
            format(
                "Could not resolve type for parameter '%s'.",
                signature
            )
        );
    }

    public static RuntimeException typeArgumentsMustContainBoundType() {
        throw new RuntimeException(
            "Type arguments must bind at least one generic parameter."
        );
    }

    public static RuntimeException compoundTypeMayOnlyHaveOneClassBound() {
        throw new RuntimeException(
            "Compound types may only be bounded by one class, and it must be the first type in " +
            "the bound list.  All other bounds must be interface types."
        );
    }

    public static RuntimeException compoundTypeMayNotHaveGenericParameterBound() {
        throw new RuntimeException(
            "Compound types may not be bounded by a generic parameter."
        );
    }

    public static RuntimeException bytecodeGeneratorNotOwnedByMethodBuilder() {
        throw new RuntimeException(
            "This BytecodeGenerator was not created by a MethodBuilder."
        );
    }

    public static RuntimeException typeHasBeenCreated() {
        throw new RuntimeException(
            "This type has already been created."
        );
    }
}
