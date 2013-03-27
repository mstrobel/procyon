/*
 * AstMethodBodyBuilder.java
 *
 * Copyright (c) 2013 Mike Strobel
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
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.*;
import com.strobel.reflection.SimpleType;
import com.strobel.util.ContractUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.strobel.core.CollectionUtilities.firstOrDefault;

/**
 * @author strobelm
 */
public class AstMethodBodyBuilder {
    private final MethodDefinition _method;
    private final DecompilerContext _context;
    private final Set<Variable> _localVariablesToDefine = new LinkedHashSet<>();

    public static BlockStatement createMethodBody(
        final MethodDefinition method,
        final DecompilerContext context,
        final Iterable<ParameterDeclaration> parameters) {

        VerifyArgument.notNull(method, "method");
        VerifyArgument.notNull(context, "context");

        final MethodDefinition oldCurrentMethod = context.getCurrentMethod();

        assert oldCurrentMethod == null || oldCurrentMethod == method;

        context.setCurrentMethod(method);

        try {
            final AstMethodBodyBuilder builder = new AstMethodBodyBuilder(method, context);
            return builder.createMethodBody(parameters);
        }
        finally {
            context.setCurrentMethod(oldCurrentMethod);
        }
    }

    private AstMethodBodyBuilder(final MethodDefinition method, final DecompilerContext context) {
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

        final BlockStatement astBlock = transformBlock(method);
        final AstNodeCollection<Statement> statements = astBlock.getStatements();
        final Statement insertionPoint = firstOrDefault(statements);

        for (final Variable v : _localVariablesToDefine) {
            final AstType type = AstBuilder.convertType(v.getType());
            final VariableDeclarationStatement declaration = new VariableDeclarationStatement(type, v.getName());

            statements.insertBefore(insertionPoint, declaration);
        }

        return astBlock;
    }

    private BlockStatement transformBlock(final Block block) {
        final BlockStatement astBlock = new BlockStatement();

        if (block != null) {
            for (final Node node : block.getChildren()) {
                astBlock.getStatements().add(transformNode(node));
            }
        }

        return astBlock;
    }

    private Statement transformNode(final Node node) {
        if (node instanceof Label) {
            return new LabelStatement(((Label) node).getName());
        }

        if (node instanceof Block) {
            return transformBlock((Block) node);
        }

        if (node instanceof com.strobel.decompiler.ast.Expression) {
            final List<Range> ranges = new ArrayList<>();

            final List<com.strobel.decompiler.ast.Expression> childExpressions = node.getSelfAndChildrenRecursive(
                com.strobel.decompiler.ast.Expression.class
            );

            for (final com.strobel.decompiler.ast.Expression e : childExpressions) {
                ranges.addAll(e.getRanges());
            }

            @SuppressWarnings("UnusedDeclaration")
            final List<Range> orderedAndJoinedRanges = Range.orderAndJoint(ranges);
            final AstNode codeExpression = transformExpression((com.strobel.decompiler.ast.Expression) node);

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
                whileStatement.setCondition((Expression) transformExpression(loopCondition));
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
                (Expression) transformExpression(testCondition),
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
            final SwitchStatement switchStatement = new SwitchStatement((Expression) transformExpression(testCondition));

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
            }

            return switchStatement;
        }

        if (node instanceof TryCatchBlock) {
            return transformBlock(((TryCatchBlock) node).getTryBlock());
        }

