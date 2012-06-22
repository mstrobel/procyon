package com.strobel.expressions;

import com.strobel.reflection.MethodBuilder;
import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.emit.BytecodeGenerator;
import com.strobel.reflection.emit.TypeBuilder;
import com.strobel.util.ContractUtils;
import com.sun.tools.javac.code.Flags;

/**
 * @author Mike Strobel
 */
final class LambdaCompiler {
    final LambdaExpression<?> lambda;
    final TypeBuilder typeBuilder;
    final MethodBuilder methodBuilder;
    final BytecodeGenerator generator;
    
    private final boolean _hasClosureArgument;

    LambdaCompiler(final LambdaExpression<?> lambda) {
        this.lambda = lambda;

        typeBuilder = new TypeBuilder();
        
        final MethodInfo interfaceMethod = lambda.getType().getMethods().get(0);
        
        methodBuilder = typeBuilder.defineMethod(
            interfaceMethod.getName(),
            Flags.asModifierSet(interfaceMethod.getModifiers()),
            interfaceMethod.getReturnType(),
            interfaceMethod.getParameters().getParameterTypes()
        );
        
        generator = new BytecodeGenerator(methodBuilder);

        _hasClosureArgument = true;
    }

    boolean canEmitBoundConstants() {
        return _hasClosureArgument;
    }

    void emitClosureArgument() {
        assert _hasClosureArgument : "must have a Closure argument";
        assert methodBuilder.isStatic() : "must be a static method";

        generator.emitThis();
    }

    static <T> Delegate<T> compile(final LambdaExpression<T> lambda) {
        throw ContractUtils.unreachable();
    }

    static void compile(final LambdaExpression<?> lambda, final MethodBuilder methodBuilder) {
        throw ContractUtils.unreachable();
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
