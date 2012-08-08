package com.strobel.expressions;

import com.strobel.compilerservices.Closure;
import com.strobel.compilerservices.DebugInfoGenerator;
import com.strobel.core.KeyedQueue;
import com.strobel.reflection.*;
import com.strobel.reflection.emit.*;
import com.strobel.reflection.emit.CodeGenerator;
import com.strobel.util.TypeUtils;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mike Strobel
 */
final class LambdaCompiler {
    final static AtomicInteger nextId  = new AtomicInteger();

    final LambdaExpression<?> lambda;
    final TypeBuilder typeBuilder;
    final MethodBuilder methodBuilder;
    final FieldBuilder closureField;
    final CodeGenerator generator;

    private final AnalyzedTree _tree;
    private final boolean _hasClosureArgument;
    private final KeyedQueue<Type<?>, LocalBuilder> _freeLocals;
    private final BoundConstants _boundConstants;
    private final Map<LabelTarget, LabelInfo> _labelInfo = new HashMap<>();

    private CompilerScope _scope;

    LambdaCompiler(final AnalyzedTree tree, final LambdaExpression<?> lambda) {
        this.lambda = lambda;

        typeBuilder = new TypeBuilder(
            "<>f__Lambda" + Integer.toHexString(nextId.incrementAndGet()),
            Modifier.PUBLIC | Modifier.FINAL,
            Types.Object,
            Type.list(lambda.getType())
        );

        final MethodInfo interfaceMethod = lambda.getType().getMethods().get(0);

        methodBuilder = typeBuilder.defineMethod(
            interfaceMethod.getName(),
            interfaceMethod.getModifiers(),
            interfaceMethod.getReturnType(),
            interfaceMethod.getParameters().getParameterTypes()
        );

        closureField = typeBuilder.defineField(
            "$closure",
            Type.of(Closure.class),
            Modifier.PRIVATE | Modifier.FINAL
        );

        generator = methodBuilder.getCodeGenerator();

        _tree = tree;
        _hasClosureArgument = true;
        _freeLocals = new KeyedQueue<>();
        _scope = tree.scopes.get(lambda);
        _boundConstants = tree.constants.get(lambda);
    }

    LambdaCompiler(final AnalyzedTree tree, final LambdaExpression<?> lambda, final MethodBuilder method) {
        this.lambda = lambda;

        _hasClosureArgument = tree.scopes.get(lambda).needsClosure;

        final TypeList parameterTypes = getParameterTypes(lambda);

        method.setReturnType(lambda.getReturnType());
        method.setParameters(parameterTypes);

        final ParameterExpressionList lambdaParameters = lambda.getParameters();
        final int startIndex = 1;

        for (int i = 0, n = lambdaParameters.size(); i < n; i++) {
            method.defineParameter(i + startIndex, lambdaParameters.get(i).getName());
        }

        this.typeBuilder = method.getDeclaringType();
        this.methodBuilder = method;

        this.closureField = typeBuilder.defineField(
            "$closure",
            Type.of(Closure.class),
            Modifier.PRIVATE | Modifier.FINAL
        );

        this.generator = methodBuilder.getCodeGenerator();

        _freeLocals = new KeyedQueue<>();
        _tree = tree;
        _scope = tree.scopes.get(lambda);
        _boundConstants = tree.constants.get(lambda);
    }

    private TypeList getParameterTypes(final LambdaExpression<?> lambda) {
        final ParameterExpressionList parameters = lambda.getParameters();

        if (parameters.isEmpty()) {
            return TypeList.empty();
        }

        final Type<?>[] types = new Type<?>[parameters.size()];

        for (int i = 0, n = parameters.size(); i < n; i++) {
            final ParameterExpression parameter = parameters.get(i);
            types[i] = parameter.getType();
        }

        return Type.list(types);
    }

    ParameterExpressionList getParameters() {
        return lambda.getParameters();
    }

    boolean canEmitBoundConstants() {
        return _hasClosureArgument;
    }

    boolean emitDebugSymbols() {
        return _tree.getDebugInfoGenerator() != null;
    }

    void emitClosureArgument() {
        assert _hasClosureArgument
            : "must have a Closure argument";
        assert methodBuilder.isStatic()
            : "must be a static method";

        generator.emitThis();
        generator.getField(closureField);
    }

    void emitLambdaArgument(final int index) {
        generator.emitLoadArgument(getLambdaArgument(index));
    }

    private void emitLambdaBody() {}

    void initializeMethod() {
        // See if we can find a return label, so we can emit better IL 
        addReturnLabel(lambda);
        _boundConstants.emitCacheConstants(this);
    }

