package com.strobel.expressions;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static java.lang.String.format;

/**
 * @author Mike Strobel
 */
final class Error {
    private Error() {}

    public static RuntimeException extensionMustOverride(final String memberName) {
        return new RuntimeException(
            format("Expression extensions must override %s.", memberName)
        );
    }

    public static RuntimeException reducibleMustOverride(final String memberName) {
        return new RuntimeException(
            format("Reducible expression extensions must override %s.", memberName)
        );
    }

    public static RuntimeException memberNotField(final AccessibleObject member) {
        return new RuntimeException(
            format("Member '%s' must be a field.", member)
        );
    }

    public static RuntimeException mustBeReducible() {
        return new RuntimeException(
            "Expression must be reducible to perform this operation."
        );
    }

    public static RuntimeException mustReduceToDifferent() {
        return new RuntimeException(
            "Expression must reducible to a different expression."
        );
    }

    public static RuntimeException reducedNotCompatible() {
        return new RuntimeException(
            "Expression was reduced to an expression of a non-compatible type."
        );
    }

    public static RuntimeException argumentTypesMustMatch() {
        return new RuntimeException(
            format("Argument types must match.")
        );
    }

    public static RuntimeException argumentCannotBeOfTypeVoid() {
        return new RuntimeException(
            format("Argument cannot be of type 'void'.")
        );
    }

    public static RuntimeException expressionMustBeWriteable(final String parameterName) {
        return new RuntimeException(
            format("Argument '%s' must be writeable.", parameterName)
        );
    }

    public static RuntimeException expressionMustBeReadable(final String parameterName) {
        return new RuntimeException(
            format("Argument '%s' must be readable.", parameterName)
        );
    }

    public static RuntimeException mustRewriteChildToSameType(final Class before, final Class after, final String callerName) {
        return new RuntimeException(
            format("Method '%s' performed an illegal type change from %s to %s.", callerName, before, after)
        );
    }

    public static RuntimeException mustRewriteWithoutMethod(final Method method, final String callerName) {
        return new RuntimeException(
            format(
                "Rewritten expression calls method '%s', but the original node had no method.  " +
                "If this is is intentional, override '%s' and change it to allow this rewrite.",
                callerName,
                method
            )
        );
    }

    public static <T extends Expression> RuntimeException mustRewriteToSameNode(
        final String callerName,
        final Class<T> type,
        final String overrideMethodName) {

        return new RuntimeException(
            format(
                "When called from '%s', rewriting a node of type '%s' must return a non-null value of the " +
                "same type.  Alternatively, override '%s' and change it to not visit children of this type.",
                callerName,
                type.getName(),
                overrideMethodName
            )
        );
    }

    public static RuntimeException unhandledUnary(final ExpressionType unaryType) {
        return new RuntimeException(
            format("Unhandled unary expression type: %s.", unaryType)
        );
    }

    public static RuntimeException unhandledBinary(final ExpressionType binaryType) {
        return new RuntimeException(
            format("Unhandled binary expression type: %s.", binaryType)
        );
    }

    public static RuntimeException unmodifiableCollection() {
        return new RuntimeException("Collection cannot be modified.");
    }

    public static RuntimeException duplicateVariable(final ParameterExpression variable) {
        return new RuntimeException(
            format(
                "Found duplicate variable '%s'.  Each ParameterExpression in the list " +
                "must be a unique object.",
                variable.getName()
            )
        );
    }

    public static RuntimeException unaryOperatorNotDefined(final ExpressionType operator, final Class operandType) {
        return new RuntimeException(
            format(
                "The unary operator '%s' is not defined for type '%s'",
                operator,
                operandType
            )
        );
    }

