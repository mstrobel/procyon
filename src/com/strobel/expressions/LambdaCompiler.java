package com.strobel.expressions;

import com.strobel.compilerservices.Closure;
import com.strobel.compilerservices.DebugInfoGenerator;
import com.strobel.core.KeyedQueue;
import com.strobel.core.StringUtilities;
import com.strobel.reflection.*;
import com.strobel.reflection.emit.*;
import com.strobel.util.ContractUtils;
import com.strobel.util.TypeUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mike Strobel
 */
@SuppressWarnings({"PackageVisibleField", "UnusedParameters", "UnusedDeclaration"})
final class LambdaCompiler {
    final static AtomicInteger nextId = new AtomicInteger();

    final LambdaExpression<?> lambda;
    final TypeBuilder typeBuilder;
    final MethodBuilder methodBuilder;
    final ConstructorBuilder constructorBuilder;
    final FieldBuilder closureField;
    final CodeGenerator generator;

    private final AnalyzedTree _tree;
    private final boolean _hasClosureArgument;
    private final KeyedQueue<Type<?>, LocalBuilder> _freeLocals;
    private final BoundConstants _boundConstants;
    private final Map<LabelTarget, LabelInfo> _labelInfo = new HashMap<>();

    private CompilerScope _scope;
    private LabelScopeInfo _labelBlock = new LabelScopeInfo(null, LabelScopeKind.Lambda);

    LambdaCompiler(final AnalyzedTree tree, final LambdaExpression<?> lambda) {
        this.lambda = lambda;

        typeBuilder = new TypeBuilder(
            getUniqueLambdaName(lambda.getCreationContext()),
            Modifier.PUBLIC | Modifier.FINAL,
            Types.Object,
            Type.list(lambda.getType())
        );

        final MethodInfo interfaceMethod = lambda.getType().getMethods().get(0);

        methodBuilder = typeBuilder.defineMethod(
            interfaceMethod.getName(),
            Modifier.PUBLIC | Modifier.FINAL,
            interfaceMethod.getReturnType(),
            interfaceMethod.getParameters().getParameterTypes(),
            interfaceMethod.getThrownTypes()
        );

        generator = methodBuilder.getCodeGenerator();

        _tree = tree;
        _hasClosureArgument = true; //tree.scopes.get(lambda).needsClosure;
        _freeLocals = new KeyedQueue<>();
        _scope = tree.scopes.get(lambda);
        _boundConstants = tree.constants.get(lambda);

        //noinspection ConstantConditions
        if (_hasClosureArgument) {
            closureField = typeBuilder.defineField(
                "__closure",
                Type.of(Closure.class),
                Modifier.PRIVATE | Modifier.FINAL
            );

            constructorBuilder = typeBuilder.defineConstructor(
                Modifier.PUBLIC,
                Type.list(Types.Closure)
            );

            final CodeGenerator ctor = constructorBuilder.getCodeGenerator();

            ctor.emitThis();
            ctor.call(Types.Object.getConstructors().get(0));
            ctor.emitThis();
            ctor.emitLoadArgument(0);
            ctor.putField(closureField);
            ctor.emitReturn();
        }
        else {
            closureField = null;
            constructorBuilder = typeBuilder.defineDefaultConstructor();
        }
    }

    LambdaCompiler(final AnalyzedTree tree, final LambdaExpression<?> lambda, final MethodBuilder method) {
        this.lambda = lambda;

        _hasClosureArgument = false;

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
        this.constructorBuilder = null;
        this.closureField = null;

        this.generator = methodBuilder.getCodeGenerator();

        _freeLocals = new KeyedQueue<>();
        _tree = tree;
        _scope = tree.scopes.get(lambda);
        _boundConstants = tree.constants.get(lambda);
    }

    private LambdaCompiler(final LambdaCompiler parent, final LambdaExpression lambda) {
        _tree = parent._tree;
        _freeLocals = parent._freeLocals;
        this.lambda = lambda;
        this.methodBuilder = parent.methodBuilder;
        this.closureField = parent.closureField;
        this.constructorBuilder = parent.constructorBuilder;
        this.generator = parent.generator;
        _hasClosureArgument = parent._hasClosureArgument;
        this.typeBuilder = parent.typeBuilder;
        _scope = _tree.scopes.get(lambda);
        _boundConstants = parent._boundConstants;
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

        generator.emitThis();
        generator.getField(closureField);
    }

    void emitLambdaArgument(final int index) {
        generator.emitLoadArgument(getLambdaArgument(index));
    }

    private FieldBuilder createStaticField(final String name, final Type type) {
        return typeBuilder.defineField(
            "<ExpressionCompilerImplementationDetails>{" + nextId.getAndIncrement() + "}" + name,
            type,
            Modifier.STATIC | Modifier.PRIVATE
        );
    }

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

        // 3. emit
        c.emitLambdaBody();

        final Type<T> generatedType = (Type<T>)c.typeBuilder.createType();
        final Class<T> generatedClass = generatedType.getErasedClass();

