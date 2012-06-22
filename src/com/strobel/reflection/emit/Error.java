package com.strobel.reflection.emit;

import com.strobel.util.ContractUtils;

/**
 * @author strobelm
 */
final class Error {
    private Error() {
        throw ContractUtils.unreachable();
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

    public static RuntimeException methodIsFinished() {
        throw new RuntimeException(
            "Cannot modify a method after it has been finished."
        );
    }

    public static RuntimeException unmatchedLocal() {
        throw new RuntimeException(
            "Local variable does not belong to this method."
        );
    }
}
