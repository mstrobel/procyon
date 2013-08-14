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

import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.*;
import com.strobel.core.Pair;
import com.strobel.core.Predicate;
import com.strobel.core.StringUtilities;
import com.strobel.core.StrongBox;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.functions.Supplier;
import com.strobel.util.ContractUtils;

import java.util.*;

import static com.strobel.core.CollectionUtilities.firstOrDefault;
import static com.strobel.decompiler.ast.PatternMatching.*;

public final class TypeAnalysis {
    private final List<ExpressionToInfer> _allExpressions = new ArrayList<>();
    private final Set<Variable> _singleLoadVariables = new LinkedHashSet<>();
    private final Set<Pair<Variable, TypeReference>> _previouslyInferred = new LinkedHashSet<>();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<Variable, List<ExpressionToInfer>> _assignmentExpressions = new IdentityHashMap<Variable, List<ExpressionToInfer>>() {
        @Override
        @SuppressWarnings("unchecked")
        public List<ExpressionToInfer> get(final Object key) {
            List<ExpressionToInfer> value = super.get(key);

            if (value == null) {
                if (_doneInitializing) {
                    return Collections.emptyList();
                }

                put((Variable) key, value = new ArrayList<>());
            }

            return value;
        }
    };

    private final IdentityHashMap<Variable, TypeReference> _inferredVariableTypes = new IdentityHashMap<>();

    private DecompilerContext _context;
    private CoreMetadataFactory _factory;
    private boolean _preserveMetadataTypes;
    private boolean _preserveMetadataGenericTypes;
    private Stack<Expression> _stack = new Stack<>();
    private boolean _doneInitializing;

    public static void run(final DecompilerContext context, final Block method) {
        final TypeAnalysis ta = new TypeAnalysis();

        final SourceAttribute localVariableTable = SourceAttribute.find(
            AttributeNames.LocalVariableTable,
            context.getCurrentMethod().getSourceAttributes()
        );

        final SourceAttribute localVariableTypeTable = SourceAttribute.find(
            AttributeNames.LocalVariableTypeTable,
            context.getCurrentMethod().getSourceAttributes()
        );

        ta._context = context;
        ta._factory = CoreMetadataFactory.make(context.getCurrentType(), context.getCurrentMethod());
        ta._preserveMetadataTypes = localVariableTable != null;
        ta._preserveMetadataGenericTypes = localVariableTypeTable != null;

        ta.createDependencyGraph(method);
        ta.identifySingleLoadVariables();
        ta._doneInitializing = true;
        ta.runInference();
    }

