package com.strobel.expressions;

import com.strobel.reflection.MemberInfo;
import com.strobel.reflection.MethodBase;
import com.strobel.reflection.Type;

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

    public static RuntimeException memberNotField(final MemberInfo member) {
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

    public static RuntimeException mustRewriteChildToSameType(final Type before, final Type after, final String callerName) {
        return new RuntimeException(
            format("MethodBase '%s' performed an illegal type change from %s to %s.", callerName, before, after)
        );
    }

    public static RuntimeException mustRewriteWithoutMethod(final MethodBase method, final String callerName) {
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

    public static RuntimeException unaryOperatorNotDefined(final ExpressionType operator, final Type operandType) {
        return new RuntimeException(
            format(
                "The unary operator '%s' is not defined for type '%s'",
                operator,
                operandType
            )
        );
    }

    public static RuntimeException operatorMethodMustNotBeStatic(final MethodBase method) {
        return new RuntimeException(
            format(
                "MethodBase '%s.%s' cannot be used as an operator because it is static.",
                method.getDeclaringType().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException operatorMethodMustNotReturnVoid(final MethodBase method) {
        return new RuntimeException(
            format(
                "MethodBase '%s.%s' cannot be used as an operator because it returns void.",
                method.getDeclaringType().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException operatorMethodParametersMustMatchReturnValue(final MethodBase method) {
        return new RuntimeException(
            format(
                "MethodBase '%s.%s' cannot be used as an operator because its parameters do not match " +
                "its return value.",
                method.getDeclaringType().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException returnTypeDoesNotMatchOperandType(final ExpressionType expressionType, final MethodBase method) {
        return new RuntimeException(
            format(
                "The return type for operator '%s' does not match the declaring type of method '%s.%s'.",
                expressionType,
                method.getDeclaringType().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException returnTypeDoesNotMatchOperandType(final MethodBase method) {
        return new RuntimeException(
            format(
                "The return type of operator method '%s.%s' does not match the method's declaring type.",
                method.getDeclaringType().getName(),
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

    public static RuntimeException incorrectNumberOfLambdaDeclarationParameters() {
        return new RuntimeException("Incorrect number of parameters supplied for lambda declaration.");
    }

    public static RuntimeException incorrectNumberOfMethodCallArguments(final MethodBase method) {
        return new RuntimeException(
            format(
                "Incorrect number of arguments supplied for call to method '%s.%s'",
                method.getDeclaringType().getName(),
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

    public static RuntimeException argumentMustBeBoolean() {
        return new RuntimeException("Argument must be a boolean.");
    }

    public static RuntimeException argumentMustBeInteger() {
        return new RuntimeException("Argument must be an integer.");
    }

    public static RuntimeException argumentMustBeIntegral() {
        return new RuntimeException("Argument must be an integral numeric type.");
    }

    public static RuntimeException coercionOperatorNotDefined(final Type sourceType, final Type destinationType) {
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

    public static RuntimeException methodBasedOperatorMustHaveValidReturnType(final MethodBase method) {
        return new RuntimeException(
            format(
                "The operator method '%s.%s' must return the same type as its declaring type " +
                "or a derived type.",
                method.getDeclaringType().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException methodBasedOperatorMustHaveValidReturnType(final ExpressionType operator, final MethodBase method) {
        return new RuntimeException(
            format(
                "The operator method '%s.%s' for operator '%s' must return the same type as its " +
                "declaring type or a derived type.",
                method.getDeclaringType().getName(),
                method.getName(),
                operator
            )
        );
    }

    public static RuntimeException expressionTypeNotInvokable(final Type type) {
        return new RuntimeException(
            format(
                "Expression of type '%s' cannot be invoked.  Invokable types must be interfaces " +
                "with exactly one method.",
                type
            )
        );
    }

    public static RuntimeException binaryOperatorNotDefined(final ExpressionType operator, final Type leftType, final Type rightType) {
        return new RuntimeException(
            format(
                "The binary operator '%s' is not defined for the types '%s' and '%s'.",
                operator,
                leftType,
                rightType
            )
        );
    }

    public static RuntimeException referenceEqualityNotDefined(final Type leftType, final Type rightType) {
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

    public static RuntimeException targetRequiredForNonStaticMethodCall(final MethodBase method) {
        return new RuntimeException(
            format(
                "An invocation target expression is required for a call to non-static " +
                "method '%s.%s'.",
                method.getDeclaringType().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException targetInvalidForStaticMethodCall(final MethodBase method) {
        return new RuntimeException(
            format(
                "An invocation target expression cannot be used to call static " +
                "method '%s.%s'.",
                method.getDeclaringType().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException targetAndMethodTypeMismatch(final MethodBase method, final Type targetType) {
        return new RuntimeException(
            format(
                "Expression of type '%s' is not a valid invocation target for instance " +
                "method '%s.%s'.",
                targetType.getName(),
                method.getDeclaringType().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException expressionTypeDoesNotMatchParameter(final Type argType, final Type parameterType) {
        return new RuntimeException(
            format(
                "Expression of type '%s' cannot be used for constructor parameter of type '%s'.",
                parameterType.getName(),
                argType.getName()
            )
        );
    }

    public static RuntimeException expressionTypeDoesNotMatchReturn(final Type bodyType, final Type returnType) {
        return new RuntimeException(
            format(
                "Expression of type '%s' cannot be used as the body of a lambda with return type '%s'.",
                bodyType.getName(),
                returnType.getName()
            )
        );
    }

    public static RuntimeException expressionTypeDoesNotMatchConstructorParameter(final Type argType, final Type parameterType) {
        return new RuntimeException(
            format(
                "Expression of type '%s' cannot be used for parameter of type '%s'.",
                parameterType.getName(),
                argType.getName()
            )
        );
    }

    public static RuntimeException expressionTypeDoesNotMatchMethodParameter(final Type argType, final Type parameterType, final MethodBase method) {
        return new RuntimeException(
            format(
                "Expression of type '%s' cannot be used for parameter of type '%s' of method '%s.%s'.",
                parameterType.getName(),
                argType.getName(),
                method.getDeclaringType().getName(),
                method.getName()
            )
        );
    }

    public static RuntimeException expressionTypeDoesNotMatchAssignment(final Type leftType, final Type rightType) {
        return new RuntimeException(
            format(
                "Expression of type '%s' cannot be used for assignment to type '%s'.",
                rightType.getName(),
                leftType.getName()
            )
        );
    }

    public static RuntimeException methodDoesNotExistOnType(final String methodName, final Type type) {
        return new RuntimeException(
            format(
                "No method '%s' exists on type '%s'.",
                methodName,
                type.getName()
            )
        );
    }

    public static RuntimeException genericMethodWithArgsDoesNotExistOnType(final String methodName, final Type type) {
        return new RuntimeException(
            format(
                "No generic method '%s' on type '%s' is compatible with the supplied type arguments and arguments.  " +
                "No type arguments should be provided if the method is non-generic.",
                methodName,
                type.getName()
            )
        );
    }

    public static RuntimeException methodWithArgsDoesNotExistOnType(final String methodName, final Type type) {
        return new RuntimeException(
            format(
                "No method '%s' on type '%s' is compatible with the supplied arguments.",
                methodName,
                type.getName()
            )
        );
    }

    public static RuntimeException methodWithMoreThanOneMatch(final String methodName, final Type type) {
        return new RuntimeException(
            format(
                "More than one method '%s' on type '%s' is compatible with the supplied arguments.",
                methodName,
                type.getName()
            )
        );
    }

    public static RuntimeException argumentMustBeArrayIndexType() {
        return new RuntimeException(
            "Expression must be an integer-based array index."
        );
    }

    public static RuntimeException conversionIsNotSupportedForArithmeticTypes() {
        return new RuntimeException(
            "A conversion expression is not supported for arithmetic types."
        );
    }

    public static RuntimeException operandTypesDoNotMatchParameters(final ExpressionType nodeType, final MethodBase method) {
        return new RuntimeException(
            format(
                "The operands for operator '%s' do not match the parameters of method '%s'.",
                nodeType,
                method.getName()
            )
        );
    }

    public static RuntimeException overloadOperatorTypeDoesNotMatchConversionType(final ExpressionType nodeType, final MethodBase method) {
        return new RuntimeException(
            format(
                "The return type of overload method for operator '%s' does not match the parameter " +
                "type of conversion method '%s'.",
                nodeType,
                method.getName()
            )
        );
    }

    public static RuntimeException lambdaTypeMustBeSingleMethodInterface() {
        return new RuntimeException("Lambda type parameter must be an interface type with exactly one method.");
    }

    public static RuntimeException parameterExpressionNotValidForDelegate(final Type parameterType, final Type delegateParameterType) {
        return new RuntimeException(
            format(
                "ParameterExpression of type '%s' cannot be used for delegate parameter of type '%s'.",
                parameterType.getName(),
                delegateParameterType.getName()
            )
        );
    }

    public static RuntimeException labelMustBeVoidOrHaveExpression() {
        return new RuntimeException("Label type must be void if an expression is not supplied.");
    }

    public static RuntimeException expressionTypeDoesNotMatchLabel(final Type valueType, final Type expectedType) {
        return new RuntimeException(
            format(
                "Expression of type '%s' cannot be used for return type '%s'.",
                valueType.getName(),
                expectedType.getName()
            )
        );
    }

    public static RuntimeException labelTypeMustBeVoid() {
        return new RuntimeException("Type must be void for this label argument.");
    }

    public static RuntimeException expressionTypeCannotInitializeArrayType(final Type itemType, final Type arrayElementType) {
        return new RuntimeException(
            format(
                "An expression of type '%s' cannot be used to initialize an array of type '%s'.",
                itemType.getName(),
                arrayElementType.getName()
            )
        );
    }

    public static RuntimeException catchVariableMustBeCompatibleWithCatchType(final Type catchType, final Type variableType) {
        return new RuntimeException(
            format(
                "A variable of type '%s' cannot be used with a catch block with filter type '%s'.",
                variableType.getName(),
                catchType.getName()
            )
        );
    }

    public static RuntimeException bodyOfCatchMustHaveSameTypeAsBodyOfTry() {
        return new RuntimeException("Body of catch must have the same type as body of try.");
    }

    public static RuntimeException tryMustHaveCatchOrFinally() {
        return new RuntimeException("A try expression must have at least one catch or finally clause.");
    }
}
