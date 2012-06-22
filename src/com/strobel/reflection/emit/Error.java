package com.strobel.reflection.emit;

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
}
