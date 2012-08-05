package com.strobel.reflection.emit;

import com.strobel.core.VerifyArgument;
import com.strobel.reflection.MethodBase;
import com.strobel.reflection.Type;
import com.strobel.util.ContractUtils;
import sun.misc.Unsafe;

import java.lang.annotation.Annotation;

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
            "Operation cannot be performed after createType() has been called."
        );
    }

    public static RuntimeException typeHasNotBeenCreated() {
        return new RuntimeException(
            "Operation cannot be performed until createType() has been called."
        );
    }

    public static RuntimeException typeIsGeneric() {
        return new IllegalStateException(
            "Operation is not valid on bound generic types."
        );
    }

    public static RuntimeException methodIsGeneric() {
        return new IllegalStateException(
            "Operation is not valid on bound generic methods."
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

    public static RuntimeException cannotConvertToOrFromVoid() {
        return new RuntimeException(
            "Cannot convert to or from 'void'."
        );
    }

    public static RuntimeException invalidCast(final Type sourceType, final Type targetType) {
        return new RuntimeException(
            format(
                "Cannot cast from '%s' to '%s'.",
                sourceType,
                targetType
            )
        );
    }

    public static RuntimeException newArrayDimensionsOutOfRange(final Type<?> arrayType, final int dimensions) {
        VerifyArgument.notNull(arrayType, "arrayType");

        int actualDimensions = 0;
        Type<?> currentType = arrayType;

        while (currentType.isArray()) {
            ++actualDimensions;
            currentType = arrayType.getElementType();
        }

        return new RuntimeException(
            format(
                "Cannot initialize %s dimensions of a(n) %s because the array only has %s dimensions.",
                dimensions,
                arrayType,
                actualDimensions
            )
        );
    }

    public static RuntimeException argumentIndexOutOfRange(final MethodBase method, final int index) {
        return new RuntimeException(
            format(
                "Argument %s is out of range.  Method: %s",
                index,
                method
            )
        );
    }

    public static RuntimeException cannotLoadThisForStaticMethod() {
        return new RuntimeException(
            "Cannot reference 'this' from within a static method."
        );
    }

    public static RuntimeException invalidBranchOpCode(final OpCode opCode) {
        return new RuntimeException(
            format("Expected a GOTO or JSR opcode, but found %s.", opCode)
        );
    }

    public static RuntimeException cannotModifyTypeAfterCreateType() {
        return new IllegalStateException("Type cannot be modified after calling createType().");
    }

    public static RuntimeException typeNameTooLong() {
        return new IllegalArgumentException("The specified name is too long.");
    }

    public static RuntimeException baseTypeCannotBeInterface() {
        return new IllegalArgumentException("Base type cannot be an interface.");
    }

    public static RuntimeException typeNotCreated() {
        return new RuntimeException(
            "Type has not been created yet."
        );
    }

    public static RuntimeException cannotModifyMethodAfterCallingGetGenerator() {
        return new IllegalStateException("Method cannot be modified after calling getCodeGenerator().");
    }

    public static RuntimeException genericParametersAlreadySet() {
        return new IllegalStateException("Generic parameters have already been defined.");
    }

    public static RuntimeException methodHasOpenLocalScope() {
        return new IllegalStateException("Method body still has an open local scope.");
    }

    public static RuntimeException abstractMethodDeclaredOnNonAbstractType() {
        return new IllegalStateException("Abstract method declared on non-abstract class.");
    }

    public static RuntimeException abstractMethodCannotHaveBody() {
        return new IllegalStateException("Abstract method cannot have a body.");
    }

    public static RuntimeException methodHasEmptyBody(final MethodBuilder method) {
        return new IllegalStateException(
            format(
                "Method '%s' on type '%s' has an empty body.", method.getName(),
                method.getDeclaringType().getName()
            )
        );
    }

    public static RuntimeException notInExceptionBlock() {
        return new IllegalStateException("Not in an exception block.");
    }

    public static RuntimeException badExceptionCodeGenerated() {
        return new IllegalStateException("Incorrect code generated for exception block.");
    }

    public static RuntimeException catchRequiresThrowableType() {
        return new IllegalStateException("Catch block requires a Throwable type.");
    }

    public static RuntimeException couldNotLoadUnsafeClassInstance() {
        return new IllegalStateException(
            format("Could not load an instance of the %s class.", Unsafe.class.getName())
        );
    }

    public static RuntimeException valueMustBeConstant() {
        return new IllegalArgumentException("Value must be a primitive compile-time constant.");
    }

    public static RuntimeException annotationRequiresValue(final Type<? extends Annotation> annotationType) {
        return new IllegalArgumentException(
            format(
                "Annotation '%s' requires an argument.",
                annotationType.getName()
            )
        );
    }

    public static RuntimeException attributeValueCountMismatch() {
        return new IllegalArgumentException("A matching number of attributes and values is required.");
    }

    public static RuntimeException attributeValueIncompatible(final Type<?> attributeType, final Type<?> valueType) {
        if (valueType == null || valueType == Type.NullType) {
            return new IllegalArgumentException(
                format(
                    "A null value is invalid for a attribute of type '%s'.",
                    attributeType.getName()
                )
            );
        }
        return new IllegalArgumentException(
            format(
                "A value of type '%s' is invalid for a attribute of type '%s'.",
                valueType.getName(),
                attributeType.getName()
            )
        );
    }

    public static RuntimeException annotationHasNoDefaultAttribute() {
        return new IllegalArgumentException("Annotation has no default attribute.");
    }

    public static RuntimeException typeNotAnAnnotation(final Type<? extends Annotation> type) {
        return new IllegalArgumentException(
            format("Type '%s' is not an annotation.", type.getName())
        );
    }
}
