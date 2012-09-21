package com.strobel.expressions;

import com.strobel.reflection.PrimitiveTypes;
import com.strobel.reflection.Type;
import com.strobel.reflection.Types;
import com.strobel.util.TypeUtils;

/**
 * @author Mike Strobel
 */
final class ConstantCheck {

    static boolean isNull(final Expression e) {
        switch (e.getNodeType()) {
            case Constant:
                return ((ConstantExpression)e).getValue() == null;
            case Convert:
                return isNull(((UnaryExpression)e).getOperand());
            case DefaultValue:
                return !e.getType().isPrimitive();
            default:
                return false;
        }
    }
    
    static boolean isStringLiteral(final Expression e) {
        return TypeUtils.getUnderlyingPrimitiveOrSelf(e.getType()) == Types.String &&
               e.getNodeType() == ExpressionType.Constant;
    }
    
    static boolean isTrue(final Expression e) {
        return TypeUtils.getUnderlyingPrimitiveOrSelf(e.getType()) == PrimitiveTypes.Boolean &&
               e.getNodeType() == ExpressionType.Constant &&
               Boolean.TRUE.equals(((ConstantExpression)e).getValue());
    }

    static boolean isFalse(final Expression e) {
        return TypeUtils.getUnderlyingPrimitiveOrSelf(e.getType()) == PrimitiveTypes.Boolean &&
               e.getNodeType() == ExpressionType.Constant &&
               Boolean.FALSE.equals(((ConstantExpression)e).getValue());
    }

    static AnalyzeTypeIsResult analyzeInstanceOf(final TypeBinaryExpression typeIs) {
        return analyzeInstanceOf(typeIs.getOperand(), typeIs.getTypeOperand());
    }

    private static AnalyzeTypeIsResult analyzeInstanceOf(final Expression operand, final Type<?> testType) {
        final Type<?> operandType = operand.getType();

        if (operandType == PrimitiveTypes.Void) {
            return AnalyzeTypeIsResult.KnownFalse;
        }


        if (operandType.isPrimitive()) {
            if (testType == TypeUtils.getBoxedType(operandType)) {
                return AnalyzeTypeIsResult.KnownTrue;
            }
            return AnalyzeTypeIsResult.KnownFalse;
        }

        if (testType.isPrimitive()) {
            if (operandType == TypeUtils.getUnderlyingPrimitive(testType)) {
                return AnalyzeTypeIsResult.KnownAssignable;
            }
            return AnalyzeTypeIsResult.KnownFalse;
        }

        if (testType.isAssignableFrom(operandType)) {
            return AnalyzeTypeIsResult.KnownAssignable;
        }

        return AnalyzeTypeIsResult.Unknown;
    }
}

enum AnalyzeTypeIsResult {
    KnownFalse,
    KnownTrue,
    KnownAssignable, // need null check only 
    Unknown,         // need full runtime check
}
