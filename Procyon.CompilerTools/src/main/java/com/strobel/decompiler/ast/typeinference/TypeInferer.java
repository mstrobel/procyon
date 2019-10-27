package com.strobel.decompiler.ast.typeinference;

import com.strobel.assembler.metadata.*;
import com.strobel.core.StringComparison;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.Label;
import com.strobel.decompiler.ast.*;

import java.util.*;

public class TypeInferer {
    public static final TypeDefinition INT_OR_BOOLEAN = new TypeDefinition() {
        @Override
        public String getSimpleName() {
            return "int|boolean";
        }

        @Override
        public String getName() {
            return "int|boolean";
        }

        @Override
        public boolean isPrimitive() {
            return true;
        }

        @Override
        public <R, P> R accept(TypeMetadataVisitor<P, R> visitor, P parameter) {
            return visitor.visitClassType(this, parameter);
        }
    };

    public static final TypeDefinition NUMERIC = new TypeDefinition() { // TODO: "or" types
        @Override
        public String getSimpleName() {
            return "int|long|float|double";
        }

        @Override
        public String getName() {
            return "int|long|float|double";
        }

        @Override
        public boolean isPrimitive() {
            return true;
        }

        @Override
        public <R, P> R accept(TypeMetadataVisitor<P, R> visitor, P parameter) {
            return visitor.visitClassType(this, parameter);
        }
    };

    private final DecompilerContext context;
    private final ConstraintSolver constraints = new ConstraintSolver();
    private Set<Expression> inferred = new HashSet<>();

    public TypeInferer(DecompilerContext context) {
        this.context = context;
    }

    public void solve(Block block) {
        print("Inferring " + context.getCurrentMethod());
        print("");
        print(block);
        print("");
        long startTime = System.nanoTime();

        findConstraintsFor(block);

        Collection<EquivalenceSet> types = constraints.solve();

        for (EquivalenceSet equivalenceSet : types) {
            for (Expression expression : equivalenceSet.expressions) {
                expression.setInferredType(expression.getInferredType() != null ? expression.getInferredType() : equivalenceSet.solution);
                expression.setExpectedType(expression.getExpectedType() != null ? expression.getExpectedType() : equivalenceSet.solution);
            }

            for (Variable variable : equivalenceSet.variables) {
                variable.setType(equivalenceSet.solution == null ? BuiltinTypes.Void : equivalenceSet.solution);
            }
        }
        print((System.nanoTime() - startTime) / 1000000d + " ms");
        print("");
        print("");
        print("");
        print("");
        print("");
    }

    public void findConstraintsFor(Block block) {
        for (Node node : block.getBody()) {
            if (node instanceof BasicBlock) {
                findConstraintsFor((BasicBlock) node);
            } else {
                throw new RuntimeException("Unknown node: " + block);
            }
        }
    }

    public void findConstraintsFor(BasicBlock block) {
        for (Node node : block.getBody()) {
            if (node instanceof BasicBlock) {
                findConstraintsFor((BasicBlock) node);
            } else if (node instanceof Block) {
                findConstraintsFor((Block) node);
            } else if (node instanceof Expression) {
                findConstraintsFor((Expression) node);
            } else if (node instanceof TryCatchBlock) {
                TryCatchBlock tryCatchBlock = (TryCatchBlock) node;
                findConstraintsFor(tryCatchBlock.getTryBlock());
                for (CatchBlock catchBlock : tryCatchBlock.getCatchBlocks()) {
                    constraints.addEquality(catchBlock.getExceptionVariable(), catchBlock.getExceptionType());
                    findConstraintsFor(catchBlock);
                }
                if (tryCatchBlock.getFinallyBlock() != null) {
                    findConstraintsFor(tryCatchBlock.getFinallyBlock());
                }
            } else if (!(node instanceof Label)) {
                throw new RuntimeException("Unknown node: " + block);
            }
        }
    }