    // See if this lambda has a return label
    // If so, we'll create it now and mark it as allowing the "ret" opcode
    // This allows us to generate better IL
    @SuppressWarnings("ConstantConditions")
    private void addReturnLabel(final LambdaExpression lambda) {
        Expression expression = lambda.getBody();

        while (true) {
            switch (expression.getNodeType()) {
                default:
                    // Didn't find return label.
                    return;

                case Label:
                    // Found the label.  We can directly return from this place only if
                    // the label type is reference assignable to the lambda return type.
                    final LabelTarget label = ((LabelExpression)expression).getTarget();

                    _labelInfo.put(
                        label,
                        new LabelInfo(
                            generator,
                            label,
                            TypeUtils.hasIdentityPrimitiveOrBoxingConversion(
                                lambda.getReturnType(),
                                label.getType()
                            )
                        )
                    );

                    return;

                case Block:
                    // Look in the last significant expression of a block.
                    final BlockExpression body = (BlockExpression)expression;

                    // Omit empty and debug info at the end of the block since they
                    // are not going to emit any bytecode.
                    for (int i = body.getExpressionCount() - 1; i >= 0; i--) {
                        expression = body.getExpression(i);
                        if (significant(expression)) {
                            break;
                        }
                    }

                    break;
            }
        }
    }

    private static boolean notEmpty(final Expression node) {
        return !(node instanceof DefaultValueExpression) ||
               node.getType() != PrimitiveTypes.Void;
    }

    private static boolean significant(final Expression node) {
        if (node instanceof BlockExpression) {
            final BlockExpression block = (BlockExpression)node;
            for (int i = 0; i < block.getExpressionCount(); i++) {
                if (significant(block.getExpression(i))) {
                    return true;
                }
            }
            return false;
        }

        return notEmpty(node)/* && !(node instanceof DebugInfoExpression)*/;
    }

    @SuppressWarnings("unchecked")
    static <T> Delegate<T> compile(
        final LambdaExpression<T> lambda,
        final DebugInfoGenerator debugInfoGenerator) {

        // 1. Bind lambda
        final AnalyzedTree tree = analyzeLambda(lambda);

        tree.setDebugInfoGenerator(debugInfoGenerator);

        // 2. Create lambda compiler
        final LambdaCompiler c = new LambdaCompiler(tree, lambda);

        // 3. Emit
        c.emitLambdaBody();

        final Type<T> generatedType = (Type<T>)c.typeBuilder.createType();
        final Class<T> generatedClass = generatedType.getErasedClass();

        try {
            final T instance = generatedClass.newInstance();
            return new Delegate<>(instance, generatedClass.getDeclaredMethods()[0]);
        }
        catch (InstantiationException | IllegalAccessException e) {
            throw Error.couldNotCreateDelegate(e);
        }
    }

    static void compile(
        final LambdaExpression<?> lambda,
        final MethodBuilder methodBuilder,
        final DebugInfoGenerator debugInfoGenerator) {

        // 1. Bind lambda
        final AnalyzedTree tree = analyzeLambda(lambda);

        tree.setDebugInfoGenerator(debugInfoGenerator);

        // 2. Create lambda compiler
        final LambdaCompiler c = new LambdaCompiler(tree, lambda, methodBuilder);

        // 3. Emit
        c.emitLambdaBody();
    }

    private static AnalyzedTree analyzeLambda(final LambdaExpression<?> lambda) {
        // Spill the stack for any exception handling blocks or other
        // constructs which require entering with an empty stack.
        final LambdaExpression<?> analyzedLambda = StackSpiller.analyzeLambda(lambda);

        // Bind any variable references in this lambda.
        return VariableBinder.bind(analyzedLambda);
    }

    LocalBuilder getNamedLocal(final Type type, final ParameterExpression variable) {
        assert type != null && variable != null
            : "type != null && variable != null";

        final LocalBuilder lb = generator.declareLocal(type);

        if (emitDebugSymbols() && variable.getName() != null) {
            _tree.getDebugInfoGenerator().setLocalName(lb, variable.getName());
        }

        return lb;
    }

    int getLambdaArgument(final int index) {
        return index + (methodBuilder.isStatic() ? 0 : 1);
    }

    LocalBuilder getLocal(final Type<?> type) {
        assert type != null
            : "type != null";

        final LocalBuilder local = _freeLocals.poll(type);

        if (local != null) {
            assert type.equals(local.getLocalType())
                : "type.equals(local.getLocalType())";

            return local;
        }

        return generator.declareLocal(type);
    }

