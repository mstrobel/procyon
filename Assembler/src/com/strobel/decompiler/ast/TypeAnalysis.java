/*
 * TypeAnalysis.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.ast;

import com.strobel.assembler.metadata.*;
import com.strobel.core.Comparer;
import com.strobel.core.Predicate;
import com.strobel.core.StringUtilities;
import com.strobel.core.StrongBox;
import com.strobel.core.delegates.Func;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.reflection.SimpleType;
import com.strobel.util.ContractUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.strobel.decompiler.ast.PatternMatching.matchGetOperand;

public final class TypeAnalysis {
    private final List<ExpressionToInfer> _allExpressions = new ArrayList<>();
    private final Set<Variable> _singleLoadVariables = new LinkedHashSet<>();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<Variable, List<ExpressionToInfer>> assignmentExpressions = new DefaultMap<>(
        new Func<List<ExpressionToInfer>>() {
            @Override
            public List<ExpressionToInfer> invoke() {
                return new ArrayList<>();
            }
        }
    );

    private DecompilerContext _context;
    private MetadataSystem _metadataSystem;

    public static void run(final DecompilerContext context, final Block method) {
        final TypeAnalysis ta = new TypeAnalysis();

        ta._context = context;
        ta._metadataSystem = MetadataSystem.instance();
        ta.createDependencyGraph(method);
        ta.identifySingleLoadVariables();
        ta.runInference();
    }

    public static void reset(final Block method) {
        for (final Expression e : method.getSelfAndChildrenRecursive(Expression.class)) {
            e.setInferredType(null);
            e.setExpectedType(null);

            final Object operand = e.getOperand();

            if (operand instanceof Variable) {
                final Variable variable = (Variable) operand;

                if (variable.isGenerated()) {
//                    variable.setType(null);
                }
            }
        }
    }

    private void createDependencyGraph(final Node node) {
        if (node instanceof Condition) {
            ((Condition) node).getCondition().setExpectedType(BuiltinTypes.Boolean);
        }
        else if (node instanceof Loop &&
                 ((Loop) node).getCondition() != null) {

            ((Loop) node).getCondition().setExpectedType(BuiltinTypes.Boolean);
        }
        else if (node instanceof CatchBlock) {
            final CatchBlock catchBlock = (CatchBlock) node;

            if (catchBlock.getExceptionVariable() != null &&
                catchBlock.getExceptionType() != null &&
                catchBlock.getExceptionVariable().getType() == null) {

                catchBlock.getExceptionVariable().setType(catchBlock.getExceptionType());
            }
        }
        else if (node instanceof Expression) {
            final Expression expression = (Expression) node;
            final ExpressionToInfer expressionToInfer = new ExpressionToInfer();

            expressionToInfer.expression = expression;

            _allExpressions.add(expressionToInfer);

            findNestedAssignments(expression, expressionToInfer);

            if (expression.getCode() == AstCode.Store &&
                ((Variable) expression.getOperand()).getType() == null) {

                assignmentExpressions.get(expression.getOperand()).add(expressionToInfer);
            }

            return;
        }

        for (final Node child : node.getChildren()) {
            createDependencyGraph(child);
        }
    }

    private void findNestedAssignments(final Expression expression, final ExpressionToInfer parent) {
        for (final Expression argument : expression.getArguments()) {
            if (argument.getCode() == AstCode.Store) {
                final ExpressionToInfer expressionToInfer = new ExpressionToInfer();

                expressionToInfer.expression = argument;

                _allExpressions.add(expressionToInfer);

                findNestedAssignments(argument, expressionToInfer);

                final Variable variable = (Variable) argument.getOperand();

                if (variable.getType() == null) {
                    assignmentExpressions.get(variable).add(expressionToInfer);

                    //
                    // The instruction that consumes the Store result is handled as if it was reading the variable.
                    //
                    parent.dependencies.add(variable);
                }
            }
            else {
                final StrongBox<Variable> variable = new StrongBox<>();

                if (matchGetOperand(argument, AstCode.Load, variable) &&
                    variable.get().getType() == null) {

                    parent.dependencies.add(variable.get());
                }

                findNestedAssignments(argument, parent);
            }
        }
    }

    private void identifySingleLoadVariables() {
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        final Map<Variable, List<ExpressionToInfer>> groupedExpressions = new DefaultMap<>(
            new Func<List<ExpressionToInfer>>() {
                @Override
                public List<ExpressionToInfer> invoke() {
                    return new ArrayList<>();
                }
            }
        );

        for (final ExpressionToInfer expressionToInfer : _allExpressions) {
            for (final Variable variable : expressionToInfer.dependencies) {
                groupedExpressions.get(variable).add(expressionToInfer);
            }
        }

        for (final Variable variable : groupedExpressions.keySet()) {
            final List<ExpressionToInfer> expressions = groupedExpressions.get(variable);

            if (expressions.size() == 1) {
                int references = 0;

                for (final Expression expression : expressions.get(0).expression.getSelfAndChildrenRecursive(Expression.class)) {
                    if (expression.getOperand() == variable &&
                        ++references > 1) {

                        break;
                    }
                }

                if (references == 1) {
                    _singleLoadVariables.add(variable);

                    //
                    // Mark the assignments as dependent on the type from the single load:
                    //
                    for (final ExpressionToInfer assignment : assignmentExpressions.get(variable)) {
                        assignment.dependsOnSingleLoad = variable;
                    }
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void runInference() {
        int numberOfExpressionsAlreadyInferred = 0;

        //
        // Two flags that allow resolving cycles:
        //

        boolean ignoreSingleLoadDependencies = false;
        boolean assignVariableTypesBasedOnPartialInformation = false;

        while (numberOfExpressionsAlreadyInferred < _allExpressions.size()) {
            final int oldCount = numberOfExpressionsAlreadyInferred;

            for (final ExpressionToInfer e : _allExpressions) {
                if (!e.done &&
                    trueForAll(
                        e.dependencies, new Predicate<Variable>() {
                        @Override
                        public boolean test(final Variable v) {
                            return v.getType() != null || _singleLoadVariables.contains(v);
                        }
                    }
                    ) &&
                    (e.dependsOnSingleLoad == null || e.dependsOnSingleLoad.getType() != null || ignoreSingleLoadDependencies)) {

                    runInference(e.expression);
                    e.done = true;
                    numberOfExpressionsAlreadyInferred++;
                }
            }

            if (numberOfExpressionsAlreadyInferred == oldCount) {
                if (ignoreSingleLoadDependencies) {
                    if (assignVariableTypesBasedOnPartialInformation) {
                        throw new IllegalStateException("Could not infer any expression.");
                    }

                    assignVariableTypesBasedOnPartialInformation = true;
                }
                else {
                    //
                    // We have a cyclic dependency; we'll try to see if we can resolve it by ignoring single-load
                    // dependencies  This can happen if the variable was not actually assigned an expected type by
                    // the single-load instruction.
                    //
                    ignoreSingleLoadDependencies = true;
                    continue;
                }
            }
            else {
                assignVariableTypesBasedOnPartialInformation = false;
                ignoreSingleLoadDependencies = false;
            }

            //
            // Infer types for variables.
            //
            for (final Variable variable : assignmentExpressions.keySet()) {
                final List<ExpressionToInfer> expressionsToInfer = assignmentExpressions.get(variable);

                if (variable.getType() == null &&
                    (assignVariableTypesBasedOnPartialInformation ? anyDone(expressionsToInfer) : allDone(expressionsToInfer))) {

                    TypeReference inferredType = null;

                    for (final ExpressionToInfer e : expressionsToInfer) {
                        final List<Expression> arguments = e.expression.getArguments();

                        assert e.expression.getCode() == AstCode.Store &&
                               arguments.size() == 1;

                        final Expression assignedValue = arguments.get(0);

                        if (assignedValue.getInferredType() != null) {
                            if (inferredType == null) {
                                inferredType = assignedValue.getInferredType();
                            }
                            else {
                                //
                                // Pick the common base type.
                                //
                                inferredType = typeWithMoreInformation(inferredType, assignedValue.getInferredType());
                            }
                        }
                    }

                    if (inferredType == null) {
                        inferredType = BuiltinTypes.Object;
                    }
                    else if (inferredType.isWildcardType()) {
                        inferredType = inferredType.hasSuperBound() ? inferredType.getSuperBound()
                                                                    : inferredType.getExtendsBound();
                    }

                    if (shouldInferVariableType(variable)) {
                        variable.setType(inferredType);
                    }

                    //
                    // Assign inferred types to all the assignments (in case they used different inferred types).
                    //
                    for (final ExpressionToInfer e : expressionsToInfer) {
                        e.expression.setInferredType(inferredType);

                        //
                        // Re-infer if the expected type has changed.
                        //
                        inferTypeForExpression(e.expression.getArguments().get(0), inferredType);
                    }
                }
            }
        }
    }

    private boolean shouldInferVariableType(final Variable variable) {
        if (variable.isGenerated()) {
            return true;
        }

        return !variable.isParameter() &&
               !variable.getOriginalVariable().isFromMetadata();
    }

    private void runInference(final Expression expression) {
        final List<Expression> arguments = expression.getArguments();

        boolean anyArgumentIsMissingExpectedType = false;

        for (final Expression argument : arguments) {
            if (argument.getExpectedType() == null) {
                anyArgumentIsMissingExpectedType = true;
                break;
            }
        }

        if (expression.getInferredType() == null || anyArgumentIsMissingExpectedType) {
            inferTypeForExpression(expression, expression.getExpectedType(), anyArgumentIsMissingExpectedType);
        }
        else if (expression.getInferredType() == BuiltinTypes.Integer &&
                 expression.getExpectedType() == BuiltinTypes.Boolean) {

            if (expression.getCode() == AstCode.Load || expression.getCode() == AstCode.Store) {
                final Variable variable = (Variable) expression.getOperand();

                expression.setInferredType(BuiltinTypes.Boolean);

                if (variable.getType().getSimpleType() == SimpleType.Integer &&
                    shouldInferVariableType(variable)) {

                    variable.setType(BuiltinTypes.Boolean);
                }
            }
        }
        else if (expression.getInferredType() == BuiltinTypes.Integer &&
                 expression.getExpectedType() == BuiltinTypes.Character) {

            if (expression.getCode() == AstCode.Load || expression.getCode() == AstCode.Store) {
                final Variable variable = (Variable) expression.getOperand();

                expression.setInferredType(BuiltinTypes.Character);

                if (variable.getType().getSimpleType() == SimpleType.Integer &&
                    shouldInferVariableType(variable) &&
                    _singleLoadVariables.contains(variable)) {

                    variable.setType(BuiltinTypes.Character);
                }
            }
        }

        for (final Expression argument : arguments) {
            if (argument.getCode() != AstCode.Store) {
                runInference(argument);
            }
        }
    }

    private TypeReference inferTypeForExpression(final Expression expression, final TypeReference expectedType) {
        return inferTypeForExpression(expression, expectedType, false);
    }

    private TypeReference inferTypeForExpression(final Expression expression, final TypeReference expectedType, final boolean forceInferChildren) {
        boolean actualForceInferChildren = forceInferChildren;

        if (expectedType != null &&
            !isSameType(expression.getExpectedType(), expectedType)) {

            expression.setExpectedType(expectedType);

            //
            // Store is a special case and never gets reevaluated.
            //
            if (expression.getCode() != AstCode.Store) {
                actualForceInferChildren = true;
            }
        }

        if (actualForceInferChildren || expression.getInferredType() == null) {
            expression.setInferredType(doInferTypeForExpression(expression, expectedType, actualForceInferChildren));
        }

        return expression.getInferredType();
    }

    private TypeReference doInferTypeForExpression(final Expression expression, final TypeReference expectedType, final boolean forceInferChildren) {
        final AstCode code = expression.getCode();
        final Object operand = expression.getOperand();
        final List<Expression> arguments = expression.getArguments();

        switch (code) {
            case LogicalNot: {
                if (forceInferChildren) {
                    inferTypeForExpression(arguments.get(0), BuiltinTypes.Boolean);
                }

                return BuiltinTypes.Boolean;
            }

            case LogicalAnd:
            case LogicalOr: {
                if (forceInferChildren) {
                    inferTypeForExpression(arguments.get(0), BuiltinTypes.Boolean);
                    inferTypeForExpression(arguments.get(1), BuiltinTypes.Boolean);
                }

                return BuiltinTypes.Boolean;
            }

            case TernaryOp: {
                if (forceInferChildren) {
                    inferTypeForExpression(arguments.get(0), BuiltinTypes.Boolean);
                }

                return inferBinaryArguments(
                    arguments.get(1),
                    arguments.get(2),
                    expectedType,
                    forceInferChildren,
                    null,
                    null
                );
            }

            case MonitorEnter:
            case MonitorExit:
                return null;

            case Store: {
                final Variable v = (Variable) operand;

                if (forceInferChildren) {
                    inferTypeForExpression(arguments.get(0), v.getType());
                }

                return v.getType();
            }

            case Load: {
                final Variable v = (Variable) operand;

                if (v.getType() == null && _singleLoadVariables.contains(v)) {
                    TypeReference inferredType = expectedType;

                    if (inferredType != null && inferredType.isWildcardType()) {
                        inferredType = inferredType.hasSuperBound() ? inferredType.getSuperBound()
                                                                    : inferredType.getExtendsBound();
                    }

                    if (shouldInferVariableType(v)) {
                        v.setType(inferredType);
                    }
                }

                return v.getType();
            }

            case InvokeVirtual:
            case InvokeSpecial:
            case InvokeStatic:
            case InvokeInterface: {
                final MethodReference methodReference = (MethodReference) operand;
                final MethodDefinition methodDefinition = methodReference.resolve();
                final List<ParameterDefinition> parameters = methodReference.getParameters();

                final boolean hasThis;

                if (methodDefinition != null) {
                    hasThis = !methodDefinition.isStatic();
                }
                else {
                    hasThis = code != AstCode.InvokeStatic && code != AstCode.InvokeDynamic;
                }

                if (forceInferChildren) {
                    if (hasThis) {
                        final TypeReference targetType;

                        inferTypeForExpression(
                            arguments.get(0),
                            methodReference.getDeclaringType()
                        );

                        targetType = arguments.get(0).getInferredType();

                        for (int i = 0; i < parameters.size(); i++) {
                            inferTypeForExpression(
                                arguments.get(i + 1),
                                substituteTypeArguments(
                                    substituteTypeArguments(parameters.get(i).getParameterType(), methodReference),
                                    targetType
                                )
                            );
                        }
                    }
                    else {
                        for (int i = 0; i < parameters.size(); i++) {
                            inferTypeForExpression(
                                arguments.get(i),
                                substituteTypeArguments(parameters.get(i).getParameterType(), methodReference)
                            );
                        }
                    }
                }

                if (methodReference.isConstructor() && hasThis) {
                    return methodReference.getDeclaringType();
                }

                if (hasThis) {
                    inferTypeForExpression(
                        arguments.get(0),
                        methodReference.getDeclaringType()
                    );

                    return substituteTypeArguments(
                        substituteTypeArguments(
                            methodReference.getReturnType(),
                            methodReference
                        ),
                        arguments.get(0).getInferredType()
                    );
                }

                return substituteTypeArguments(
                    methodReference.getReturnType(),
                    methodReference
                );
            }

            case InvokeDynamic: {
                final DynamicCallSite callSite = (DynamicCallSite) operand;
                return callSite.getMethodType().getReturnType();
            }

            case GetField: {
                if (forceInferChildren) {
                    inferTypeForExpression(
                        arguments.get(0),
                        ((FieldReference) operand).getDeclaringType()
                    );
                }

                return getFieldType((FieldReference) operand);
            }

            case GetStatic: {
                return getFieldType((FieldReference) operand);
            }

            case PutField: {
                if (forceInferChildren) {
                    inferTypeForExpression(
                        arguments.get(0),
                        ((FieldReference) operand).getDeclaringType()
                    );

                    inferTypeForExpression(
                        arguments.get(1),
                        getFieldType((FieldReference) operand)
                    );
                }

                return null; //getFieldType((FieldReference) operand);
            }

            case PutStatic: {
                if (forceInferChildren) {
                    inferTypeForExpression(
                        arguments.get(0),
                        getFieldType((FieldReference) operand)
                    );
                }

                return null; //getFieldType((FieldReference) operand);
            }

            case __New: {
                return (TypeReference) operand;
            }

            case PostIncrement: {
                inferTypeForExpression(arguments.get(0), null);
                return null;
            }

            case Not:
            case Neg: {
                return inferTypeForExpression(arguments.get(0), expectedType);
            }

            case Add:
            case Sub:
            case Mul:
            case Or:
            case And:
            case Xor:
            case Div:
            case Rem: {
                return inferBinaryArguments(arguments.get(0), arguments.get(1), expectedType, false, null, null);
            }

            case Shl: {
                if (forceInferChildren) {
                    inferTypeForExpression(arguments.get(1), BuiltinTypes.Integer);
                }

                if (expectedType != null &&
                    (expectedType.getSimpleType() == SimpleType.Integer ||
                     expectedType.getSimpleType() == SimpleType.Long)) {

                    return numericPromotion(inferTypeForExpression(arguments.get(0), expectedType));
                }

                return numericPromotion(inferTypeForExpression(arguments.get(0), null));
            }

            case Shr:
            case UShr: {
                if (forceInferChildren) {
                    inferTypeForExpression(arguments.get(1), BuiltinTypes.Integer);
                }

                final TypeReference type = numericPromotion(inferTypeForExpression(arguments.get(0), null));

                TypeReference expectedInputType = null;

                switch (type.getSimpleType()) {
                    case Integer:
                        expectedInputType = BuiltinTypes.Integer;
                        break;
                    case Long:
                        expectedInputType = BuiltinTypes.Long;
                        break;
                }

                if (expectedInputType != null) {
                    inferTypeForExpression(arguments.get(0), expectedInputType);
                    return expectedInputType;
                }

                return type;
            }

            case CompoundAssignment: {
                final Expression op = arguments.get(0);
                final TypeReference targetType = inferTypeForExpression(op.getArguments().get(0), null);

                if (forceInferChildren) {
                    inferTypeForExpression(arguments.get(0), targetType);
                }

                return targetType;
            }

            case AConstNull: {
                return BuiltinTypes.Object;
            }

            case LdC: {
                if (operand instanceof Number) {
                    final Number number = (Number) operand;

                    if (number instanceof Integer) {
                        if (expectedType != null) {
                            if (expectedType.getSimpleType() == SimpleType.Boolean &&
                                (number.intValue() == 0 || number.intValue() == 1)) {

                                return BuiltinTypes.Boolean;
                            }

                            if (expectedType.getSimpleType() == SimpleType.Character &&
                                number.intValue() >= Character.MIN_VALUE &&
                                number.intValue() <= Character.MAX_VALUE) {

                                return BuiltinTypes.Character;
                            }
                        }

                        return BuiltinTypes.Integer;
                    }

                    if (number instanceof Long) {
                        return BuiltinTypes.Long;
                    }

                    if (number instanceof Float) {
                        return BuiltinTypes.Float;
                    }

                    return BuiltinTypes.Double;
                }

                if (operand instanceof TypeReference) {
                    return _metadataSystem.lookupType("java/lang/Class");
                }

                return _metadataSystem.lookupType("java/lang/String");
            }

            case NewArray:
            case __NewArray:
            case __ANewArray: {
                if (forceInferChildren) {
                    inferTypeForExpression(arguments.get(0), BuiltinTypes.Integer);
                }
                return ((TypeReference) operand).makeArrayType();
            }

            case MultiANewArray: {
                if (forceInferChildren) {
                    for (int i = 0; i < arguments.size(); i++) {
                        inferTypeForExpression(arguments.get(i), BuiltinTypes.Integer);
                    }
                }
                return (TypeReference) operand;
            }

            case InitObject: {
                final MethodReference constructor = (MethodReference) operand;

                if (forceInferChildren) {
                    final List<ParameterDefinition> parameters = constructor.getParameters();

                    for (int i = 0; i < arguments.size() && i < parameters.size(); i++) {
                        inferTypeForExpression(arguments.get(i), parameters.get(i).getParameterType());
                    }
                }

                return constructor.getDeclaringType();
            }

            case InitArray: {
                final TypeReference arrayType = (TypeReference) operand;
                final TypeReference elementType = arrayType.getElementType();

                if (forceInferChildren) {
                    for (final Expression argument : arguments) {
                        inferTypeForExpression(argument, elementType);
                    }
                }

                return arrayType;
            }

            case ArrayLength: {
                return BuiltinTypes.Integer;
            }

            case LoadElement: {
                final TypeReference arrayType = inferTypeForExpression(arguments.get(0), null);

                if (forceInferChildren) {
                    inferTypeForExpression(arguments.get(1), BuiltinTypes.Integer);
                }

                return arrayType != null && arrayType.isArray() ? arrayType.getElementType() : arrayType;
            }

            case StoreElement: {
                final TypeReference arrayType = inferTypeForExpression(arguments.get(0), null);

                if (forceInferChildren) {
                    inferTypeForExpression(arguments.get(1), BuiltinTypes.Integer);

                    if (arrayType != null && arrayType.isArray()) {
                        inferTypeForExpression(arguments.get(2), arrayType.getElementType());
                    }
                }

                return arrayType != null && arrayType.isArray() ? arrayType.getElementType() : arrayType;
            }

            case __BIPush:
            case __SIPush: {
                final Number number = (Number) operand;

                if (expectedType != null) {
                    if (expectedType.getSimpleType() == SimpleType.Boolean &&
                        (number.intValue() == 0 || number.intValue() == 1)) {

                        return BuiltinTypes.Boolean;
                    }

                    if (expectedType.getSimpleType() == SimpleType.Byte &&
                        number.intValue() >= Byte.MIN_VALUE &&
                        number.intValue() <= Byte.MAX_VALUE) {

                        return BuiltinTypes.Byte;
                    }

                    if (expectedType.getSimpleType() == SimpleType.Character &&
                        number.intValue() >= Character.MIN_VALUE &&
                        number.intValue() <= Character.MAX_VALUE) {

                        return BuiltinTypes.Character;
                    }

                    if (expectedType.getSimpleType().isIntegral()) {
                        return expectedType;
                    }
                }
                else if (code == AstCode.__BIPush) {
                    return BuiltinTypes.Byte;
                }

                return BuiltinTypes.Short;
            }

            case I2L:
            case I2F:
            case I2D:
            case L2I:
            case L2F:
            case L2D:
            case F2I:
            case F2L:
            case F2D:
            case D2I:
            case D2L:
            case D2F:
            case I2B:
            case I2C:
            case I2S: {
                final TypeReference conversionResult;

                switch (code) {
                    case I2L:
                        conversionResult = BuiltinTypes.Long;
                        break;
                    case I2F:
                        conversionResult = BuiltinTypes.Float;
                        break;
                    case I2D:
                        conversionResult = BuiltinTypes.Double;
                        break;
                    case L2I:
                        conversionResult = BuiltinTypes.Integer;
                        break;
                    case L2F:
                        conversionResult = BuiltinTypes.Float;
                        break;
                    case L2D:
                        conversionResult = BuiltinTypes.Double;
                        break;
                    case F2I:
                        conversionResult = BuiltinTypes.Integer;
                        break;
                    case F2L:
                        conversionResult = BuiltinTypes.Long;
                        break;
                    case F2D:
                        conversionResult = BuiltinTypes.Double;
                        break;
                    case D2I:
                        conversionResult = BuiltinTypes.Integer;
                        break;
                    case D2L:
                        conversionResult = BuiltinTypes.Long;
                        break;
                    case D2F:
                        conversionResult = BuiltinTypes.Float;
                        break;
                    case I2B:
                        conversionResult = BuiltinTypes.Byte;
                        break;
                    case I2C:
                        conversionResult = BuiltinTypes.Character;
                        break;
                    case I2S:
                        conversionResult = BuiltinTypes.Short;
                        break;
                    default:
                        throw ContractUtils.unsupported();
                }

                arguments.get(0).setExpectedType(conversionResult);
                return conversionResult;
            }

            case CheckCast:
            case Unbox: {
                return (TypeReference) operand;
            }

            case Box: {
                final TypeReference type = (TypeReference) operand;

                if (forceInferChildren) {
                    inferTypeForExpression(arguments.get(0), type);
                }

                return type.isPrimitive() ? BuiltinTypes.Object : type;
            }

            case CmpEq:
            case CmpNe:
            case CmpLt:
            case CmpGe:
            case CmpGt:
            case CmpLe: {
                if (forceInferChildren) {
                    final List<Expression> binaryArguments;

                    if (arguments.size() == 1) {
                        binaryArguments = arguments.get(0).getArguments();
                    }
                    else {
                        binaryArguments = arguments;
                    }

                    runInference(binaryArguments.get(0));
                    runInference(binaryArguments.get(1));

                    binaryArguments.get(0).setExpectedType(binaryArguments.get(0).getInferredType());
                    binaryArguments.get(1).setExpectedType(binaryArguments.get(0).getInferredType());
                    binaryArguments.get(0).setInferredType(null);
                    binaryArguments.get(1).setInferredType(null);

                    inferBinaryArguments(
                        binaryArguments.get(0),
                        binaryArguments.get(1),
                        typeWithMoreInformation(
                            binaryArguments.get(0).getExpectedType(),
                            binaryArguments.get(1).getExpectedType()
                        ),
                        false,
                        null,
                        null
                    );
                }

                return BuiltinTypes.Boolean;
            }

            case __DCmpG:
            case __DCmpL:
            case __FCmpG:
            case __FCmpL:
            case __LCmp: {
                if (forceInferChildren) {
                    final List<Expression> binaryArguments;

                    if (arguments.size() == 1) {
                        binaryArguments = arguments.get(0).getArguments();
                    }
                    else {
                        binaryArguments = arguments;
                    }

                    inferBinaryArguments(
                        binaryArguments.get(0),
                        binaryArguments.get(1),
                        expectedType,
                        false,
                        null,
                        null
                    );
                }

                return BuiltinTypes.Integer;
            }

            case IfTrue: {
                if (forceInferChildren) {
                    inferTypeForExpression(arguments.get(0), BuiltinTypes.Boolean);
                }
                return null;
            }

            case Goto:
            case TableSwitch:
            case LookupSwitch:
            case AThrow:
            case LoopOrSwitchBreak:
            case LoopContinue:
            case __Return: {
                return null;
            }

            case __IReturn:
            case __LReturn:
            case __FReturn:
            case __DReturn:
            case __AReturn:
            case Return: {
                if (forceInferChildren && arguments.size() == 1) {
                    final TypeReference returnType = _context.getCurrentMethod().getReturnType();
                    inferTypeForExpression(arguments.get(0), returnType);
                }
                return null;
            }

            case Pop:
            case Pop2: {
                return null;
            }

            case Dup:
            case Dup2: {
                //
                // TODO: Handle the more obscure DUP instructions.
                //

                final Expression argument = arguments.get(0);
                final TypeReference result = inferTypeForExpression(argument, expectedType);

                argument.setExpectedType(result);

                return result;
            }

            case InstanceOf: {
                return BuiltinTypes.Boolean;
            }

            case __IInc:
            case __IIncW:
            case Inc: {
                return null;
            }

            case Leave:
            case Nop: {
                return null;
            }

            default: {
                System.err.printf("Type inference can't handle opcode '%s'.\n", code.getName());
                return null;
            }
        }
    }

    private TypeReference numericPromotion(final TypeReference type) {
        if (type == null) {
            return null;
        }

        switch (type.getSimpleType()) {
            case Byte:
            case Short:
                return BuiltinTypes.Integer;

            default:
                return type;
        }
    }

    private TypeReference inferBinaryArguments(
        final Expression left,
        final Expression right,
        final TypeReference expectedType,
        final boolean forceInferChildren,
        final TypeReference leftPreferred,
        final TypeReference rightPreferred) {

        TypeReference actualLeftPreferred = leftPreferred;
        TypeReference actualRightPreferred = rightPreferred;

        if (actualLeftPreferred == null) {
            actualLeftPreferred = doInferTypeForExpression(left, expectedType, forceInferChildren);
        }

        if (actualRightPreferred == null) {
            actualRightPreferred = doInferTypeForExpression(right, expectedType, forceInferChildren);
        }

        if (isSameType(actualLeftPreferred, actualRightPreferred)) {
            left.setInferredType(actualLeftPreferred);
            left.setExpectedType(actualLeftPreferred);
            right.setInferredType(actualLeftPreferred);
            right.setExpectedType(actualLeftPreferred);
            return actualLeftPreferred;
        }

        if (isSameType(actualRightPreferred, doInferTypeForExpression(left, actualRightPreferred, forceInferChildren))) {
            left.setInferredType(actualRightPreferred);
            left.setExpectedType(actualRightPreferred);
            right.setInferredType(actualRightPreferred);
            right.setExpectedType(actualRightPreferred);
            return actualRightPreferred;
        }

        if (isSameType(actualLeftPreferred, doInferTypeForExpression(right, actualLeftPreferred, forceInferChildren))) {
            left.setInferredType(actualLeftPreferred);
            left.setExpectedType(actualLeftPreferred);
            right.setInferredType(actualLeftPreferred);
            right.setExpectedType(actualLeftPreferred);
            return actualLeftPreferred;
        }

        final TypeReference result = typeWithMoreInformation(actualLeftPreferred, actualRightPreferred);

        left.setExpectedType(result);
        right.setExpectedType(result);
        left.setInferredType(doInferTypeForExpression(left, result, forceInferChildren));
        right.setInferredType(doInferTypeForExpression(right, result, forceInferChildren));

        return result;
    }

    private TypeReference typeWithMoreInformation(final TypeReference leftPreferred, final TypeReference rightPreferred) {
        final int left = getInformationAmount(leftPreferred);
        final int right = getInformationAmount(rightPreferred);

        if (left < right) {
            return rightPreferred;
        }

        if (left > right) {
            return leftPreferred;
        }

        //
        // TODO: Calculate this case;
        //
        return leftPreferred;
    }

    private static int getInformationAmount(final TypeReference type) {
        if (type == null) {
            return 0;
        }

        switch (type.getSimpleType()) {
            case Boolean:
                return 1;

            case Byte:
                return 8;

            case Character:
            case Short:
                return 16;

            case Integer:
            case Float:
                return 32;

            case Long:
            case Double:
                return 64;

            default:
                return 100;
        }
    }

    static TypeReference getFieldType(final FieldReference field) {
        return substituteTypeArguments(field.getFieldType(), field);
    }

    static TypeReference substituteTypeArguments(final TypeReference type, final MemberReference member) {
        if (type instanceof ArrayType) {
            final ArrayType arrayType = (ArrayType) type;

            final TypeReference elementType = substituteTypeArguments(
                arrayType.getElementType(),
                member
            );

            if (!MetadataResolver.areEquivalent(elementType, arrayType.getElementType())) {
                return elementType.makeArrayType();
            }

            return type;
        }

        if (type instanceof IGenericInstance) {
            final IGenericInstance genericInstance = (IGenericInstance) type;
            final List<TypeReference> newTypeArguments = new ArrayList<>();

            boolean isChanged = false;

            for (final TypeReference typeArgument : genericInstance.getTypeArguments()) {
                final TypeReference newTypeArgument = substituteTypeArguments(typeArgument, member);

                newTypeArguments.add(newTypeArgument);
                isChanged |= newTypeArgument != typeArgument;
            }

            return isChanged ? type.makeGenericType(newTypeArguments)
                             : type;
        }

        if (type instanceof GenericParameter) {
            final GenericParameter genericParameter = (GenericParameter) type;
            final IGenericParameterProvider owner = genericParameter.getOwner();

            if (member.getDeclaringType() instanceof ArrayType) {
                return member.getDeclaringType().getElementType();
            }
            else if (owner instanceof MethodReference && member instanceof MethodReference) {
                final MethodReference method = (MethodReference) member;
                final MethodReference ownerMethod = (MethodReference) owner;

                if (method.isGenericMethod() &&
                    MetadataResolver.areEquivalent(ownerMethod.getDeclaringType(), method.getDeclaringType()) &&
                    StringUtilities.equals(ownerMethod.getName(), method.getName()) &&
                    StringUtilities.equals(ownerMethod.getErasedSignature(), method.getErasedSignature())) {

                    if (method instanceof IGenericInstance) {
                        final List<TypeReference> typeArguments = ((IGenericInstance) member).getTypeArguments();
                        return typeArguments.get(genericParameter.getPosition());
                    }
                    else {
                        return method.getGenericParameters().get(genericParameter.getPosition());
                    }
                }
            }
            else if (owner instanceof TypeReference) {
                TypeReference declaringType;

                if (member instanceof TypeReference) {
                    declaringType = (TypeReference) member;
                }
                else {
                    declaringType = member.getDeclaringType();
                }

                if (MetadataResolver.areEquivalent((TypeReference) owner, declaringType)) {
                    if (declaringType instanceof IGenericInstance) {
                        final List<TypeReference> typeArguments = ((IGenericInstance) declaringType).getTypeArguments();
                        return typeArguments.get(genericParameter.getPosition());
                    }

                    if (!declaringType.isGenericDefinition()) {
                        declaringType = declaringType.resolve();
                    }

                    if (declaringType != null && declaringType.isGenericDefinition()) {
                        return declaringType.getGenericParameters().get(genericParameter.getPosition());
                    }
                }
            }
        }

        return type;
    }


/*
    static TypeReference substituteTypeArguments(final TypeReference type, final MemberReference member, final TypeReference targetType) {
        if (type instanceof ArrayType) {
            final ArrayType arrayType = (ArrayType) type;
            final TypeReference elementType = substituteTypeArguments(arrayType.getElementType(), member, targetType);

            if (elementType != arrayType.getElementType()) {
                return elementType.makeArrayType();
            }

            return type;
        }

        if (type instanceof IGenericInstance) {
            final IGenericInstance genericInstance = (IGenericInstance) type;
            final List<TypeReference> newTypeArguments = new ArrayList<>();

            boolean isChanged = false;

            for (final TypeReference typeArgument : genericInstance.getTypeArguments()) {
                final TypeReference newTypeArgument = substituteTypeArguments(typeArgument, member, targetType);

                newTypeArguments.add(newTypeArgument);
                isChanged |= newTypeArgument != typeArgument;
            }

            return isChanged ? type.resolve().makeGenericType(newTypeArguments)
                             : type;
        }

        if (type instanceof GenericParameter) {
            final GenericParameter genericParameter = (GenericParameter) type;
            final IGenericParameterProvider owner = genericParameter.getOwner();

            if (owner == member && member instanceof IGenericInstance) {
                final List<TypeReference> typeArguments = ((IGenericInstance) member).getTypeArguments();
                return typeArguments.get(genericParameter.getPosition());
            }
            else if (targetType != null && owner == targetType.resolve() && targetType instanceof IGenericInstance) {
                final List<TypeReference> typeArguments = ((IGenericInstance) targetType).getTypeArguments();
                return typeArguments.get(genericParameter.getPosition());
            }
//            else {
//                return genericParameter.getExtendsBound();
//            }
        }

        return type;
    }
*/

    private boolean isSameType(final TypeReference t1, final TypeReference t2) {
        //noinspection SimplifiableIfStatement
        if (t1 == t2) {
            return true;
        }

        return t1 != null &&
               t2 != null &&
               Comparer.equals(t1.getFullName(), t2.getFullName());
    }

    private boolean anyDone(final List<ExpressionToInfer> expressions) {
        for (final ExpressionToInfer expression : expressions) {
            if (expression.done) {
                return true;
            }
        }
        return false;
    }

    private boolean allDone(final List<ExpressionToInfer> expressions) {
        for (final ExpressionToInfer expression : expressions) {
            if (!expression.done) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean trueForAll(final Iterable<T> sequence, final Predicate<T> condition) {
        for (final T item : sequence) {
            if (!condition.test(item)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isBoolean(final TypeReference type) {
        return type != null && type.getSimpleType() == SimpleType.Boolean;
    }

    // <editor-fold defaultstate="collapsed" desc="ExpressionToInfer Class">

    final static class ExpressionToInfer {
        private final List<Variable> dependencies = new ArrayList<>();

        Expression expression;
        boolean done;
        Variable dependsOnSingleLoad;

        @Override
        public String toString() {
            if (done) {
                return "[Done] " + expression;
            }
            return expression.toString();
        }
    }

    // </editor-fold>
}