        throw new IllegalArgumentException("Unknown node type: " + node);
    }

    private AstNode transformExpression(final com.strobel.decompiler.ast.Expression e) {
        return transformByteCode(e);
    }

    @SuppressWarnings("ConstantConditions")
    private AstNode transformByteCode(final com.strobel.decompiler.ast.Expression byteCode) {
        final Object operand = byteCode.getOperand();
        final AstType operandType = operand instanceof TypeReference ? AstBuilder.convertType((TypeReference) operand) : AstType.NULL;
        final Variable variableOperand = operand instanceof Variable ? (Variable) operand : null;
        final FieldReference fieldOperand = operand instanceof FieldReference ? (FieldReference) operand : null;

        final List<Expression> arguments = new ArrayList<>();

        for (final com.strobel.decompiler.ast.Expression e : byteCode.getArguments()) {
            arguments.add((Expression) transformExpression(e));
        }

        final Expression arg1 = arguments.size() >= 1 ? arguments.get(0) : null;
        final Expression arg2 = arguments.size() >= 2 ? arguments.get(1) : null;
        final Expression arg3 = arguments.size() >= 3 ? arguments.get(2) : null;

        switch (byteCode.getCode()) {
            case Nop:
                return null;

            case AConstNull:
                return new NullReferenceExpression();

            case LdC:
                return new PrimitiveExpression(arg1);

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
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Long, arg1));
            case I2F:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Float, arg1));
            case I2D:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Double, arg1));
            case L2I:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Integer, arg1));
            case L2F:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Float, arg1));
            case L2D:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Double, arg1));
            case F2I:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Integer, arg1));
            case F2L:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Long, arg1));
            case F2D:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Double, arg1));
            case D2I:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Integer, arg1));
            case D2L:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Long, arg1));
            case D2F:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Float, arg1));
            case I2B:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Byte, arg1));
            case I2C:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Character, arg1));
            case I2S:
                return new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Short, arg1));

            case GetStatic:
            case PutStatic:
                return AstBuilder.convertType(fieldOperand.getDeclaringType()).member(fieldOperand.getName());

            case GetField:
            case PutField:
                return arg1.member(fieldOperand.getName());

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
                throw ContractUtils.unreachable();

            case Breakpoint:
                return null;

            case Load:
                if (!variableOperand.isParameter()) {
                    _localVariablesToDefine.add(variableOperand);
                }
                if (variableOperand.isParameter() && variableOperand.getOriginalParameter().getPosition() < 0) {
                    return new ThisReferenceExpression();
                }
                return new IdentifierExpression(variableOperand.getName());

            case Store:
                if (!variableOperand.isParameter()) {
                    _localVariablesToDefine.add(variableOperand);
                }
                return new IdentifierExpression(variableOperand.getName());

            case LoadElement:
            case StoreElement:
                return new IndexerExpression(
                    arg1,
                    new PrimitiveExpression(JavaPrimitiveCast.cast(SimpleType.Integer, operand))
                );

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
            case Inc:
                return new UnaryOperatorExpression(UnaryOperatorType.POST_INCREMENT, arg1);
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

            case NewArray:
                final ArrayCreationExpression arrayCreation = new ArrayCreationExpression();
                arrayCreation.setType(operandType);
                arrayCreation.setDimension(arg1);
                return arrayCreation;

            case LogicalNot:
                break;

            case LogicalAnd:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.LOGICAL_AND, arg2);
            case LogicalOr:
                return new BinaryOperatorExpression(arg1, BinaryOperatorType.LOGICAL_OR, arg2);

            case InitObject:
                return transformCall(false, byteCode, arguments);

            case InitArray:
                final ArrayCreationExpression arrayInitialization = new ArrayCreationExpression();
                arrayInitialization.setType(operandType);
                arrayInitialization.setInitializer(new ArrayInitializerExpression(arguments));
                return arrayInitialization;

            case Wrap:
                return null;

            case TernaryOp:
                return new ConditionalExpression(arg1, arg2, arg3);

            case LoopOrSwitchBreak:
                return new BreakStatement();

            case LoopContinue:
                return new ContinueStatement();

            case CompoundAssignment:
                throw ContractUtils.unreachable();

            case PostIncrement:
                return new UnaryOperatorExpression(UnaryOperatorType.POST_INCREMENT, arg1);

            case Box:
            case Unbox:
                throw ContractUtils.unreachable();

            case Leave:
                return null;

            case DefaultValue:
                return AstBuilder.makeDefaultValue((TypeReference)operand);
        }

        throw ContractUtils.unsupported();
    }

    private AstNode transformCall(
        final boolean isVirtual,
        final com.strobel.decompiler.ast.Expression byteCode,
        final List<Expression> arguments) {

        final MethodReference methodReference = (MethodReference)byteCode.getOperand();
        final MethodDefinition methodDefinition = methodReference.resolve();

        final boolean hasThis = byteCode.getCode() == AstCode.InvokeVirtual ||
                                byteCode.getCode() == AstCode.InvokeInterface;

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
        else {
            target = new TypeReferenceExpression(AstBuilder.convertType(declaringType));
        }

        if (target instanceof ThisReferenceExpression && !isVirtual) {
            if (!declaringType.isEquivalentTo(_method.getDeclaringType())) {
                target = new SuperReferenceExpression();
            }
        }

        if (methodReference.isConstructor()) {
            final ObjectCreationExpression creation = new ObjectCreationExpression(
                AstBuilder.convertType(declaringType)
            );

            creation.getArguments().addAll(adjustArgumentsForMethodCall(methodReference, arguments));

            return creation;
        }
        
        return target.invoke(
            methodReference.getName(),
            convertTypeArguments(methodReference),
            adjustArgumentsForMethodCall(methodReference, arguments)
        );
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
        //
        // TODO: Convert arguments.
        //
        return arguments;
    }
}
