package com.strobel.expressions;

import com.strobel.reflection.PrimitiveTypes;
import com.strobel.reflection.Type;
import com.strobel.util.TypeUtils;

/**
 * @author Mike Strobel
 */
final class ConstantCheck {

    static boolean isNull(final Expression e) {
        return e.getNodeType() == ExpressionType.Constant &&
               ((ConstantExpression)e).getValue() == null;
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
