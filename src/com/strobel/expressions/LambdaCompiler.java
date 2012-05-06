package com.strobel.expressions;

import com.strobel.reflection.emit.MethodBuilder;
import com.strobel.util.ContractUtils;

/**
 * @author Mike Strobel
 */
class LambdaCompiler {
    static <T> Delegate<T> compile(final LambdaExpression<T> lambda) {
        throw ContractUtils.unreachable();
    }

    static void compile(final LambdaExpression<?> lambda, final MethodBuilder methodBuilder) {
        throw ContractUtils.unreachable();
    }
}