    public static RuntimeException operatorMethodMustNotBeStatic(final Method method) {
        return new RuntimeException(
            format(
                "Method '%s.%s' cannot be used as an operator because it is static.",
                method.getDeclaringClass().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException operatorMethodMustNotReturnVoid(final Method method) {
        return new RuntimeException(
            format(
                "Method '%s.%s' cannot be used as an operator because it returns void.",
                method.getDeclaringClass().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException operatorMethodParametersMustMatchReturnValue(final Method method) {
        return new RuntimeException(
            format(
                "Method '%s.%s' cannot be used as an operator because its parameters do not match " +
                "its return value.",
                method.getDeclaringClass().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException returnTypeDoesNotMatchOperandType(final ExpressionType expressionType, final Method method) {
        return new RuntimeException(
            format(
                "The return type for operator '%s' does not match the declaring type of method '%s.%s'.",
                expressionType,
                method.getDeclaringClass().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException returnTypeDoesNotMatchOperandType(final Method method) {
        return new RuntimeException(
            format(
                "The return type of operator method '%s.%s' does not match the method's declaring type.",
                method.getDeclaringClass().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException incorrectNumberOfConstructorArguments() {
        return new RuntimeException("Incorrect number of arguments supplied for constructor call.");
    }

    public static RuntimeException incorrectNumberOfLambdaArguments() {
        return new RuntimeException("Incorrect number of arguments supplied for lambda invocation.");
    }

    public static RuntimeException incorrectNumberOfMethodCallArguments(final Method method) {
        return new RuntimeException(
            format(
                "Incorrect number of arguments supplied for call to method '%s.%s'",
                method.getDeclaringClass().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException invalidUnboxType() {
        return new RuntimeException(
            "Can only unbox from a standard boxed type or java.lang.Object to a primitive type."
        );
    }

    public static RuntimeException argumentMustBeArray() {
        return new RuntimeException("Argument must be an array.");
    }

    public static RuntimeException coercionOperatorNotDefined(final Class sourceType, final Type destinationType) {
        return new RuntimeException(
            format(
                "No coercion operator is defined between types '%s' and '%s'.",
                sourceType,
                destinationType
            )
        );
    }

    public static RuntimeException argumentMustNotHaveValueType() {
        return new RuntimeException(
            "Argument must not have a primitive type."
        );
    }

    public static RuntimeException methodBasedOperatorMustHaveValidReturnType(final Method method) {
        return new RuntimeException(
            format(
                "The operator method '%s.%s' must return the same type as its declaring type " +
                "or a derived type.",
                method.getDeclaringClass().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException methodBasedOperatorMustHaveValidReturnType(final ExpressionType operator, final Method method) {
        return new RuntimeException(
            format(
                "The operator method '%s.%s' for operator '%s' must return the same type as its " +
                "declaring type or a derived type.",
                method.getDeclaringClass().getName(),
                method.getName(),
                operator
            )
        );
    }

    public static RuntimeException expressionTypeNotInvokable(final Class type) {
        return new RuntimeException(
            format(
                "Expression of type '%s' cannot be invoked.  Invokable types must be interfaces " +
                "with exactly one method.",
                type
            )
        );
    }

    public static RuntimeException binaryOperatorNotDefined(final ExpressionType operator, final Class leftType, final Class rightType) {
        return new RuntimeException(
            format(
                "The binary operator '%s' is not defined for the types '%s' and '%s'.",
                operator,
                leftType,
                rightType
            )
        );
    }

    public static RuntimeException referenceEqualityNotDefined(final Class leftType, final Class rightType) {
        return new RuntimeException(
            format(
                "Reference equality is not defined for the types '%s' and '%s'.",
                leftType,
                rightType
            )
        );
    }

    public static RuntimeException invalidOperator(final ExpressionType operator) {
        return new RuntimeException(
            format("Invalid operator: %s", operator)
        );
    }

    public static RuntimeException targetRequiredForNonStaticMethodCall(final Method method) {
        return new RuntimeException(
            format(
                "An invocation target expression is required for a call to non-static " +
                "method '%s.%s'.",
                method.getDeclaringClass().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException targetInvalidForStaticMethodCall(final Method method) {
        return new RuntimeException(
            format(
                "An invocation target expression cannot be used to call static " +
                "method '%s.%s'.",
                method.getDeclaringClass().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException targetAndMethodTypeMismatch(final Method method, final Class targetType) {
        return new RuntimeException(
            format(
                "Expression of type '%s' is not a valid invocation target for instance " +
                "method '%s.%s'.",
                targetType.getName(),
                method.getDeclaringClass().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException expressionTypeDoesNotMatchParameter(final Class argType, final Class<?> parameterType) {
        return new RuntimeException(
            format(
                "Expression of type '%s' cannot be used for constructor parameter of type '%s'.",
                parameterType.getName(),
                argType.getName()
            )
        );
    }

    public static RuntimeException expressionTypeDoesNotMatchConstructorParameter(final Class argType, final Class<?> parameterType) {
        return new RuntimeException(
            format(
                "Expression of type '%s' cannot be used for parameter of type '%s'.",
                parameterType.getName(),
                argType.getName()
            )
        );
    }

    public static RuntimeException expressionTypeDoesNotMatchMethodParameter(final Class argType, final Class<?> parameterType, final Method method) {
        return new RuntimeException(
            format(
                "Expression of type '%s' cannot be used for parameter of type '%s' of method '%s.%s'.",
                parameterType.getName(),
                argType.getName(),
                method.getDeclaringClass().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException methodDoesNotExistOnType(final String methodName, final Class<?> type) {
        return new RuntimeException(
            format(
                "No method '%s' exists on type '%s'.",
                methodName,
                type.getName()
            )
        );
    }

    public static RuntimeException genericMethodWithArgsDoesNotExistOnType(final String methodName, final Class type) {
        return new RuntimeException(
            format(
                "No generic method '%s' on type '%s' is compatible with the supplied type arguments and arguments.  " +
                "No type arguments should be provided if the method is non-generic.",
                methodName,
                type.getName()
            )
        );
    }

    public static RuntimeException methodWithArgsDoesNotExistOnType(final String methodName, final Class type) {
        return new RuntimeException(
            format(
                "No method '%s' on type '%s' is compatible with the supplied arguments.",
                methodName,
                type.getName()
            )
        );
    }

    public static RuntimeException methodWithMoreThanOneMatch(final String methodName, final Class type) {
        return new RuntimeException(
            format(
                "More than one method '%s' on type '%s' is compatible with the supplied arguments.",
                methodName,
                type.getName()
            )
        );
    }
}