    public static void reset(final DecompilerContext context, final Block method) {
        final SourceAttribute localVariableTable = SourceAttribute.find(
            AttributeNames.LocalVariableTable,
            context.getCurrentMethod().getSourceAttributes()
        );

        final SourceAttribute localVariableTypeTable = SourceAttribute.find(
            AttributeNames.LocalVariableTypeTable,
            context.getCurrentMethod().getSourceAttributes()
        );

        final boolean preserveTypesFromMetadata = localVariableTable != null;
        final boolean preserveGenericTypesFromMetadata = localVariableTypeTable != null;

        for (final Expression e : method.getSelfAndChildrenRecursive(Expression.class)) {
            e.setInferredType(null);
            e.setExpectedType(null);

            final Object operand = e.getOperand();

            if (operand instanceof Variable) {
                final Variable variable = (Variable) operand;

                if (shouldResetVariableType(variable, preserveTypesFromMetadata, preserveGenericTypesFromMetadata)) {
                    variable.setType(null);
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

            if (expression.getCode().isStore() &&
                shouldInferVariableType((Variable) expression.getOperand())
                /*((Variable) expression.getOperand()).getType() == null*/) {

                _assignmentExpressions.get(expression.getOperand()).add(expressionToInfer);
            }

            return;
        }

        for (final Node child : node.getChildren()) {
            createDependencyGraph(child);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void findNestedAssignments(final Expression expression, final ExpressionToInfer parent) {
        for (final Expression argument : expression.getArguments()) {
            if (argument.getCode() == AstCode.Store) {
                final ExpressionToInfer expressionToInfer = new ExpressionToInfer();

                expressionToInfer.expression = argument;

                _allExpressions.add(expressionToInfer);

                findNestedAssignments(argument, expressionToInfer);

                final Variable variable = (Variable) argument.getOperand();

                if (shouldInferVariableType(variable)) {
                    _assignmentExpressions.get(variable).add(expressionToInfer);

                    //
                    // The instruction that consumes the Store result is handled as if it was reading the variable.
                    //
                    parent.dependencies.add(variable);
                }
            }
            else if (argument.getCode() == AstCode.Inc) {
                final ExpressionToInfer expressionToInfer = new ExpressionToInfer();

                expressionToInfer.expression = argument;

                _allExpressions.add(expressionToInfer);

                final Variable variable = (Variable) argument.getOperand();

                if (shouldInferVariableType(variable)) {
                    _assignmentExpressions.get(variable).add(expressionToInfer);

                    //
                    // The instruction that consumes the Store result is handled as if it was reading the variable.
                    //
                    parent.dependencies.add(variable);
                }
            }
            else if (argument.getCode() == AstCode.PreIncrement ||
                     argument.getCode() == AstCode.PostIncrement) {

                final ExpressionToInfer expressionToInfer = new ExpressionToInfer();

                expressionToInfer.expression = argument;

                _allExpressions.add(expressionToInfer);

                final Expression load = firstOrDefault(argument.getArguments());
                final StrongBox<Variable> variable = new StrongBox<>();

                if (load != null &&
                    matchLoadOrRet(load, variable) &&
                    shouldInferVariableType(variable.get())) {

                    _assignmentExpressions.get(variable.get()).add(expressionToInfer);

                    //
                    // The instruction that consumes the Store result is handled as if it was reading the variable.
                    //
                    parent.dependencies.add(variable.get());
                }
            }
            else {
                final StrongBox<Variable> variable = new StrongBox<>();

                if (matchLoadOrRet(argument, variable) &&
                    shouldInferVariableType(variable.get())) {

                    parent.dependencies.add(variable.get());
                }

                findNestedAssignments(argument, parent);
            }
        }
    }

    private void identifySingleLoadVariables() {
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        final Map<Variable, List<ExpressionToInfer>> groupedExpressions = new DefaultMap<>(
            new Supplier<List<ExpressionToInfer>>() {
                @Override
                public List<ExpressionToInfer> get() {
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
                    for (final ExpressionToInfer assignment : _assignmentExpressions.get(variable)) {
                        assignment.dependsOnSingleLoad = variable;
                    }
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void runInference() {
        _inferredVariableTypes.clear();

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
                        e.dependencies,
                        new Predicate<Variable>() {
                            @Override
                            public boolean test(final Variable v) {
                                return inferTypeForVariable(v, null) != null || _singleLoadVariables.contains(v);
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
            inferTypesForVariables(assignVariableTypesBasedOnPartialInformation);
        }

        verifyResults();
    }

    private void verifyResults() {
        final StrongBox<Expression> a = new StrongBox<>();

        for (final Variable variable : _assignmentExpressions.keySet()) {
            if (variable.getType() == null) {
                variable.setType(BuiltinTypes.Object);
            }
            else if (variable.getType().getSimpleType() == JvmType.Boolean) {
                //
                // Make sure constant assignments to boolean variables have boolean values,
                // and not integer values.
                //

                for (final ExpressionToInfer e : _assignmentExpressions.get(variable)) {
                    if (matchStore(e.expression, variable, a)) {
                        final Boolean booleanConstant = matchBooleanConstant(a.get());

                        if (booleanConstant != null) {
                            e.expression.setExpectedType(BuiltinTypes.Boolean);
                            e.expression.setInferredType(BuiltinTypes.Boolean);
                            a.get().setExpectedType(BuiltinTypes.Boolean);
                            a.get().setInferredType(BuiltinTypes.Boolean);
                            a.get().setOperand(booleanConstant);
                        }
                    }
                }
            }
            else if (variable.getType().getSimpleType() == JvmType.Character) {
                //
                // Make sure constant assignments to boolean variables have boolean values,
                // and not integer values.
                //

                for (final ExpressionToInfer e : _assignmentExpressions.get(variable)) {
                    if (matchStore(e.expression, variable, a)) {
                        final Character characterConstant = matchCharacterConstant(a.get());

                        if (characterConstant != null) {
                            e.expression.setExpectedType(BuiltinTypes.Character);
                            e.expression.setInferredType(BuiltinTypes.Character);
                            a.get().setExpectedType(BuiltinTypes.Character);
                            a.get().setInferredType(BuiltinTypes.Character);
                            a.get().setOperand(characterConstant);
                        }
                    }
                }
            }
        }
    }

    private void inferTypesForVariables(final boolean assignVariableTypesBasedOnPartialInformation) {
        for (final Variable variable : _assignmentExpressions.keySet()) {
            final List<ExpressionToInfer> expressionsToInfer = _assignmentExpressions.get(variable);

            if (assignVariableTypesBasedOnPartialInformation ? anyDone(expressionsToInfer)
                                                             : allDone(expressionsToInfer)) {

                TypeReference inferredType = null;

                for (final ExpressionToInfer e : expressionsToInfer) {
                    final List<Expression> arguments = e.expression.getArguments();

                    assert e.expression.getCode().isStore() &&
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
                    inferredType = variable.getType();
                }
                else if (!inferredType.isUnbounded()) {
                    inferredType = inferredType.hasSuperBound() ? inferredType.getSuperBound()
                                                                : inferredType.getExtendsBound();
                }

                if (shouldInferVariableType(variable) && inferredType != null) {
                    variable.setType(inferredType);
                    _inferredVariableTypes.put(variable, inferredType);

/*
                    //
                    // Assign inferred type to all the assignments (in case they used different inferred types).
                    //
                    for (final ExpressionToInfer e : expressionsToInfer) {
                        e.expression.setInferredType(inferredType);
                        runInference(single(e.expression.getArguments()));
                    }
*/

                    //
                    // Assign inferred types to all dependent expressions (in case they used different inferred types).
                    //
                    for (final ExpressionToInfer e : _allExpressions) {
                        if (e.dependencies.contains(variable)/* ||
                            expressionsToInfer.contains(e)*/) {

                            if (_stack.contains(e.expression)) {
                                continue;
                            }

                            for (final Expression c : e.expression.getSelfAndChildrenRecursive(Expression.class)) {
                                if (_stack.contains(c)) {
                                    continue;
                                }

                                c.setExpectedType(null);
                                c.setInferredType(null);
                            }

                            runInference(e.expression);
                        }
                    }
                }
            }
        }
    }

    private boolean shouldInferVariableType(final Variable variable) {
        final VariableDefinition variableDefinition = variable.getOriginalVariable();

        if (variable.isParameter()) {
            final TypeReference parameterType = variable.getOriginalParameter().getParameterType();

            if (parameterType.isGenericType() || MetadataHelper.isRawType(parameterType)) {
                return !_preserveMetadataGenericTypes;
            }

            return false;
        }

        //noinspection RedundantIfStatement
        if (variableDefinition != null &&
            variableDefinition.isFromMetadata() &&
            (variableDefinition.getVariableType().isGenericType() ? _preserveMetadataGenericTypes
                                                                  : _preserveMetadataTypes)) {

            return false;
        }

        return true;
    }

    private static boolean shouldResetVariableType(
        final Variable variable,
        final boolean preserveTypesFromMetadata,
        final boolean preserveGenericTypesFromMetadata) {

        final VariableDefinition variableDefinition = variable.getOriginalVariable();

        //noinspection SimplifiableIfStatement
        if (variableDefinition != null &&
            variableDefinition.isFromMetadata() &&
            (variableDefinition.getVariableType().isGenericType() ? preserveGenericTypesFromMetadata
                                                                  : preserveTypesFromMetadata)) {

            return false;
        }

        return variable.isGenerated() ||
               variableDefinition != null && variableDefinition.getVariableType() == BuiltinTypes.Integer ||
               variableDefinition != null && !variableDefinition.isTypeKnown();
    }

    private void runInference(final Expression expression) {
        final List<Expression> arguments = expression.getArguments();

        Variable changedVariable = null;
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

                if (variable.getType() == BuiltinTypes.Integer &&
                    shouldInferVariableType(variable)) {

                    variable.setType(BuiltinTypes.Boolean);
                    changedVariable = variable;
                }
            }
        }
        else if (expression.getInferredType() == BuiltinTypes.Integer &&
                 expression.getExpectedType() == BuiltinTypes.Character) {

            if (expression.getCode() == AstCode.Load || expression.getCode() == AstCode.Store) {
                final Variable variable = (Variable) expression.getOperand();

                expression.setInferredType(BuiltinTypes.Character);

                if (variable.getType() == BuiltinTypes.Integer &&
                    shouldInferVariableType(variable) &&
                    _singleLoadVariables.contains(variable)) {

                    variable.setType(BuiltinTypes.Character);
                    changedVariable = variable;
                }
            }
        }

        for (final Expression argument : arguments) {
            if (!argument.getCode().isStore()) {
                runInference(argument);
            }
        }

        if (changedVariable != null) {
            if (_previouslyInferred.add(Pair.create(changedVariable, changedVariable.getType()))) {
                invalidateDependentExpressions(expression, changedVariable);
            }
        }
    }

    private void invalidateDependentExpressions(final Expression expression, final Variable variable) {
        final List<ExpressionToInfer> assignments = _assignmentExpressions.get(variable);

        for (final ExpressionToInfer e : _allExpressions) {
            if (_stack.contains(e.expression)) {
                continue;
            }

            if (e.expression != expression &&
                (e.dependencies.contains(variable) ||
                 assignments.contains(e))) {

                for (final Expression c : e.expression.getSelfAndChildrenRecursive(Expression.class)) {
                    if (_stack.contains(c)) {
                        continue;
                    }

                    c.setExpectedType(null);
                    c.setInferredType(null);
                }

                runInference(e.expression);
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
            // Store and Inc are special cases and never gets reevaluated.
            //
            if (!expression.getCode().isStore()) {
                actualForceInferChildren = true;
            }
        }

        if (actualForceInferChildren || expression.getInferredType() == null) {
            expression.setInferredType(doInferTypeForExpression(expression, expectedType, actualForceInferChildren));
        }

        return expression.getInferredType();
    }

    @SuppressWarnings("ConstantConditions")
    private TypeReference doInferTypeForExpression(final Expression expression, final TypeReference expectedType, final boolean forceInferChildren) {
        if (_stack.contains(expression) && !match(expression, AstCode.LdC)) {
            return expectedType;
        }

        _stack.push(expression);

        try {
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
                    final TypeReference lastInferredType = _inferredVariableTypes.get(v);

                    if (forceInferChildren) {
                        //
                        // NOTE: Do not use 'expectedType' here!
                        //
                        final TypeReference inferredType = inferTypeForExpression(
                            expression.getArguments().get(0),
                            inferTypeForVariable(v, null)
                        );

                        if (inferredType != null) {
                            return inferredType;
                        }
                    }

                    return lastInferredType != null ? lastInferredType : v.getType();
                }

                case Load: {
                    final Variable v = (Variable) expression.getOperand();
                    final TypeReference lastInferredType = _inferredVariableTypes.get(v);

                    if (expectedType != null) {
                        TypeReference result = null;

                        if (expectedType.isGenericType()) {
                            if (MetadataHelper.areGenericsSupported(_context.getCurrentType())) {
                                if (lastInferredType != null) {
                                    result = MetadataHelper.asSubType(lastInferredType, expectedType);
                                }

                                if (result == null && v.getType() != null) {
                                    result = MetadataHelper.asSubType(v.getType(), expectedType);

                                    if (result == null) {
                                        result = MetadataHelper.asSubType(MetadataHelper.eraseRecursive(v.getType()), expectedType);
                                    }
                                }

                                if (MetadataHelper.getUnboundGenericParameterCount(result) > 0 && lastInferredType != null) {
                                    result = MetadataHelper.substituteGenericArguments(result, lastInferredType);
                                }
                            }
                            else {
                                result = new RawType(expectedType.getUnderlyingType());
                            }

                            _inferredVariableTypes.put(v, result);

                            if (result != null && lastInferredType == null ||
                                !MetadataHelper.isSameType(result, lastInferredType) &&
                                _previouslyInferred.add(Pair.create(v, result))) {

                                invalidateDependentExpressions(expression, v);
                            }
                        }

                        if (_singleLoadVariables.contains(v) && v.getType() == null) {
                            v.setType(result != null ? result : expectedType);
                        }

                        return result != null ? result : v.getType();
                    }

                    return lastInferredType != null ? lastInferredType : v.getType();
                }

                case InvokeDynamic: {
                    final DynamicCallSite callSite = (DynamicCallSite) operand;

                    if (expectedType == null) {
                        return callSite.getMethodType().getReturnType();
                    }

                    return MetadataHelper.substituteGenericArguments(
                        expectedType,
                        callSite.getMethodType().getReturnType()
                    );
                }

                case InvokeVirtual:
                case InvokeSpecial:
                case InvokeStatic:
                case InvokeInterface: {
                    final MethodReference method = (MethodReference) operand;
                    final List<ParameterDefinition> parameters = method.getParameters();
                    final boolean hasThis = code != AstCode.InvokeStatic && code != AstCode.InvokeDynamic;

                    MethodReference boundMethod = method;

                    if (forceInferChildren) {
                        final MethodDefinition r = method.resolve();

                        MethodReference actualMethod;

                        if (hasThis) {
                            final TypeReference targetType;
                            final Expression thisArg = arguments.get(0);

                            final TypeReference expectedTargetType = thisArg.getInferredType() != null ? thisArg.getInferredType()
                                                                                                       : thisArg.getExpectedType();

                            if (expectedTargetType != null &&
                                expectedTargetType.isGenericType() &&
                                !expectedTargetType.isGenericDefinition()) {

                                boundMethod = MetadataHelper.asMemberOf(method, expectedTargetType);

                                targetType = inferTypeForExpression(
                                    arguments.get(0),
                                    expectedTargetType
                                );
                            }
                            else if (method.isConstructor()) {
                                targetType = method.getDeclaringType();
                            }
                            else {
                                targetType = inferTypeForExpression(
                                    arguments.get(0),
                                    method.getDeclaringType()
                                );
                            }

                            final MethodReference m = targetType != null ? MetadataHelper.asMemberOf(r != null ? r : method, targetType)
                                                                         : method;

                            if (m != null) {
                                actualMethod = m;
                                boundMethod = m;
                                expression.setOperand(m);
                            }
                            else {
                                actualMethod = r != null ? r : boundMethod;
                            }
                        }
                        else {
                            actualMethod = r != null ? r : boundMethod;
                        }

                        List<ParameterDefinition> p = r != null ? r.getParameters()
                                                                : boundMethod.getParameters();

                        Map<TypeReference, TypeReference> mappings = null;

                        if (actualMethod.containsGenericParameters() || r != null && r.containsGenericParameters()) {
                            final Map<TypeReference, TypeReference> oldMappings = new HashMap<>();
                            final Map<TypeReference, TypeReference> newMappings = new HashMap<>();

                            final List<ParameterDefinition> rp = r != null ? r.getParameters() : p;

                            if (expectedType != null) {
                                final TypeReference returnType = r != null ? r.getReturnType()
                                                                           : actualMethod.getReturnType();

                                if (returnType.containsGenericParameters()) {
                                    new AddMappingsForArgumentVisitor(expectedType).visit(returnType, newMappings);
                                }
                            }

                            for (int i = 0; i < parameters.size(); i++) {
                                final TypeReference rType = rp.get(i).getParameterType();
                                final TypeReference pType = p.get(i).getParameterType();
                                final TypeReference aType = inferTypeForExpression(arguments.get(hasThis ? i + 1 : i), null);

                                if (aType != null && pType.containsGenericParameters()) {
                                    new AddMappingsForArgumentVisitor(aType).visit(pType, oldMappings);
                                }

                                if (aType != null && rType.containsGenericParameters()) {
                                    new AddMappingsForArgumentVisitor(aType).visit(pType, newMappings);
                                }
                            }

                            if (!oldMappings.isEmpty() || !newMappings.isEmpty()) {
                                mappings = newMappings;
                                mappings.putAll(oldMappings);
                            }

                            if (mappings != null) {
                                boundMethod = TypeSubstitutionVisitor.instance().visitMethod(actualMethod, mappings);
                                actualMethod = boundMethod;
                                expression.setOperand(boundMethod);
                                p = boundMethod.getParameters();
                            }

                            final TypeReference boundDeclaringType = boundMethod.getDeclaringType();

                            if (boundDeclaringType.isGenericType()) {
                                if (mappings == null) {
                                    mappings = new HashMap<>();
                                }

                                for (final GenericParameter gp : boundDeclaringType.getGenericParameters()) {
                                    final GenericParameter inScope = _context.getCurrentMethod().findTypeVariable(gp.getName());

                                    if (inScope != null && MetadataHelper.isSameType(gp, inScope)) {
                                        continue;
                                    }

                                    if (!mappings.containsKey(gp)) {
                                        mappings.put(gp, BuiltinTypes.Object);
                                    }
                                }

                                boundMethod = TypeSubstitutionVisitor.instance().visitMethod(actualMethod, mappings);
                                expression.setOperand(boundMethod);
                                p = boundMethod.getParameters();
                            }

                            if (boundMethod.isGenericMethod()) {
                                if (mappings == null) {
                                    mappings = new HashMap<>();
                                }

                                for (final GenericParameter gp : boundMethod.getGenericParameters()) {
                                    if (!mappings.containsKey(gp)) {
                                        mappings.put(gp, BuiltinTypes.Object);
                                    }
                                }

                                boundMethod = TypeSubstitutionVisitor.instance().visitMethod(actualMethod, mappings);
                                expression.setOperand(boundMethod);
                                p = boundMethod.getParameters();
                            }

                            if (r != null && method.isGenericMethod()) {
                                final HashMap<TypeReference, TypeReference> tempMappings = new HashMap<>();
                                final List<ParameterDefinition> bp = method.getParameters();

                                for (int i = 0, n = bp.size(); i < n; i++) {
                                    new AddMappingsForArgumentVisitor(bp.get(i).getParameterType()).visit(
                                        rp.get(i).getParameterType(),
                                        tempMappings
                                    );
                                }

                                boolean changed = false;

                                if (mappings == null) {
                                    mappings = tempMappings;
                                    changed = true;
                                }
                                else {
                                    for (final TypeReference key : tempMappings.keySet()) {
                                        if (!mappings.containsKey(key)) {
                                            mappings.put(key, tempMappings.get(key));
                                            changed = true;
                                        }
                                    }
                                }

                                if (changed) {
                                    boundMethod = TypeSubstitutionVisitor.instance().visitMethod(actualMethod, mappings);
                                    expression.setOperand(boundMethod);
                                    p = boundMethod.getParameters();
                                }
                            }
                        }
                        else {
                            boundMethod = actualMethod;
                        }

                        if (hasThis && mappings != null) {
                            TypeReference expectedTargetType;

                            if (boundMethod.isConstructor()) {
                                expectedTargetType = MetadataHelper.substituteGenericArguments(boundMethod.getDeclaringType(), mappings);
                            }
                            else {
                                expectedTargetType = boundMethod.getDeclaringType();
                            }

                            if (expectedTargetType != null &&
                                expectedTargetType.isGenericDefinition() &&
                                arguments.get(0).getInferredType() != null) {

                                expectedTargetType = MetadataHelper.asSuper(expectedTargetType, arguments.get(0).getInferredType());
                            }

                            inferTypeForExpression(
                                arguments.get(0),
                                expectedTargetType,
                                forceInferChildren
                            );
                        }

                        for (int i = 0; i < parameters.size(); i++) {
                            final TypeReference pType = p.get(i).getParameterType();
                            inferTypeForExpression(
                                arguments.get(hasThis ? i + 1 : i),
                                pType.isPrimitive() ? pType : null, //p.get(i).getParameterType(),
                                forceInferChildren
                            );
                        }
                    }

                    if (hasThis) {
                        if (boundMethod.isConstructor()) {
                            return boundMethod.getDeclaringType();
                        }
                    }

                    return boundMethod.getReturnType();
                }

                case GetField: {
                    final FieldReference field = (FieldReference) operand;

                    if (forceInferChildren) {
                        final TypeReference targetType = inferTypeForExpression(arguments.get(0), field.getDeclaringType());

                        if (targetType != null) {
                            final FieldReference asMember = MetadataHelper.asMemberOf(field, targetType);

                            return asMember.getFieldType();
                        }
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

                case PreIncrement:
                case PostIncrement: {
                    final TypeReference inferredType = inferTypeForExpression(arguments.get(0), null);

                    if (inferredType == null) {
                        final Number n = (Number) operand;

                        if (n instanceof Long) {
                            return BuiltinTypes.Long;
                        }

                        return BuiltinTypes.Integer;
                    }

                    return inferredType;
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
                    if (forceInferChildren) {
                        inferTypeForExpression(arguments.get(0), expectedType);
                        inferTypeForExpression(arguments.get(1), expectedType);
                    }
                    return inferBinaryArguments(arguments.get(0), arguments.get(1), expectedType, false, null, null);
                }

                case Shl: {
                    if (forceInferChildren) {
                        inferTypeForExpression(arguments.get(1), BuiltinTypes.Integer);
                    }

                    if (expectedType != null &&
                        (expectedType.getSimpleType() == JvmType.Integer ||
                         expectedType.getSimpleType() == JvmType.Long)) {

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

                    if (type == null) {
                        return null;
                    }

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
                    return null;
                }

                case LdC: {
                    if (operand instanceof Boolean) {
                        return BuiltinTypes.Boolean;
                    }

                    if (operand instanceof Character) {
                        return BuiltinTypes.Character;
                    }

                    if (operand instanceof Number) {
                        final Number number = (Number) operand;

                        if (number instanceof Integer) {
                            if (expectedType != null) {
                                switch (expectedType.getSimpleType()) {
                                    case Boolean:
                                        if (number.intValue() == 0 || number.intValue() == 1) {
                                            return BuiltinTypes.Boolean;
                                        }
                                        return BuiltinTypes.Integer;

                                    case Byte:
                                        if (number.intValue() >= Byte.MIN_VALUE &&
                                            number.intValue() <= Byte.MAX_VALUE) {

                                            return BuiltinTypes.Byte;
                                        }
                                        return BuiltinTypes.Integer;

                                    case Character:
                                        if (number.intValue() >= Character.MIN_VALUE &&
                                            number.intValue() <= Character.MAX_VALUE) {

                                            return BuiltinTypes.Character;
                                        }
                                        return BuiltinTypes.Integer;

                                    case Short:
                                        if (number.intValue() >= Short.MIN_VALUE &&
                                            number.intValue() <= Short.MAX_VALUE) {

                                            return BuiltinTypes.Short;
                                        }
                                        return BuiltinTypes.Integer;
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
                        return _factory.makeParameterizedType(
                            _factory.makeNamedType("java.lang.Class"),
                            null,
                            (TypeReference) operand
                        );
                    }

                    return _factory.makeNamedType("java.lang.String");
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
                    final MethodReference instanceCtor = (MethodReference) operand;
                    final MethodReference resolvedCtor = instanceCtor instanceof IGenericInstance ? instanceCtor.resolve() : instanceCtor;
                    final MethodReference constructor = resolvedCtor != null ? resolvedCtor : instanceCtor;
                    final TypeReference type = constructor.getDeclaringType();

                    final TypeReference inferredType;

                    if (expectedType != null && !MetadataHelper.isSameType(expectedType, BuiltinTypes.Object)) {
                        final TypeReference asSubType = MetadataHelper.asSubType(type, expectedType);
                        inferredType = asSubType != null ? asSubType : type;
                    }
                    else {
                        inferredType = type;
                    }

                    final Map<TypeReference, TypeReference> mappings;

                    if (inferredType.isGenericDefinition()) {
                        mappings = new HashMap<>();

                        for (final GenericParameter gp : inferredType.getGenericParameters()) {
                            mappings.put(gp, BuiltinTypes.Object);
                        }
                    }
                    else {
                        mappings = Collections.emptyMap();
                    }

                    if (forceInferChildren) {
                        final MethodReference asMember = MetadataHelper.asMemberOf(
                            constructor,
                            TypeSubstitutionVisitor.instance().visit(inferredType, mappings)
                        );

                        final List<ParameterDefinition> parameters = asMember.getParameters();

                        for (int i = 0; i < arguments.size() && i < parameters.size(); i++) {
                            inferTypeForExpression(
                                arguments.get(i),
                                parameters.get(i).getParameterType()
                            );
                        }

                        expression.setOperand(asMember);
                    }

                    if (inferredType != null) {
                        if (inferredType instanceof IGenericInstance) {
                            expression.putUserData(
                                AstKeys.TYPE_ARGUMENTS,
                                ((IGenericInstance) inferredType).getTypeArguments()
                            );
                        }

                        return inferredType;
                    }

                    return type;
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
                        if (expectedType.getSimpleType() == JvmType.Boolean &&
                            (number.intValue() == 0 || number.intValue() == 1)) {

                            return BuiltinTypes.Boolean;
                        }

                        if (expectedType.getSimpleType() == JvmType.Byte &&
                            number.intValue() >= Byte.MIN_VALUE &&
                            number.intValue() <= Byte.MAX_VALUE) {

                            return BuiltinTypes.Byte;
                        }

                        if (expectedType.getSimpleType() == JvmType.Character &&
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
                    final TypeReference expectedArgumentType;
                    final TypeReference conversionResult;

                    switch (code) {
                        case I2L:
                            conversionResult = BuiltinTypes.Long;
                            expectedArgumentType = BuiltinTypes.Integer;
                            break;
                        case I2F:
                            conversionResult = BuiltinTypes.Float;
                            expectedArgumentType = BuiltinTypes.Integer;
                            break;
                        case I2D:
                            conversionResult = BuiltinTypes.Double;
                            expectedArgumentType = BuiltinTypes.Integer;
                            break;
                        case L2I:
                            conversionResult = BuiltinTypes.Integer;
                            expectedArgumentType = BuiltinTypes.Long;
                            break;
                        case L2F:
                            conversionResult = BuiltinTypes.Float;
                            expectedArgumentType = BuiltinTypes.Long;
                            break;
                        case L2D:
                            conversionResult = BuiltinTypes.Double;
                            expectedArgumentType = BuiltinTypes.Long;
                            break;
                        case F2I:
                            conversionResult = BuiltinTypes.Integer;
                            expectedArgumentType = BuiltinTypes.Float;
                            break;
                        case F2L:
                            conversionResult = BuiltinTypes.Long;
                            expectedArgumentType = BuiltinTypes.Float;
                            break;
                        case F2D:
                            conversionResult = BuiltinTypes.Double;
                            expectedArgumentType = BuiltinTypes.Float;
                            break;
                        case D2I:
                            conversionResult = BuiltinTypes.Integer;
                            expectedArgumentType = BuiltinTypes.Double;
                            break;
                        case D2L:
                            conversionResult = BuiltinTypes.Long;
                            expectedArgumentType = BuiltinTypes.Double;
                            break;
                        case D2F:
                            conversionResult = BuiltinTypes.Float;
                            expectedArgumentType = BuiltinTypes.Double;
                            break;
                        case I2B:
                            conversionResult = BuiltinTypes.Byte;
                            expectedArgumentType = BuiltinTypes.Integer;
                            break;
                        case I2C:
                            conversionResult = BuiltinTypes.Character;
                            expectedArgumentType = BuiltinTypes.Integer;
                            break;
                        case I2S:
                            conversionResult = BuiltinTypes.Short;
                            expectedArgumentType = BuiltinTypes.Integer;
                            break;
                        default:
                            throw ContractUtils.unsupported();
                    }

                    arguments.get(0).setExpectedType(expectedArgumentType);
                    return conversionResult;
                }

                case CheckCast:
                case Unbox: {
                    if (expectedType != null) {
                        final TypeReference castType = (TypeReference) operand;

                        TypeReference inferredType = MetadataHelper.asSubType(castType, expectedType);

                        if (forceInferChildren) {
                            inferredType = inferTypeForExpression(
                                arguments.get(0),
                                inferredType != null ? inferredType
                                                     : (TypeReference) operand
                            );
                        }

                        if (inferredType != null && MetadataHelper.isSubType(inferredType, castType)) {
                            expression.setOperand(inferredType);
                            return inferredType;
                        }
                    }
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

                        doInferTypeForExpression(
                            binaryArguments.get(0),
                            binaryArguments.get(0).getInferredType() != null ? binaryArguments.get(0).getInferredType()
                                                                             : binaryArguments.get(0).getExpectedType(),
                            true
                        );

                        doInferTypeForExpression(
                            binaryArguments.get(1),
                            binaryArguments.get(1).getInferredType() != null ? binaryArguments.get(1).getInferredType()
                                                                             : binaryArguments.get(1).getExpectedType(),
                            true
                        );

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
                    final TypeReference returnType = _context.getCurrentMethod().getReturnType();
                    if (forceInferChildren && arguments.size() == 1) {
                        inferTypeForExpression(arguments.get(0), returnType);
                    }
                    return returnType;
                }

                case Jsr: {
                    return BuiltinTypes.Integer;
                }

                case Ret: {
                    if (forceInferChildren) {
                        inferTypeForExpression(arguments.get(0), BuiltinTypes.Integer);
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
                    return inferTypeForVariable((Variable) operand, expectedType);
                }

                case Leave:
                case Nop: {
                    return null;
                }

                case DefaultValue: {
                    return (TypeReference) expression.getOperand();
                }

                default: {
                    System.err.printf("Type inference can't handle opcode '%s'.\n", code.getName());
                    return null;
                }
            }
        }
        finally {
            _stack.pop();
        }
    }

    private TypeReference inferTypeForVariable(final Variable v, final TypeReference expectedType) {
        final TypeReference lastInferredType = _inferredVariableTypes.get(v);

        if (lastInferredType != null) {
            return lastInferredType;
        }

        final TypeReference variableType = v.getType();

        if (variableType != null) {
            return variableType;
        }

        if (v.isGenerated()) {
            return expectedType;
        }

        return v.isParameter() ? v.getOriginalParameter().getParameterType()
                               : v.getOriginalVariable().getVariableType();
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

        if (leftPreferred != null && rightPreferred != null) {
            return MetadataHelper.findCommonSuperType(
                leftPreferred.isGenericDefinition() ? new RawType(leftPreferred)
                                                    : leftPreferred,
                rightPreferred.isGenericDefinition() ? new RawType(rightPreferred)
                                                     : rightPreferred
            );
        }

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
        final FieldDefinition resolvedField = field.resolve();

        if (resolvedField != null) {
            final FieldReference asMember = MetadataHelper.asMemberOf(resolvedField, field.getDeclaringType());

            return asMember.getFieldType();
        }

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
                        declaringType = declaringType.getUnderlyingType();
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
/*
        //noinspection SimplifiableIfStatement
        if (t1 == t2) {
            return true;
        }

        return t1 != null &&
               t2 != null &&
               Comparer.equals(t1.getFullName(), t2.getFullName());
*/
        return MetadataHelper.isSameType(t1, t2, true);
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
        return type != null && type.getSimpleType() == JvmType.Boolean;
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

    private final static class AddMappingsForArgumentVisitor extends DefaultTypeVisitor<Map<TypeReference, TypeReference>, Void> {
        private TypeReference argumentType;

        AddMappingsForArgumentVisitor(final TypeReference argumentType) {
            this.argumentType = VerifyArgument.notNull(argumentType, "argumentType");
        }

        public Void visit(final TypeReference t, final Map<TypeReference, TypeReference> map) {
            final TypeReference a = argumentType;
            t.accept(this, map);
            argumentType = a;
            return null;
        }

        @Override
        public Void visitArrayType(final ArrayType t, final Map<TypeReference, TypeReference> map) {
            final TypeReference a = argumentType;

            if (a.isArray() && t.isArray()) {
                argumentType = a.getElementType();
                visit(t.getElementType(), map);
            }

            return null;
        }

        @Override
        public Void visitGenericParameter(final GenericParameter t, final Map<TypeReference, TypeReference> map) {
            if (MetadataResolver.areEquivalent(argumentType, t)) {
                return null;
            }

            final TypeReference existingMapping = map.get(t);

            if (existingMapping == null) {
                map.put(t, argumentType);
            }
            else {
                map.put(t, MetadataHelper.findCommonSuperType(existingMapping, argumentType));
            }

            return null;
        }

        @Override
        public Void visitWildcard(final WildcardType t, final Map<TypeReference, TypeReference> map) {
            return null;
        }

        @Override
        public Void visitCompoundType(final CompoundTypeReference t, final Map<TypeReference, TypeReference> map) {
            return null;
        }

        @Override
        public Void visitParameterizedType(final TypeReference t, final Map<TypeReference, TypeReference> map) {
            final TypeReference r = MetadataHelper.asSuper(t.getUnderlyingType(), argumentType);
            final TypeReference s = MetadataHelper.asSubType(argumentType, r != null ? r : t.getUnderlyingType());

            if (s != null && s instanceof IGenericInstance) {
                final List<TypeReference> tArgs = ((IGenericInstance) t).getTypeArguments();
                final List<TypeReference> sArgs = ((IGenericInstance) s).getTypeArguments();

                if (tArgs.size() == sArgs.size()) {
                    for (int i = 0, n = tArgs.size(); i < n; i++) {
                        argumentType = sArgs.get(i);
                        visit(tArgs.get(i), map);
                    }
                }
            }

            return null;
        }

        @Override
        public Void visitPrimitiveType(final PrimitiveType t, final Map<TypeReference, TypeReference> map) {
            return null;
        }

        @Override
        public Void visitClassType(final TypeReference t, final Map<TypeReference, TypeReference> map) {
            return null;
        }

        @Override
        public Void visitNullType(final TypeReference t, final Map<TypeReference, TypeReference> map) {
            return null;
        }

        @Override
        public Void visitBottomType(final TypeReference t, final Map<TypeReference, TypeReference> map) {
            return null;
        }

        @Override
        public Void visitRawType(final TypeReference t, final Map<TypeReference, TypeReference> map) {
            return null;
        }
    }
}
