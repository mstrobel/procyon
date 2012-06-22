package com.strobel.reflection.emit;

import com.strobel.reflection.Type;
import com.strobel.util.ContractUtils;

import static java.lang.String.format;

/**
 * @author strobelm
 */
final class Error {
    private Error() {
        throw ContractUtils.unreachable();
    }
    
    public static RuntimeException bytecodeGeneratorNotOwnedByMethodBuilder() {
        return new RuntimeException(
            "This BytecodeGenerator was not created by a MethodBuilder."
        );
    }

    public static RuntimeException typeHasBeenCreated() {
        return new RuntimeException(
            "This type has already been created."
        );
    }

    public static RuntimeException methodIsFinished() {
        return new RuntimeException(
            "Cannot modify a method after it has been finished."
        );
    }

    public static RuntimeException unmatchedLocal() {
        return new RuntimeException(
            "Local variable does not belong to this method."
        );
    }

    public static RuntimeException badLabel() {
        return new RuntimeException(
            "Label does not belong to this method."
        );
    }

    public static RuntimeException badLabelContent() {
        return new RuntimeException(
            "Label has no value."
        );
    }

    public static RuntimeException labelAlreadyDefined() {
        return new RuntimeException(
            "Label has already been defined."
        );
    }

    public static RuntimeException unclosedExceptionBlock() {
        return new RuntimeException(
            "Unclosed exception block."
        );
    }

    public static RuntimeException illegalTwoByteBranch(final int position, final int address) {
        return new RuntimeException(
            format(
                "Illegal two byte branch (position = %s, address = %s).",
                position,
                address
            )
        );
    }

    public static RuntimeException invokeOpCodeRequired() {
        return new RuntimeException(
            "OpCode must be one of: INVOKEDYNAMIC, INVOKEINTERFACE, INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL"
        );
    }

    public static RuntimeException invalidType(final Type<?> type) {
        return new RuntimeException(
            format("Invalid type: %s", type)
        );
    }

    public static RuntimeException constructorNotFound() {
        return new RuntimeException(
            "Type does not have a constructor matching the specified arguments."
        );
    }

    public static RuntimeException cannotInstantiateUnboundGenericType(final Type<?> type) {
        return new RuntimeException(
            format(
                "Cannot instantiate type '%s' because it has unbound generic type parameters.",
                type
            )
        );
    }

    public static RuntimeException boxFailure(final Type<?> type) {
        return new RuntimeException(
            format(
                "Could not find a boxing method or constructor for type '%s'.",
                type
            )
        );
    }
}
