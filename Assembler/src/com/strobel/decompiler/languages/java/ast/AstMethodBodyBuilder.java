/*
 * AstMethodBodyBuilder.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast;

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MethodBody;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.assembler.metadata.VariableDefinition;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.*;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.util.ContractUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.strobel.core.CollectionUtilities.firstOrDefault;

public class AstMethodBodyBuilder {
    private final AstBuilder _astBuilder;
    private final MethodDefinition _method;
    private final DecompilerContext _context;
    private final Set<Variable> _localVariablesToDefine = new LinkedHashSet<>();

    public static BlockStatement createMethodBody(
        final AstBuilder astBuilder,
        final MethodDefinition method,
        final DecompilerContext context,
        final Iterable<ParameterDeclaration> parameters) {

        VerifyArgument.notNull(astBuilder, "astBuilder");
        VerifyArgument.notNull(method, "method");
        VerifyArgument.notNull(context, "context");

        final MethodDefinition oldCurrentMethod = context.getCurrentMethod();

/*
        assert oldCurrentMethod == null ||
               oldCurrentMethod == method ||
               method.getDeclaringType().getDeclaringMethod() == oldCurrentMethod;
*/

        context.setCurrentMethod(method);

        try {
            final AstMethodBodyBuilder builder = new AstMethodBodyBuilder(astBuilder, method, context);
            return builder.createMethodBody(parameters);
        }
        finally {
            context.setCurrentMethod(oldCurrentMethod);
        }
    }

    private AstMethodBodyBuilder(final AstBuilder astBuilder, final MethodDefinition method, final DecompilerContext context) {
        _astBuilder = astBuilder;
        _method = method;
        _context = context;
    }

    @SuppressWarnings("UnusedParameters")
    private BlockStatement createMethodBody(final Iterable<ParameterDeclaration> parameters) {
        final MethodBody body = _method.getBody();

        if (body == null) {
            return null;
        }

        final Block method = new Block();

        method.getBody().addAll(
            com.strobel.decompiler.ast.AstBuilder.build(body, true, _context)
        );

        AstOptimizer.optimize(_context, method);

        final Set<Variable> methodParameters = new LinkedHashSet<>();
        final Set<Variable> localVariables = new LinkedHashSet<>();

        final List<com.strobel.decompiler.ast.Expression> expressions = method.getSelfAndChildrenRecursive(
            com.strobel.decompiler.ast.Expression.class
        );

        for (final com.strobel.decompiler.ast.Expression e : expressions) {
            final Object operand = e.getOperand();

            if (operand instanceof Variable) {
                final Variable variable = (Variable) operand;

                if (variable.isParameter()) {
                    methodParameters.add(variable);
                }
                else {
                    localVariables.add(variable);
                }
            }
        }

        NameVariables.assignNamesToVariables(_context, methodParameters, localVariables, method);

        final BlockStatement astBlock = transformBlock(method);

        CommentStatement.replaceAll(astBlock);

        final AstNodeCollection<Statement> statements = astBlock.getStatements();
        final Statement insertionPoint = firstOrDefault(statements);

        for (final Variable v : _localVariablesToDefine) {
            final AstType type = AstBuilder.convertType(v.getType());
            final VariableDeclarationStatement declaration = new VariableDeclarationStatement(type, v.getName());

            declaration.putUserData(Keys.VARIABLE, v);
            statements.insertBefore(insertionPoint, declaration);
        }

        return astBlock;
    }

    private BlockStatement transformBlock(final Block block) {
        final BlockStatement astBlock = new BlockStatement();

        if (block != null) {
            final List<Node> children = block.getChildren();
            for (int i = 0; i < children.size(); i++) {
                final Node node = children.get(i);

                final Statement statement = transformNode(
                    node,
                    i < children.size() - 1 ? children.get(i + 1) : null
                );

                astBlock.getStatements().add(statement);

                if (statement instanceof SynchronizedStatement) {
                    i++;
                }
            }
        }

        return astBlock;
    }

    private Statement transformNode(final Node node, final Node next) {
        if (node instanceof Label) {
            return new LabelStatement(((Label) node).getName());
        }

        if (node instanceof Block) {
            return transformBlock((Block) node);
        }

        if (node instanceof com.strobel.decompiler.ast.Expression) {
            final com.strobel.decompiler.ast.Expression expression = (com.strobel.decompiler.ast.Expression) node;

            if (expression.getCode() == AstCode.MonitorEnter &&
                next instanceof TryCatchBlock) {

                final TryCatchBlock tryCatch = (TryCatchBlock) next;
                final Block finallyBlock = tryCatch.getFinallyBlock();
                final List<CatchBlock> catchBlocks = tryCatch.getCatchBlocks();

                if (finallyBlock != null &&
                    catchBlocks.isEmpty() &&
                    finallyBlock.getBody().size() == 1) {

                    final Node finallyNode = finallyBlock.getBody().get(0);

                    if (finallyNode instanceof com.strobel.decompiler.ast.Expression &&
                        ((com.strobel.decompiler.ast.Expression) finallyNode).getCode() == AstCode.MonitorExit) {

                        return transformSynchronized(expression, tryCatch);
                    }
                }
            }

            final List<Range> ranges = new ArrayList<>();

            final List<com.strobel.decompiler.ast.Expression> childExpressions = node.getSelfAndChildrenRecursive(
                com.strobel.decompiler.ast.Expression.class
            );

            for (final com.strobel.decompiler.ast.Expression e : childExpressions) {
                ranges.addAll(e.getRanges());
            }

            @SuppressWarnings("UnusedDeclaration")
            final List<Range> orderedAndJoinedRanges = Range.orderAndJoint(ranges);
            final AstNode codeExpression = transformExpression((com.strobel.decompiler.ast.Expression) node, true);

            if (codeExpression != null) {
                if (codeExpression instanceof Expression) {
                    return new ExpressionStatement((Expression) codeExpression);
                }
                return (Statement) codeExpression;
            }
        }

        if (node instanceof Loop) {
            final Loop loop = (Loop) node;
            final WhileStatement whileStatement = new WhileStatement();
            final com.strobel.decompiler.ast.Expression loopCondition = loop.getCondition();

            if (loopCondition != null) {
                whileStatement.setCondition((Expression) transformExpression(loopCondition, false));
            }
            else {
                whileStatement.setCondition(new PrimitiveExpression(true));
            }

            whileStatement.setEmbeddedStatement(transformBlock(loop.getBody()));

            return whileStatement;
        }

        if (node instanceof Condition) {
            final Condition condition = (Condition) node;
            final com.strobel.decompiler.ast.Expression testCondition = condition.getCondition();
            final Block trueBlock = condition.getTrueBlock();
            final Block falseBlock = condition.getFalseBlock();
            final boolean hasFalseBlock = falseBlock.getEntryGoto() != null || !falseBlock.getBody().isEmpty();

            return new IfElseStatement(
                (Expression) transformExpression(testCondition, false),
                transformBlock(trueBlock),
                hasFalseBlock ? transformBlock(falseBlock) : null
            );
        }

        if (node instanceof Switch) {
            final Switch switchNode = (Switch) node;
            final com.strobel.decompiler.ast.Expression testCondition = switchNode.getCondition();

            if (TypeAnalysis.isBoolean(testCondition.getInferredType())) {
                testCondition.setExpectedType(BuiltinTypes.Integer);
            }

            final List<CaseBlock> caseBlocks = switchNode.getCaseBlocks();
            final SwitchStatement switchStatement = new SwitchStatement((Expression) transformExpression(testCondition, false));

            for (final CaseBlock caseBlock : caseBlocks) {
                final SwitchSection section = new SwitchSection();
                final AstNodeCollection<CaseLabel> caseLabels = section.getCaseLabels();

                if (caseBlock.getValues().isEmpty()) {
                    caseLabels.add(new CaseLabel());
                }
                else {
                    final TypeReference referenceType;

                    if (testCondition.getExpectedType() != null) {
                        referenceType = testCondition.getExpectedType();
                    }
                    else {
                        referenceType = testCondition.getInferredType();
                    }

                    for (final Integer value : caseBlock.getValues()) {
                        final CaseLabel caseLabel = new CaseLabel();
                        caseLabel.setExpression(AstBuilder.makePrimitive(value, referenceType));
                        caseLabels.add(caseLabel);
                    }
                }

                section.getStatements().add(transformBlock(caseBlock));
                switchStatement.getSwitchSections().add(section);
            }

            return switchStatement;
        }

        if (node instanceof TryCatchBlock) {
            final TryCatchBlock tryCatchNode = ((TryCatchBlock) node);
            final Block finallyBlock = tryCatchNode.getFinallyBlock();
            final List<CatchBlock> catchBlocks = tryCatchNode.getCatchBlocks();

            final TryCatchStatement tryCatch = new TryCatchStatement();

            tryCatch.setTryBlock(transformBlock(tryCatchNode.getTryBlock()));

            for (final CatchBlock catchBlock : catchBlocks) {
                final CatchClause catchClause = new CatchClause(transformBlock(catchBlock));

                for (final TypeReference caughtType : catchBlock.getCaughtTypes()) {
                    catchClause.getExceptionTypes().add(AstBuilder.convertType(caughtType));
                }

                final Variable exceptionVariable = catchBlock.getExceptionVariable();

                if (exceptionVariable != null) {
                    catchClause.setVariableName(exceptionVariable.getName());
                    catchClause.putUserData(Keys.VARIABLE, exceptionVariable);
                }

                tryCatch.getCatchClauses().add(catchClause);
            }

            if (finallyBlock != null && !finallyBlock.getBody().isEmpty()) {
                tryCatch.setFinallyBlock(transformBlock(finallyBlock));
            }

            return tryCatch;
        }

        throw new IllegalArgumentException("Unknown node type: " + node);
    }

    private SynchronizedStatement transformSynchronized(final com.strobel.decompiler.ast.Expression expression, final TryCatchBlock tryCatch) {
        final SynchronizedStatement s = new SynchronizedStatement();
        s.setExpression((Expression) transformExpression(expression.getArguments().get(0), false));
        s.setEmbeddedStatement(transformBlock(tryCatch.getTryBlock()));
        return s;
    }

    private AstNode transformExpression(final com.strobel.decompiler.ast.Expression e, final boolean isTopLevel) {
        return transformByteCode(e, isTopLevel);
    }

    @SuppressWarnings("ConstantConditions")
    private AstNode transformByteCode(final com.strobel.decompiler.ast.Expression byteCode, final boolean isTopLevel) {
        final Object operand = byteCode.getOperand();
        final Label label = operand instanceof Label ? (Label) operand : null;
        final AstType operandType = operand instanceof TypeReference ? AstBuilder.convertType((TypeReference) operand) : AstType.NULL;
        final Variable variableOperand = operand instanceof Variable ? (Variable) operand : null;
        final FieldReference fieldOperand = operand instanceof FieldReference ? (FieldReference) operand : null;

        final List<Expression> arguments = new ArrayList<>();

        for (final com.strobel.decompiler.ast.Expression e : byteCode.getArguments()) {
            arguments.add((Expression) transformExpression(e, false));
        }

        final Expression arg1 = arguments.size() >= 1 ? arguments.get(0) : null;
        final Expression arg2 = arguments.size() >= 2 ? arguments.get(1) : null;
        final Expression arg3 = arguments.size() >= 3 ? arguments.get(2) : null;

        switch (byteCode.getCode()) {
            case Nop:
                return null;

            case AConstNull:
                return new NullReferenceExpression();

            case LdC: {
                if (operand instanceof TypeReference) {
                    return new ClassOfExpression(operandType);
                }

                final TypeReference type = byteCode.getInferredType() != null ? byteCode.getInferredType()
                                                                              : byteCode.getExpectedType();

                if (type != null) {
                    return new PrimitiveExpression(JavaPrimitiveCast.cast(type.getSimpleType(), operand));
                }

                return new PrimitiveExpression(operand);
            }

            case Pop:
            case Pop2:
            case Dup:
            case DupX1:
            case DupX2:
            case Dup2:
            case Dup2X1:
            case Dup2X2:
                return arg1;

            case Swap:
                return arg1;

            case I2L:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Long), arg1);
            case I2F:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Float), arg1);
            case I2D:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Double), arg1);
            case L2I:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Integer), arg1);
            case L2F:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Float), arg1);
            case L2D:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Double), arg1);
            case F2I:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Integer), arg1);
            case F2L:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Long), arg1);
            case F2D:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Double), arg1);
            case D2I:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Integer), arg1);
            case D2L:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Long), arg1);
            case D2F:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Float), arg1);
            case I2B:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Byte), arg1);
            case I2C:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Character), arg1);
            case I2S:
                return new CastExpression(AstBuilder.convertType(BuiltinTypes.Short), arg1);

            case Goto:
                return new GotoStatement(((Label) operand).getName());

            case GetStatic: {
                final MemberReferenceExpression staticFieldReference = AstBuilder.convertType(fieldOperand.getDeclaringType())
                                                                                 .member(fieldOperand.getName());
                staticFieldReference.putUserData(Keys.MEMBER_REFERENCE, fieldOperand);
                return staticFieldReference;
            }

            case PutStatic: {
                final MemberReferenceExpression staticFieldReference = AstBuilder.convertType(fieldOperand.getDeclaringType())
                                                                                 .member(fieldOperand.getName());
                staticFieldReference.putUserData(Keys.MEMBER_REFERENCE, fieldOperand);
                return new AssignmentExpression(staticFieldReference, arg1);
            }

            case GetField: {
                final MemberReferenceExpression fieldReference = arg1.member(fieldOperand.getName());
                fieldReference.putUserData(Keys.MEMBER_REFERENCE, fieldOperand);
                return fieldReference;
            }

            case PutField: {
                final MemberReferenceExpression fieldReference = arg1.member(fieldOperand.getName());
                fieldReference.putUserData(Keys.MEMBER_REFERENCE, fieldOperand);
                return new AssignmentExpression(fieldReference, arg2);
            }

            case InvokeVirtual:
                return transformCall(true, byteCode, arguments);
            case InvokeSpecial:
            case InvokeStatic:
                return transformCall(false, byteCode, arguments);
            case InvokeInterface:
                return transformCall(false, byteCode, arguments);
            case InvokeDynamic:
                return transformCall(false, byteCode, arguments);

            case ArrayLength:
                return arg1.member("length");

            case AThrow:
                return new ThrowStatement(arg1);

            case CheckCast:
                return new CastExpression(operandType, arg1);

            case InstanceOf:
                return new InstanceOfExpression(arg1, operandType);

            case MonitorEnter:
            case MonitorExit:
            case MultiANewArray:
                break;

            case Breakpoint:
                return null;

            case Load: {
                if (!variableOperand.isParameter()) {
                    _localVariablesToDefine.add(variableOperand);
                }
                if (variableOperand.isParameter() && variableOperand.getOriginalParameter().getPosition() < 0) {
                    return new ThisReferenceExpression();
                }
                final IdentifierExpression name = new IdentifierExpression(variableOperand.getName());
                name.putUserData(Keys.VARIABLE, variableOperand);
                return name;
            }

            case Store: {
                if (!variableOperand.isParameter()) {
                    _localVariablesToDefine.add(variableOperand);
                }
                final IdentifierExpression name = new IdentifierExpression(variableOperand.getName());
                name.putUserData(Keys.VARIABLE, variableOperand);
                return new AssignmentExpression(name, arg1);
            }

            case LoadElement: {
                return new IndexerExpression(arg1, arg2);
            }
            case StoreElement: {
                return new AssignmentExpression(
                    new IndexerExpression(arg1, arg2),
                    arg3
                );
            }

            case Add:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.ADD, arg2);
            case Sub:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.SUBTRACT, arg2);
            case Mul:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.MULTIPLY, arg2);
            case Div:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.DIVIDE, arg2);
            case Rem:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.MODULUS, arg2);
            case Neg:
                return new UnaryOperatorExpression(UnaryOperatorType.MINUS, arg1);
            case Shl:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.SHIFT_LEFT, arg2);
            case Shr:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.SHIFT_RIGHT, arg2);
            case UShr:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.UNSIGNED_SHIFT_RIGHT, arg2);
            case And:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.BITWISE_AND, arg2);
            case Or:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.BITWISE_OR, arg2);
            case Not:
                return new UnaryOperatorExpression(UnaryOperatorType.NOT, arg1);
            case Xor:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.EXCLUSIVE_OR, arg2);

            case Inc: {
                if (!variableOperand.isParameter()) {
                    _localVariablesToDefine.add(variableOperand);
                }

                final IdentifierExpression name = new IdentifierExpression(variableOperand.getName());

                name.getIdentifierToken().putUserData(Keys.VARIABLE, variableOperand);
                name.putUserData(Keys.VARIABLE, variableOperand);

                final PrimitiveExpression deltaExpression = (PrimitiveExpression) arg1;
                final int delta = (Integer) deltaExpression.getValue();

                switch (delta) {
                    case -1:
                        return new UnaryOperatorExpression(UnaryOperatorType.DECREMENT, name);
                    case 1:
                        return new UnaryOperatorExpression(UnaryOperatorType.INCREMENT, name);
                    default:
                        return new AssignmentExpression(name, AssignmentOperatorType.ADD, arg1);
                }
            }

            case CmpEq:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.EQUALITY, arg2);
            case CmpNe:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.INEQUALITY, arg2);
            case CmpLt:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.LESS_THAN, arg2);
            case CmpGe:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.GREATER_THAN_OR_EQUAL, arg2);
            case CmpGt:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.GREATER_THAN, arg2);
            case CmpLe:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.LESS_THAN_OR_EQUAL, arg2);

            case Return:
                return new ReturnStatement(arg1);

            case NewArray: {
                final ArrayCreationExpression arrayCreation = new ArrayCreationExpression();
                arrayCreation.setType(operandType);
                arrayCreation.setDimension(arg1);
                return arrayCreation;
            }

            case LogicalNot:
                return new UnaryOperatorExpression(UnaryOperatorType.NOT, arg1);

            case LogicalAnd:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.LOGICAL_AND, arg2);
            case LogicalOr:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.LOGICAL_OR, arg2);

            case InitObject:
                return transformCall(false, byteCode, arguments);

            case InitArray: {
                final ArrayCreationExpression arrayInitialization = new ArrayCreationExpression();
                arrayInitialization.setType(operandType);
                arrayInitialization.setInitializer(new ArrayInitializerExpression(arguments));
                return arrayInitialization;
            }

            case Wrap:
                return null;

            case TernaryOp:
                return new ConditionalExpression(arg1, arg2, arg3);

            case LoopOrSwitchBreak:
                return label != null ? new BreakStatement(label.getName()) : new BreakStatement();

            case LoopContinue:
                return label != null ? new ContinueStatement(label.getName()) : new ContinueStatement();

            case CompoundAssignment:
                throw ContractUtils.unreachable();

            case PostIncrement:
                final Integer incrementAmount = (Integer) operand;
                if (incrementAmount < 0) {
                    return new UnaryOperatorExpression(UnaryOperatorType.POST_DECREMENT, arg1);
                }
                return new UnaryOperatorExpression(UnaryOperatorType.POST_INCREMENT, arg1);

            case Box:
            case Unbox:
                throw ContractUtils.unreachable();

            case Leave:
                return null;

            case DefaultValue:
                return AstBuilder.makeDefaultValue((TypeReference) operand);
        }

        final Expression inlinedAssembly = inlineAssembly(byteCode, arguments);

        if (isTopLevel) {
            return new CommentStatement(" " + inlinedAssembly.toString());
        }

        return inlinedAssembly;
    }

    private AstNode transformCall(
        final boolean isVirtual,
        final com.strobel.decompiler.ast.Expression byteCode,
        final List<Expression> arguments) {

        final MethodReference methodReference = (MethodReference) byteCode.getOperand();
        final MethodDefinition methodDefinition = methodReference.resolve();

        final boolean hasThis = byteCode.getCode() == AstCode.InvokeVirtual ||
                                byteCode.getCode() == AstCode.InvokeInterface ||
                                byteCode.getCode() == AstCode.InvokeSpecial;

        Expression target;

        final TypeReference declaringType = methodDefinition != null ? methodDefinition.getDeclaringType()
                                                                     : methodReference.getDeclaringType();

        if (hasThis) {
            target = arguments.remove(0);

            if (methodDefinition != null) {
                if (target instanceof NullReferenceExpression) {
                    target = new CastExpression(AstBuilder.convertType(declaringType), target);
                }
            }
        }
        else if (byteCode.getCode() == AstCode.InvokeStatic &&
                 declaringType.isEquivalentTo(_context.getCurrentType())) {

            target = Expression.NULL;
        }
        else {
            target = new TypeReferenceExpression(AstBuilder.convertType(declaringType));
        }

        if (target instanceof ThisReferenceExpression) {
            if (!isVirtual && !declaringType.isEquivalentTo(_method.getDeclaringType())) {
                target = new SuperReferenceExpression();
            }
        }
        else if (methodReference.isConstructor()) {
            final ObjectCreationExpression creation;
            final TypeDefinition resolvedType = declaringType.resolve();

            if (resolvedType != null && resolvedType.isAnonymous()) {
                final AstType declaredType;

                if (resolvedType.getExplicitInterfaces().isEmpty()) {
                    declaredType = AstBuilder.convertType(resolvedType.getBaseType());
                }
                else {
                    declaredType = AstBuilder.convertType(resolvedType.getExplicitInterfaces().get(0));
                }

                creation = new AnonymousObjectCreationExpression(
                    _astBuilder.createType(resolvedType).clone(),
                    declaredType
                );
            }
            else {
                creation = new ObjectCreationExpression(AstBuilder.convertType(declaringType));
            }

            creation.getArguments().addAll(adjustArgumentsForMethodCall(methodReference, arguments));
            creation.putUserData(Keys.MEMBER_REFERENCE, methodReference);

            return creation;
        }

        final InvocationExpression invocation;

        if (methodReference.isConstructor()) {
            invocation = new InvocationExpression(
                target,
                adjustArgumentsForMethodCall(methodReference, arguments)
            );
        }
        else {
            invocation = target.invoke(
                methodReference.getName(),
                convertTypeArguments(methodReference),
                adjustArgumentsForMethodCall(methodReference, arguments)
            );
        }

        invocation.putUserData(Keys.MEMBER_REFERENCE, methodReference);

        return invocation;
    }

    @SuppressWarnings("UnusedParameters")
    private List<AstType> convertTypeArguments(final MethodReference methodReference) {
        //
        // TODO: Convert type arguments.
        //
        return Collections.emptyList();
    }

    @SuppressWarnings("UnusedParameters")
    private List<Expression> adjustArgumentsForMethodCall(final MethodReference method, final List<Expression> arguments) {
        if (!arguments.isEmpty() && method.isConstructor()) {
            final TypeDefinition declaringType = method.getDeclaringType().resolve();

            if (declaringType != null &&
                !declaringType.isStatic() &&
                (declaringType.isInnerClass() || declaringType.isLocalClass())) {

                return arguments.subList(1, arguments.size());
            }
        }
        //
        // TODO: Convert arguments.
        //
        return arguments;
    }

    private static Expression inlineAssembly(final com.strobel.decompiler.ast.Expression byteCode, final List<Expression> arguments) {
        if (byteCode.getOperand() != null) {
            arguments.add(0, new IdentifierExpression(formatByteCodeOperand(byteCode.getOperand())));
        }
        return new IdentifierExpression(byteCode.getCode().getName()).invoke(arguments);
    }

    private static String formatByteCodeOperand(final Object operand) {
        if (operand == null) {
            return StringUtilities.EMPTY;
        }
        else if (operand instanceof MethodReference) {
            return ((MethodReference) operand).getName() + "()";
        }
        else if (operand instanceof TypeReference) {
            return ((TypeReference) operand).getFullName();
        }
        else if (operand instanceof VariableDefinition) {
            return ((VariableDefinition) operand).getName();
        }
        else if (operand instanceof ParameterDefinition) {
            return ((ParameterDefinition) operand).getName();
        }
        else if (operand instanceof FieldReference) {
            return ((FieldReference) operand).getName();
        }
        else if (operand instanceof String) {
            return JavaOutputVisitor.convertString((String) operand, true);
        }
        else {
            return String.valueOf(operand);
        }
    }
}