        return c.createDelegate(generatedClass);
    }

    @SuppressWarnings("unchecked")
    private <T> Delegate<T> createDelegate(final Class<T> generatedClass) {
        try {
            final T instance;

            if (_hasClosureArgument) {
                final Constructor<?> constructor = generatedClass.getConstructor(Closure.class);
                final Closure closure = new Closure(_boundConstants.toArray(), null);
                instance = (T)constructor.newInstance(closure);
            }
            else {
                instance = generatedClass.newInstance();
            }

            return new Delegate<>(
                instance,
                generatedClass.getInterfaces()[0].getDeclaredMethods()[0]
            );
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
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

        // 3. emit
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
        return index;// + (methodBuilder.isStatic() ? 0 : 1);
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

    void emitExpression(final Expression node) {
        emitExpression(
            node,
            CompilationFlags.EmitAsNoTail | CompilationFlags.EmitExpressionStart
        );
    }

    private int emitExpressionStart(final Expression node) {
        if (tryPushLabelBlock(node)) {
            return CompilationFlags.EmitExpressionStart;
        }
        return CompilationFlags.EmitNoExpressionStart;
    }

    private void emitExpressionEnd(final int flags) {
        if ((flags & CompilationFlags.EmitExpressionStartMask) == CompilationFlags.EmitExpressionStart) {
            popLabelBlock(_labelBlock.kind);
        }
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
/*
            case Quote:
                emitQuoteUnaryExpression(node);
                break;
*/
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
/*
            case Switch:
                emitSwitchExpression(node, compilationFlags);
                break;
            case Throw:
                emitThrowUnaryExpression(node);
                break;
            case Try:
                emitTryExpression(node);
                break;
*/
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

    private void emitExtensionExpression(final Expression node) {
        throw Error.extensionNotReduced();
    }

    // <editor-fold defaultstate="collapsed" desc="Emit as Void/Type">

    private void emitExpressionAsVoid(final Expression node) {
        emitExpressionAsVoid(node, CompilationFlags.EmitAsNoTail);
    }

    private void emitExpressionAsVoid(final Expression node, final int flags) {
        assert node != null;

        final int startEmitted = emitExpressionStart(node);

        switch (node.getNodeType()) {
            case Assign:
                emitAssign((BinaryExpression)node, CompilationFlags.EmitAsVoidType);
                break;
            case Block:
                emit((BlockExpression)node, updateEmitAsTypeFlag(flags, CompilationFlags.EmitAsVoidType));
                break;
            case Throw:
                emitThrow((UnaryExpression)node, CompilationFlags.EmitAsVoidType);
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
                    generator.emit(OpCode.POP);
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
                generator.emitConversion(node.getType(), type);
            }
            else {
                // emit the with the flags and emit emit expression start
                emitExpression(
                    node,
                    updateEmitExpressionStartFlag(
                        flags,
                        CompilationFlags.EmitExpressionStart
                    )
                );
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Assignments">

    private void emitAssignBinaryExpression(final Expression expr) {
        emitAssign((BinaryExpression)expr, CompilationFlags.EmitAsDefaultType);
    }

    private void emitAssign(final BinaryExpression node, final int emitAs) {
        switch (node.getLeft().getNodeType()) {
            case ArrayIndex:
                emitIndexAssignment(node, emitAs);
                return;
            case MemberAccess:
                emitMemberAssignment(node, emitAs);
                return;
            case Parameter:
                emitVariableAssignment(node, emitAs);
                return;
            default:
                throw Error.invalidLValue(node.getLeft().getNodeType());
        }
    }

    private void emitMemberAssignment(final BinaryExpression node, final int flags) {
        final MemberExpression lvalue = (MemberExpression)node.getLeft();
        final MemberInfo member = lvalue.getMember();

        // emit "this", if any
        if (lvalue.getTarget() != null) {
            emitExpression(lvalue.getTarget());
        }

        // emit value
        emitExpression(node.getRight());

        LocalBuilder temp = null;

        final int emitAs = flags & CompilationFlags.EmitAsTypeMask;

        if (emitAs != CompilationFlags.EmitAsVoidType) {
            // Save the value so we can return it.
            generator.dup();
            generator.emitStore(temp = getLocal(node.getType()));
        }

        switch (member.getMemberType()) {
            case Field:
                generator.emit(OpCode.PUTFIELD, (FieldInfo)member);
                break;

            default:
                throw Error.invalidMemberType(member.getMemberType());
        }

        if (emitAs != CompilationFlags.EmitAsVoidType) {
            generator.emitLoad(temp);
            freeLocal(temp);
        }
    }

    private void emitVariableAssignment(final BinaryExpression node, final int flags) {
        final ParameterExpression variable = (ParameterExpression)node.getLeft();
        final int emitAs = flags & CompilationFlags.EmitAsTypeMask;

        emitExpression(node.getRight());

        if (emitAs != CompilationFlags.EmitAsVoidType) {
            generator.dup();
        }

        _scope.emitSet(variable);
    }

    private void emitIndexAssignment(final BinaryExpression node, final int flags) {
        final Expression left = node.getLeft();
        final BinaryExpression index = (BinaryExpression)left;
        final int emitAs = flags & CompilationFlags.EmitAsTypeMask;

        // Emit the target array.
        emitExpression(left);

        // Emit the index.
        emitExpression(index.getRight());

        // Emit the value
        emitExpression(node.getRight());

        // Save the expression value, if needed 
        LocalBuilder temp = null;

        if (emitAs != CompilationFlags.EmitAsVoidType) {
            generator.dup();
            generator.emitStore(temp = getLocal(node.getType()));
        }

        emitSetIndexCall(index);

        // Restore the value 
        if (emitAs != CompilationFlags.EmitAsVoidType) {
            generator.emitLoad(temp);
            freeLocal(temp);
        }
    }

    private void emitSetIndexCall(final BinaryExpression index) {
        generator.emitStoreElement(index.getType());
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Blocks">

    private static boolean hasVariables(final Object node) {
        if (node instanceof BlockExpression)  {
            return ((BlockExpression)node).getVariables().size() > 0;
        }
        return ((CatchBlock)node).getVariable() != null;
    }

    private void enterScope(final Object node) {
        if (hasVariables(node) &&
            (_scope.mergedScopes == null || !_scope.mergedScopes.contains(node))) {

            CompilerScope scope = _tree.scopes.get(node);

            if (scope == null) {
                //
                // Very often, we want to compile nodes as reductions
                // rather than as IL, but usually they need to allocate
                // some IL locals. To support this, we allow emitting a
                // BlockExpression that was not bound by VariableBinder.
                // This works as long as the variables are only used
                // locally -- i.e. not closed over.
                //
                // User-created blocks will never hit this case; only our
                // internally reduced nodes will.
                //
                scope = new CompilerScope(node, false);
                scope.needsClosure = _scope.needsClosure;
            }

            _scope = scope.enter(this, _scope);

            assert (_scope.node == node);
        }
    }

    private void exitScope(final Object node) {
        if (_scope.node == node) {
            _scope = _scope.exit();
        }
    }

    private void emitBlockExpression(final Expression expr, final int flags) {
        emit((BlockExpression)expr, updateEmitAsTypeFlag(flags, CompilationFlags.EmitAsDefaultType));
    }

    private void emit(final BlockExpression node, final int flags) {
        enterScope(node);

        final int emitAs = flags & CompilationFlags.EmitAsTypeMask;
        final int count = node.getExpressionCount();

        final int tailCall = flags & CompilationFlags.EmitAsTailCallMask;

        final int middleTailCall = tailCall == CompilationFlags.EmitAsNoTail
                                   ? CompilationFlags.EmitAsNoTail
                                   : CompilationFlags.EmitAsMiddle;

        for (int index = 0; index < count - 1; index++) {
            final Expression e = node.getExpression(index);
            final Expression next = node.getExpression(index + 1);

/*
            if (emitDebugSymbols()) {
                // No need to emit a clearance if the next expression in the block is also a
                // DebugInfoExpression.
                if (e instanceof DebugInfoExpression &&
                    ((DebugInfoExpression)e).isClear() &&
                    next instanceof DebugInfoExpression) {

                    continue;
                }
            }
*/
            //
            // In the middle of the block.
            // We may do better here by marking it as Tail if the following expressions are
            // not going to emit any bytecode.
            //
            int tailCallFlag = middleTailCall;

            if (next instanceof GotoExpression) {
                final GotoExpression g = (GotoExpression)next;

                if (g.getValue() == null || !significant(g.getValue())) {
                    final LabelInfo labelInfo = referenceLabel(g.getTarget());

                    if (labelInfo.canReturn()) {
                        //
                        // Since tail call flags are not passed into emitTryExpression(),
                        // canReturn() means the goto will be emitted as [T]RETURN. Therefore
                        // we can emit the current expression with tail call.
                        //
                        tailCallFlag = CompilationFlags.EmitAsTail;
                    }
                }
            }

            final int updatedFlags = updateEmitAsTailCallFlag(flags, tailCallFlag);

            emitExpressionAsVoid(e, updatedFlags);
        }

        // if the type of Block it means this is not a Comma
        // so we will force the last expression to emit as void.
        // We don't need EmitAsType flag anymore, should only pass
        // the EmitTailCall field in flags to emitting the last expression.
        if (emitAs == CompilationFlags.EmitAsVoidType || node.getType() == PrimitiveTypes.Void) {
            emitExpressionAsVoid(node.getExpression(count - 1), tailCall);
        }
        else {
            emitExpressionAsType(node.getExpression(count - 1), node.getType(), tailCall);
        }

        exitScope(node);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="AndAlso Binary Expressions">

    private void emitAndAlsoBinaryExpression(final Expression expr, final int flags) {
        final BinaryExpression b = (BinaryExpression)expr;

        if (b.getMethod() != null) {
            throw Error.andAlsoCannotProvideMethod();
        }
        else if (b.getLeft().getType() == Types.Boolean) {
            emitUnboxingAndAlso(b);
        }
        else {
            emitPrimitiveAndAlso(b);
        }
    }

    private void emitPrimitiveAndAlso(final BinaryExpression b) {
        final Expression left = b.getLeft();
        final Expression right = b.getRight();

        final Type<?> leftType = left.getType();
        final Type<?> rightType = right.getType();

        final Label returnFalse = generator.defineLabel();
        final Label exit = generator.defineLabel();

        final LocalBuilder leftStorage = getLocal(leftType);
        final LocalBuilder rightStorage = getLocal(rightType);

        emitExpression(left);

        generator.emitStore(leftStorage);
        generator.emitLoad(leftStorage);

        generator.emit(OpCode.IFEQ, returnFalse);

        emitExpression(right);

        generator.emitStore(rightStorage);
        generator.emitLoad(rightStorage);

        generator.emit(OpCode.IFEQ, returnFalse);
        generator.emitBoolean(true);
        generator.emitGoto(exit);

        generator.markLabel(returnFalse);
        generator.emitBoolean(false);

        generator.markLabel(exit);
    }

    private void emitUnboxingAndAlso(final BinaryExpression b) {
        final Expression left = b.getLeft();
        final Expression right = b.getRight();

        final Type<?> leftType = left.getType();
        final Type<?> rightType = right.getType();

        final Label returnFalse = generator.defineLabel();
        final Label exit = generator.defineLabel();

        final LocalBuilder leftStorage = getLocal(leftType);
        final LocalBuilder rightStorage = getLocal(rightType);

        emitExpression(left);

        generator.emitStore(leftStorage);
        generator.emitLoad(leftStorage);

        if (leftType != PrimitiveTypes.Boolean) {
            generator.emitConversion(leftType, PrimitiveTypes.Boolean);
        }

        generator.emit(OpCode.IFEQ, returnFalse);

        emitExpression(right);

        generator.emitStore(rightStorage);
        generator.emitLoad(rightStorage);

        if (rightType != PrimitiveTypes.Boolean) {
            generator.emitConversion(rightType, PrimitiveTypes.Boolean);
        }

        generator.emit(OpCode.IFEQ, returnFalse);
        generator.emitBoolean(true);
        generator.emitGoto(exit);

        generator.markLabel(returnFalse);
        generator.emitBoolean(false);

        generator.markLabel(exit);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="OrElse Binary Expressions">

    private void emitOrElseBinaryExpression(final Expression expr, final int flags) {
        final BinaryExpression b = (BinaryExpression)expr;

        if (b.getMethod() != null) {
            throw Error.orElseCannotProvideMethod();
        }
        else if (b.getLeft().getType() == Types.Boolean) {
            emitUnboxingOrElse(b);
        }
        else {
            emitPrimitiveOrElse(b);
        }
    }

    private void emitPrimitiveOrElse(final BinaryExpression b) {
        final Expression left = b.getLeft();
        final Expression right = b.getRight();

        final Type<?> leftType = left.getType();
        final Type<?> rightType = right.getType();

        final Label returnTrue = generator.defineLabel();
        final Label exit = generator.defineLabel();

        final LocalBuilder leftStorage = getLocal(leftType);
        final LocalBuilder rightStorage = getLocal(rightType);

        emitExpression(left);

        generator.emitStore(leftStorage);
        generator.emitLoad(leftStorage);

        generator.emit(OpCode.IFNE, returnTrue);

        emitExpression(right);

        generator.emitStore(rightStorage);
        generator.emitLoad(rightStorage);

        generator.emit(OpCode.IFNE, returnTrue);
        generator.emitBoolean(false);
        generator.emitGoto(exit);

        generator.markLabel(returnTrue);
        generator.emitBoolean(true);

        generator.markLabel(exit);
    }

    private void emitUnboxingOrElse(final BinaryExpression b) {
        final Expression left = b.getLeft();
        final Expression right = b.getRight();

        final Type<?> leftType = left.getType();
        final Type<?> rightType = right.getType();

        final Label returnTrue = generator.defineLabel();
        final Label exit = generator.defineLabel();

        final LocalBuilder leftStorage = getLocal(leftType);
        final LocalBuilder rightStorage = getLocal(rightType);

        emitExpression(left);

        generator.emitStore(leftStorage);
        generator.emitLoad(leftStorage);

        if (leftType != PrimitiveTypes.Boolean) {
            generator.emitConversion(leftType, PrimitiveTypes.Boolean);
        }

        generator.emit(OpCode.IFNE, returnTrue);

        emitExpression(right);

        generator.emitStore(rightStorage);
        generator.emitLoad(rightStorage);

        if (rightType != PrimitiveTypes.Boolean) {
            generator.emitConversion(rightType, PrimitiveTypes.Boolean);
        }

        generator.emit(OpCode.IFNE, returnTrue);
        generator.emitBoolean(false);
        generator.emitGoto(exit);

        generator.markLabel(returnTrue);
        generator.emitBoolean(true);

        generator.markLabel(exit);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Coalesce Expressions">

    private void emitCoalesceBinaryExpression(final Expression expr) {
        final BinaryExpression b = (BinaryExpression)expr;
        assert (b.getMethod() == null);

        if (b.getLeft().getType().isPrimitive()) {
            throw Error.coalesceUsedOnNonNullableType();
        }
        else if (b.getConversion() != null) {
            emitLambdaReferenceCoalesce(b);
        }
        else {
            emitReferenceCoalesceWithoutConversion(b);
        }
    }

    private void emitReferenceCoalesceWithoutConversion(final BinaryExpression b) {
        final Label end = generator.defineLabel();
        final Label cast = generator.defineLabel();

        emitExpression(b.getLeft());

        generator.dup();
        generator.emit(OpCode.IFNONNULL, cast);
        generator.pop();

        emitExpression(b.getRight());

        if (!TypeUtils.areEquivalent(b.getRight().getType(), b.getType())) {
            generator.emitConversion(b.getRight().getType(), b.getType());
        }

        generator.emitGoto(end);
        generator.markLabel(cast);

        if (!TypeUtils.areEquivalent(b.getLeft().getType(), b.getType())) {
            generator.emitConversion(b.getLeft().getType(), b.getType());
        }

        generator.markLabel(end);
    }

    private void emitLambdaReferenceCoalesce(final BinaryExpression b) {
        final LocalBuilder operandStorage = getLocal(b.getLeft().getType());

        final Label end = generator.defineLabel();
        final Label notNull = generator.defineLabel();

        emitExpression(b.getLeft());

        generator.dup();
        generator.emitStore(operandStorage);
        generator.emit(OpCode.IFNONNULL, notNull);

        emitExpression(b.getRight());

        generator.emitGoto(end);

        // If not null, call conversion.
        generator.markLabel(notNull);

        final LambdaExpression lambda = b.getConversion();
        final ParameterExpressionList conversionParameters = lambda.getParameters();

        assert (conversionParameters.size() == 1);

        // Emit the delegate instance.
        emitLambdaExpression(lambda);

        // Emit argument.
        generator.emitLoad(operandStorage);

        freeLocal(operandStorage);

        final MethodInfo method = Expression.getInvokeMethod(lambda);

        // Emit call to invoke.
        generator.call(method);

        generator.markLabel(end);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Conditional Expressions">

    private void emitConditionalExpression(final Expression expr, final int flags) {
        final ConditionalExpression node = (ConditionalExpression)expr;

        assert node.getTest().getType() == PrimitiveTypes.Boolean;

        final Label ifFalse = generator.defineLabel();

        emitExpressionAndBranch(false, node.getTest(), ifFalse);
        emitExpressionAsType(node.getIfTrue(), node.getType(), flags);

        if (notEmpty(node.getIfFalse())) {
            final Label end = generator.defineLabel();

            generator.emitGoto(end);
            generator.markLabel(ifFalse);

            emitExpressionAsType(node.getIfFalse(), node.getType(), flags);

            generator.markLabel(end);
        }
        else {
            generator.markLabel(ifFalse);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Binary Expressions">

    private void emitBinaryExpression(final Expression expr) {
        emitBinaryExpression(expr, CompilationFlags.EmitAsNoTail);
    }

    private void emitBinaryExpression(final Expression expr, final int flags) {
        final BinaryExpression b = (BinaryExpression)expr;

        assert b.getNodeType() != ExpressionType.AndAlso &&
               b.getNodeType() != ExpressionType.OrElse &&
               b.getNodeType() != ExpressionType.Coalesce;

        if (b.getMethod() != null) {
            emitBinaryMethod(b, flags);
            return;
        }

        final Expression left = b.getLeft();
        final Expression right = b.getRight();

        if ((b.getNodeType() == ExpressionType.Equal || b.getNodeType() == ExpressionType.NotEqual) &&
            (b.getType() == PrimitiveTypes.Boolean || b.getType() == Types.Boolean)) {

            emitExpression(getEqualityOperand(left));
            emitExpression(getEqualityOperand(right));
        }
        else {
            emitExpression(left);
            emitExpression(right);
        }

        emitBinaryOperator(b.getNodeType(), left.getType(), right.getType(), b.getType());
    }

    private Expression getEqualityOperand(final Expression expression) {
        if (expression.getNodeType() == ExpressionType.Convert) {
            final UnaryExpression convert = (UnaryExpression)expression;
            if (TypeUtils.areReferenceAssignable(convert.getType(), convert.getOperand().getType())) {
                return convert.getOperand();
            }
        }
        return expression;
    }

    private void emitBinaryMethod(final BinaryExpression b, final int flags) {
        emitMethodCallExpression(Expression.call(null, b.getMethod(), b.getLeft(), b.getRight()), flags);
    }

    private void emitBinaryOperator(final ExpressionType op, final Type leftType, final Type rightType, final Type resultType) {
        final boolean leftIsNullable = TypeUtils.isAutoUnboxed(leftType);
        final boolean rightIsNullable = TypeUtils.isAutoUnboxed(rightType);

        switch (op) {
            case ArrayIndex: {
                if (rightType != PrimitiveTypes.Integer && rightType != Types.Integer) {
                    throw ContractUtils.unreachable();
                }
                generator.emitLoadElement(leftType.getElementType());
                return;
            }

            case Coalesce: {
                throw Error.unexpectedCoalesceOperator();
            }
        }

        if (leftIsNullable || rightIsNullable) {
            emitUnboxingBinaryOp(op, leftType, rightType, resultType);
        }
        else {
            final Type<?> opResultType = emitPrimitiveBinaryOp(op, leftType, rightType);
            emitConvertArithmeticResult(op, opResultType, resultType);
        }
    }

    private void emitConvertArithmeticResult(final ExpressionType op, final Type sourceType, final Type resultType) {
        generator.emitConversion(sourceType, resultType);
    }

    private Type<?> emitPrimitiveBinaryOp(final ExpressionType op, final Type leftType, final Type rightType) {
        final Type<?> operandType = performBinaryNumericPromotion(leftType, rightType);

        if (leftType != operandType) {
            final LocalBuilder rightStorage = getLocal(rightType);

            generator.emitStore(rightStorage);
            generator.emitConversion(leftType, operandType);
            generator.emitLoad(rightStorage);

            freeLocal(rightStorage);
        }

        if (rightType != operandType) {
            generator.emitConversion(rightType, operandType);
        }

        switch (operandType.getKind()) {
            case INT:
                emitIntegerBinaryOp(op);
                break;
            case LONG:
                emitLongBinaryOp(op);
                break;
            case FLOAT:
                emitFloatBinaryOp(op);
                break;
            case DOUBLE:
                emitDoubleBinaryOp(op);
                break;
            default:
                throw ContractUtils.unreachable();
        }

        switch (op) {
            case Equal:
            case GreaterThan:
            case GreaterThanOrEqual:
            case LessThan:
            case LessThanOrEqual:
            case NotEqual:
                return PrimitiveTypes.Boolean;

            default:
                return operandType;
        }
    }

    private Type<?> performBinaryNumericPromotion(final Type leftType, final Type rightType) {
        if (leftType == PrimitiveTypes.Double || rightType == PrimitiveTypes.Double) {
            return PrimitiveTypes.Double;
        }

        if (leftType == PrimitiveTypes.Float || rightType == PrimitiveTypes.Float) {
            return PrimitiveTypes.Float;
        }

        if (leftType == PrimitiveTypes.Long || rightType == PrimitiveTypes.Long) {
            return PrimitiveTypes.Long;
        }

        return PrimitiveTypes.Integer;
    }

    private void emitUnboxingBinaryOp(final ExpressionType op, final Type leftType, final Type rightType, final Type resultType) {
        assert TypeUtils.isAutoUnboxed(leftType) || TypeUtils.isAutoUnboxed(rightType);

        switch (op) {
            case And: {
                if (leftType == Types.Boolean) {
                    emitLiftedBooleanAnd(leftType, rightType);
                }
                else {
                    emitUnboxingBinaryArithmetic(op, leftType, rightType, resultType);
                }
                break;
            }

            case Or: {
                if (leftType == Types.Boolean) {
                    emitLiftedBooleanOr(leftType, rightType);
                }
                else {
                    emitUnboxingBinaryArithmetic(op, leftType, rightType, resultType);
                }
                break;
            }

            case ExclusiveOr:
            case Add:
            case Subtract:
            case Multiply:
            case Divide:
            case Modulo:
            case LeftShift:
            case RightShift: {
                emitUnboxingBinaryArithmetic(op, leftType, rightType, resultType);
                break;
            }

            case LessThan:
            case LessThanOrEqual:
            case GreaterThan:
            case GreaterThanOrEqual:
            case Equal:
            case NotEqual: {
                emitLiftedRelational(op, leftType, rightType, resultType);
                break;
            }

            case AndAlso:
            case OrElse:
            default:
                throw ContractUtils.unreachable();
        }
    }

    private void emitUnboxingBinaryArithmetic(
        final ExpressionType op,
        final Type leftType,
        final Type rightType,
        final Type resultType) {

        final boolean leftIsBoxed = TypeUtils.isAutoUnboxed(leftType);
        final boolean rightIsBoxed = TypeUtils.isAutoUnboxed(rightType);

        Type finalLeftType = leftType;
        Type finalRightType = rightType;

        if (leftIsBoxed) {
            finalLeftType = unboxLeftBinaryOperand(leftType, rightType);
        }

        if (rightIsBoxed) {
            finalRightType = unboxRightBinaryOperand(rightType);
        }

        emitBinaryOperator(
            op,
            finalLeftType,
            finalRightType,
            TypeUtils.getUnderlyingPrimitiveOrSelf(resultType)
        );
    }

    private void emitLiftedRelational(
        final ExpressionType op,
        final Type leftType,
        final Type rightType,
        final Type resultType) {

        final boolean leftIsBoxed = TypeUtils.isAutoUnboxed(leftType);
        final boolean rightIsBoxed = TypeUtils.isAutoUnboxed(rightType);

        Type finalLeftType = leftType;
        Type finalRightType = rightType;

        if (leftIsBoxed) {
            finalLeftType = unboxLeftBinaryOperand(leftType, rightType);
        }

        if (rightIsBoxed) {
            finalRightType = unboxRightBinaryOperand(rightType);
        }

        emitBinaryOperator(
            op,
            finalLeftType,
            finalRightType,
            TypeUtils.getUnderlyingPrimitiveOrSelf(resultType)
        );
    }

    private Type unboxRightBinaryOperand(final Type rightType) {
        final Type finalRightType = TypeUtils.getUnderlyingPrimitive(rightType);
        generator.emitUnbox(finalRightType);
        return finalRightType;
    }

    private Type unboxLeftBinaryOperand(final Type leftType, final Type rightType) {
        final Type finalLeftType = TypeUtils.getUnderlyingPrimitive(leftType);
        final LocalBuilder rightStorage = getLocal(rightType);

        generator.emitStore(rightStorage);
        generator.emitUnbox(finalLeftType);
        generator.emitLoad(rightStorage);

        freeLocal(rightStorage);

        return finalLeftType;
    }

    private void emitLiftedBooleanAnd(final Type leftType, final Type rightType) {
        final Type type = PrimitiveTypes.Boolean;
        final Label returnFalse = generator.defineLabel();
        final Label exit = generator.defineLabel();

        final LocalBuilder rightStorage = getLocal(type);

        generator.emitStore(rightStorage);

        generator.emitConversion(leftType, PrimitiveTypes.Boolean);
        generator.emit(OpCode.IFEQ, returnFalse);

        generator.emitLoad(rightStorage);
        generator.emitConversion(rightType, PrimitiveTypes.Boolean);
        generator.emit(OpCode.IFEQ, returnFalse);

        generator.emitBoolean(true);
        generator.emitGoto(exit);

        generator.markLabel(returnFalse);
        generator.emitBoolean(false);
        generator.emitGoto(exit);

        generator.markLabel(exit);

        freeLocal(rightStorage);
    }

    private void emitLiftedBooleanOr(final Type leftType, final Type rightType) {
        final Type type = PrimitiveTypes.Boolean;
        final Label returnTrue = generator.defineLabel();
        final Label exit = generator.defineLabel();

        final LocalBuilder rightStorage = getLocal(type);

        generator.emitStore(rightStorage);

        generator.emitConversion(leftType, PrimitiveTypes.Boolean);
        generator.emit(OpCode.IFNE, returnTrue);

        generator.emitLoad(rightStorage);
        generator.emitConversion(rightType, PrimitiveTypes.Boolean);
        generator.emit(OpCode.IFNE, returnTrue);

        generator.emitBoolean(false);
        generator.emitGoto(exit);

        generator.markLabel(returnTrue);
        generator.emitBoolean(true);
        generator.emitGoto(exit);

        generator.markLabel(exit);

        freeLocal(rightStorage);
    }

    private void emitDoubleBinaryOp(final ExpressionType op) {
        switch (op) {
            case Add: {
                generator.emit(OpCode.DADD);
                break;
            }

            case Divide: {
                generator.emit(OpCode.DDIV);
                break;
            }

            case Equal: {
                final Label ifNotEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.DCMPL);
                generator.emit(OpCode.IFNE, ifNotEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifNotEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case GreaterThan: {
                final Label ifLessThanOrEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.DCMPL);
                generator.emit(OpCode.IFLE, ifLessThanOrEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifLessThanOrEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case GreaterThanOrEqual: {
                final Label ifLessThan = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.DCMPL);
                generator.emit(OpCode.IFLT, ifLessThan);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifLessThan);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case LessThan: {
                final Label ifGreaterThanOrEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.DCMPG);
                generator.emit(OpCode.IFGE, ifGreaterThanOrEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifGreaterThanOrEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case LessThanOrEqual: {
                final Label ifGreaterThan = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.DCMPG);
                generator.emit(OpCode.IFGT, ifGreaterThan);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifGreaterThan);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case Modulo: {
                generator.emit(OpCode.DREM);
                break;
            }

            case Multiply: {
                generator.emit(OpCode.DMUL);
                break;
            }

            case NotEqual: {
                final Label ifEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.IFEQ, ifEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case Subtract: {
                generator.emit(OpCode.DSUB);
                break;
            }

            default: {
                throw ContractUtils.unreachable();
            }
        }
    }

    private void emitFloatBinaryOp(final ExpressionType op) {
        switch (op) {
            case Add: {
                generator.emit(OpCode.FADD);
                break;
            }

            case Divide: {
                generator.emit(OpCode.FDIV);
                break;
            }

            case Equal: {
                final Label ifNotEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.FCMPL);
                generator.emit(OpCode.IFNE, ifNotEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifNotEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case GreaterThan: {
                final Label ifLessThanOrEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.FCMPL);
                generator.emit(OpCode.IFLE, ifLessThanOrEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifLessThanOrEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case GreaterThanOrEqual: {
                final Label ifLessThan = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.FCMPL);
                generator.emit(OpCode.IFLT, ifLessThan);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifLessThan);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case LessThan: {
                final Label ifGreaterThanOrEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.FCMPG);
                generator.emit(OpCode.IFGE, ifGreaterThanOrEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifGreaterThanOrEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case LessThanOrEqual: {
                final Label ifGreaterThan = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.FCMPG);
                generator.emit(OpCode.IFGT, ifGreaterThan);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifGreaterThan);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case Modulo: {
                generator.emit(OpCode.FREM);
                break;
            }

            case Multiply: {
                generator.emit(OpCode.FMUL);
                break;
            }

            case NotEqual: {
                final Label ifEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.IFEQ, ifEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case Subtract: {
                generator.emit(OpCode.FSUB);
                break;
            }

            default: {
                throw ContractUtils.unreachable();
            }
        }
    }

    private void emitLongBinaryOp(final ExpressionType op) {
        switch (op) {
            case Add: {
                generator.emit(OpCode.LADD);
                break;
            }

            case And:
            case AndAlso: {
                generator.emit(OpCode.LAND);
                break;
            }

            case Divide: {
                generator.emit(OpCode.LDIV);
                break;
            }

            case Equal: {
                final Label ifNotEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.LCMP);
                generator.emit(OpCode.IFNE, ifNotEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifNotEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case ExclusiveOr: {
                generator.emit(OpCode.LXOR);
                break;
            }

            case GreaterThan: {
                final Label ifLessThanOrEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.LCMP);
                generator.emit(OpCode.IFLE, ifLessThanOrEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifLessThanOrEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case GreaterThanOrEqual: {
                final Label ifLessThan = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.LCMP);
                generator.emit(OpCode.IFLT, ifLessThan);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifLessThan);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case LeftShift: {
                generator.emit(OpCode.LSHL);
                break;
            }

            case LessThan: {
                final Label ifGreaterThanOrEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.LCMP);
                generator.emit(OpCode.IFGE, ifGreaterThanOrEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifGreaterThanOrEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case LessThanOrEqual: {
                final Label ifGreaterThan = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.LCMP);
                generator.emit(OpCode.IFGT, ifGreaterThan);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifGreaterThan);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case Modulo: {
                generator.emit(OpCode.LREM);
                break;
            }

            case Multiply: {
                generator.emit(OpCode.LMUL);
                break;
            }

            case NotEqual: {
                final Label ifEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.IFEQ, ifEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case Or:
            case OrElse: {
                generator.emit(OpCode.LOR);
                break;
            }

            case RightShift: {
                generator.emit(OpCode.LSHR);
                break;
            }

            case UnsignedRightShift: {
                generator.emit(OpCode.LUSHR);
                break;
            }

            case Subtract: {
                generator.emit(OpCode.LSUB);
                break;
            }

            default: {
                throw ContractUtils.unreachable();
            }
        }
    }

    private void emitIntegerBinaryOp(final ExpressionType op) {
        switch (op) {
            case Add: {
                generator.emit(OpCode.IADD);
                break;
            }

            case And:
            case AndAlso: {
                generator.emit(OpCode.IAND);
                break;
            }

            case Divide: {
                generator.emit(OpCode.IDIV);
                break;
            }

            case Equal: {
                final Label ifNotEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.IF_ICMPNE, ifNotEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifNotEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case ExclusiveOr: {
                generator.emit(OpCode.IXOR);
                break;
            }

            case GreaterThan: {
                final Label ifLessThanOrEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.IF_ICMPLE, ifLessThanOrEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifLessThanOrEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case GreaterThanOrEqual: {
                final Label ifLessThan = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.IF_ICMPLT, ifLessThan);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifLessThan);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case LeftShift: {
                generator.emit(OpCode.ISHL);
                break;
            }

            case LessThan: {
                final Label ifGreaterThanOrEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.IF_ICMPGE, ifGreaterThanOrEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifGreaterThanOrEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case LessThanOrEqual: {
                final Label ifGreaterThan = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.IF_ICMPGT, ifGreaterThan);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifGreaterThan);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case Modulo: {
                generator.emit(OpCode.IREM);
                break;
            }

            case Multiply: {
                generator.emit(OpCode.IMUL);
                break;
            }

            case NotEqual: {
                final Label ifEqual = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emit(OpCode.IF_ICMPEQ, ifEqual);

                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifEqual);
                generator.emitBoolean(false);
                generator.emitGoto(exit);

                generator.markLabel(exit);

                break;
            }

            case Or:
            case OrElse: {
                generator.emit(OpCode.IOR);
                break;
            }

            case RightShift: {
                generator.emit(OpCode.ISHR);
                break;
            }

            case UnsignedRightShift: {
                generator.emit(OpCode.IUSHR);
                break;
            }

            case Subtract: {
                generator.emit(OpCode.ISUB);
                break;
            }

            default: {
                throw ContractUtils.unreachable();
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Constants and Default Values">

    private void emitConstantExpression(final Expression expr) {
        final ConstantExpression node = (ConstantExpression)expr;
        emitConstant(node.getValue(), node.getType());
    }

    private void emitConstant(final Object value, final Type<?> type) {
        // Try to emit the constant directly into IL
        if (CodeGenerator.canEmitConstant(value, type)) {
            generator.emitConstant(value, type);
            return;
        }

        _boundConstants.emitConstant(this, value, type);
    }

    private void emitDefaultValueExpression(final Expression node) {
        generator.emitDefaultValue(node.getType());
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Branching">

    private void emitGotoExpression(final Expression expr, final int flags) {
        final GotoExpression node = (GotoExpression)expr;
        final LabelInfo labelInfo = referenceLabel(node.getTarget());

        int finalFlags = flags;

        if (node.getValue() != null) {
            if (node.getTarget().getType() == PrimitiveTypes.Void) {
                emitExpressionAsVoid(node.getValue(), flags);
            }
            else {
                finalFlags = updateEmitExpressionStartFlag(flags, CompilationFlags.EmitExpressionStart);
                emitExpression(node.getValue(), finalFlags);
            }
        }

        labelInfo.emitJump();

        emitUnreachable(node, finalFlags);
    }

    private void emitUnreachable(final Expression node, final int flags) {
        if (node.getType() != PrimitiveTypes.Void && (flags & CompilationFlags.EmitAsVoidType) == 0) {
            generator.emitDefaultValue(node.getType());
        }
    }

    private void emitExpressionAndBranch(final boolean branchValue, final Expression node, final Label label) {
        final int startEmitted = emitExpressionStart(node);

        try {
            if (node.getType() == PrimitiveTypes.Boolean) {
                switch (node.getNodeType()) {
                    case Not:
                        emitBranchNot(branchValue, (UnaryExpression)node, label);
                        return;
                    case AndAlso:
                    case OrElse:
                        emitBranchLogical(branchValue, (BinaryExpression)node, label);
                        return;
                    case Block:
                        emitBranchBlock(branchValue, (BlockExpression)node, label);
                        return;
                    case Equal:
                    case NotEqual:
                        emitBranchComparison(branchValue, (BinaryExpression)node, label);
                        return;
                }
            }
            emitExpression(node, CompilationFlags.EmitAsNoTail | CompilationFlags.EmitNoExpressionStart);
            emitBranchOp(branchValue, label);
        }
        finally {
            emitExpressionEnd(startEmitted);
        }
    }

    private void emitBranchNot(final boolean branch, final UnaryExpression node, final Label label) {
        if (node.getMethod() != null) {
            emitExpression(node, CompilationFlags.EmitAsNoTail | CompilationFlags.EmitNoExpressionStart);
            emitBranchOp(branch, label);
            return;
        }
        emitExpressionAndBranch(!branch, node.getOperand(), label);
    }

    private void emitBranchComparison(final boolean branch, final BinaryExpression node, final Label label) {
        assert node.getNodeType() == ExpressionType.Equal || node.getNodeType() == ExpressionType.NotEqual;

        // To share code paths, we want to treat NotEqual as an inverted Equal
        final boolean branchWhenEqual = branch == (node.getNodeType() == ExpressionType.Equal);

        if (node.getMethod() != null) {
            emitBinaryMethod(node, CompilationFlags.EmitAsNoTail);
            //
            // emitBinaryMethod() takes into account the Equal/NotEqual
            // node kind, so use the original branch value
            //
            emitBranchOp(branch, label);
        }
        else if (ConstantCheck.isNull(node.getLeft())) {
            emitExpression(getEqualityOperand(node.getRight()));
            emitBranchOp(!branchWhenEqual, label);
        }
        else if (ConstantCheck.isNull(node.getRight())) {
            assert !node.getLeft().getType().isPrimitive();
            emitExpression(getEqualityOperand(node.getLeft()));
            emitBranchOp(!branchWhenEqual, label);
        }
        else if (TypeUtils.isAutoUnboxed(node.getLeft().getType()) ||
                 TypeUtils.isAutoUnboxed(node.getRight().getType())) {

            emitBinaryExpression(node);
            //
            // emitBinaryExpression() takes into account the Equal/NotEqual
            // node kind, so use the original branch value
            //
            emitBranchOp(branch, label);
        }
        else {
            final Expression equalityOperand = getEqualityOperand(node.getLeft());

            emitExpression(equalityOperand);
            emitExpression(getEqualityOperand(node.getRight()));

            final Type<?> compareType;

            if (TypeUtils.isArithmetic(equalityOperand.getType())) {
                compareType = TypeUtils.getUnderlyingPrimitiveOrSelf(equalityOperand.getType());
            }
            else {
                compareType = equalityOperand.getType();
            }

            switch (compareType.getKind()) {
                case BOOLEAN:
                case BYTE:
                case SHORT:
                case INT:
                    generator.emit(branchWhenEqual ? OpCode.IF_ICMPEQ : OpCode.IF_ICMPNE, label);
                    break;

                case LONG:
                    generator.emit(OpCode.LCMP);
                    generator.emit(branchWhenEqual ? OpCode.IFEQ : OpCode.IFNE, label);
                    break;

                case CHAR:
                    generator.emit(branchWhenEqual ? OpCode.IF_ICMPEQ : OpCode.IF_ICMPNE, label);
                    break;

                case FLOAT:
                    generator.emit(OpCode.FCMPL);
                    generator.emit(branchWhenEqual ? OpCode.IFEQ : OpCode.IFNE, label);
                    break;

                case DOUBLE:
                    generator.emit(OpCode.DCMPL);
                    generator.emit(branchWhenEqual ? OpCode.IFEQ : OpCode.IFNE, label);
                    break;

                default:
                    generator.emit(branchWhenEqual ? OpCode.IF_ACMPEQ : OpCode.IF_ACMPNE, label);
                    break;
            }
        }
    }

    private void emitBranchOp(final boolean branch, final Label label) {
        generator.emit(branch ? OpCode.IFNE : OpCode.IFEQ, label);
    }

    private void emitBranchLogical(final boolean branch, final BinaryExpression node, final Label label) {
        assert (node.getNodeType() == ExpressionType.AndAlso || node.getNodeType() == ExpressionType.OrElse);

        if (node.getMethod() != null) {
            emitExpression(node);
            emitBranchOp(branch, label);
            return;
        }

        final boolean isAnd = node.getNodeType() == ExpressionType.AndAlso;

        //
        // To share code, we make the following substitutions:
        //     if (!(left || right)) branch value 
        // becomes:
        //     if (!left && !right) branch value 
        // and: 
        //     if (!(left && right)) branch value
        // becomes: 
        //     if (!left || !right) branch value
        //
        //
        if (branch == isAnd) {
            emitBranchAnd(branch, node, label);
        } else {
            emitBranchOr(branch, node, label);
        }
    }

    private void emitBranchAnd(final boolean branch, final BinaryExpression node, final Label label) {
        // if (left) then
        //   if (right) branch label
        // endIf

        final Label endIf = generator.defineLabel();

        emitExpressionAndBranch(!branch, node.getLeft(), endIf);
        emitExpressionAndBranch(branch, node.getRight(), label);

        generator.markLabel(endIf);
    }

    private void emitBranchOr(final boolean branch, final BinaryExpression node, final Label label) {
        // if (left OR right) branch label

        emitExpressionAndBranch(branch, node.getLeft(), label);
        emitExpressionAndBranch(branch, node.getRight(), label);
    }

    private void emitBranchBlock(final boolean branch, final BlockExpression node, final Label label) {
        enterScope(node);

        final int count = node.getExpressionCount();

        for (int i = 0; i < count - 1; i++) {
            emitExpressionAsVoid(node.getExpression(i));
        }

        emitExpressionAndBranch(branch, node.getExpression(count - 1), label);

        exitScope(node);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Invocation Expressions">

    private void emitInvocationExpression(final Expression expr, final int flags) {
        final InvocationExpression node = (InvocationExpression)expr;
        final LambdaExpression<?> lambdaOperand = node.getLambdaOperand();

        // Optimization: inline code for literal lambda's directly
        //
        // This is worth it because otherwise we end up with a extra call
        // to DynamicMethod.CreateDelegate, which is expensive.
        //
        if (lambdaOperand != null) {
            emitInlinedInvoke(node, flags);
            return;
        }

        Expression e = node.getExpression();

        if (Type.of(LambdaExpression.class).isAssignableFrom(e.getType())) {
            // if the invoke target is a lambda expression tree, first compile it into a delegate
            e = Expression.call(e, e.getType().getMethod("compile"));
        }

        e = Expression.call(e, Expression.getInvokeMethod(e), node.getArguments());

        emitExpression(e);
    }

    private void emitInlinedInvoke(final InvocationExpression invoke, final int flags) {
        final LambdaExpression<?> lambda = invoke.getLambdaOperand();

        // This is tricky: we need to emit the arguments outside of the
        // scope, but set them inside the scope. Fortunately, using the
        // stack it is entirely doable.

        // 1. Emit invoke arguments.
        emitArguments(Expression.getInvokeMethod(lambda), invoke);

        // 2. Create the nested LambdaCompiler.
        final LambdaCompiler inner = new LambdaCompiler(this, lambda);

        // 3. Emit the body.
        inner.emitLambdaBody(_scope, true, flags);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Labels">

    private void emitLabelExpression(final Expression expr, int flags) {
        final LabelExpression node = (LabelExpression)expr;

        assert node.getTarget() != null;

        // If we're an immediate child of a block, our label will already
        // be defined. If not, we need to define our own block so this
        // label isn't exposed except to its own child expression.
        LabelInfo label = null;

        if (_labelBlock.kind == LabelScopeKind.Block) {
            label = _labelBlock.tryGetLabelInfo(node.getTarget());

            // We're in a block but didn't find our label, try switch
            if (label == null && _labelBlock.parent.kind == LabelScopeKind.Switch) {
                label = _labelBlock.parent.tryGetLabelInfo(node.getTarget());
            }

            // if we're in a switch or block, we should have found the label
            assert label != null;
        }

        if (label == null) {
            label = defineLabel(node.getTarget());
        }

        if (node.getDefaultValue() != null) {
            if (node.getTarget().getType() == PrimitiveTypes.Void) {
                emitExpressionAsVoid(node.getDefaultValue(), flags);
            }
            else {
                flags = updateEmitExpressionStartFlag(flags, CompilationFlags.EmitExpressionStart);
                emitExpression(node.getDefaultValue(), flags);
            }
        }

        label.mark();
    }

    private void pushLabelBlock(final LabelScopeKind type) {
        _labelBlock = new LabelScopeInfo(_labelBlock, type);
    }

    @SuppressWarnings("ConstantConditions")
    private boolean tryPushLabelBlock(final Expression node) {
        ExpressionType nodeType = node.getNodeType();

        // Anything that is "statement-like" -- e.g. has no associated
        // stack state can be jumped into, with the exception of try-blocks
        // We indicate this by a "Block"
        //
        // Otherwise, we push an "Expression" to indicate that it can't be
        // jumped into
        while (true) {
            switch (nodeType) {
                default: {
                    if (_labelBlock.kind != LabelScopeKind.Expression) {
                        pushLabelBlock(LabelScopeKind.Expression);
                        return true;
                    }
                    return false;
                }

                case Label: {
                    // LabelExpression is a bit special, if it's directly in a
                    // block it becomes associate with the block's scope. Same
                    // thing if it's in a switch case body.
                    if (_labelBlock.kind == LabelScopeKind.Block) {
                        final LabelTarget label = ((LabelExpression)node).getTarget();

                        if (_labelBlock.containsTarget(label)) {
                            return false;
                        }

                        if (_labelBlock.parent.kind == LabelScopeKind.Switch &&
                            _labelBlock.parent.containsTarget(label)) {

                            return false;
                        }
                    }

                    pushLabelBlock(LabelScopeKind.Statement);
                    return true;
                }

                case Block: {
                    if (node instanceof StackSpiller.SpilledExpressionBlock) {
                        // treat it as an expression
                        nodeType = ExpressionType.Extension;
                        continue;
                    }

                    pushLabelBlock(LabelScopeKind.Block);

                    // Labels defined immediately in the block are valid for
                    // the whole block.
                    if (_labelBlock.parent.kind != LabelScopeKind.Switch) {
                        defineBlockLabels(node);
                    }

                    return true;
                }

                case Switch: {
                    pushLabelBlock(LabelScopeKind.Switch);

                    // Define labels inside of the switch cases so they are in
                    // scope for the whole switch. This allows "goto case" and
                    // "goto default" to be considered as local jumps.
                    final SwitchExpression s = (SwitchExpression)node;

                    for (final SwitchCase c : s.getCases()) {
                        defineBlockLabels(c.getBody());
                    }

                    defineBlockLabels(s.getDefaultBody());
                    return true;
                }

                // Remove this when Convert(Void) goes away.
                case Convert: {
                    if (node.getType() != PrimitiveTypes.Void) {
                        // treat it as an expression
                        nodeType = ExpressionType.Extension;
                        continue;
                    }

                    pushLabelBlock(LabelScopeKind.Statement);
                    return true;
                }

                case Conditional:
                case Loop:
                case Goto: {
                    pushLabelBlock(LabelScopeKind.Statement);
                    return true;
                }
            }
        }
    }

    private void popLabelBlock(final LabelScopeKind kind) {
        assert _labelBlock != null && _labelBlock.kind == kind;
        _labelBlock = _labelBlock.parent;
    }

    private void defineBlockLabels(final Expression node) {
        if (!(node instanceof BlockExpression)) {
            return;
        }

        if (node instanceof StackSpiller.SpilledExpressionBlock) {
            return;
        }

        final BlockExpression block = (BlockExpression)node;

        for (int i = 0, n = block.getExpressionCount(); i < n; i++) {
            final Expression e = block.getExpression(i);

            if (e instanceof LabelExpression) {
                defineLabel(((LabelExpression)e).getTarget());
            }
        }
    }

    private LabelInfo ensureLabel(final LabelTarget node) {
        LabelInfo result = _labelInfo.get(node);

        if (result == null) {
            _labelInfo.put(node, result = new LabelInfo(generator, node, false));
        }

        return result;
    }

    private LabelInfo referenceLabel(final LabelTarget node) {
        final LabelInfo result = ensureLabel(node);
        result.reference(_labelBlock);
        return result;
    }

    private LabelInfo defineLabel(final LabelTarget node) {
        if (node == null) {
            return new LabelInfo(generator, null, false);
        }
        final LabelInfo result = ensureLabel(node);
        result.define(_labelBlock);
        return result;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Lambda Expressions">

    private void emitLambdaExpression(final Expression expr) {
        final LambdaExpression node = (LambdaExpression)expr;
        emitDelegateConstruction(node);
    }

    private void emitDelegateConstruction(final LambdaExpression lambda) {
        // 1. Create the new compiler
        final LambdaCompiler compiler;

        // When the lambda does not have a name or the name is empty, generate a unique name for it.
        final String name = StringUtilities.isNullOrEmpty(lambda.getName()) ? getUniqueMethodName() : lambda.getName();

        final MethodBuilder mb = typeBuilder.defineMethod(
            name, Modifier.PRIVATE | Modifier.STATIC,
            lambda.getReturnType(),
            lambda.getParameters().getParameterTypes()
        );

        compiler = new LambdaCompiler(_tree, lambda, mb);

        // 2. Emit the lambda
        compiler.emitLambdaBody(_scope, false, CompilationFlags.EmitAsNoTail);

        // 3. emit the delegate creation in the outer lambda
        emitDelegateConstruction(compiler);
    }

    private static String getUniqueMethodName() {
        return "<ExpressionCompilerImplementationDetails>{" + nextId.getAndIncrement() + "}lambda_method";
    }

    private static String getUniqueLambdaName(final Class<?> creationContext) {
        final Package p = creationContext != null ? creationContext.getPackage()
                                                  : LambdaCompiler.class.getPackage();

        return p.getName() + ".f__Lambda" + Integer.toHexString(nextId.getAndIncrement());
    }


    private void emitLambdaBody() {
        // The lambda body is the "last" expression of the lambda
        final int tailCallFlag = lambda.isTailCall() ? CompilationFlags.EmitAsTail : CompilationFlags.EmitAsNoTail;
        emitLambdaBody(null, false, tailCallFlag);
    }

    private void emitLambdaBody(final CompilerScope parent, final boolean inlined, int flags) {
        _scope.enter(this, parent);

        if (inlined) {
            final ParameterExpressionList parameters = lambda.getParameters();

            //
            // The arguments were already pushed onto the stack.
            // Store them into locals, popping in reverse order.
            //
            for (int i = parameters.size() - 1; i >= 0; i--) {
                _scope.emitSet(parameters.get(i));
            }
        }

        // Need to emit the expression start for the lambda body
        flags = updateEmitExpressionStartFlag(flags, CompilationFlags.EmitExpressionStart);

        if (lambda.getReturnType() == PrimitiveTypes.Void) {
            emitExpressionAsVoid(lambda.getBody(), flags);
        }
        else {
            emitExpression(lambda.getBody(), flags);
        }

        // Return must be the last instruction in a CLI method.
        // But if we're inlining the lambda, we want to leave the return
        // value on the IL stack.
        if (!inlined) {
            generator.emitReturn(lambda.getType());
        }

        _scope.exit();

        // Validate labels
        assert (_labelBlock.parent == null && _labelBlock.kind == LabelScopeKind.Lambda);

        for (final LabelInfo label : _labelInfo.values()) {
            label.validateFinish();
        }
    }

    private void emitDelegateConstruction(final LambdaCompiler inner) {
        // new DelegateType(closure)
        emitClosureCreation(inner);
        generator.emitNew(inner.constructorBuilder);
    }

    private void emitClosureCreation(final LambdaCompiler inner) {
        final boolean closure = inner._scope.needsClosure;
        final boolean boundConstants = inner._boundConstants.count() > 0;

        if (!closure && !boundConstants) {
            generator.emitNull();
            return;
        }

        final Type<Object[]> objectArrayType = Type.of(Object[].class);

        //
        // new Closure(constantPool, currentHoistedLocals)
        //

        if (boundConstants) {
            _boundConstants.emitConstant(this, inner._boundConstants.toArray(), objectArrayType);
        }
        else {
            generator.emitNull();
        }

        if (closure) {
            _scope.emitGet(_scope.getNearestHoistedLocals().selfVariable);
        }
        else {
            generator.emitNull();
        }

        generator.emitNew(Types.Closure.getConstructor(objectArrayType, objectArrayType));
    }

    final void emitConstantArray(final Object array) {
        // Emit as runtime constant if possible
        // if not, emit into IL
        if (typeBuilder != null) {
            // store into field in our type builder, we will initialize
            // the value only once.
            final FieldBuilder fb = createStaticField("ConstantArray", Type.getType(array));
            final Label l = generator.defineLabel();

            generator.getField(fb);
            generator.emit(OpCode.IFNONNULL, l);
            generator.emitConstantArray(array);
            generator.putField(fb);
            generator.markLabel(l);
            generator.getField(fb);
        }
        else {
            generator.emitConstantArray(array);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Loop Expressions">

    private void emitLoopExpression(final Expression expr) {
        final LoopExpression node = (LoopExpression)expr;

        pushLabelBlock(LabelScopeKind.Statement);

        final LabelInfo breakTarget = defineLabel(node.getBreakLabel());
        final LabelInfo continueTarget = defineLabel(node.getContinueLabel());

        continueTarget.markWithEmptyStack();

        emitExpressionAsVoid(node.getBody());

        generator.emitGoto(continueTarget.getLabel());

        popLabelBlock(LabelScopeKind.Statement);

        breakTarget.markWithEmptyStack();
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Member Expressions">

    private void emitMemberExpression(final Expression expr) {
        final MemberExpression node = (MemberExpression)expr;

        // Emit "this", if any.
        if (node.getTarget() != null) {
            emitExpression(node.getTarget());
        }

        emitMemberGet(node.getMember());
    }

    // assumes instance is already on the stack
    private void emitMemberGet(final MemberInfo member) {
        switch (member.getMemberType()) {
            case Field:
                final FieldInfo field = (FieldInfo)member;
                if (field.getFieldType().isPrimitive() && field.isStatic() && field.isFinal()) {
                    try {
                        emitConstant(field.getRawField().get(null), field.getFieldType());
                    }
                    catch (IllegalAccessException e) {
                        generator.getField(field);
                    }
                }
                else {
                    generator.getField(field);
                }
                break;
            default:
                throw ContractUtils.unreachable();
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Method Call Expressions">

    private void emitMethodCallExpression(final Expression expr) {
        emitMethodCallExpression(expr, CompilationFlags.EmitAsNoTail);
    }

    private void emitMethodCallExpression(final Expression expr, final int flags) {
        final MethodCallExpression node = (MethodCallExpression)expr;
        emitMethodCall(node.getTarget(), node.getMethod(), node, flags);
    }

    private void emitMethodCall(final Expression target, final MethodInfo method, final IArgumentProvider methodCallExpr) {
        emitMethodCall(target, method, methodCallExpr, CompilationFlags.EmitAsNoTail);
    }

    private void emitMethodCall(
        final Expression target,
        final MethodInfo method,
        final IArgumentProvider expr,
        final int flags) {

        // Emit instance, if calling an instance method
        Type targetType = null;

        if (!method.isStatic()) {
            targetType = target.getType();
            emitExpression(target);
        }

        emitMethodCall(method, expr, targetType, flags);
    }

    private void emitMethodCall(final Expression target, final MethodInfo method, final MethodCallExpression expr, final int flags) {
        // Emit instance, if calling an instance method
        Type targetType = null;

        if (!method.isStatic()) {
            targetType = target.getType();
            emitExpression(target);
        }

        emitMethodCall(method, expr, targetType, flags);
    }

    private void emitMethodCall(final MethodInfo method, final IArgumentProvider args, final Type objectType, final int flags) {
        // Emit arguments
        emitArguments(method, args);
        generator.call(method);
    }

    private void emitArguments(final MethodBase method, final IArgumentProvider args) {
        emitArguments(method, args, 0);
    }

    private void emitArguments(final MethodBase method, final IArgumentProvider args, final int skipParameters) {
        final ParameterList parameters = method.getParameters();

        assert args.getArgumentCount() + skipParameters == parameters.size();

        for (int i = skipParameters, n = parameters.size(); i < n; i++) {
            final ParameterInfo parameter = parameters.get(i);
            final Expression argument = args.getArgument(i - skipParameters);

            emitExpression(argument);

            if (argument.getType() != parameter.getParameterType()) {
                generator.emitConversion(argument.getType(), parameter.getParameterType());
            }
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="New and New Array Expressions">

    private void emitNewExpression(final Expression expr) {
        final NewExpression node = (NewExpression)expr;
        final ConstructorInfo constructor = node.getConstructor();

        if (constructor != null) {
            emitArguments(constructor, node);
            generator.emitNew(constructor);
        }
        else {
            assert node.getArguments().size() == 0
                : "Node with arguments must have a constructor.";

            assert node.getType().isPrimitive() :
                "Only primitive type may have no constructor set.";

            generator.emitDefaultValue(node.getType());
        }
    }

    private void emitNewArrayExpression(final Expression expr) {
        final NewArrayExpression node = (NewArrayExpression)expr;
        final ExpressionList<? extends Expression> expressions = node.getExpressions();

        if (node.getNodeType() == ExpressionType.NewArrayInit) {
            generator.emitArray(
                node.getType().getElementType(),
                node.getExpressions().size(),
                new CodeGenerator.EmitArrayElementCallback() {
                    @Override
                    public void emit(final int index) {
                        emitExpression(expressions.get(index));
                    }
                }
            );
        }
        else {
            for (int i = 0, n = expressions.size(); i < n; i++) {
                final Expression x = expressions.get(i);
                emitExpression(x);
                generator.emitConversion(x.getType(), PrimitiveTypes.Integer);
            }
            generator.emitNewArray(node.getType());
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Parameter Expressions">

    private void emitParameterExpression(final Expression expr) {
        final ParameterExpression node = (ParameterExpression)expr;
        _scope.emitGet(node);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Runtime Variables">

    private void emitRuntimeVariablesExpression(final Expression expr) {
        final RuntimeVariablesExpression node = (RuntimeVariablesExpression)expr;
        _scope.emitVariableAccess(this, node.getVariables());
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Type Binary Expressions">

    private void emitTypeBinaryExpression(final Expression expr) {
        final TypeBinaryExpression node = (TypeBinaryExpression)expr;

        if (node.getNodeType() == ExpressionType.TypeEqual) {
            emitExpression(node.reduceTypeEqual());
            return;
        }

        final Type type = node.getOperand().getType();

        //
        // Try to determine the result statically
        //
        final AnalyzeTypeIsResult result = ConstantCheck.analyzeInstanceOf(node);

        if (result == AnalyzeTypeIsResult.KnownTrue ||
            result == AnalyzeTypeIsResult.KnownFalse) {
            //
            // Result is known statically, so just emit the expression for
            // its side effects and return the result.
            //
            emitExpressionAsVoid(node.getOperand());
            generator.emitBoolean(result == AnalyzeTypeIsResult.KnownTrue);
            return;
        }

        if (result == AnalyzeTypeIsResult.KnownAssignable) {
            //
            // We know the type can be assigned, but still need to check
            // for null at runtime.
            //
            assert !type.isPrimitive();

            final Label ifNull = generator.defineLabel();
            final Label exit = generator.defineLabel();

            emitExpression(node.getOperand());

            generator.emit(OpCode.IFNULL, ifNull);
            generator.emitBoolean(true);
            generator.emitGoto(exit);

            generator.markLabel(ifNull);
            generator.emitBoolean(false);

            generator.markLabel(exit);

            return;
        }

        assert result == AnalyzeTypeIsResult.Unknown;

        emitExpression(node.getOperand());

        generator.emit(OpCode.INSTANCEOF, type);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Unary Expressions">

    private void emitUnaryExpression(final Expression expr, final int flags) {
        emitUnary((UnaryExpression)expr, flags);
    }

    private void emitUnary(final UnaryExpression node, final int flags) {
        if (node.getMethod() != null) {
            emitUnaryMethod(node, flags);
        }
        else {
            emitExpression(node.getOperand());
            emitUnaryOperator(node.getNodeType(), node.getOperand().getType(), node.getType());
        }
    }

    private void emitUnaryOperator(final ExpressionType op, final Type operandType, final Type resultType) {
        final boolean operandIsBoxed = TypeUtils.isAutoUnboxed(operandType);

        if (op == ExpressionType.ArrayLength) {
            generator.emit(OpCode.ARRAYLENGTH);
            return;
        }

        final Type unboxedType = TypeUtils.getUnderlyingPrimitiveOrSelf(operandType);

        if (operandIsBoxed) {
            generator.emitUnbox(operandType);
        }

        switch (op) {
            case Not:
            case OnesComplement: {
                switch (unboxedType.getKind()) {
                    case BOOLEAN: {
                        final Label ifTrue = generator.defineLabel();
                        final Label exit = generator.defineLabel();

                        generator.emitBoolean(false);
                        generator.emit(OpCode.IF_ICMPNE, ifTrue);
                        generator.emitBoolean(true);
                        generator.emitGoto(exit);

                        generator.markLabel(ifTrue);
                        generator.emitBoolean(false);

                        generator.markLabel(exit);

                        break;
                    }

                    case BYTE:
                    case SHORT:
                    case INT: {
                        generator.emitInteger(-1);
                        generator.emit(OpCode.IXOR);
                        break;
                    }

                    case LONG: {
                        generator.emitLong(-1L);
                        generator.emit(OpCode.LXOR);
                        break;
                    }

                    case CHAR: {
                        generator.emitInteger(-1);
                        generator.emit(OpCode.IXOR);
                        break;
                    }

                    default: {
                        throw Error.unaryOperatorNotDefined(op, unboxedType);
                    }
                }
                break;
            }

            case IsFalse: {
                final Label ifTrue = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emitBoolean(false);
                generator.emit(OpCode.IF_ICMPNE, ifTrue);
                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifTrue);
                generator.emitBoolean(false);

                generator.markLabel(exit);

                // Not an arithmetic operation -> no conversion
                return;
            }

            case IsTrue: {
                final Label ifFalse = generator.defineLabel();
                final Label exit = generator.defineLabel();

                generator.emitBoolean(false);
                generator.emit(OpCode.IF_ICMPEQ, ifFalse);
                generator.emitBoolean(true);
                generator.emitGoto(exit);

                generator.markLabel(ifFalse);
                generator.emitBoolean(false);

                generator.markLabel(exit);

                // Not an arithmetic operation -> no conversion
                return;
            }

            case UnaryPlus: {
                generator.emit(OpCode.NOP);
                break;
            }

            case Negate: {
                switch (unboxedType.getKind()) {
                    case BYTE:
                    case SHORT:
                    case INT: {
                        generator.emit(OpCode.INEG);
                        break;
                    }

                    case LONG: {
                        generator.emit(OpCode.LNEG);
                        break;
                    }

                    case CHAR: {
                        generator.emit(OpCode.INEG);
                        break;
                    }

                    case FLOAT: {
                        generator.emit(OpCode.FNEG);
                        break;
                    }

                    case DOUBLE: {
                        generator.emit(OpCode.DNEG);
                        break;
                    }

                    default: {
                        throw Error.unaryOperatorNotDefined(op, unboxedType);
                    }
                }
                break;
            }

            case Increment: {
                switch (unboxedType.getKind()) {
                    case BYTE:
                    case SHORT:
                    case INT: {
                        generator.emit(OpCode.IINC);
                        break;
                    }

                    case LONG: {
                        generator.emitLong(1L);
                        generator.emit(OpCode.LADD);
                        break;
                    }

                    case CHAR: {
                        generator.emit(OpCode.IINC);
                        break;
                    }

                    case FLOAT: {
                        generator.emitFloat(1f);
                        generator.emit(OpCode.FADD);
                        break;
                    }

                    case DOUBLE: {
                        generator.emitDouble(1d);
                        generator.emit(OpCode.DADD);
                        break;
                    }

                    default: {
                        throw Error.unaryOperatorNotDefined(op, unboxedType);
                    }
                }
                break;
            }

            case Decrement: {
                switch (unboxedType.getKind()) {
                    case BYTE:
                    case SHORT:
                    case INT: {
                        generator.emitInteger(1);
                        generator.emit(OpCode.ISUB);
                        break;
                    }

                    case LONG: {
                        generator.emitLong(1L);
                        generator.emit(OpCode.LSUB);
                        break;
                    }

                    case CHAR: {
                        generator.emitInteger(1);
                        generator.emit(OpCode.ISUB);
                        break;
                    }

                    case FLOAT: {
                        generator.emitFloat(1f);
                        generator.emit(OpCode.FSUB);
                        break;
                    }

                    case DOUBLE: {
                        generator.emitDouble(1d);
                        generator.emit(OpCode.DSUB);
                        break;
                    }

                    default: {
                        throw Error.unaryOperatorNotDefined(op, unboxedType);
                    }
                }
                break;
            }

            default: {
                throw Error.unhandledUnary(op);
            }
        }

        emitConvertArithmeticResult(op, unboxedType, resultType);
    }

    private void emitUnaryMethod(final UnaryExpression node, final int flags) {
        emitMethodCallExpression(
            Expression.call(node.getMethod(), node.getOperand()),
            flags
        );
    }

    private void emitConvertUnaryExpression(final Expression expr, final int flags) {
        emitConvert((UnaryExpression)expr, flags);
    }

    private void emitConvert(final UnaryExpression node, final int flags) {
        if (node.getMethod() != null) {
            emitUnaryMethod(node, flags);
        }
        else if (node.getType() == PrimitiveTypes.Void) {
            emitExpressionAsVoid(node.getOperand(), flags);
        }
        else {
            if (TypeUtils.areEquivalent(node.getOperand().getType(), node.getType())) {
                emitExpression(node.getOperand(), flags);
            }
            else {
                emitExpression(node.getOperand());
                generator.emitConversion(node.getOperand().getType(), node.getType());
            }
        }
    }

    private void emitUnboxUnaryExpression(final Expression expr) {
        final UnaryExpression node = (UnaryExpression)expr;

        assert node.getType().isPrimitive();

        // Unboxing leaves the unboxed value on the stack
        emitExpression(node.getOperand());

        generator.emitUnbox(node.getType());
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Throw Expressions">

    private void emitThrowUnaryExpression(final Expression expr) {
        emitThrow((UnaryExpression)expr, CompilationFlags.EmitAsDefaultType);
    }

    private void emitThrow(final UnaryExpression expr, final int flags) {
        emitExpression(expr.getOperand());
        generator.emit(OpCode.ATHROW);
        emitUnreachable(expr, flags);
    }

    // </editor-fold>
}

enum LabelScopeKind {
    // any "statement like" node that can be jumped into
    Statement,

    // these correspond to the node of the same name 
    Block,
    Switch,
    Lambda,
    Try,

    // these correspond to the part of the try block we're in
    Catch,
    Finally,
    Filter,

    // the catch-all value for any other expression type 
    // (means we can't jump into it) 
    Expression,
}

@SuppressWarnings("PackageVisibleField")
final class LabelScopeInfo {
    // lazily allocated, we typically use this only once every 6th-7th block
    private HashMap<LabelTarget, LabelInfo> labels;

    final LabelScopeKind kind;
    final LabelScopeInfo parent;

    LabelScopeInfo(final LabelScopeInfo parent, final LabelScopeKind kind) {
        this.parent = parent;
        this.kind = kind;
    }

    /**
     * Returns true if we can jump into this node
     */
    boolean canJumpInto() {
        switch (kind) {
            case Block:
            case Statement:
            case Switch:
            case Lambda:
                return true;
        }
        return false;
    }

    boolean containsTarget(final LabelTarget target) {
        return labels != null &&
               labels.containsKey(target);
    }

    LabelInfo tryGetLabelInfo(final LabelTarget target) {
        if (labels == null) {
            return null;
        }

        return labels.get(target);
    }

    void addLabelInfo(final LabelTarget target, final LabelInfo info) {
        assert canJumpInto();

        if (labels == null) {
            labels = new HashMap<>();
        }

        labels.put(target, info);
    }
}
