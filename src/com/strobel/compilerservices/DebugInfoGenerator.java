package com.strobel.compilerservices;

import com.strobel.expressions.Expression;
import com.strobel.expressions.LambdaExpression;
import com.strobel.reflection.MethodBase;
import com.strobel.reflection.emit.BytecodeGenerator;
import com.strobel.reflection.emit.LocalBuilder;

/**
 * @author strobelm
 */
@SuppressWarnings("UnusedParameters")
public abstract class DebugInfoGenerator {
    private static final DebugInfoGenerator EMPTY = new DebugInfoGenerator() {
        @Override
        public void markSequencePoint(final LambdaExpression<?> method, final int bytecodeOffset, final Expression sequencePoint) {
        }
    };
    
    public static DebugInfoGenerator empty() {
        return EMPTY;
    }
    
    public abstract void markSequencePoint(
        final LambdaExpression<?> method,
        final int bytecodeOffset,
        final Expression sequencePoint);

    public void markSequencePoint(
        final LambdaExpression<?> method,
        final MethodBase methodBase,
        final BytecodeGenerator generator,
        final Expression sequencePoint) {
        
        markSequencePoint(method, generator.offset(), sequencePoint);
    }
    
    public void setLocalName(final LocalBuilder local, final String name) {}
}