    void freeLocal(final LocalBuilder local) {
        if (local != null) {
            _freeLocals.offer(local.getLocalType(), local);
        }
    }

/*

    private static final class CompilationFlags {
        private static final int EmitExpressionStart = 0x0001;
        private static final int EmitNoExpressionStart = 0x0002;
        private static final int EmitAsDefaultType = 0x0010;
        private static final int EmitAsVoidType = 0x0020;
        private static final int EmitAsTail = 0x0100;   // at the tail position of a lambda; tail call can be safely emitted
        private static final int EmitAsMiddle = 0x0200; // in the middle of a lambda; tail call can be emitted if it is in a return
        private static final int EmitAsNoTail = 0x0400; // neither at the tail or in a return; or tail call is not turned on; no tail call is emitted

        private static final int EmitExpressionStartMask = 0x000f;
        private static final int EmitAsTypeMask = 0x00f0;
        private static final int EmitAsTailCallMask = 0x0f00;
    }

    private static int updateEmitAsTailCallFlag(final int flags, final int newValue) {
        assert newValue == CompilationFlags.EmitAsTail ||
            newValue == CompilationFlags.EmitAsMiddle ||
            newValue == CompilationFlags.EmitAsNoTail;
        final int oldValue = flags & CompilationFlags.EmitAsTailCallMask;
        return flags ^ oldValue | newValue;
    }

    private static int updateEmitExpressionStartFlag(final int flags, final int newValue) {
        assert newValue == CompilationFlags.EmitExpressionStart ||
            newValue == CompilationFlags.EmitNoExpressionStart;
        final int oldValue = flags & CompilationFlags.EmitExpressionStartMask;
        return flags ^ oldValue | newValue;
    }

    private static int updateEmitAsTypeFlag(final int flags, final int newValue) {
        assert newValue == CompilationFlags.EmitAsDefaultType ||
            newValue == CompilationFlags.EmitAsVoidType;
        final int oldValue = flags & CompilationFlags.EmitAsTypeMask;
        return flags ^ oldValue | newValue;
    }

    private final CodeEmitter _emitter = null;

    void emitExpression(final Expression node) {
        emitExpression(
            node,
            CompilationFlags.EmitAsNoTail | CompilationFlags.EmitExpressionStart);
    }

    private void emitExpression(final Expression node, final int flags) {
        assert node != null;

        final boolean emitStart = (flags & CompilationFlags.EmitExpressionStartMask) == CompilationFlags.EmitExpressionStart;

        final int startEmitted = emitStart
            ? emitExpressionStart(node)
            : CompilationFlags.EmitNoExpressionStart;

        // only pass tail call flags to emit the expression
        final int compilationFlags = flags & CompilationFlags.EmitAsTailCallMask;

        switch (node.getNodeType()) {
            case Add:
                emitBinaryExpression(node, compilationFlags);
                break;
            case And:
                emitBinaryExpression(node, compilationFlags);
                break;
            case AndAlso:
                emitAndAlsoBinaryExpression(node, compilationFlags);
                break;
            case ArrayLength:
                emitUnaryExpression(node, compilationFlags);
                break;
            case ArrayIndex:
                emitBinaryExpression(node, compilationFlags);
                break;
            case Call:
                emitMethodCallExpression(node, compilationFlags);
                break;
            case Coalesce:
                emitCoalesceBinaryExpression(node);
                break;
            case Conditional:
                emitConditionalExpression(node, compilationFlags);
                break;
            case Constant:
                emitConstantExpression(node);
                break;
            case Convert:
                emitConvertUnaryExpression(node, compilationFlags);
                break;
            case ConvertChecked:
                emitConvertUnaryExpression(node, compilationFlags);
                break;
            case Divide:
                emitBinaryExpression(node, compilationFlags);
                break;
            case Equal:
                emitBinaryExpression(node, compilationFlags);
                break;
            case ExclusiveOr:
                emitBinaryExpression(node, compilationFlags);
                break;
            case GreaterThan:
                emitBinaryExpression(node, compilationFlags);
                break;
            case GreaterThanOrEqual:
                emitBinaryExpression(node, compilationFlags);
                break;
            case Invoke:
                emitInvocationExpression(node, compilationFlags);
                break;
            case Lambda:
                emitLambdaExpression(node);
                break;
            case LeftShift:
                emitBinaryExpression(node, compilationFlags);
                break;
            case LessThan:
                emitBinaryExpression(node, compilationFlags);
                break;
            case LessThanOrEqual:
                emitBinaryExpression(node, compilationFlags);
                break;
            case MemberAccess:
                emitMemberExpression(node);
                break;
            case Modulo:
                emitBinaryExpression(node, compilationFlags);
                break;
            case Multiply:
                emitBinaryExpression(node, compilationFlags);
                break;
            case Negate:
                emitUnaryExpression(node, compilationFlags);
                break;
            case UnaryPlus:
                emitUnaryExpression(node, compilationFlags);
                break;
            case New:
                emitNewExpression(node);
                break;
            case NewArrayInit:
                emitNewArrayExpression(node);
                break;
            case NewArrayBounds:
                emitNewArrayExpression(node);
                break;
            case Not:
                emitUnaryExpression(node, compilationFlags);
                break;
            case NotEqual:
                emitBinaryExpression(node, compilationFlags);
                break;
            case Or:
                emitBinaryExpression(node, compilationFlags);
                break;
            case OrElse:
                emitOrElseBinaryExpression(node, compilationFlags);
                break;
            case Parameter:
                emitParameterExpression(node);
                break;
            case Quote:
                emitQuoteUnaryExpression(node);
                break;
            case RightShift:
                emitBinaryExpression(node, compilationFlags);
                break;
            case UnsignedRightShift:
                emitBinaryExpression(node, compilationFlags);
                break;
            case Subtract:
                emitBinaryExpression(node, compilationFlags);
                break;
            case InstanceOf:
                emitTypeBinaryExpression(node);
                break;
            case Assign:
                emitAssignBinaryExpression(node);
                break;
            case Block:
                emitBlockExpression(node, compilationFlags);
                break;
            case Decrement:
                emitUnaryExpression(node, compilationFlags);
                break;
            case DefaultValue:
                emitDefaultValueExpression(node);
                break;
            case Extension:
                emitExtensionExpression(node);
                break;
            case Goto:
                emitGotoExpression(node, compilationFlags);
                break;
            case Increment:
                emitUnaryExpression(node, compilationFlags);
                break;
            case Label:
                emitLabelExpression(node, compilationFlags);
                break;
            case RuntimeVariables:
                emitRuntimeVariablesExpression(node);
                break;
            case Loop:
                emitLoopExpression(node);
                break;
            case Switch:
                emitSwitchExpression(node, compilationFlags);
                break;
            case Throw:
                emitThrowUnaryExpression(node);
                break;
            case Try:
                emitTryExpression(node);
                break;
            case Unbox:
                emitUnboxUnaryExpression(node);
                break;
            case TypeEqual:
                emitTypeBinaryExpression(node);
                break;
            case OnesComplement:
                emitUnaryExpression(node, compilationFlags);
                break;
            case IsTrue:
                emitUnaryExpression(node, compilationFlags);
                break;
            case IsFalse:
                emitUnaryExpression(node, compilationFlags);
                break;

            default:
                throw ContractUtils.unreachable();
        }

        if (emitStart) {
            emitExpressionEnd(startEmitted);
        }
    }

    private void emitExpressionAsVoid(final Expression node) {
        emitExpressionAsVoid(node, CompilationFlags.EmitAsNoTail);
    }

    private void emitExpressionAsVoid(final Expression node, final int flags) {
        assert node != null;

        final CompilationFlags startEmitted = emitExpressionStart(node);

        switch (node.getNodeType()) {
            case Assign:
                emitAssign((BinaryExpression) node, CompilationFlags.EmitAsVoidType);
                break;
            case Block:
                emit((BlockExpression) node, updateEmitAsTypeFlag(flags, CompilationFlags.EmitAsVoidType));
                break;
            case Throw:
                emitThrow((UnaryExpression) node, CompilationFlags.EmitAsVoidType);
                break;
            case Goto:
                emitGotoExpression(node, updateEmitAsTypeFlag(flags, CompilationFlags.EmitAsVoidType));
                break;
            case Constant:
            case DefaultValue:
            case Parameter:
                // no-op
                break;
            default:
                if (node.getType() == PrimitiveTypes.Void) {
                    emitExpression(node, updateEmitExpressionStartFlag(flags, CompilationFlags.EmitNoExpressionStart));
                }
                else {
                    emitExpression(node, CompilationFlags.EmitAsNoTail | CompilationFlags.EmitNoExpressionStart);
                    _ilg.Emit(Opcodes.POP);
                }
                break;
        }

        emitExpressionEnd(startEmitted);
    }

    private void emitExpressionAsType(final Expression node, final Type type, final int flags) {
        if (type == PrimitiveTypes.Void) {
            emitExpressionAsVoid(node, flags);
        }
        else {
            // if the node is emitted as a different type, CastClass IL is emitted at the end,
            // should not emit with tail calls.
            if (!TypeUtils.areEquivalent(node.getType(), type)) {
                emitExpression(node);
                assert TypeUtils.areReferenceAssignable(type, node.getType());
                emitConvertToType(node.getType(), type);
            }
            else {
                // emit the with the flags and emit emit expression start
                emitExpression(node, updateEmitExpressionStartFlag(flags, CompilationFlags.EmitExpressionStart));
            }
        }
    }

    private void emitConvertToType(final Type typeFrom, final Type typeTo) {
        if (TypeUtils.areEquivalent(typeFrom, typeTo)) {
            return;
        }

        if (typeFrom == PrimitiveTypes.Void || typeTo == PrimitiveTypes.Void) {
            throw ContractUtils.unreachable();
        }
    }
*/
}