    private void findConstraintsFor(Expression expression) {
        if (!inferred.add(expression)) return;

        if (expression.getOperand() instanceof Expression) {
            findConstraintsFor((Expression) expression.getOperand());
        }

        for (Expression argument : expression.getArguments()) {
            findConstraintsFor(argument);
        }

        AstCode code = expression.getCode();
        switch (code) {
            case LdC: {
                Object constant = expression.getOperand();
                if (constant instanceof String) {
                    constraints.addEquality(expression, CommonTypeReferences.String);
                } else if (constant instanceof TypeReference) {
                    constraints.addEquality(expression, CommonTypeReferences.Class.resolve().makeGenericType((TypeReference) constant));
                } else if (constant.getClass() == Integer.class) { // Integer can be an 'int' or 'boolean'
                    if ((Integer) constant != 0 && (Integer) constant != 1) {
                        constraints.addEquality(expression, BuiltinTypes.Integer);
                    } else {
                        constraints.addExtends(expression, INT_OR_BOOLEAN);
                    }
                } else {
                    constraints.addEquality(expression, BuiltinTypes.forClass(constant.getClass()));
                }
                break;
            }

            case Store: {
                constraints.addExtends(expression.getArguments().get(0), expression.getOperand());
                break;
            }

            case Load: {
                Variable variable = (Variable) expression.getOperand();
                if (variable.isParameter()) {
                    // Parameters have the correct generic types, generics don't need to be renamed
                    ParameterDefinition parameter = variable.getOriginalParameter();
                    TypeReference parameterType = parameter.getParameterType();
                    constraints.addEquality(variable, parameterType);
                }

                constraints.addEquality(expression, variable);
                break;
            }

            case CmpEq:
            case CmpNe:
            case CmpLt:
            case CmpLe:
            case CmpGt:
            case CmpGe: {
                constraints.addEquality(expression.getArguments().get(0), expression.getArguments().get(1));
                constraints.addEquality(expression, BuiltinTypes.Boolean);
                break;
            }

            case InvokeSpecial:
            case InvokeStatic:
            case InvokeInterface:
            case InvokeVirtual: {
                MethodReference methodRef = (MethodReference) expression.getOperand();
                MethodDefinition method = methodRef.resolve();
                Expression thisArgument = code != AstCode.InvokeStatic && code != AstCode.InvokeDynamic ? expression.getArguments().get(0) : null;

                TypeVariableRenamingVisitor renamingVisitor = new TypeVariableRenamingVisitor();

                if (thisArgument != null) {
                    constraints.addExtends(thisArgument, renamingVisitor.visit(method.getDeclaringType()));
                }

                for (int i = 0; i < method.getParameters().size(); i++) {
                    Expression argument = expression.getArguments().get(thisArgument != null ? i + 1 : i);
                    TypeReference parameterType = method.getParameters().get(i).getParameterType();
                    constraints.addExtends(argument, renamingVisitor.visit(parameterType));
                }

                constraints.addEquality(expression, renamingVisitor.visit(method.getReturnType())); // TODO: extends (for cast)
                break;
            }

            case InvokeDynamic: {
                DynamicCallSite callSite = (DynamicCallSite) expression.getOperand();
                MethodReference bootstrapMethod = callSite.getBootstrapMethod();

                if ("java/lang/invoke/LambdaMetafactory".equals(bootstrapMethod.getDeclaringType().getInternalName()) &&
                    StringUtilities.equals("metafactory", bootstrapMethod.getName(), StringComparison.OrdinalIgnoreCase) &&
                    callSite.getBootstrapArguments().size() == 3 &&
                    callSite.getBootstrapArguments().get(1) instanceof MethodHandle) {

                    MethodHandle targetHandle = (MethodHandle) callSite.getBootstrapArguments().get(1);
                    MethodDefinition lambdaMethod = targetHandle.getMethod().resolve();

                    TypeDefinition interfaceType = callSite.getMethodType().getReturnType().resolve();

                    final List<MethodReference> methods = MetadataHelper.findMethods(interfaceType, MetadataFilters.matchName(callSite.getMethodName()));

                    MethodDefinition interfaceMethod = null;
                    for (MethodReference method : methods) {
                        MethodDefinition resolvedMethod = method.resolve();
                        if (resolvedMethod != null && resolvedMethod.isAbstract() && !resolvedMethod.isStatic() && !resolvedMethod.isDefault()) {
                            interfaceMethod = resolvedMethod;
                            break;
                        }
                    }

                    TypeVariableRenamingVisitor renamingVisitor = new TypeVariableRenamingVisitor();

                    // Get all the lambda argument parameters, including 'this'
                    List<TypeReference> argumentTypes = new ArrayList<>();
                    if (lambdaMethod.hasThis()) {
                        argumentTypes.add(renamingVisitor.visit(lambdaMethod.getDeclaringType().resolve()));
                    }
                    for (ParameterDefinition parameterDefinition : lambdaMethod.getParameters()) {
                        argumentTypes.add(renamingVisitor.visit(parameterDefinition.getParameterType().resolve()));
                    }

                    int interfaceParameterCount = interfaceMethod.getParameters().size();
                    int invokedynamicArgumentCount = expression.getArguments().size();
                    if (invokedynamicArgumentCount + interfaceParameterCount != argumentTypes.size()) {
                        throw new IllegalStateException("Lambda parameter counts don't match");
                    }

                    // Match captured variables with invokedynamic arguments
                    for (int i = 0; i < invokedynamicArgumentCount; i++) {
                        constraints.addExtends(expression.getArguments().get(i), argumentTypes.get(i));
                    }

                    // Match remaining arguments with interface arguments
                    for (int i = 0; i < interfaceParameterCount; i++) {
                        TypeReference interfaceParam = renamingVisitor.visit(interfaceMethod.getParameters().get(i).getParameterType().resolve());
                        constraints.addExtends(interfaceParam, argumentTypes.get(i + invokedynamicArgumentCount));
                    }

                    // Interface return type extends lambda return type. This is because type parameters are
                    // dropped in lambda signatures ('T' -> 'Object', 'T extends String' -> 'String', etc.)
                    // TODO: Infer the actual return type by inferring the lambda method in this TypeInferer
                    if (targetHandle.getHandleType() == MethodHandleType.NewInvokeSpecial) { // TODO: extends (for cast)
                        constraints.addEquality(renamingVisitor.visit(interfaceMethod.getReturnType()), renamingVisitor.visit(lambdaMethod.getDeclaringType().resolve()));
                    } else {
                        constraints.addEquality(renamingVisitor.visit(interfaceMethod.getReturnType()), renamingVisitor.visit(lambdaMethod.getReturnType()));
                    }

                    // Type of expresion is interfaceType
                    constraints.addEquality(expression, renamingVisitor.visit(interfaceType));
                }
                break;
            }

            case IfTrue: {
                constraints.addEquality(expression.getArguments().get(0), BuiltinTypes.Boolean);
                break;
            }

            case LogicalNot:
            case LogicalAnd:
            case LogicalOr: {
                constraints.addEquality(expression.getOperand(), BuiltinTypes.Boolean);
                for (Expression arg : expression.getArguments()) {
                    constraints.addEquality(arg, BuiltinTypes.Boolean);
                }
                break;
            }

            case Goto: {
                break;
            }

            case Return: { // TODO: resolve?
                if (expression.getArguments().size() > 0) {
                    constraints.addExtends(expression.getArguments().get(0), context.getCurrentMethod().getReturnType());
                }
                break;
            }

            case __New: {
                TypeDefinition resolved = ((TypeReference) expression.getOperand()).resolve();
                constraints.addEquality(expression, new TypeVariableRenamingVisitor().visit(resolved)); // TODO: extends (for cast)
                break;
            }

            case GetField: {
                TypeVariableRenamingVisitor renamingVisitor = new TypeVariableRenamingVisitor();
                TypeReference fieldType = renamingVisitor.visit(((FieldReference) expression.getOperand()).resolve().getFieldType());
                TypeReference fieldOwner = renamingVisitor.visit(((FieldReference) expression.getOperand()).resolve().getDeclaringType());
                constraints.addExtends(expression.getArguments().get(0), fieldOwner); // TODO: Use access level?
                constraints.addEquality(expression, fieldType); // TODO: extends (for cast)
                break;
            }

            case PutField: {
                TypeVariableRenamingVisitor renamingVisitor = new TypeVariableRenamingVisitor();
                TypeReference fieldType = renamingVisitor.visit(((FieldReference) expression.getOperand()).resolve().getFieldType());
                TypeReference fieldOwner = renamingVisitor.visit(((FieldReference) expression.getOperand()).resolve().getDeclaringType());
                constraints.addExtends(expression.getArguments().get(0), fieldOwner); // TODO: Use access level?
                constraints.addExtends(expression.getArguments().get(1), fieldType);
                break;
            }

            case GetStatic: {
                TypeVariableRenamingVisitor renamingVisitor = new TypeVariableRenamingVisitor();
                TypeReference fieldType = renamingVisitor.visit(((FieldReference) expression.getOperand()).resolve().getFieldType());
                constraints.addEquality(expression, fieldType);
                break;
            }

            case PutStatic: {
                TypeVariableRenamingVisitor renamingVisitor = new TypeVariableRenamingVisitor();
                TypeReference fieldType = renamingVisitor.visit(((FieldReference) expression.getOperand()).resolve().getFieldType()); // TODO: resolve necessary for equality but what about generics
                constraints.addExtends(expression.getArguments().get(0), fieldType);
                break;
            }

            case Sub:
            case Add:
            case Or:
            case And:
            case Mul:
            case Div:
            case Shl:
            case Shr:
            case UShr:
            case Rem:
            case Xor: {
                constraints.addEquality(expression.getArguments().get(0), expression.getArguments().get(1));
                constraints.addEquality(expression, expression.getArguments().get(1));
                constraints.addExtends(expression, NUMERIC); // Equality makes sure args extend numeric too
                break;
            }

            case AConstNull: {
                // TODO: use Null type?
                // that would require extra code to prevent (Null <= T) assigining Null to T
                constraints.addExtends(expression, BuiltinTypes.Object);
                break;
            }

            case CheckCast: {
                if (InferenceSettings.I_USE_CHECKCAST_TARGET) {
                    TypeReference targetType = (TypeReference) expression.getOperand();
                    constraints.addExtends(expression, new TypeVariableRenamingVisitor().visit(targetType.resolve()));
                }
                // TODO: ignoring casts produces better results with generics, but may cause incorrect results.
                constraints.addEquality(expression, expression.getArguments().get(0));
                break;
            }

            case I2B:
            case I2S:
            case I2C:
            case I2L:
            case I2D:
            case I2F:
            case D2F:
            case D2I:
            case D2L:
            case L2F:
            case L2I:
            case L2D:
            case F2D:
            case F2I:
            case F2L: {
                if (code == AstCode.I2B || code == AstCode.I2S || code == AstCode.I2C ||
                    code == AstCode.I2L || code == AstCode.I2D || code == AstCode.I2F) {
                    constraints.addEquality(expression.getArguments().get(0), BuiltinTypes.Integer);
                } else if (code == AstCode.D2F || code == AstCode.D2I || code == AstCode.D2L) {
                    constraints.addEquality(expression.getArguments().get(0), BuiltinTypes.Double);
                } else if (code == AstCode.L2D || code == AstCode.L2F || code == AstCode.L2I) {
                    constraints.addEquality(expression.getArguments().get(0), BuiltinTypes.Long);
                } else if (code == AstCode.F2D || code == AstCode.F2I || code == AstCode.F2L) {
                    constraints.addEquality(expression.getArguments().get(0), BuiltinTypes.Float);
                } else {
                    throw new RuntimeException("Forgot to add code " + code);
                }

                if (code == AstCode.D2I || code == AstCode.F2I || code == AstCode.L2I) {
                    constraints.addEquality(expression, BuiltinTypes.Integer);
                } else if (code == AstCode.I2D || code == AstCode.F2D || code == AstCode.L2D) {
                    constraints.addEquality(expression, BuiltinTypes.Double);
                } else if (code == AstCode.I2F || code == AstCode.D2F || code == AstCode.L2F) {
                    constraints.addEquality(expression, BuiltinTypes.Float);
                } else if (code == AstCode.I2L || code == AstCode.D2L || code == AstCode.F2L) {
                    constraints.addEquality(expression, BuiltinTypes.Long);
                } else if (code == AstCode.I2B) {
                    constraints.addEquality(expression, BuiltinTypes.Byte);
                } else if (code == AstCode.I2S) {
                    constraints.addEquality(expression, BuiltinTypes.Short);
                } else if (code == AstCode.I2C) {
                    constraints.addEquality(expression, BuiltinTypes.Character);
                } else {
                    throw new RuntimeException("Forgot to add code " + code);
                }

                break;
            }

            case NewArray: {
                constraints.addEquality(expression.getArguments().get(0), BuiltinTypes.Integer);
                TypeReference elementType = new TypeVariableRenamingVisitor().visit(((TypeReference) expression.getOperand()).resolve());
                constraints.addEquality(expression, ArrayType.create(elementType));
                break;
            }

            case __LCmp: {
                constraints.addEquality(expression.getArguments().get(0), BuiltinTypes.Long);
                constraints.addEquality(expression.getArguments().get(1), BuiltinTypes.Long);
                constraints.addEquality(expression, BuiltinTypes.Boolean);
                break;
            }

            case __DCmpG:
            case __DCmpL: {
                constraints.addEquality(expression.getArguments().get(0), BuiltinTypes.Double);
                constraints.addEquality(expression.getArguments().get(1), BuiltinTypes.Double);
                constraints.addEquality(expression, BuiltinTypes.Boolean);
                break;
            }

            case __FCmpG:
            case __FCmpL: {
                constraints.addEquality(expression.getArguments().get(0), BuiltinTypes.Float);
                constraints.addEquality(expression.getArguments().get(1), BuiltinTypes.Float);
                constraints.addEquality(expression, BuiltinTypes.Boolean);
                break;
            }

            case InstanceOf: {
                constraints.addEquality(expression, BuiltinTypes.Boolean);
                break;
            }

            case Inc: {
                constraints.addEquality(expression.getOperand(), expression.getArguments().get(0));
                constraints.addExtends(expression.getOperand(), NUMERIC);
                break;
            }

            case AThrow: {
                constraints.addExtends(expression.getArguments().get(0), CommonTypeReferences.Throwable);
                break;
            }

            case LoadElement: {
                TypeReference elementType = new TypeVariableRenamingVisitor().visit(new GenericParameter("ElementType"));
                constraints.addEquality(expression.getArguments().get(0), ArrayType.create(elementType));
                constraints.addEquality(expression.getArguments().get(1), BuiltinTypes.Integer);
                constraints.addEquality(expression, elementType);
                break;
            }

            case StoreElement: {
                TypeReference elementType = new TypeVariableRenamingVisitor().visit(new GenericParameter("ElementType"));
                constraints.addEquality(expression.getArguments().get(0), ArrayType.create(elementType));
                constraints.addEquality(expression.getArguments().get(1), BuiltinTypes.Integer);
                constraints.addExtends(expression.getArguments().get(1), elementType);
                break;
            }

            case ArrayLength: {
                TypeReference elementType = new TypeVariableRenamingVisitor().visit(new GenericParameter("ElementType"));
                constraints.addExtends(expression.getArguments().get(0), ArrayType.create(elementType));
                constraints.addEquality(expression, BuiltinTypes.Integer);
                break;
            }

            case PreIncrement: {
                constraints.addEquality(expression, expression.getArguments().get(0));
                break;
            }

            case Switch: {
                constraints.addEquality(expression.getArguments().get(0), BuiltinTypes.Integer);
                break;
            }

            case MonitorEnter:
            case MonitorExit: {
                constraints.addExtends(expression.getArguments(), BuiltinTypes.Object);
                break;
            }

            case Neg: {
                constraints.addEquality(expression, expression.getArguments().get(0));
                break;
            }

            case Leave:
            case EndFinally: {
                break;
            }

            case MultiANewArray: { // TODO: resolve and box
                constraints.addEquality(expression, expression.getOperand());
                for (Expression e : expression.getArguments()) {
                    constraints.addEquality(e, BuiltinTypes.Integer);
                }
                break;
            }

            default: {
                throw new RuntimeException("Unknown expression: " + expression);
            }
        }
    }

    private static void print(Object o) {
        if (InferenceSettings.PRINT) {
            System.out.println(o);
        }
    }
}
